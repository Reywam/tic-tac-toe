package com.example.tictactoe.messaging.sender;

import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.rabbitmq.client.ShutdownSignalException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {
    private final RabbitTemplate template;
    private final FanoutExchange exchange;
    private final Game game;

    @PostConstruct
    void init() {
        template.getConnectionFactory().addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
                if (game.getState() == GameState.INCONSISTENT) {
                    game.rollbackState();
                }
            }

            @Override
            public void onShutDown(ShutdownSignalException signal) {
                if (game.getState() != GameState.INCONSISTENT) {
                    game.setState(GameState.INCONSISTENT);
                }
            }
        });
    }

    public void send(Object event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }
}
