package com.icrn.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class Room implements Entity {
    Map<Movement,Long> allowedDirections;
    long roomLocation;
    private Long id;
    private String name;
    private Long lastAttackedBy;
    public Room(){

    }

    public String getName(){
        if (this.name == null)
            return id.toString();
        else
            return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    @Override
    public void setHP(int HP) {

    }

    @Override
    public int getHP() {
        return -1;
    }

    @Override
    public void setLastAttackedById(Long id) {

    }

    @Override
    public Optional<Long> getLastAttackedById() {
        return Optional.empty();
    }

    public Room(Long id){
        this.id = id;
    }

    @Override
    public EntityType getType() {
        return EntityType.ROOM;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public void setOnline(boolean b) {

    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public long getRoomLocation() {
        return roomLocation;
    }

    @Override
    public void setRoomLocation(long l) {
        this.roomLocation = l;
    }

    public boolean allowsMovement(Movement movement) {
        return this.allowedDirections.containsKey(movement);

    }
    public void addRoomDirection(Movement movement, Long otherRoomId){
        this.allowedDirections.put(movement,otherRoomId);
    }
    public long getRoomFromDirection(Movement movement){
        return this.allowedDirections.get(movement);
    }
}
