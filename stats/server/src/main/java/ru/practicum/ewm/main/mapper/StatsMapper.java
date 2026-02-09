package ru.practicum.ewm.main.mapper;

import ru.practicum.ewm.stats.dto.StatsDto;

import java.util.List;
import java.util.stream.Collectors;

public class StatsMapper {
    public static StatsDto toStatsDto(Object[] row) {
        return StatsDto.builder()
                .app((String) row[0])
                .uri((String) row[1])
                .hits((Long) row[2])
                .build();
    }

    public static List<StatsDto> toStatsDtoList(List<Object[]> rows) {
        return rows.stream()
                .map(StatsMapper::toStatsDto)
                .collect(Collectors.toList());
    }
}
