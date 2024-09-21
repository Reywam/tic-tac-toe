package com.example.tictactoe.messaging;

import com.example.tictactoe.game.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveApprovedEvent implements Serializable {
    private String sender;
    private String moveType;
    private Coordinates coordinates;
}
