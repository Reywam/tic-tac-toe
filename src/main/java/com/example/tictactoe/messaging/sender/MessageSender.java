package com.example.tictactoe.messaging.sender;

import com.example.tictactoe.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageSender {
    private final RabbitTemplate template;
    private final FanoutExchange exchange;

    public void send(Object event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }
}
