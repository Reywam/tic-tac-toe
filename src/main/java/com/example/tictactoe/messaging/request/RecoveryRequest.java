package com.example.tictactoe.messaging.request;

import com.example.tictactoe.game.GameState;
import com.example.tictactoe.game.MoveType;
import com.example.tictactoe.messaging.event.MoveMadeEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryRequest {
    private String sender;
    private GameState state;
    private MoveType type;
    private List<MoveMadeEvent> moves;}
