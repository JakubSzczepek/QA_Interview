package com.recruitment.skybook.dto.flight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightRequest {

    @NotBlank(message = "Flight number is required")
    @Pattern(regexp = "^[A-Za-z]{2,3}-\\d{1,5}$", message = "Flight number format: 2-3 letters + '-' + 1-5 digits (e.g. SB-1234)")
    private String flightNumber;

    @NotBlank(message = "Airline is required")
    @Size(max = 100, message = "Airline name max 100 characters")
    private String airline;

    private String status;

    @NotEmpty(message = "At least one segment is required (BR-02)")
    @Valid
    private List<SegmentRequestDto> segments;

    @NotNull(message = "Pricing is required")
    @Valid
    private PricingRequestDto pricing;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SegmentRequestDto {
        @Min(value = 1, message = "Segment number must be >= 1")
        private int segmentNumber;

        @NotNull(message = "Departure is required")
        @Valid
        private DepartureArrivalRequestDto departure;

        @NotNull(message = "Arrival is required")
        @Valid
        private DepartureArrivalRequestDto arrival;

        @Valid
        private AircraftRequestDto aircraft;

        @Min(value = 1, message = "Duration must be > 0 (BR-03)")
        private int durationMinutes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DepartureArrivalRequestDto {
        @NotBlank(message = "Airport code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Airport code must be 3 uppercase letters (IATA)")
        private String airportCode;

        private String terminal;

        @NotBlank(message = "DateTime is required (ISO 8601)")
        private String dateTime;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AircraftRequestDto {
        @NotBlank(message = "Aircraft model is required")
        @Size(max = 100, message = "Aircraft model max 100 characters (BR-04)")
        private String model;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PricingRequestDto {
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^(USD|EUR|GBP|PLN)$", message = "Currency must be one of: USD, EUR, GBP, PLN (BR-05)")
        private String currency;

        @NotNull(message = "Base fare is required")
        @DecimalMin(value = "0.00", message = "Base fare must be >= 0 (BR-05)")
        private BigDecimal baseFare;

        // BUG-05: No @PositiveOrZero on TaxDto.amount — intentional
        @Valid
        @Size(max = 50, message = "Maximum 50 taxes")
        private List<TaxDto> taxes;

        // BUG-08: fees are accepted but ignored in totalAmount calculation
        @Valid
        @Size(max = 10, message = "Maximum 10 fees (BR-07)")
        private List<FeeDto> fees;

        // BUG-06: No @Max(100) on DiscountDto.percentage — intentional
        @Valid
        private DiscountDto discount;
    }
}
