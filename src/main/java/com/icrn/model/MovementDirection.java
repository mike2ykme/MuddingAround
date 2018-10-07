package com.icrn.model;

public class MovementDirection {
//    North
//    East
//    West
//    South
//    Up
//    Down
    public static Movement of(String direction) {
//        if (direction.toUpperCase().contains("N")){
//            return Movement.NORTH;
//        }
        switch (direction.toUpperCase().charAt(0)){
            case 'N':
                return Movement.NORTH;

            case 'E':
                return Movement.EAST;

            case 'W':
                return Movement.WEST;

            case 'S':
                return Movement.SOUTH;

            case 'U':
                return Movement.UP;

            case 'D':
                return Movement.DOWN;

            default:
                throw new RuntimeException("Unable to parse direction");
        }

    }
}
