package com.example.tictactoe.game;

public enum GameState {
    READY,
    SEARCHING_FOR_THE_OPPONENT,
    OPPONENT_FOUND,
    CHOOSING_MOVE_TYPE,
    IN_PROGRESS,
    IS_OVER,
    INCONSISTENT,
    WAITING_FOR_THE_OPPONENT_TO_RECOVER
}
