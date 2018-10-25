package com.icrn.model;

import java.time.LocalDateTime;
import java.util.Optional;

public interface StatsBasedEntity extends Entity {

    int getMaxHP();
    int getHP();
    int getSTR();
    int getDEX();
    int getCON();

    void setMaxHP(int maxHP);
    void setHP(int hp);
    void setSTR(int str);
    void setDEX(int dex);
    void setCON(int con);

    //Since it's attackable, we need to verify if it has been attacked

    Optional<Long> getLastAttackedById();
    void setLastAttackedById(Long id);

    Optional<Actions> getLastCommand();
    void setLastCommand(Actions action);

    boolean isOnline();
    @Override
    default boolean isStatsBased(){
        return true;
    }

    boolean canPerformAction();

    LocalDateTime performedAction();
}
