package com.example.tictactoe.messaging.event;

import com.example.tictactoe.game.GameState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateProvidedEvent implements Serializable {
    private String sender;
    private GameState state;
    private List<MoveMadeEvent> moves;
}
