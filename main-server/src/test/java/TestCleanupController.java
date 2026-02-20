import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Profile("test")
public class TestCleanupController {

    private final JdbcTemplate jdbcTemplate;

    @DeleteMapping("/cleanup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE comments," +
                " users, " +
                "categories, " +
                "events, requests," +
                " compilations," +
                " compilation_events " +
                "RESTART IDENTITY CASCADE");
    }
}