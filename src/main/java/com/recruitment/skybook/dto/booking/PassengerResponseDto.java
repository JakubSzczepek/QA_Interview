package com.recruitment.skybook.dto.booking;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PassengerResponseDto {
    private int sequenceNumber;
    private PersonalInfoDto personalInfo;
    private ContactDto contact;
    private SeatAssignmentDto seatAssignment;
    private List<BaggageDto> baggage;
}
