package com.example.tictactoe.game;

import com.example.tictactoe.messaging.PlayGameRequest;
import com.example.tictactoe.messaging.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RabbitListener(queues = "#{queue.name}")
@RequiredArgsConstructor
public class EventProcessor {
    @Value("${spring.application.name}")
    private String myself;
    private final MessageSender sender;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        sender.send(new PlayGameRequest(myself));
    }

    public void receive(PlayGameRequest event) {
        if (event.getSender().equals(myself)) {
            return;
        }
        log.info("{} received play game request from {}", myself, event.getSender());
    }
}
