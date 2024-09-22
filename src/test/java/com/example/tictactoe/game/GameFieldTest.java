package com.example.tictactoe.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.tictactoe.game.MoveType.O;
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
    public void canRecordMoveMade() {
        List<Coordinates> availableSpace = field.getAvailableSpaceToMove();
        assertEquals(WIDTH * HEIGHT, availableSpace.size());

        field.put(X, new Coordinates(0, 0));
        assertEquals(X, field.get(0, 0));

        assertEquals(WIDTH * HEIGHT - 1, availableSpace.size());
    }

    @Test
    public void moveToOccupiedCoordinatedIsNotAllowed() {
        field.put(X, new Coordinates(0, 0));
        assertThrows(RuntimeException.class, () -> field.put(O, new Coordinates(0, 0)));
    }

}