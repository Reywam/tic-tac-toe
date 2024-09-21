package com.example.tictactoe.game;

import com.example.tictactoe.messaging.*;
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

import static com.example.tictactoe.game.MoveType.X;

@Slf4j
@Service
@RabbitListener(queues = "#{queue.name}")
@RequiredArgsConstructor
public class EventProcessor {
    private final Game game;
    private MoveType moveType;

    @Value("${spring.application.name}")
    private String myself;
    private String opponent;

    private final MessageSender sender;

    private boolean isOpponentFoundAlready = false;
    private boolean gameIsStarted = false;
    private final List<MoveMadeEvent> moves = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("{} is started and ready for play. Sending play request", myself);
        sender.send(new PlayRequest(myself));
    }

    @RabbitHandler
    public void receive(PlayRequest event) {
        if (event.getSender().equals(myself)) {
            return;
        }

        if (isOpponentFoundAlready && !opponent.equals(event.getSender())) {
            log.error("{} already playing with someone, ignoring play request from {}", myself, event.getSender());
            return;
        }

        if (event.getSender().equals(opponent)) {
            log.info("Already processing play request from {}", event.getSender());
            return;
        }

        log.info("{} received play game request from {}", myself, event.getSender());

        sender.send(new PlayRequestAcceptedEvent(myself));
    }

    @RabbitHandler
    public void receive(PlayRequestAcceptedEvent event) {
        if (event.getSender().equals(myself)) {
            return;
        }

        if (isOpponentFoundAlready && !opponent.equals(event.getSender())) {
            // Already playing with someone
            log.error("{} already playing with someone, ignoring play request from {}", myself, event.getSender());
            return;
        }

        if (event.getSender().equals(opponent)) {
            log.info("Already accepting request from {}", event.getSender());
            return;
        }

        isOpponentFoundAlready = true;
        opponent = event.getSender();
        log.info("{} accepted play request. Starting game", event.getSender());

        moveType = game.defineMoveType();
        log.info("{} defined move type {}", myself, moveType.name());
        sender.send(new MoveTypeApprovalRequest(myself, moveType.name()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovalRequest event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        if (MoveType.valueOf(event.getMoveType()) == moveType) {
            log.info("{} and {} have same types, need to change", myself, event.getSender());
            sender.send(new MoveTypeRejectedEvent(myself, moveType.name()));
        } else {
            log.info("{} is {}, {} is {}", myself, moveType, event.getSender(), event.getMoveType());
            sender.send(new MoveTypeApprovedEvent(myself, event.getMoveType()));
        }
    }

    @RabbitHandler
    public void receive(MoveTypeRejectedEvent event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        log.info("{} rejected move type {}, changing", event.getSender(), event.getMoveType());
        moveType = game.defineMoveType();

        log.info("{} defined move type {}", myself, moveType.name());
        sender.send(new MoveTypeApprovalRequest(myself, moveType.name()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovedEvent event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        log.info("{} approved move type {}", event.getSender(), event.getMoveType());
        if (moveType == X) {
            // Make turn
            log.info("{} is making first move", myself);

            Coordinates coordinates = game.getFreeSpaceToMakeMove();
            sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
//            game.makeMove(moveType, coordinates);
//            log.info("{} made move to {}", myself, coordinates);

//            //moves.add(t);
//            MoveMadeEvent t = new MoveMadeEvent(myself, moveType.name(), coordinates);
//            sender.send(t);
        } else {
            log.info("{} is waiting for the {} to make first move", myself, opponent);
        }
        gameIsStarted = true;
    }

    @RabbitHandler
    public void receive(MoveApprovalRequest event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        if (!gameIsStarted) {
            log.error("{} tries to make move even if game is not started yet", event.getSender());
            return;
        }

        if (game.isOver()) {
            log.error("{} tries to make move even if game if over", event.getSender());
            return;
        }

        if (moves.isEmpty()) {
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            return;
        }

        MoveMadeEvent lastMoveMade = moves.get(moves.size() - 1);
        if (lastMoveMade.getMoveType().equals(moveType.name())
                && game.isSpaceFreeToMove(event.getCoordinates())) {
            log.info("Move of {} to {} is approved", event.getSender(), event.getCoordinates());
            sender.send(new MoveApprovedEvent(myself, event.getMoveType(), event.getCoordinates()));
            return;
        }

        sender.send(new MoveRejectedEvent(myself, event.getMoveType(), event.getCoordinates()));
        log.info("Rejected move of {} to {}", event.getSender(), event.getCoordinates());
    }

    @RabbitHandler
    public void receive(MoveRejectedEvent event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        log.info("Move to {} was rejected by {}", event.getCoordinates(), event.getSender());

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
    }

    @RabbitHandler
    public void receive(MoveApprovedEvent event) throws InterruptedException {
        Thread.sleep(3000);
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        log.info("Move to {} was approved by {}", event.getCoordinates(), event.getSender());
        game.makeMove(moveType, event.getCoordinates());
        MoveMadeEvent t = new MoveMadeEvent(myself, moveType.name(), event.getCoordinates());
        moves.add(t);

        sender.send(t);
    }

    @RabbitHandler
    public void receive(MoveMadeEvent event) throws InterruptedException {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        log.info("{}:{}->{}", event.getSender(), event.getMoveType(), event.getCoordinates());

        game.makeMove(MoveType.valueOf(event.getMoveType()), event.getCoordinates());
        moves.add(event);

        game.printField();
        if (game.isOver()) {
            game.printField();
            log.info("Game is over, sending game over event");
            sender.send(new GameIsOverEvent(myself));
            return;
        }

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
    }

    @RabbitHandler
    public void receive(GameIsOverEvent event) {
        if (event.getSender().equals(myself) || !event.getSender().equals(opponent)) {
            return;
        }

        if (!gameIsStarted) {
            log.error("{} tries to end not started game", event.getSender());
            return;
        }

        if (!game.isOver()) {
            log.error("Game is not over on the side of {}", myself);
            return;
        }

        log.info("Game is over");
        game.printField();
    }
}
