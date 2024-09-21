package com.example.tictactoe.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

@Slf4j
@Component
public class Game {
    private final GameField FIELD;
    private final MoveType[] ALLOWED_MOVE_TYPES;

    public Game() {
        FIELD = new GameField(3, 3);
        ALLOWED_MOVE_TYPES = new MoveType[]{X, O};
    }

    public MoveType defineMoveType() {
        int moveTypeIdx = new Random().nextInt(ALLOWED_MOVE_TYPES.length);
        MoveType type = ALLOWED_MOVE_TYPES[moveTypeIdx];
        return type;
    }

    public void makeMove(MoveType move, Coordinates coordinates) {
        FIELD.put(move, coordinates);
    }

    public void printField() {
        FIELD.printField();
    }

    public Coordinates getFreeSpaceToMakeMove() {
        return FIELD.findFreeSpace();
    }

    public boolean isOver() {
        return isAnySequenceCreated() || FIELD.getAvailableSpaceToMove().isEmpty();
    }

    public boolean isSpaceFreeToMove(Coordinates coordinates) {
        return FIELD.get(coordinates.x(), coordinates.y()) == null;
    }

    private boolean isAnySequenceCreated() {
        return checkHorizontal() || checkVertical() || checkDiagonal() || checkAntiDiagonal();
    }

    private boolean checkHorizontal() {
        MoveType targetMove = null;
        for (int y = 0; y < FIELD.getHeight(); y++) {
            boolean isSequence = true;
            for (int x = 0; x < FIELD.getWidth(); x++) {
                if (targetMove == null) {
                    targetMove = FIELD.get(x, y);
                }
                MoveType nextCell = FIELD.get(x, y);
                if (nextCell != targetMove || nextCell == null) {
                    isSequence = false;
                    targetMove = null;
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
        MoveType targetMove = null;
        for (int x = 0; x < FIELD.getWidth(); x++) {
            boolean isSequence = true;
            for (int y = 0; y < FIELD.getHeight(); y++) {
                if (targetMove == null) {
                    targetMove = FIELD.get(x, y);
                }
                MoveType nextCell = FIELD.get(x, y);
                if (nextCell != targetMove || nextCell == null) {
                    isSequence = false;
                    targetMove = null;
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
            MoveType nextCell = FIELD.get(x, y);
            if (nextCell != targetMove || nextCell == null) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAntiDiagonal() {
        MoveType targetMove = FIELD.get(FIELD.getWidth() - 1, 0);
        for (int x = FIELD.getWidth() - 1, y = 0;
             x >= 0 && y < FIELD.getHeight();
             x--, y++) {
            MoveType nextCell = FIELD.get(x, y);
            if (nextCell != targetMove || nextCell == null) {
                return false;
            }
        }
        return true;
    }
}
