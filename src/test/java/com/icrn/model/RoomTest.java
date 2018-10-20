package com.icrn.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class RoomTest {

    @Test
    public void testJoiningRooms(){
        Room roomA = new Room(0L);
        Room roomB = new Room(10L);

        roomA.AddRoom(roomB,Movement.of("N"))
                .test()
                .assertValueCount(1)
                .assertValue(movementLongEntry -> movementLongEntry.getKey() == Movement.NORTH)
                .assertValue(movementLongEntry -> movementLongEntry.getValue() == 10L)
                .assertComplete()
                .assertNoErrors();

        assertTrue(roomB.getAllowedDirections().get(Movement.SOUTH) == 0L);
        assertFalse(roomB.getAllowedDirections().containsKey(Movement.NORTH));
    }
}