package com.example.tictactoe.game;

import com.example.tictactoe.messaging.event.*;
import com.example.tictactoe.messaging.request.MoveApprovalRequest;
import com.example.tictactoe.messaging.request.MoveTypeApprovalRequest;
import com.example.tictactoe.messaging.request.PlayRequest;
import com.example.tictactoe.messaging.sender.MessageSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.concurrent.TimeUnit;

import static com.example.tictactoe.game.GameState.*;
import static com.example.tictactoe.game.MoveType.O;
import static com.example.tictactoe.game.MoveType.X;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class EventProcessorTest {
    @Container
    static final RabbitMQContainer rabbitmq =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    @DynamicPropertySource
    static void overridePropertiesInternal(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private MessageSender sender;
    @SpyBean
    private RabbitTemplate template;
    @Autowired
    private FanoutExchange exchange;

    @SpyBean
    private Game game;
    @Value("${spring.application.name}")
    private String myself;
    private String opponent = "opponent";

    @BeforeEach
    void beforeEach() {
        game.restart();
    }

    @Test
    void ignorePlayRequestFromMyself() {
        PlayRequest playRequest = new PlayRequest(myself);
        sender.send(playRequest);

        assertEquals(GameState.SEARCHING_FOR_THE_OPPONENT, game.getState());
    }

    @Test
    void acceptPlayRequestFromOtherInstance() throws InterruptedException {
        PlayRequest playRequest = new PlayRequest(opponent);
        sender.send(playRequest);

        Thread.sleep(1000);

        Mockito.verify(template).convertAndSend(exchange.getName(), "",  new PlayRequestAcceptedEvent(myself));
    }


    @Test
    void requestApproveForAMoveTypeOncePlayRequestIsAccepted() throws InterruptedException {
        PlayRequestAcceptedEvent requestAccepted = new PlayRequestAcceptedEvent(opponent);
        sender.send(requestAccepted);

        await().atMost(10, TimeUnit.SECONDS).until(() -> game.getOpponent() != null);

        assertEquals(opponent, game.getOpponent());
        assertEquals(GameState.OPPONENT_FOUND, game.getPreviousState());
        assertEquals(CHOOSING_MOVE_TYPE, game.getState());
        Mockito.verify(template).convertAndSend(exchange.getName(), "",  new MoveTypeApprovalRequest(myself, game.getMoveType().name()));
    }

    @Test
    void moveTypeApprovedIfItsDifferent() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        MoveTypeApprovalRequest approvalRequest = new MoveTypeApprovalRequest(opponent, O.name());
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template).convertAndSend(exchange.getName(), "",  new MoveTypeApprovedEvent(myself, O.name()));
    }

    @Test
    void moveTypeRejectedWhenItsTheSame() throws InterruptedException {
        game.setMoveType(O);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        MoveTypeApprovalRequest approvalRequest = new MoveTypeApprovalRequest(opponent, O.name());
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template, times(1)).convertAndSend(exchange.getName(), "",  new MoveTypeRejectedEvent(myself, O.name()));
    }

    @Test
    void requestsNewApproveIfMoveTypeIsRejected() throws InterruptedException {
        game.setMoveType(O);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        MoveTypeRejectedEvent rejectedEvent = new MoveTypeRejectedEvent(opponent, O.name());
        sender.send(rejectedEvent);

        Thread.sleep(1000);
        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveTypeApprovalRequest(myself, game.getMoveType().name()));
    }

    @Test
    void startsGameIfMoveTypeIsApproved() throws InterruptedException {
        PlayRequest playRequest = new PlayRequest(opponent);
        sender.send(playRequest);

        PlayRequestAcceptedEvent requestAccepted = new PlayRequestAcceptedEvent(opponent);
        sender.send(requestAccepted);

        game.setMoveType(O);

        MoveTypeApprovedEvent approvedEvent = new MoveTypeApprovedEvent(opponent, O.name());
        sender.send(approvedEvent);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> game.getState() == GameState.IN_PROGRESS);
    }

    @Test
    void xMoveFirst() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        Coordinates targetCoordinates = new Coordinates(0, 0);
        when(game.getFreeSpaceToMakeMove()).thenReturn(targetCoordinates);

        MoveTypeApprovedEvent approvedEvent = new MoveTypeApprovedEvent(opponent, X.name());
        sender.send(approvedEvent);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> game.getState() == GameState.IN_PROGRESS);

        Thread.sleep(1000);
        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveApprovalRequest(myself, X.name(), targetCoordinates));
    }

    @Test
    void approveMoveIfItIsValid() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        Coordinates targetCoordinates = new Coordinates(0, 0);

        MoveApprovalRequest approvalRequest = new MoveApprovalRequest(opponent, O.name(), targetCoordinates);
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveApprovedEvent(myself, O.name(), targetCoordinates));
    }

    @Test
    void doNotApproveMoveIsGameIsNotInProgress() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        Coordinates targetCoordinates = new Coordinates(0, 0);

        MoveApprovalRequest approvalRequest = new MoveApprovalRequest(opponent, O.name(), targetCoordinates);
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template, times(0))
                .convertAndSend(exchange.getName(), "",  new MoveApprovedEvent(myself, O.name(), targetCoordinates));
    }

    @Test
    void doNotApproveMoveToTheOccupiedSpace() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        Coordinates targetCoordinates = new Coordinates(0, 0);
        game.makeMove(X, targetCoordinates);
        game.getMoves().add(new MoveMadeEvent(myself, X.name(), targetCoordinates));

        MoveApprovalRequest approvalRequest = new MoveApprovalRequest(opponent, O.name(), targetCoordinates);
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveRejectedEvent(myself, O.name(), targetCoordinates));
    }

    @Test
    void rejectRepeatedOpponentMove() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        game.getMoves().add(new MoveMadeEvent(opponent, O.name(), new Coordinates(0, 1)));

        Coordinates targetCoordinates = new Coordinates(0, 0);

        MoveApprovalRequest approvalRequest = new MoveApprovalRequest(opponent, O.name(), targetCoordinates);
        sender.send(approvalRequest);

        Thread.sleep(1000);

        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveRejectedEvent(myself, O.name(), targetCoordinates));
    }

    @Test
    void makeMoveOnlyAfterOpponentApproval() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        Coordinates targetCoordinates = new Coordinates(0, 1);
        MoveApprovedEvent approvedEvent = new MoveApprovedEvent(opponent, X.name(), targetCoordinates);
        sender.send(approvedEvent);

        await().atMost(10, TimeUnit.SECONDS).until(() -> game.getMoves().size() == 1);
        assertFalse(game.isSpaceFreeToMove(targetCoordinates));

        Mockito.verify(template, times(1))
                .convertAndSend(exchange.getName(), "",  new MoveMadeEvent(myself, X.name(), targetCoordinates));
    }

    @Test
    void acceptValidOpponentMoves() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        Coordinates targetCoordinates = new Coordinates(0, 1);
        MoveMadeEvent event = new MoveMadeEvent(opponent, O.name(), targetCoordinates);

        assertTrue(game.getMoves().isEmpty());
        sender.send(event);

        Thread.sleep(1000);

        assertEquals(1, game.getMoves().size());
        assertFalse(game.isSpaceFreeToMove(targetCoordinates));
    }

    @Test
    void acceptGameOverEventFromTheOpponent() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        game.getField().put(X, new Coordinates(0, 0));
        game.getField().put(X, new Coordinates(1, 0));
        game.getField().put(X, new Coordinates(2, 0));

        GameIsOverEvent event = new GameIsOverEvent(opponent);
        sender.send(event);

        Thread.sleep(1000);

        await().atMost(10, TimeUnit.SECONDS).until(() -> game.getState() == IS_OVER);
    }

    @Test
    void stateIsInconsistentIfInstancesDoNotAgreeAboutGameOver() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(IN_PROGRESS);

        GameIsOverEvent event = new GameIsOverEvent(opponent);
        sender.send(event);

        Thread.sleep(1000);

        await().atMost(10, TimeUnit.SECONDS).until(() -> game.getState() == INCONSISTENT);
    }

    @Test
    void cantEndIfGameIsNotInProgress() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);

        GameIsOverEvent event = new GameIsOverEvent(opponent);
        sender.send(event);

        Thread.sleep(1000);

        assertEquals(CHOOSING_MOVE_TYPE, game.getState());
        assertFalse(game.isOver());
    }

    @Test
    void cantMakeMoveIfGameIsNotInProgress() throws InterruptedException {
        game.setMoveType(X);
        game.setOpponent(opponent);
        game.setState(CHOOSING_MOVE_TYPE);
        Coordinates targetCoordinates = new Coordinates(0, 0);

        assertTrue(game.getMoves().isEmpty());

        MoveMadeEvent event = new MoveMadeEvent(opponent, O.name(), targetCoordinates);
        sender.send(event);

        Thread.sleep(1000);
        assertTrue(game.getMoves().isEmpty());
        assertTrue(game.isSpaceFreeToMove(targetCoordinates));
    }
}