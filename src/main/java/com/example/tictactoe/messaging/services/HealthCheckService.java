package com.example.tictactoe.messaging.services;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.example.tictactoe.messaging.event.AliveEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    @Setter
    @Getter
    private boolean opponentResponded = true;
    private final int MAX_RETRIES = 3;
    private int retryCount = 0;

    private final MessageSender sender;
    private final Game game;
    @Value("${spring.application.name}")
    private String myself;

    public void refresh() {
        opponentResponded = true;
        retryCount = 0;
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void notifyThatServiceIsAlive() {
        sender.send(new AliveEvent(myself));
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void invalidateState() {
        if (game.getOpponent() == null || game.getState() != GameState.IN_PROGRESS) {
            return;
        }
        opponentResponded = false;
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void healthCheck() {
        if (game.getOpponent() == null || game.getState() == GameState.INCONSISTENT) {
            return;
        }

        if (!opponentResponded) {
            if (retryCount == MAX_RETRIES) {
                log.info("Reached retry count for healthcheck. Put game in inconsistent state");
                game.setState(GameState.INCONSISTENT);
            } else {
                retryCount++;
            }
        }
    }
}
