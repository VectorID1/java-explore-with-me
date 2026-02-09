package ru.practicum.ewm.main.service;

import ru.practicum.ewm.stats.dto.HitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import ru.practicum.ewm.main.mapper.HitMapper;
import ru.practicum.ewm.main.model.Hit;
import ru.practicum.ewm.main.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;

    @Transactional
    public void saveHit(HitDto hitDto) {
        Hit hit = HitMapper.toHit(hitDto);

        statsRepository.save(hit);

        log.info("Hit сохранен с id: {}", hit.getId());
    }

    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                   List<String> uris, Boolean unique) {

        if (unique != null && unique) {
            return  statsRepository.getStatsWithUniqueIp(start, end, uris);
        } else {
            return  statsRepository.getStats(start, end, uris);
        }
    }

}