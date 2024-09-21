package com.example.tictactoe.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.example.tictactoe.game.MoveType.X;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Game game;

    @BeforeEach
    void init() {
        game = new Game();
    }

    @Test
    public void gameIsOverWhenHorizontalSequenceCreated() {
        game.makeMove(X, new Coordinates(0, 0));
        game.makeMove(X, new Coordinates(1, 0));
        game.makeMove(X, new Coordinates(2, 0));

        assertTrue(game.isOver());
    }

    @Test
    public void gameIsOverWhenVerticalSequenceCreated() {
        game.makeMove(X, new Coordinates(0, 0));
        game.makeMove(X, new Coordinates(0, 1));
        game.makeMove(X, new Coordinates(0, 2));

        assertTrue(game.isOver());
    }

    @Test
    public void gameIsOverWhenDiagonalSequenceCreated() {
        game.makeMove(X, new Coordinates(0, 0));
        game.makeMove(X, new Coordinates(1, 1));
        game.makeMove(X, new Coordinates(2, 2));

        assertTrue(game.isOver());
    }

    @Test
    public void gameIsOverWhenAntiDiagonalSequenceCreated() {
        game.makeMove(X, new Coordinates(2, 0));
        game.makeMove(X, new Coordinates(1, 1));
        game.makeMove(X, new Coordinates(0, 2));

        assertTrue(game.isOver());
    }

}