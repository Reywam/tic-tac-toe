package com.example.tictactoe.controller;

import com.example.tictactoe.dto.GameStateDto;
import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameField;
import com.example.tictactoe.game.MoveType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("game")
@RequiredArgsConstructor
public class GameController {

    private final Game game;

    @GetMapping
    public ResponseEntity<?> getState() {
        GameField field = game.getField();
        MoveType[][] cells = new MoveType[field.getWidth()][field.getHeight()];
        for (int x = 0; x < field.getWidth(); x++) {
            for (int y = 0; y < field.getHeight(); y++) {
                cells[x][y] = field.get(x, y);
            }
        }

        return ResponseEntity.ok(new GameStateDto(cells, game.getWinner()));
    }
}
