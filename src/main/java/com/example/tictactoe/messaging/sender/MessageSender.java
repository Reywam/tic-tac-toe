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

    public void send(PlayRequest event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(PlayRequestAcceptedEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveTypeApprovalRequest event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveTypeApprovedEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveTypeRejectedEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }


    public void send(MoveApprovalRequest event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveApprovedEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveRejectedEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(MoveMadeEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }

    public void send(GameIsOverEvent event) {
        this.template.convertAndSend(exchange.getName(), "", event);
    }
}
