package com.example.tictactoe.dto;

import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;

public record GameStateDto(
        MoveType[][] field,
        GameState state,
        String winner) {
}
