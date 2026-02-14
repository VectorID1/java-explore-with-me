package ru.practicum.ewm.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import ru.practicum.ewm.stats.client.config.StatsClientConfig;

@SpringBootApplication
@Import(StatsClientConfig.class)
@ComponentScan(basePackages = {"ru.practicum.ewm.main", "ru.practicum.ewm.stats"})
public class AppMainServer {
    public static void main(String[] args) {
        SpringApplication.run(AppMainServer.class, args);
    }
}
