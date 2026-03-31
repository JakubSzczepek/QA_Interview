package com.recruitment.skybook.dto.booking;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PassengerDto {
    private PersonalInfoDto personalInfo;
    private ContactDto contact;
    private SeatAssignmentDto seatAssignment;
    private List<BaggageDto> baggage;
}
