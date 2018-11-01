package com.icrn.model;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.*;

public class RoomTest {

    @Test
    public void testJoiningRooms(){
        Room roomA = new Room(0L);
        Room roomB = new Room(10L);

        roomA.addRoom(roomB,Movement.of("N"))
                .test()
                .assertValueCount(1)
                .assertValue(movementLongEntry -> movementLongEntry.getKey() == Movement.NORTH)
                .assertValue(movementLongEntry -> movementLongEntry.getValue() == 10L)
                .assertComplete()
                .assertNoErrors();

        assertTrue(roomB.getAllowedDirections().get(Movement.SOUTH) == 0L);
        assertFalse(roomB.getAllowedDirections().containsKey(Movement.NORTH));
    }

    @Test
    public void testCreatingFullRoom(){

        val ROOM_NUMBER = 100L;
        val HP = 10;
        val STR = 10;
        val CON = 10;
        val DEX = 10;
        val MAX_HP = 25;
        val SAFE_ZONE = false;

        val MOB_COUNT = 2L;
        val MOB_NAME = "NAME";

        val builder = StatsBasedEntityTemplate.builder();
        val TEMPLATE = builder.room(ROOM_NUMBER)
                .STR(STR)
                .maxHP(MAX_HP)
                .HP(HP)
                .DEX(DEX)
                .CON(CON)
                .build();

        val mobInfo = MobInfo.of(MOB_COUNT,MOB_NAME,TEMPLATE);
        val room = new Room(ROOM_NUMBER);

        room.setSafeZone(SAFE_ZONE);
        room.setMobInfo(mobInfo);


        assertTrue(room.getRoomLocation() == room.getId());
        assertTrue(room.getId() == ROOM_NUMBER);
        assertTrue(room.isSafeZone() == SAFE_ZONE);

    }
}