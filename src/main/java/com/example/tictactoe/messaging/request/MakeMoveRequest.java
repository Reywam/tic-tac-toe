package com.example.tictactoe.messaging.request;

import com.example.tictactoe.game.Coordinates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeMoveRequest implements Serializable {
    private String sender;
}
