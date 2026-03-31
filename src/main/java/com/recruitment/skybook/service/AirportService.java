package com.recruitment.skybook.service;

import com.recruitment.skybook.dto.airport.*;
import com.recruitment.skybook.exception.ResourceNotFoundException;
import com.recruitment.skybook.model.Airport;
import com.recruitment.skybook.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;

    public List<AirportResponse> getAllAirports() {
        return airportRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AirportResponse getAirportByCode(String code) {
        Airport airport = airportRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with code: " + code));
        return toResponse(airport);
    }

    private AirportResponse toResponse(Airport airport) {
        List<TerminalDto> terminalDtos = airport.getTerminals().stream().map(t ->
                TerminalDto.builder()
                        .name(t.getName())
                        .build()
        ).collect(Collectors.toList());

        return AirportResponse.builder()
                .code(airport.getCode())
                .name(airport.getName())
                .city(airport.getCity())
                .country(airport.getCountry())
                .timezone(airport.getTimezone())
                .coordinates(CoordinatesDto.builder()
                        .latitude(airport.getLatitude())
                        .longitude(airport.getLongitude())
                        .build())
                .terminals(terminalDtos)
                .build();
    }
}
