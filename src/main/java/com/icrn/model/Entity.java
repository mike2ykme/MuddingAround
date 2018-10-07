package com.icrn.model;

public interface Entity {
    EntityType getType();
    boolean isOnline();
    void setOnline(boolean b);
    Long getId();
    long getRoomLocation();
    void setRoomLocation(long l);
    String getName();
    void setName(String name);
}
