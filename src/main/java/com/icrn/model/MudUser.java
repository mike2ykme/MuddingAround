package com.icrn.model;

import com.icrn.exceptions.TO;
import lombok.*;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Data
@AllArgsConstructor
@ToString
public class MudUser implements Entity, StatsBasedEntity{
    @NonNull private Long id;
    private static int SECONDS_TO_COMPARE = 20;
    private String name;
    private String password;
    private LocalDateTime lastActionPerformedTime;
    private int maxHP;
    private int HP;
    private int STR;
    private int DEX;
    private int CON;
    private boolean online;
    private EntityType type;
    private long roomLocation;
    private UserStatus userStatus;
    private Long lastAttackedById;
    private Actions lastCommand;

    public Optional<Long> getLastAttackedById(){
        if (this.lastAttackedById == null)
            return Optional.empty();

        else
            return Optional.of(this.lastAttackedById);
    }

    @Override
    public Optional<Actions> getLastCommand() {

        if (lastCommand == null)
            return Optional.empty();
        else
            return Optional.of(lastCommand);
    }

    @Override
    public void setLastCommand(Actions action) {
        this.lastCommand = action;
    }

    public LocalDateTime performAction(){
        val oldTime = this.lastActionPerformedTime;
        this.lastActionPerformedTime = LocalDateTime.now();
        return oldTime;
    }
    public boolean canPerformAction() {
        if (lastActionPerformedTime
                .plusSeconds((SECONDS_TO_COMPARE/this.DEX))
                .isBefore(LocalDateTime.now())){
            return true;
        }
        return false;
    }
    public LocalDateTime rest(){
        this.HP += this.CON * DEX /20;
        if (this.HP > this.maxHP)
            this.HP = this.maxHP;

        this.lastActionPerformedTime = LocalDateTime.now();
        return lastActionPerformedTime;
    }
    public static MudUser makeJoe(){
        LocalDateTime time = LocalDateTime.of(2018,1,1,1,1);
        return new MudUser(1L,"JOE","JOE",time,25,12,
                10,10,10,true,EntityType.USER,0L,UserStatus.ACTIVE,null,null);
    }

    @Override
    public void setHP(int hp){
        if (hp <=0){
            this.HP = 0;
            this.setUserStatus(UserStatus.KO);

        }else if (hp > this.maxHP){
            this.HP = this.maxHP;

        }else {
            this.HP = hp;

        }

        if (this.HP >0)
            this.setUserStatus(UserStatus.ACTIVE);

    }

    @Override
    public boolean isStatsBased() {
        return true;
    }
}
