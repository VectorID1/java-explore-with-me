import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestCleanupController {

    private final JdbcTemplate jdbcTemplate;

    @DeleteMapping("/cleanup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE users, categories, events, requests, compilations, compilation_events RESTART IDENTITY CASCADE");
    }

    //Пока сюда, на 3м задании, сделаю тесты для фичи и сделаю этот эндпоинт встроеным после тестов что бы чистил БД.
    //Этот эндпоит вообще создавался для прогона тестов на Postman я смотрел реальные сохранения в БД после чего чистил =)
}