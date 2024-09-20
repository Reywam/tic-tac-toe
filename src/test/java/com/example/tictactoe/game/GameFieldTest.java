package com.example.tictactoe.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.tictactoe.game.MoveType.X;
import static org.junit.jupiter.api.Assertions.*;

class GameFieldTest {
    GameField field;
    private final int WIDTH = 3;
    private final int HEIGHT = 3;

    @BeforeEach
    void init() {
        field = new GameField(WIDTH, HEIGHT);
    }

    @Test
    public void canMakeMove() {
        List<Coordinates> availableSpace = field.getAvailableSpaceToMove();
        assertEquals(availableSpace.size(), WIDTH * HEIGHT);

        field.put(X, new Coordinates(0, 0));
        assertEquals(field.get(0, 0), X);

        assertEquals(availableSpace.size(), WIDTH * HEIGHT - 1);
    }

}