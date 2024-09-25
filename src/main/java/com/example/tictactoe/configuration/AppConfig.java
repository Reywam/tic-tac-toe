package com.example.tictactoe.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AppConfig {
    @Value("${spring.application.name}")
    private String myself;
    @Value("${application.move.delay}")
    private long delay;
}
