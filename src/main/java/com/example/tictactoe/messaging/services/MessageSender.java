package com.example.tictactoe.messaging.services;

import com.example.tictactoe.Utils;
import com.example.tictactoe.configuration.AppConfig;
import com.example.tictactoe.game.Game;
import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;
import com.rabbitmq.client.ShutdownSignalException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.tictactoe.Utils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    private final AppConfig config;
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
                    if (game.getMoveType() == MoveType.X) {
                        sendConsistencyCheck(MessageSender.this, config.getMyself(), game);
                    }
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
