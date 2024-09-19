package com.example.tictactoe.game;


public class GameField {
    private final int WIDTH = 3;
    private final int HEIGHT = 3;
    private final MoveType[][] field = new MoveType[WIDTH][HEIGHT];

    public GameField() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                field[x][y] = MoveType.UNDEFINED;
            }
        }
    }

    public void makeMove(MoveType moveType, int x, int y) {
        field[x][y] = moveType;
    }
}
