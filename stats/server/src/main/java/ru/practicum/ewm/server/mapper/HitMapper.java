package ru.practicum.ewm.server.mapper;

import ru.practicum.ewm.server.model.Hit;
import ru.practicum.ewm.stats.dto.HitDto;


public class HitMapper {

    public static Hit toHit(HitDto dto) {
        return Hit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public static HitDto toHitDto(Hit hit) {
        return HitDto.builder()
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();
    }
}
