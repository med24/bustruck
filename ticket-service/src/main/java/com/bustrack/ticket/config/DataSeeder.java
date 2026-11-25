package com.bustrack.ticket.config;

import com.bustrack.ticket.model.Line;
import com.bustrack.ticket.repository.LineRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Demo lines matching the buses of the tracking-service simulator. */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedLines(LineRepository lines) {
        return args -> {
            if (lines.count() == 0) {
                lines.save(new Line("L10", "Centre-ville - Gare", new BigDecimal("1.50"), 36.8065, 10.1815));
                lines.save(new Line("L20", "Universite - Aeroport", new BigDecimal("2.50"), 36.7950, 10.1700));
            }
        };
    }
}
