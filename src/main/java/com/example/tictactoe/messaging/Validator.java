package com.example.tictactoe.messaging;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Validator {
    @Value("${spring.application.name}")
    private String myself;
    private final Game game;

    public boolean isIncorrectState(GameState expected) {
        if (game.getState() != expected) {
            log.error("Expected game state is {}, actual {}", expected, game.getState());
            return true;
        }

        return false;
    }

    public boolean isMessageFromMyself(String sender) {
        return sender.equals(myself);
    }

    public boolean gameIsNotReadyYet() {
        if (game.getState().compareTo(GameState.IN_PROGRESS) < 0) {
            log.error("Game is not ready to accept play events yet");
            return true;
        }
        return false;
    }
}
