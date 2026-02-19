package ru.practicum.ewm.main.dto.compilation;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.main.dto.event.EventShortDto;

import java.util.List;

@Builder
@Data
public class CompilationDto {
    private Long id;
    private String title;
    private Boolean pinned;
    private List<EventShortDto> events;
}