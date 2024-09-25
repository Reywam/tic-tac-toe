package com.example.tictactoe.messaging;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;
import com.example.tictactoe.messaging.event.MoveMadeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Validator {
    @Value("${spring.application.name}")
    private String myself;
    private final Game game;

    public boolean isNotExpectedState(GameState expected) {
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

    public boolean isStateConsistent(GameState state, MoveType moveType, List<MoveMadeEvent> moves) {
        boolean stateIsConsistent = game.getState() == state;
        boolean moveTypeIsConsistent = game.getMoveType() != moveType;
        boolean movesAreConsistent = game.getMoves().equals(moves);
        return stateIsConsistent && moveTypeIsConsistent && movesAreConsistent;
    }
}
