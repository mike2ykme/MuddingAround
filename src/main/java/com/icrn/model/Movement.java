package com.icrn.model;

public enum Movement {
    NORTH,EAST,WEST,SOUTH,UP,DOWN,BAD_DIRECTION;

    public static Movement of(String direction) {
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
                return Movement.BAD_DIRECTION;
        }
    }

    public static Movement reverse(Movement movement){
        switch (movement){
            case NORTH:
                return Movement.SOUTH;
            case EAST:
                return Movement.WEST;
            case WEST:
                return Movement.EAST;
            case SOUTH:
                return Movement.NORTH;
            case UP:
                return Movement.DOWN;
            case DOWN:
                return Movement.UP;
            default:
                return Movement.BAD_DIRECTION;
        }
    }
}
