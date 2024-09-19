package com.example.tictactoe.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

public class GameLogic {
    private final GameField field;
    private final MoveType[] ALLOWED_MOVE_TYPES;
    private final Random rand;
    private MoveType moveType;

    public GameLogic() {
        rand = new Random();
        field = new GameField();
        ALLOWED_MOVE_TYPES = new MoveType[]{X, O};
    }

    private MoveType defineMoveType() {
        int moveTypeIdx = new Random().nextInt(ALLOWED_MOVE_TYPES.length);
        return ALLOWED_MOVE_TYPES[moveTypeIdx];
    }

    public void play() {
        this.moveType = defineMoveType();
        // notify about the move type
        // wait for response of the opponent move type

        // if you is X then make move, if you is O then wait for the move event from other instance
        // process move events from the other instance
        // send your actions

        // process situations where connection is lost
    }
}
