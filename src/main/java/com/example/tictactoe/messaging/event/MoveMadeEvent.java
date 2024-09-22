package com.example.tictactoe.messaging.event;

import com.example.tictactoe.game.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveMadeEvent implements Serializable {
    private String sender;
    private String moveType;
    private Coordinates coordinates;
}