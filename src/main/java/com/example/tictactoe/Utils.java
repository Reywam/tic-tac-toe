package com.example.tictactoe;

import com.example.tictactoe.game.Coordinates;
import com.example.tictactoe.game.Game;
import com.example.tictactoe.messaging.event.MoveMadeEvent;
import com.example.tictactoe.messaging.request.ConsistencyCheckRequest;
import com.example.tictactoe.messaging.request.MakeMoveRequest;
import com.example.tictactoe.messaging.request.MoveApprovalRequest;
import com.example.tictactoe.messaging.services.MessageSender;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.example.tictactoe.game.GameState.IS_OVER;
import static com.example.tictactoe.game.MoveType.X;


@Slf4j
public class Utils {
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

    public static void continueGame(Game game, MessageSender messageSender, String sender) {
        MoveMadeEvent lastMove = game.getLastMove();
        if (lastMove == null) {
            if (game.getMoveType() == X) {
                makeMove(game, messageSender, sender);
            } else {
                messageSender.send(new MakeMoveRequest(sender));
            }
            return;
        }

        if (!lastMove.getSender().equals(sender)) {
            log.info("Last move is not mine, making move");
            makeMove(game, messageSender, sender);
        } else {
            log.info("Last move is mine, asking opponent to move");
            messageSender.send(new MakeMoveRequest(sender));
        }
    }

    public static void makeMove(Game game, MessageSender messageSender, String sender) {
        if (game.getState() != IS_OVER) {
            Coordinates coordinates = game.getFreeSpaceToMakeMove();
            messageSender.send(new MoveApprovalRequest(sender, game.getMoveType(), coordinates));
        }
    }

    public static void sendConsistencyCheck(MessageSender messageSender, String sender, Game game) {
        messageSender.send(new ConsistencyCheckRequest(sender, game.getState(), game.getMoveType(), game.getMoves()));
    }
}
