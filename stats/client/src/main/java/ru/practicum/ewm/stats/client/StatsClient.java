package ru.practicum.ewm.stats.client;

import org.springframework.http.HttpMethod;
import ru.practicum.ewm.stats.dto.HitDto;
import ru.practicum.ewm.stats.dto.StatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StatsClient extends BaseClient {

    private final String serverUrl;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(RestTemplate rest, String serverUrl) {
        super(rest);
        this.serverUrl = serverUrl;
    }

    public void hit(HitDto hitDto) {
        String path = serverUrl + "/hit";

        ResponseEntity<Object> response = post(path, hitDto);

        if (response.getStatusCode().is2xxSuccessful()) {
            log.debug("Hit успешно отправлен: app={}, uri={}",
                    hitDto.getApp(), hitDto.getUri());
        } else {
            log.warn("Не удалось отправить hit. Status: {}",
                    response.getStatusCode());
        }
    }

    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                   List<String> uris, boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", formatDateTime(start));
        parameters.put("end", formatDateTime(end));
        parameters.put("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
        }

        String path = serverUrl + "/stats?start={start}&end={end}&unique={unique}" +
                (uris != null && !uris.isEmpty() ? "&uris={uris}" : "");

        ResponseEntity<List<StatsDto>> response = rest.exchange(
                path,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                parameters
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.debug("Получена статистика: {} записей", response.getBody().size());
            return response.getBody();
        } else {
            log.warn("Не удалось получить статистику. Status: {}",
                    response.getStatusCode());
            return List.of();
        }
    }

    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end) {
        return getStats(start, end, null, false);
    }

    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                   List<String> uris) {
        return getStats(start, end, uris, false);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}