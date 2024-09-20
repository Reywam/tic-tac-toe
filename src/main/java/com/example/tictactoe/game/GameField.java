package com.example.tictactoe.game;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

@Slf4j
public class GameField {
    private final int WIDTH;
    private final int HEIGHT;
    private final Random rand;
    private final MoveType[][] field;
    private final List<Coordinates> FREE_SPACE;

    public GameField(int width, int height) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.field = new MoveType[WIDTH][HEIGHT];

        rand = new Random();
        FREE_SPACE = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                FREE_SPACE.add(new Coordinates(x, y));
            }
        }
    }

    public int getHeight() {
        return HEIGHT;
    }

    public int getWidth() {
        return WIDTH;
    }

    public void put(MoveType moveType, Coordinates coordinates) {
        FREE_SPACE.remove(coordinates);
        field[coordinates.x()][coordinates.y()] = moveType;
    }

    public MoveType get(int x, int y) {
        return field[x][y];
    }

    public List<Coordinates> getAvailableSpaceToMove() {
        return FREE_SPACE;
    }

    public Coordinates findFreeSpace() {
        int freeSpaceIdx = rand.nextInt(FREE_SPACE.size());
        return FREE_SPACE.get(freeSpaceIdx);
    }

    public void printField() {
        StringJoiner joiner = new StringJoiner("|", "|", "");
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (field[x][y] == null) {
                    joiner.add(" ");
                } else {
                    joiner.add(field[x][y].name());
                }
            }
            joiner.add("\n");
        }
        System.out.println(joiner.toString());
    }
}
