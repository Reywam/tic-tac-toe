package com.example.tictactoe;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;
import com.example.tictactoe.messaging.event.MoveMadeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final Game game;

    public static GameState findLatestAppropriateState(GameState lastState, GameState lastOpponentState) {
        return lastState.compareTo(lastOpponentState) < 0 ? lastState : lastOpponentState;
    }

    public static MoveType findAppropriateMoveType(MoveType opponentMoveType) {
        return opponentMoveType == X ? O : X;
    }

    public void acceptState(String opponent
            , GameState opponentState
            , MoveType opponentMoveType
            , List<MoveMadeEvent> opponentMoves) {
        game.restart();
        game.setOpponent(opponent);
        game.setState(findLatestAppropriateState(game.getState(), opponentState));
        game.setMoveType(findAppropriateMoveType(opponentMoveType));
        game.setMoves(opponentMoves);
        game.getMoves().forEach(move -> {
            game.getField().put(move.getMoveType(), move.getCoordinates());
        });
    }
}
