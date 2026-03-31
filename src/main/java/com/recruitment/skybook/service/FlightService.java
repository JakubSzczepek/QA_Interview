package com.recruitment.skybook.service;

import com.recruitment.skybook.dto.flight.*;
import com.recruitment.skybook.exception.ResourceNotFoundException;
import com.recruitment.skybook.model.*;
import com.recruitment.skybook.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    public Page<FlightResponse> getAllFlights(int page, int size) {
        // BR-18: Paginacja — domyślnie page=0, size=10, max size=100
        if (size > 100) size = 100;
        if (size < 1) size = 10;
        Pageable pageable = PageRequest.of(page, size);
        return flightRepository.findAll(pageable).map(this::toResponse);
    }

    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + id));
        return toResponse(flight);
    }

    @Transactional
    public FlightResponse createFlight(FlightRequest request) {
        validateFlightRequest(request);

        // BR-01: Flight number must be unique
        if (flightRepository.existsByFlightNumber(request.getFlightNumber())) {
            throw new IllegalArgumentException("Flight number already exists: " + request.getFlightNumber());
        }

        Flight flight = mapRequestToEntity(request);

        // BUG-08: totalAmount does NOT include fees[] in calculation
        // Correct: baseFare + Σtaxes + Σfees - discount
        // Bug: baseFare + Σtaxes - discount (fees ignored)
        flight.setPricingTotalAmount(calculateTotalAmount(flight));

        // Set available seats from first segment's configuration
        if (flight.getSegments() != null && !flight.getSegments().isEmpty()) {
            Segment firstSeg = flight.getSegments().get(0);
            flight.setAvailableSeatsEconomy(firstSeg.getSeatConfigEconomy());
            flight.setAvailableSeatsBusiness(firstSeg.getSeatConfigBusiness());
            flight.setAvailableSeatsFirst(firstSeg.getSeatConfigFirst());
        }

        Flight saved = flightRepository.save(flight);
        return toResponse(saved);
    }

    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        Flight existing = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + id));

        validateFlightRequest(request);

        // Check uniqueness if flightNumber changed
        if (!existing.getFlightNumber().equals(request.getFlightNumber())
                && flightRepository.existsByFlightNumber(request.getFlightNumber())) {
            throw new IllegalArgumentException("Flight number already exists: " + request.getFlightNumber());
        }

        // Clear old collections
        existing.getSegments().clear();
        existing.getTaxes().clear();
        existing.getFees().clear();

        // Map new data
        existing.setFlightNumber(request.getFlightNumber());
        existing.setAirline(request.getAirline());
        existing.setStatus(request.getStatus());

        if (request.getPricing() != null) {
            existing.setPricingCurrency(request.getPricing().getCurrency());
            existing.setPricingBaseFare(request.getPricing().getBaseFare());

            if (request.getPricing().getDiscount() != null) {
                existing.setDiscountCode(request.getPricing().getDiscount().getCode());
                existing.setDiscountPercentage(request.getPricing().getDiscount().getPercentage());
                existing.setDiscountValidUntil(request.getPricing().getDiscount().getValidUntil());
            } else {
                existing.setDiscountCode(null);
                existing.setDiscountPercentage(null);
                existing.setDiscountValidUntil(null);
            }

            // BUG-05: Does NOT validate that tax amounts are >= 0
            if (request.getPricing().getTaxes() != null) {
                for (TaxDto t : request.getPricing().getTaxes()) {
                    Tax tax = Tax.builder()
                            .code(t.getCode())
                            .name(t.getName())
                            .amount(t.getAmount())
                            .flight(existing)
                            .build();
                    existing.getTaxes().add(tax);
                }
            }

            // BUG-06: Does NOT validate discount percentage range 0-100
            if (request.getPricing().getFees() != null) {
                for (FeeDto f : request.getPricing().getFees()) {
                    Fee fee = Fee.builder()
                            .feeCode(f.getFeeCode())
                            .description(f.getDescription())
                            .amount(f.getAmount())
                            .optional(f.isOptional())
                            .flight(existing)
                            .build();
                    existing.getFees().add(fee);
                }
            }
        }

        if (request.getSegments() != null) {
            for (FlightRequest.SegmentRequestDto segDto : request.getSegments()) {
                Segment seg = mapSegmentRequest(segDto, existing);
                existing.getSegments().add(seg);
            }
        }

        // BUG-08: totalAmount does NOT include fees
        existing.setPricingTotalAmount(calculateTotalAmount(existing));

        Flight saved = flightRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + id));

        // BUG-02: Does NOT check if flight has associated bookings
        // Correct implementation would check bookingRepository.existsByFlightId(id)
        // and throw an exception if bookings exist (violates BR-16)
        flightRepository.delete(flight);
    }

    // BUG-10: Case-sensitive search (uses LIKE without LOWER)
    public Page<FlightResponse> searchFlights(String origin, String destination, String date, int page, int size) {
        if (size > 100) size = 100;
        if (size < 1) size = 10;
        Pageable pageable = PageRequest.of(page, size);
        return flightRepository.searchFlights(origin, destination, date, pageable).map(this::toResponse);
    }

    // --- Private helpers ---

    private void validateFlightRequest(FlightRequest request) {
        if (request.getFlightNumber() == null || request.getFlightNumber().isBlank()) {
            throw new IllegalArgumentException("Flight number is required");
        }
        // BR-01: format: 2-3 letters + '-' + 1-5 digits
        if (!request.getFlightNumber().matches("^[A-Za-z]{2,3}-\\d{1,5}$")) {
            throw new IllegalArgumentException("Flight number must match format: 2-3 letters + '-' + 1-5 digits (e.g., SB-1234)");
        }

        if (request.getSegments() == null || request.getSegments().isEmpty()) {
            throw new IllegalArgumentException("At least one segment is required (BR-02)");
        }

        // BR-05: baseFare >= 0
        if (request.getPricing() != null && request.getPricing().getBaseFare() != null) {
            if (request.getPricing().getBaseFare().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Base fare must be >= 0 (BR-05)");
            }
        }

        // BR-07: max 10 fees
        if (request.getPricing() != null && request.getPricing().getFees() != null
                && request.getPricing().getFees().size() > 10) {
            throw new IllegalArgumentException("Maximum 10 fees allowed (BR-07)");
        }

        // BUG-05: MISSING validation that tax.amount >= 0 (BR-06)
        // BUG-06: MISSING validation that discount.percentage is 0-100 (BR-09)
    }

    /**
     * BUG-08: totalAmount calculation ignores fees[].
     * Correct formula: baseFare + Σtaxes + Σfees - discount
     * Bug formula:     baseFare + Σtaxes          - discount
     */
    private BigDecimal calculateTotalAmount(Flight flight) {
        BigDecimal baseFare = flight.getPricingBaseFare() != null ? flight.getPricingBaseFare() : BigDecimal.ZERO;
        BigDecimal totalTaxes = flight.getTaxes().stream()
                .map(Tax::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // BUG-08: fees are intentionally NOT included
        // BigDecimal totalFees = flight.getFees().stream()
        //         .map(Fee::getAmount)
        //         .filter(Objects::nonNull)
        //         .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subtotal = baseFare.add(totalTaxes);  // should also add totalFees

        // Apply discount
        if (flight.getDiscountPercentage() != null && flight.getDiscountPercentage() > 0) {
            BigDecimal discountAmount = subtotal.multiply(BigDecimal.valueOf(flight.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            subtotal = subtotal.subtract(discountAmount);
        }

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private Flight mapRequestToEntity(FlightRequest request) {
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .airline(request.getAirline())
                .status(request.getStatus() != null ? request.getStatus() : "SCHEDULED")
                .build();

        if (request.getPricing() != null) {
            flight.setPricingCurrency(request.getPricing().getCurrency());
            flight.setPricingBaseFare(request.getPricing().getBaseFare());

            if (request.getPricing().getDiscount() != null) {
                flight.setDiscountCode(request.getPricing().getDiscount().getCode());
                flight.setDiscountPercentage(request.getPricing().getDiscount().getPercentage());
                flight.setDiscountValidUntil(request.getPricing().getDiscount().getValidUntil());
            }

            // BUG-05: Does NOT validate tax amounts >= 0
            if (request.getPricing().getTaxes() != null) {
                for (TaxDto t : request.getPricing().getTaxes()) {
                    Tax tax = Tax.builder()
                            .code(t.getCode())
                            .name(t.getName())
                            .amount(t.getAmount())
                            .flight(flight)
                            .build();
                    flight.getTaxes().add(tax);
                }
            }

            if (request.getPricing().getFees() != null) {
                for (FeeDto f : request.getPricing().getFees()) {
                    Fee fee = Fee.builder()
                            .feeCode(f.getFeeCode())
                            .description(f.getDescription())
                            .amount(f.getAmount())
                            .optional(f.isOptional())
                            .flight(flight)
                            .build();
                    flight.getFees().add(fee);
                }
            }
        }

        if (request.getSegments() != null) {
            for (FlightRequest.SegmentRequestDto segDto : request.getSegments()) {
                Segment seg = mapSegmentRequest(segDto, flight);
                flight.getSegments().add(seg);
            }
        }

        return flight;
    }

    private Segment mapSegmentRequest(FlightRequest.SegmentRequestDto segDto, Flight flight) {
        Segment.SegmentBuilder sb = Segment.builder()
                .segmentNumber(segDto.getSegmentNumber())
                .durationMinutes(segDto.getDurationMinutes())
                .flight(flight);

        if (segDto.getDeparture() != null) {
            sb.departureAirportCode(segDto.getDeparture().getAirportCode());
            sb.departureTerminal(segDto.getDeparture().getTerminal());
            sb.departureDateTime(segDto.getDeparture().getDateTime());
        }
        if (segDto.getArrival() != null) {
            sb.arrivalAirportCode(segDto.getArrival().getAirportCode());
            sb.arrivalTerminal(segDto.getArrival().getTerminal());
            sb.arrivalDateTime(segDto.getArrival().getDateTime());
        }
        if (segDto.getAircraft() != null) {
            sb.aircraftModel(segDto.getAircraft().getModel());
        }

        return sb.build();
    }

    // --- Entity → DTO mapping ---

    public FlightResponse toResponse(Flight flight) {
        List<SegmentDto> segmentDtos = flight.getSegments().stream().map(seg -> SegmentDto.builder()
                .segmentNumber(seg.getSegmentNumber())
                .departure(DepartureArrivalDto.builder()
                        .airportCode(seg.getDepartureAirportCode())
                        .airportName(seg.getDepartureAirportName())
                        .terminal(seg.getDepartureTerminal())
                        .dateTime(seg.getDepartureDateTime())
                        .build())
                .arrival(DepartureArrivalDto.builder()
                        .airportCode(seg.getArrivalAirportCode())
                        .airportName(seg.getArrivalAirportName())
                        .terminal(seg.getArrivalTerminal())
                        .dateTime(seg.getArrivalDateTime())
                        .build())
                .aircraft(AircraftDto.builder()
                        .model(seg.getAircraftModel())
                        .build())
                .durationMinutes(seg.getDurationMinutes())
                .build()
        ).collect(Collectors.toList());

        List<TaxDto> taxDtos = flight.getTaxes().stream().map(t -> TaxDto.builder()
                .code(t.getCode())
                .name(t.getName())
                .amount(t.getAmount())
                .build()
        ).collect(Collectors.toList());

        List<FeeDto> feeDtos = flight.getFees().stream().map(f -> FeeDto.builder()
                .feeCode(f.getFeeCode())
                .description(f.getDescription())
                .amount(f.getAmount())
                .optional(f.isOptional())
                .build()
        ).collect(Collectors.toList());

        DiscountDto discountDto = null;
        if (flight.getDiscountCode() != null) {
            discountDto = DiscountDto.builder()
                    .code(flight.getDiscountCode())
                    .percentage(flight.getDiscountPercentage())
                    .validUntil(flight.getDiscountValidUntil())
                    .build();
        }

        PricingDto pricingDto = PricingDto.builder()
                .currency(flight.getPricingCurrency())
                .baseFare(flight.getPricingBaseFare())
                .taxes(taxDtos)
                .fees(feeDtos)
                .totalAmount(flight.getPricingTotalAmount())
                .discount(discountDto)
                .build();

        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airline(flight.getAirline())
                .status(flight.getStatus())
                .segments(segmentDtos)
                .pricing(pricingDto)
                .availableSeats(SeatConfigurationDto.builder()
                        .economy(flight.getAvailableSeatsEconomy())
                        .business(flight.getAvailableSeatsBusiness())
                        .first(flight.getAvailableSeatsFirst())
                        .build())
                .createdAt(flight.getCreatedAt() != null ? flight.getCreatedAt().toString() : null)
                .updatedAt(flight.getUpdatedAt() != null ? flight.getUpdatedAt().toString() : null)
                .build();
    }
}
