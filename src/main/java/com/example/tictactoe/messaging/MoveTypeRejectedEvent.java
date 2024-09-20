package com.example.tictactoe.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveTypeRejectedEvent implements Serializable {
    private String sender;
    private String moveType;
}
