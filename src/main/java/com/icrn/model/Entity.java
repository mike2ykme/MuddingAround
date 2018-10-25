package com.icrn.model;

public interface Entity {
    EntityType getType();
    Long getId();
    void setId(Long id);
    long getRoomLocation();
    String getName();
    void setName(String name);
    boolean isStatsBased();
}
