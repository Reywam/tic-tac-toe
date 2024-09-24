package com.example.tictactoe.messaging;

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

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("{} is started and ready to play. Sending play request", myself);
        sender.send(new PlayRequest(myself));
        game.setState(GameState.SEARCHING_FOR_THE_OPPONENT);
    }

    @RabbitHandler
    public void receive(PlayRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        if (validator.isIncorrectState(GameState.SEARCHING_FOR_THE_OPPONENT)) {
            log.info("Received play request even if not expected. Check opponent consistency");
            sender.send(new RecoveryRequest(myself, game.getState(), game.getMoveType(), game.getMoves()));
            game.setState(GameState.WAITING_FOR_THE_OPPONENT_TO_RECOVER);
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
    public void receive(RecoveryRequest event) {
        if (validator.isMessageFromMyself(event.getSender())) {
            return;
        }

        MoveType expectedMoveType = event.getType() == X ? O : X;
        if (!(game.getMoves().equals(event.getMoves())
                || game.getState().equals(event.getState())
                || game.getMoveType() == expectedMoveType)) {
            log.info("State is inconsistent. Recovering");
            game.setMoveType(expectedMoveType);
            game.setState(event.getState());
            game.setMoves(event.getMoves());
            game.getMoves().forEach(move -> game.getField().put(MoveType.valueOf(move.getMoveType()), move.getCoordinates()));
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
        MoveMadeEvent lastMove = game.getLastMove();
        if (!lastMove.getSender().equals(myself)) {
            log.info("Last move is not mine, making move");
            makeMove();
        } else {
            log.info("Last move is mine, asking opponent to move");
            sender.send(new MakeMoveRequest(myself));
        }
    }

    private void makeMove() {
        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, game.getMoveType().name(), coordinates));
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
        if (validator.isNotValid(GameState.SEARCHING_FOR_THE_OPPONENT, event.getSender())) {
            return;
        }

        if (event.getSender().equals(game.getOpponent())) {
            log.info("Already accepting request from {}", event.getSender());
            return;
        }

        game.setOpponent(event.getSender());
        game.setState(GameState.OPPONENT_FOUND);

        log.info("{} accepted play request. Starting game", event.getSender());

        game.setMoveType(game.defineMoveType());
        log.info("{} defined move type {}", myself, game.getMoveType().name());
        game.setState(GameState.CHOOSING_MOVE_TYPE);
        sender.send(new MoveTypeApprovalRequest(myself, game.getMoveType().name()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovalRequest event) {
        if (validator.isNotValid(GameState.CHOOSING_MOVE_TYPE, event.getSender())) {
            return;
        }

        if (MoveType.valueOf(event.getMoveType()) == game.getMoveType()) {
            log.info("{} and {} have same types, need to change", myself, event.getSender());
            sender.send(new MoveTypeRejectedEvent(myself, game.getMoveType().name()));
        } else {
            log.info("{} is {}, {} is {}", myself, game.getMoveType(), event.getSender(), event.getMoveType());
            sender.send(new MoveTypeApprovedEvent(myself, event.getMoveType()));
        }
    }

    @RabbitHandler
    public void receive(MoveTypeRejectedEvent event) {
        if (validator.isNotValid(GameState.CHOOSING_MOVE_TYPE, event.getSender())) {
            return;
        }

        log.info("{} rejected move type {}, changing", event.getSender(), event.getMoveType());
        game.setMoveType(game.defineMoveType());

        log.info("{} defined move type {}", myself, game.getMoveType().name());
        sender.send(new MoveTypeApprovalRequest(myself, game.getMoveType().name()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovedEvent event) {
        if (validator.isNotValid(GameState.CHOOSING_MOVE_TYPE, event.getSender())) {
            return;
        }

        game.setState(GameState.IN_PROGRESS);
        log.info("{} approved move type {}", event.getSender(), event.getMoveType());
        if (game.getMoveType() == X) {
            log.info("{} is making first move", myself);

            Coordinates coordinates = game.getFreeSpaceToMakeMove();
            sender.send(new MoveApprovalRequest(myself, game.getMoveType().name(), coordinates));
        } else {
            log.info("{} is waiting for the {} to make first move", myself, game.getOpponent());
        }
    }

    @RabbitHandler
    public void receive(MoveApprovalRequest event) {
        if (validator.isNotValid(GameState.IN_PROGRESS, event.getSender())) {
            return;
        }

        if (game.getMoves().isEmpty()) {
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            return;
        }

        MoveMadeEvent lastMoveMade = game.getMoves().get(game.getMoves().size() - 1);
        boolean lastMoveIsMine = lastMoveMade.getMoveType().equals(game.getMoveType().name());

        if (lastMoveIsMine && game.isSpaceFreeToMove(event.getCoordinates())) {
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            return;
        }

        sender.send(new MoveRejectedEvent(myself, event.getMoveType(), event.getCoordinates()));
        log.info("Rejected move of {} to {}", event.getSender(), event.getCoordinates());
    }

    @RabbitHandler
    public void receive(MoveRejectedEvent event) {
        if (validator.isNotValid(GameState.IN_PROGRESS, event.getSender())) {
            return;
        }

        log.info("Move to {} was rejected by {}", event.getCoordinates(), event.getSender());

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, game.getMoveType().name(), coordinates));
    }

    @RabbitHandler
    public void receive(MoveApprovedEvent event) throws InterruptedException {
        Thread.sleep(3000);
        if (validator.isNotValid(GameState.IN_PROGRESS, event.getSender())) {
            return;
        }

        log.info("Move to {} was approved by {}", event.getCoordinates(), event.getSender());

        game.makeMove(game.getMoveType(), event.getCoordinates());
        MoveMadeEvent move = new MoveMadeEvent(myself, game.getMoveType().name(), event.getCoordinates());
        game.getMoves().add(move);

        sender.send(move);
    }

    @RabbitHandler
    public void receive(MoveMadeEvent event) {
        if (validator.isNotValid(GameState.IN_PROGRESS, event.getSender())) {
            return;
        }

        game.makeMove(MoveType.valueOf(event.getMoveType()), event.getCoordinates());
        game.getMoves().add(event);

        if (game.isOver()) {
            game.setState(GameState.IS_OVER);
            log.info("Game is over, sending game over event");
            sender.send(new GameIsOverEvent(myself));
            return;
        }

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, game.getMoveType().name(), coordinates));
    }

    @RabbitHandler
    public void receive(GameIsOverEvent event) {
        if (validator.isNotValid(GameState.IN_PROGRESS, event.getSender())) {
            return;
        }

        if (!game.isOver()) {
            log.error("Game is not over on the side of {}", myself);
            game.setState(GameState.INCONSISTENT);
            return;
        }

        game.setState(GameState.IS_OVER);
        log.info("Game is over");
    }
}
