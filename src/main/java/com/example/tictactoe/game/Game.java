package com.example.tictactoe.game;

import com.example.tictactoe.messaging.event.MoveMadeEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

@Slf4j
@Component
@Data
public class Game {
    private GameField FIELD;
    private final MoveType[] ALLOWED_MOVE_TYPES;
    private GameState state;
    private GameState previousState;
    private String opponent;
    private MoveType moveType;
    private List<MoveMadeEvent> moves = new ArrayList<>();

    public Game() {
        FIELD = new GameField(3, 3);
        ALLOWED_MOVE_TYPES = new MoveType[]{X, O};
        state = GameState.SEARCHING_FOR_THE_OPPONENT;
        previousState = GameState.SEARCHING_FOR_THE_OPPONENT;
    }

    public void restart() {
        FIELD = new GameField(3, 3);
        opponent = null;
        state = GameState.SEARCHING_FOR_THE_OPPONENT;
        previousState = GameState.SEARCHING_FOR_THE_OPPONENT;
        moves.clear();
    }

    public MoveMadeEvent getLastMove() {
        if (moves.isEmpty()) {
            return null;
        }
        return moves.get(moves.size() - 1);
    }

    public void setState(GameState state) {
        previousState = this.state;
        this.state = state;
    }

    public void rollbackState() {
        this.state = previousState;
    }

    public MoveType defineMoveType() {
        int moveTypeIdx = new Random().nextInt(ALLOWED_MOVE_TYPES.length);
        MoveType type = ALLOWED_MOVE_TYPES[moveTypeIdx];
        log.info("Defined move type {}", type);
        return type;
    }

    public void makeMove(MoveType move, Coordinates coordinates) {
        FIELD.put(move, coordinates);
    }

    public void makeMove(MoveMadeEvent event) {
        FIELD.put(event.getMoveType(), event.getCoordinates());
        moves.add(event);
    }

    public GameField getField() {
        return FIELD;
    }

    public void printField() {
        FIELD.printField();
    }

    public Coordinates getFreeSpaceToMakeMove() {
        return FIELD.findFreeSpace();
    }

    public boolean isOver() {
        return isAnySequenceCreated(X)
                || isAnySequenceCreated(O)
                || FIELD.getAvailableSpaceToMove().isEmpty();
    }

    public String getWinner() {
        if (isAnySequenceCreated(X)) return X.name();
        if (isAnySequenceCreated(O)) return O.name();
        if (isOver()) {
            return "DRAW";
        }
        return "NOT_DEFINED_YET";
    }

    public boolean spaceIsNotOccupied(Coordinates coordinates) {
        return FIELD.get(coordinates.x(), coordinates.y()) == null;
    }

    private boolean isAnySequenceCreated(MoveType moveType) {
        return checkHorizontal(moveType)
                || checkVertical(moveType)
                || checkDiagonal(moveType)
                || checkAntiDiagonal(moveType);
    }

    private boolean checkHorizontal(MoveType targetMove) {
        for (int y = 0; y < FIELD.getHeight(); y++) {
            boolean isSequence = true;
            for (int x = 0; x < FIELD.getWidth(); x++) {
                MoveType nextCell = FIELD.get(x, y);
                if (nextCell != targetMove || nextCell == null) {
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

    private boolean checkVertical(MoveType targetMove) {
        for (int x = 0; x < FIELD.getWidth(); x++) {
            boolean isSequence = true;
            for (int y = 0; y < FIELD.getHeight(); y++) {
                MoveType nextCell = FIELD.get(x, y);
                if (nextCell != targetMove || nextCell == null) {
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

    private boolean checkDiagonal(MoveType targetMove) {
        for (int x = 0, y = 0; x < FIELD.getWidth() && y < FIELD.getHeight(); x++, y++) {
            MoveType nextCell = FIELD.get(x, y);
            if (nextCell != targetMove || nextCell == null) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAntiDiagonal(MoveType targetMove) {
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
