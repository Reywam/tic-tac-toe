package com.example.tictactoe.game;

import com.example.tictactoe.messaging.*;
import com.example.tictactoe.messaging.sender.MessageSender;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.tictactoe.game.MoveType.X;

@Slf4j
@Service
@RabbitListener(queues = "#{gameQueue.name}", ackMode = "MANUAL")
@RequiredArgsConstructor
public class EventProcessor {
    private final Game game;
    private MoveType moveType;

    @Value("${spring.application.name}")
    private String myself;

    private final MessageSender sender;

    private final List<MoveMadeEvent> moves = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("{} is started and ready to play. Sending play request", myself);
        sender.send(new PlayRequest(myself));
        game.setState(GameState.SEARCHING_FOR_THE_OPPONENT);
    }


    private void reject(Channel channel, long tag) throws IOException {
        channel.basicReject(tag, true);
    }

    private void ack(Channel channel, long tag) throws IOException {
        channel.basicAck(tag, false);
    }

    @RabbitHandler
    public void receive(PlayRequest event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself)) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.SEARCHING_FOR_THE_OPPONENT) {
            log.error("{} tries to play with game in state {}", event.getSender(), game.getState());
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
    public void receive(PlayRequestAcceptedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself)) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.SEARCHING_FOR_THE_OPPONENT) {
            ack(channel, tag);
            log.error("Not ready to process new opponents");
            return;
        }

        if (event.getSender().equals(game.getOpponent())) {
            log.info("Already accepting request from {}", event.getSender());
            return;
        }

        game.setOpponent(event.getSender());
        game.setState(GameState.OPPONENT_FOUND);

        log.info("{} accepted play request. Starting game", event.getSender());

        moveType = game.defineMoveType();
        log.info("{} defined move type {}", myself, moveType.name());
        sender.send(new MoveTypeApprovalRequest(myself, moveType.name()));
        game.setState(GameState.CHOOSING_MOVE_TYPE);
    }

    @RabbitHandler
    public void receive(MoveTypeApprovalRequest event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            log.info("Opponent {}", game.getOpponent());
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.CHOOSING_MOVE_TYPE) {
            log.error("{} tries to choose move type", event.getSender());
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
    public void receive(MoveTypeRejectedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.CHOOSING_MOVE_TYPE) {
            log.error("{} try to reject move type", event.getSender());
            return;
        }

        log.info("{} rejected move type {}, changing", event.getSender(), event.getMoveType());
        moveType = game.defineMoveType();

        log.info("{} defined move type {}", myself, moveType.name());
        sender.send(new MoveTypeApprovalRequest(myself, moveType.name()));
    }

    @RabbitHandler
    public void receive(MoveTypeApprovedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.CHOOSING_MOVE_TYPE) {
            log.error("{} try to approve move type", event.getSender());
            return;
        }

        game.setState(GameState.IN_PROGRESS);
        log.info("{} approved move type {}", event.getSender(), event.getMoveType());
        if (moveType == X) {
            log.info("{} is making first move", myself);

            Coordinates coordinates = game.getFreeSpaceToMakeMove();
            sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
        } else {
            log.info("{} is waiting for the {} to make first move", myself, game.getOpponent());
        }
    }

    @RabbitHandler
    public void receive(MoveApprovalRequest event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.IN_PROGRESS) {
            log.error("{} tries to approve move even if game is not in progress", event.getSender());
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
    public void receive(MoveRejectedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.IN_PROGRESS) {
            log.error("{} tries to reject move even if game is not in progress", event.getSender());
            return;
        }

        log.info("Move to {} was rejected by {}", event.getCoordinates(), event.getSender());

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
    }

    @RabbitHandler
    public void receive(MoveApprovedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws InterruptedException, IOException {
        Thread.sleep(3000);
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.IN_PROGRESS) {
            log.error("{} tries to approve move even if game is not in progress", event.getSender());
            return;
        }

        log.info("Move to {} was approved by {}", event.getCoordinates(), event.getSender());
        game.makeMove(moveType, event.getCoordinates());
        MoveMadeEvent t = new MoveMadeEvent(myself, moveType.name(), event.getCoordinates());
        moves.add(t);

        sender.send(t);
    }

    @RabbitHandler
    public void receive(MoveMadeEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws InterruptedException, IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.IN_PROGRESS) {
            log.error("{} tries to make move even if game is not in progress", event.getSender());
            return;
        }

        game.makeMove(MoveType.valueOf(event.getMoveType()), event.getCoordinates());
        moves.add(event);

        game.printField();
        if (game.isOver()) {
            game.setState(GameState.IS_OVER);
            game.printField();
            log.info("Game is over, sending game over event");
            sender.send(new GameIsOverEvent(myself));
            return;
        }

        Coordinates coordinates = game.getFreeSpaceToMakeMove();
        sender.send(new MoveApprovalRequest(myself, moveType.name(), coordinates));
    }

    @RabbitHandler
    public void receive(GameIsOverEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (event.getSender().equals(myself) || !event.getSender().equals(game.getOpponent())) {
            reject(channel, tag);
            return;
        }

        ack(channel, tag);

        if (game.getState() != GameState.IN_PROGRESS) {
            log.error("{} tries to end not started game", event.getSender());
            return;
        }

        if (!game.isOver()) {
            log.error("Game is not over on the side of {}", myself);
            game.setState(GameState.INCONSISTENT);
            return;
        }

        game.setState(GameState.IS_OVER);

        log.info("Game is over");
        game.printField();
    }
}
