package com.icrn.model;

import java.util.Optional;

public interface Entity {
    EntityType getType();
//    boolean isOnline();
//    void setOnline(boolean b);
    Long getId();
    long getRoomLocation();
//    void setRoomLocation(long l);
    String getName();
    void setName(String name);
//    void setHP(int HP);
//    int getHP();

//    void setLastAttackedById(Long id);
//    Optional<Long> getLastAttackedById();
}
