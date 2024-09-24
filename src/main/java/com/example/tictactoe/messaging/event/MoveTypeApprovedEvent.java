package com.example.tictactoe.messaging.event;

import com.example.tictactoe.game.MoveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveTypeApprovedEvent implements Serializable {
    private String sender;
    private MoveType moveType;
}
