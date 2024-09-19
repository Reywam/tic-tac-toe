package com.example.tictactoe.game;


import java.util.Random;

public class GameField {
    private final int WIDTH = 3;
    private final int HEIGHT = 3;
    private final Move[][] field = new Move[WIDTH][HEIGHT];

    public GameField() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                field[x][y] = Move.EMPTY;
            }
        }
    }

    public void makeMove(Move move, int x, int y) {
        field[x][y] = move;
    }
}
