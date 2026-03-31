package com.recruitment.skybook.service;

import com.recruitment.skybook.dto.booking.*;
import com.recruitment.skybook.exception.InvalidStatusTransitionException;
import com.recruitment.skybook.exception.ResourceNotFoundException;
import com.recruitment.skybook.model.*;
import com.recruitment.skybook.repository.BookingRepository;
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
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "BLIK");
    private static final Set<String> VALID_CURRENCIES = Set.of("USD", "EUR", "GBP", "PLN");
    private static final Set<String> CARD_METHODS = Set.of("CREDIT_CARD", "DEBIT_CARD");

    public Page<BookingResponse> getAllBookings(int page, int size) {
        if (size > 100) size = 100;
        if (size < 1) size = 10;
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findAll(pageable).map(this::toResponse);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // BUG-11 & BUG-12: No null-safety checks on payment.
        // If body is {} or payment is null → request.getPayment().getMethod() throws NPE → 500.
        // Correct impl would check if payment is null BEFORE accessing its methods.

        // Validate payment FIRST (BUG-12: NPE if payment is null, BUG-11: NPE if body is {})
        String paymentMethod = request.getPayment().getMethod();
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod
                    + ". Allowed: " + VALID_PAYMENT_METHODS);
        }
        if (CARD_METHODS.contains(paymentMethod)) {
            if (request.getPayment().getCardDetails() == null) {
                throw new IllegalArgumentException("Card details required for " + paymentMethod + " payment (BR-14)");
            }
        }
        if (!VALID_CURRENCIES.contains(request.getPayment().getCurrency())) {
            throw new IllegalArgumentException("Invalid payment currency: " + request.getPayment().getCurrency());
        }

        // Validate flight exists
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + request.getFlightId()));

        // BUG-04: Does NOT validate passengers.size() >= 1
        // The empty list [] is accepted — booking is saved without passengers
        // Correct: if (request.getPassengers() == null || request.getPassengers().isEmpty())
        //              throw new IllegalArgumentException("At least 1 passenger required (BR-11)");

        // Validate max passengers
        if (request.getPassengers() != null && request.getPassengers().size() > 9) {
            throw new IllegalArgumentException("Maximum 9 passengers per booking (BR-11)");
        }

        // BUG-01: Does NOT validate seat uniqueness across passengers or bookings
        // Two passengers can have the same seatNumber on the same flight.
        // Correct implementation would check:
        // 1. No duplicate seatNumbers within this booking's passengers
        // 2. No seatNumber already assigned in other bookings for the same flight

        // Create booking entity
        String reference = "SB-" + generateReference();
        Booking booking = Booking.builder()
                .bookingReference(reference)
                .flightId(request.getFlightId())
                .status("PENDING")
                .build();

        // Map payment
        booking.setPaymentMethod(request.getPayment().getMethod());
        booking.setPaymentStatus("COMPLETED");
        booking.setPaymentCurrency(request.getPayment().getCurrency());
        if (request.getPayment().getCardDetails() != null) {
            booking.setCardLastFour(request.getPayment().getCardDetails().getLastFourDigits());
            booking.setCardBrand(request.getPayment().getCardDetails().getBrand());
            booking.setCardHolderName(request.getPayment().getCardDetails().getHolderName());
        }

        // Map passengers
        int seq = 1;
        int totalPassengers = 0;
        if (request.getPassengers() != null) {
            for (PassengerDto pDto : request.getPassengers()) {
                Passenger passenger = mapPassengerDto(pDto, booking, seq++);
                booking.getPassengers().add(passenger);
                totalPassengers++;
            }
        }

        // Calculate pricing summary based on flight pricing
        BigDecimal baseFare = flight.getPricingBaseFare() != null ? flight.getPricingBaseFare() : BigDecimal.ZERO;
        BigDecimal totalTaxes = flight.getTaxes().stream()
                .map(Tax::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = flight.getFees().stream()
                .map(Fee::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (flight.getDiscountPercentage() != null && flight.getDiscountPercentage() > 0) {
            discountAmount = baseFare.add(totalTaxes).add(totalFees)
                    .multiply(BigDecimal.valueOf(flight.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal grandTotal = baseFare.add(totalTaxes).add(totalFees).subtract(discountAmount);

        booking.setPricingSummaryCurrency(flight.getPricingCurrency());
        booking.setPricingSummaryBaseFare(baseFare);
        booking.setPricingSummaryTotalTaxes(totalTaxes);
        booking.setPricingSummaryTotalFees(totalFees);
        booking.setPricingSummaryDiscountAmount(discountAmount);
        booking.setPricingSummaryGrandTotal(grandTotal.setScale(2, RoundingMode.HALF_UP));
        booking.setPaymentAmount(grandTotal.setScale(2, RoundingMode.HALF_UP));

        // Update available seats on flight (decrease)
        if (totalPassengers > 0) {
            flight.setAvailableSeatsEconomy(
                    Math.max(0, flight.getAvailableSeatsEconomy() - totalPassengers));
            flightRepository.save(flight);
        }

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, BookingStatusRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        String currentStatus = booking.getStatus();
        String newStatus = request.getStatus();

        // BR-15: Valid transitions
        validateStatusTransition(currentStatus, newStatus);

        // BUG-03: When cancelling, does NOT restore availableSeats
        // Correct: if CANCELLED, add back passengers count to flight.availableSeats
        // Bug: just changes status, seats are lost forever

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    // --- Private helpers ---

    private void validatePayment(PaymentDto payment) {
        if (payment.getMethod() != null && !VALID_PAYMENT_METHODS.contains(payment.getMethod())) {
            throw new IllegalArgumentException("Invalid payment method. Allowed: " + VALID_PAYMENT_METHODS);
        }
        if (payment.getCurrency() != null && !VALID_CURRENCIES.contains(payment.getCurrency())) {
            throw new IllegalArgumentException("Invalid currency. Allowed: " + VALID_CURRENCIES);
        }
        if (CARD_METHODS.contains(payment.getMethod()) && payment.getCardDetails() == null) {
            throw new IllegalArgumentException("Card details required for card payment methods (BR-14)");
        }
    }

    private void validateStatusTransition(String current, String target) {
        Map<String, Set<String>> transitionMap = new HashMap<>();
        transitionMap.put("PENDING", Set.of("CONFIRMED", "CANCELLED"));
        transitionMap.put("CONFIRMED", Set.of("CANCELLED"));
        transitionMap.put("CANCELLED", Set.of());
        Set<String> validTransitions = transitionMap.getOrDefault(current, Set.of());

        if (!validTransitions.contains(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + current + " to " + target +
                            ". Allowed transitions from " + current + ": " + validTransitions);
        }
    }

    private Passenger mapPassengerDto(PassengerDto dto, Booking booking, int seq) {
        Passenger.PassengerBuilder pb = Passenger.builder()
                .sequenceNumber(seq)
                .booking(booking);

        if (dto.getPersonalInfo() != null) {
            pb.firstName(dto.getPersonalInfo().getFirstName());
            pb.lastName(dto.getPersonalInfo().getLastName());
            pb.dateOfBirth(dto.getPersonalInfo().getDateOfBirth());
            pb.nationality(dto.getPersonalInfo().getNationality());
        }
        if (dto.getContact() != null) {
            pb.email(dto.getContact().getEmail());
            pb.phone(dto.getContact().getPhone());
        }
        if (dto.getSeatAssignment() != null) {
            pb.seatNumber(dto.getSeatAssignment().getSeatNumber());
            pb.seatClass(dto.getSeatAssignment().getSeatClass());
            pb.seatType(dto.getSeatAssignment().getType());
        }

        Passenger passenger = pb.build();

        if (dto.getBaggage() != null) {
            for (BaggageDto bDto : dto.getBaggage()) {
                BaggageItem item = BaggageItem.builder()
                        .type(bDto.getType())
                        .weightKg(bDto.getWeightKg())
                        .count(bDto.getCount())
                        .passenger(passenger)
                        .build();
                passenger.getBaggageItems().add(item);
            }
        }

        return passenger;
    }

    private String generateReference() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rng = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // --- Entity → DTO mapping ---

    private BookingResponse toResponse(Booking booking) {
        List<PassengerResponseDto> passengerDtos = booking.getPassengers().stream().map(p -> {
            SeatAssignmentDto seat = null;
            if (p.getSeatNumber() != null) {
                seat = SeatAssignmentDto.builder()
                        .seatNumber(p.getSeatNumber())
                        .seatClass(p.getSeatClass())
                        .type(p.getSeatType())
                        .build();
            }

            List<BaggageDto> baggageDtos = p.getBaggageItems().stream().map(b -> BaggageDto.builder()
                    .type(b.getType())
                    .weightKg(b.getWeightKg())
                    .count(b.getCount())
                    .build()
            ).collect(Collectors.toList());

            return PassengerResponseDto.builder()
                    .sequenceNumber(p.getSequenceNumber())
                    .personalInfo(PersonalInfoDto.builder()
                            .firstName(p.getFirstName())
                            .lastName(p.getLastName())
                            .dateOfBirth(p.getDateOfBirth())
                            .nationality(p.getNationality())
                            .build())
                    .contact(ContactDto.builder()
                            .email(p.getEmail())
                            .phone(p.getPhone())
                            .build())
                    .seatAssignment(seat)
                    .baggage(baggageDtos)
                    .build();
        }).collect(Collectors.toList());

        CardDetailsDto cardDetails = null;
        if (booking.getCardLastFour() != null) {
            cardDetails = CardDetailsDto.builder()
                    .lastFourDigits(booking.getCardLastFour())
                    .brand(booking.getCardBrand())
                    .holderName(booking.getCardHolderName())
                    .build();
        }

        PaymentResponseDto paymentDto = PaymentResponseDto.builder()
                .method(booking.getPaymentMethod())
                .status(booking.getPaymentStatus())
                .amount(booking.getPaymentAmount())
                .currency(booking.getPaymentCurrency())
                .cardDetails(cardDetails)
                .build();

        PricingSummaryDto pricingSummaryDto = PricingSummaryDto.builder()
                .currency(booking.getPricingSummaryCurrency())
                .baseFare(booking.getPricingSummaryBaseFare())
                .totalTaxes(booking.getPricingSummaryTotalTaxes())
                .totalFees(booking.getPricingSummaryTotalFees())
                .discountAmount(booking.getPricingSummaryDiscountAmount())
                .grandTotal(booking.getPricingSummaryGrandTotal())
                .build();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .flightId(booking.getFlightId())
                .status(booking.getStatus())
                .passengers(passengerDtos)
                .payment(paymentDto)
                .pricingSummary(pricingSummaryDto)
                .createdAt(booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : null)
                .updatedAt(booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : null)
                .build();
    }
}
