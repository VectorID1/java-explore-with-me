package ru.practicum.ewm.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.compilation.CompilationDto;
import ru.practicum.ewm.main.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.main.dto.compilation.UpdateCompilationDto;
import ru.practicum.ewm.main.exception.NotFoundException;
import ru.practicum.ewm.main.mapper.CompilationMapper;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.repository.CompilationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventService.getEventsByIds(newCompilationDto.getEvents());
            compilation.setEvents(events);
        }
        Compilation saved = compilationRepository.save(compilation);

        log.info("Создана подборка id={}, title={}, pinned={}, events={}",
                saved.getId(), saved.getTitle(), saved.getPinned(),
                saved.getEvents() != null ? saved.getEvents().size() : 0);

        return CompilationMapper.toCompilationDto(saved);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAllBy(pageable);
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        }
        log.info("Получено {} подборок (pinned={}, from={}, size={})",
                compilations.size(), pinned, from, size);

        return CompilationMapper.toCompilationDtoList(compilations);
    }

    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = getCompilationById(compId);

        log.info("Получена подборка id={}, title={}", compilation.getId(), compilation.getTitle());

        return CompilationMapper.toCompilationDto(compilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationById(compId);

        compilationRepository.delete(compilation);

        log.info("Удалена подборка id={}, title={}", compId, compilation.getTitle());
    }

    private Compilation getCompilationById(Long compId) {

        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException("Подборка с id " + compId + " не найдена."));
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationDto dto) {
        Compilation compilation = getCompilationById(compId);

        CompilationMapper.updateCompilation(compilation, dto);

        if (dto.getEvents() != null) {
            List<Event> events = eventService.getEventsByIds(dto.getEvents());
            compilation.setEvents(events);
        }

        Compilation updated = compilationRepository.save(compilation);

        log.info("Обновлена подборка id={}", compId);
        return CompilationMapper.toCompilationDto(updated);
    }
}
