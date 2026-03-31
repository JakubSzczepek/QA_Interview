package com.recruitment.skybook.dto.flight;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String airline;
    private String status;
    private List<SegmentDto> segments;
    private PricingDto pricing;
    private SeatConfigurationDto availableSeats;
    private String createdAt;
    private String updatedAt;
}
