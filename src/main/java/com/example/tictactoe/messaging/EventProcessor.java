package com.example.tictactoe.messaging;

import com.example.tictactoe.RecoveryService;
import com.example.tictactoe.game.Coordinates;
import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;
import com.example.tictactoe.messaging.event.*;
import com.example.tictactoe.messaging.request.*;
import com.example.tictactoe.messaging.request.RecoveryRequest;
import com.example.tictactoe.messaging.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.example.tictactoe.RecoveryService.findAppropriateMoveType;
import static com.example.tictactoe.game.GameState.OPPONENT_FOUND;
import static com.example.tictactoe.game.GameState.SEARCHING_FOR_THE_OPPONENT;
import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;

@Slf4j
@Service
@RabbitListener(queues = "#{queue.name}")
@RequiredArgsConstructor
public class EventProcessor {
    private final Game game;

    @Value("${spring.application.name}")
    private String myself;

    private final MessageSender sender;
    private final Validator validator;
    private final RecoveryService recoveryService;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("{} is started and ready to play. Sending play request", myself);
        sender.send(new PlayRequest(myself));
        game.setState(SEARCHING_FOR_THE_OPPONENT);
    }

    private void requestForTheOpponentState() {
        sender.send(new GameStateRequest(myself));
        game.setState(GameState.CHECKING_OPPONENT_STATE);
    }

    @RabbitHandler
    public void receive(PlayRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(SEARCHING_FOR_THE_OPPONENT)) {
            log.info("Received play request even if not expected. Check opponent state");
            requestForTheOpponentState();
            return;
        }

        if (event.getSender().equals(game.getOpponent())) {
            log.info("Already processing play request from {}", event.getSender());
            return;
        }

        log.info("{} received play game request from {}", myself, event.getSender());
        sender.send(new PlayRequestAcceptedEvent(myself));
    }

    @RabbitHandler
    public void receive(GameStateRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        sender.send(new GameStateProvidedEvent(myself, game.getState(), game.getMoveType(), game.getMoves()));
    }

    @RabbitHandler
    public void receive(GameStateProvidedEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.CHECKING_OPPONENT_STATE)) {
            log.error("Received game state but it was not requested");
            return;
        }

        game.rollbackState();
        boolean opponentJustStarted = event.getState() == SEARCHING_FOR_THE_OPPONENT;
        if (opponentJustStarted) {
            log.info("Seems like the opponent just started. Sending recovery request to the opponent");
            sender.send(new RecoveryRequest(myself, game.getState(), game.getMoveType(), game.getMoves()));
        } else {
            log.info("Received unexpected request from the opponent. Performing consistency check");
            sendConsistencyCheck();
        }
    }

    @RabbitHandler
    public void receive(RecoveryRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        MoveType expectedMoveType = event.getType() == X ? O : X;
        if (!(game.getMoves().equals(event.getMoves())
                || game.getState().equals(event.getState())
                || game.getMoveType() == expectedMoveType)) {
            log.info("State is inconsistent. Recovering");
            recoveryService.acceptState(event.getSender(), event.getState(), expectedMoveType, event.getMoves());
            log.info("State is recovered");
        } else {
            log.info("State is consistent already");
        }

        sender.send(new InstanceRecoveredEvent(myself));
    }

    @RabbitHandler
    public void receive(InstanceRecoveredEvent event) {
        if (validator.isNotValid(event.getSender())) {
            return;
        }

        game.rollbackState();
        continueGame();
    }

    private void makeMove() {
        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, game.getMoveType(), coordinates));
    }

    private void continueGame() {
        MoveMadeEvent lastMove = game.getLastMove();
        if (lastMove == null) {
            if (game.getMoveType() == X) {
                makeMove();
            } else {
                sender.send(new MakeMoveRequest(myself));
            }
            return;
        }

        if (!lastMove.getSender().equals(myself)) {
            log.info("Last move is not mine, making move");
            makeMove();
        } else {
            log.info("Last move is mine, asking opponent to move");
            sender.send(new MakeMoveRequest(myself));
        }
    }

    @RabbitHandler
    public void receive(MakeMoveRequest event) {
        if (validator.isNotValid(event.getSender())) {
            return;
        }

        makeMove();
    }

    @RabbitHandler
    public void receive(PlayRequestAcceptedEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(SEARCHING_FOR_THE_OPPONENT)) {
            return;
        }

        if (event.getSender().equals(game.getOpponent())) {
            log.info("Already accepting request from {}", event.getSender());
            return;
        }

        game.setOpponent(event.getSender());
        game.setState(OPPONENT_FOUND);

        log.info("{} accepted play request. Starting game", event.getSender());

        game.setMoveType(game.defineMoveType());
        game.setState(GameState.CHOOSING_MOVE_TYPE);
        sender.send(new MoveTypeApprovalRequest(myself, game.getMoveType()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovalRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.CHOOSING_MOVE_TYPE)) {
            return;
        }

        if (event.getMoveType() == game.getMoveType()) {
            log.info("{} and {} have same types, need to change", myself, event.getSender());
            sender.send(new MoveTypeRejectedEvent(myself, game.getMoveType()));
        } else {
            log.info("{} is {}, {} is {}", myself, game.getMoveType(), event.getSender(), event.getMoveType());
            sender.send(new MoveTypeApprovedEvent(myself, event.getMoveType()));
        }
    }

    @RabbitHandler
    public void receive(MoveTypeRejectedEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.CHOOSING_MOVE_TYPE)) {
            return;
        }

        log.info("{} rejected move type {}, changing", event.getSender(), event.getMoveType());
        game.setMoveType(game.defineMoveType());
        sender.send(new MoveTypeApprovalRequest(myself, game.getMoveType()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovedEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.CHOOSING_MOVE_TYPE)) {
            return;
        }

        game.setState(GameState.IN_PROGRESS);
        log.info("{} approved move type {}", event.getSender(), event.getMoveType());
        if (game.getMoveType() == X) {
            log.info("{} is making first move", myself);

            Coordinates coordinates = game.getFreeSpaceToMakeMove();
            sender.send(new MoveApprovalRequest(myself, game.getMoveType(), coordinates));
        } else {
            log.info("{} is waiting for the {} to make first move", myself, game.getOpponent());
        }
    }

    @RabbitHandler
    public void receive(MoveApprovalRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.IN_PROGRESS)) {
            return;
        }

        if (game.getMoves().isEmpty()) {
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            return;
        }

        MoveMadeEvent lastMoveMade = game.getMoves().get(game.getMoves().size() - 1);
        boolean lastMoveIsMine = lastMoveMade.getMoveType().equals(game.getMoveType());

        if (lastMoveIsMine && game.isSpaceFreeToMove(event.getCoordinates())) {
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            return;
        }

        sender.send(new MoveRejectedEvent(myself, event.getMoveType(), event.getCoordinates()));
        log.info("Rejected move of {} to {}", event.getSender(), event.getCoordinates());
    }

    private List<MoveMadeEvent> defineValidMoves(List<MoveMadeEvent> opponentMoves) {
        int minIdxAllowed = Math.min(game.getMoves().size(), opponentMoves.size());
        List<MoveMadeEvent> validMoves = new ArrayList<>();
        for (int i = 0; i < minIdxAllowed; i++) {
            MoveMadeEvent myMove = game.getMoves().get(i);
            MoveMadeEvent opponentMove = opponentMoves.get(i);
            if (myMove.equals(opponentMove)) {
                validMoves.add(myMove);
            } else {
                break;
            }
        }
        return validMoves;
    }

    @RabbitHandler
    public void receive(ConsistencyCheckRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        log.info("Received consistency check request");
        boolean stateIsConsistent = game.getState() == event.getState();
        boolean moveTypeIsConsistent = game.getMoveType() != event.getType();
        boolean movesAreConsistent = game.getMoves().equals(event.getMoves());
        boolean isConsistent = false;
        if (stateIsConsistent && movesAreConsistent && moveTypeIsConsistent) {
            log.info("State is consistent");
            isConsistent = true;
        } else {
            log.info("State is not consistent. Trying to recover health state");
            List<MoveMadeEvent> validMoves = defineValidMoves(event.getMoves());
            recoveryService.acceptState(event.getSender(), event.getState(), event.getType(), validMoves);
        }

        sender.send(new ConsistencyCheckResponse(myself
                , game.getState()
                , game.getMoveType()
                , game.getMoves()
                , isConsistent));
    }

    @RabbitHandler
    public void receive(ConsistencyCheckResponse event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (event.isConsistent()) {
            log.info("Opponent state is consistent, continue game");
        } else {
            log.info("Opponent state is not consistent, restoring consistency");
            game.restart();

            MoveType moveType = findAppropriateMoveType(event.getMoveType());

            recoveryService.acceptState(event.getSender(), event.getState(), moveType, event.getMoves());
            log.info("State is aligned with the opponent. Continue game");
        }

        continueGame();
    }

    @RabbitHandler
    public void receive(MoveRejectedEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.IN_PROGRESS)) {
            return;
        }

        /*
          We can make invalid move only if our internal state is not consistent
          => we need to align our state with the opponent
         */
        log.info("Move to {} was rejected by {}", event.getCoordinates(), event.getSender());
        sendConsistencyCheck();
    }

    private void sendConsistencyCheck() {
        sender.send(new ConsistencyCheckRequest(myself, game.getState(), game.getMoveType(), game.getMoves()));
    }

    private boolean gameIsNotReadyYet() {
        if (game.getState().compareTo(GameState.IN_PROGRESS) < 0) {
            log.error("Game is not ready to accept play events yet");
            return true;
        }
        return false;
    }

    @RabbitHandler
    public void receive(MoveApprovedEvent event) throws InterruptedException {
        Thread.sleep(3000);
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (gameIsNotReadyYet()) {
            return;
        }

        if (validator.isIncorrectState(GameState.IN_PROGRESS)) {
            sendConsistencyCheck();
            return;
        }

        log.info("Move to {} was approved by {}", event.getCoordinates(), event.getSender());

        game.makeMove(game.getMoveType(), event.getCoordinates());
        MoveMadeEvent move = new MoveMadeEvent(myself, game.getMoveType(), event.getCoordinates());
        game.getMoves().add(move);

        sender.send(move);
    }

    @RabbitHandler
    public void receive(MoveMadeEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (gameIsNotReadyYet()) {
            log.error("Seems like some old events received. Ignoring");
            return;
        }

        if (validator.isIncorrectState(GameState.IN_PROGRESS)) {
            sendConsistencyCheck();
            return;
        }

        game.makeMove(event.getMoveType(), event.getCoordinates());
        game.getMoves().add(event);

        if (game.isOver()) {
            game.setState(GameState.IS_OVER);
            log.info("Game is over, sending game over event");
            sender.send(new GameIsOverEvent(myself));
            return;
        }

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, game.getMoveType(), coordinates));
    }

    @RabbitHandler
    public void receive(GameIsOverEvent event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (gameIsNotReadyYet()) {
            log.error("Seems like some old events received. Ignoring");
            return;
        }

        if (validator.isIncorrectState(GameState.IN_PROGRESS)) {
            sendConsistencyCheck();
            return;
        }

        if (!game.isOver()) {
            log.error("Game is not over on the side of {}", myself);
            sendConsistencyCheck();
            game.setState(GameState.INCONSISTENT);
            return;
        }

        game.setState(GameState.IS_OVER);
        log.info("Game is over");
    }
}
