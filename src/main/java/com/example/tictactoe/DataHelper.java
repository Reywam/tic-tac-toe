package com.example.tictactoe;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.messaging.event.MoveMadeEvent;

import java.util.ArrayList;
import java.util.List;

public class DataHelper {
    public static List<MoveMadeEvent> defineValidMoves(List<MoveMadeEvent> moves, List<MoveMadeEvent> opponentMoves) {
        int minIdxAllowed = Math.min(moves.size(), opponentMoves.size());
        List<MoveMadeEvent> validMoves = new ArrayList<>();
        for (int i = 0; i < minIdxAllowed; i++) {
            MoveMadeEvent myMove = moves.get(i);
            MoveMadeEvent opponentMove = opponentMoves.get(i);
            if (myMove.equals(opponentMove)) {
                validMoves.add(myMove);
            } else {
                break;
            }
        }
        return validMoves;
    }
}
