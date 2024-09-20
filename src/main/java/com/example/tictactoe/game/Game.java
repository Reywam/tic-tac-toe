package com.example.tictactoe.game;

import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

public class Game {
    private final GameField FIELD;
    private final MoveType[] ALLOWED_MOVE_TYPES;

    public Game() {
        FIELD = new GameField(3, 3);
        ALLOWED_MOVE_TYPES = new MoveType[]{X, O};
    }

    public void makeMove(MoveType move, Coordinates coordinates) {
        FIELD.put(move, coordinates);
    }

    public boolean isOver() {
        return isAnySequenceCreated();
    }

    public boolean isAnySequenceCreated() {
        if (FIELD.getAvailableSpaceToMove().isEmpty()) {
            return true;
        }
        return checkHorizontal() || checkVertical() || checkDiagonal() || checkAntiDiagonal();
    }

    private boolean checkHorizontal() {
        MoveType targetMove = FIELD.get(0, 0);
        for (int y = 0; y < FIELD.getHeight(); y++) {
            boolean isSequence = true;
            for (int x = 1; x < FIELD.getWidth(); x++) {
                if (FIELD.get(x, y) != targetMove) {
                    isSequence = false;
                    break;
                }
            }
            if (isSequence) {
                return true;
            }
        }
        return false;
    }

    private boolean checkVertical() {
        MoveType targetMove = FIELD.get(0, 0);
        for (int x = 0; x < FIELD.getWidth(); x++) {
            boolean isSequence = true;
            for (int y = 1; y < FIELD.getHeight(); y++) {
                if (FIELD.get(x, y) != targetMove) {
                    isSequence = false;
                    break;
                }
            }
            if (isSequence) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDiagonal() {
        MoveType targetMove = FIELD.get(0, 0);
        for (int x = 1, y = 1; x < FIELD.getWidth() && y < FIELD.getHeight(); x++, y++) {
            if (FIELD.get(x, y) != targetMove) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAntiDiagonal() {
        MoveType targetMove = FIELD.get(FIELD.getWidth() - 1, FIELD.getHeight() - 1);
        for (int x = FIELD.getWidth() - 1, y = FIELD.getHeight() - 1; x >= 0 && y >= 0; x--, y--) {
            if (FIELD.get(x, y) != targetMove) {
                return false;
            }
        }
        return true;
    }
}
