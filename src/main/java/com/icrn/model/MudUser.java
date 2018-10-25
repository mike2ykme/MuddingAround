package com.icrn.model;

import lombok.*;

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
    private EntityStatus entityStatus;
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

    public LocalDateTime performedAction(){
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
                10,10,10,true,EntityType.USER,0L,EntityStatus.ACTIVE,null,null);
    }

    public  MudUser(Long id,String name, String password,StatsBasedEntityTemplate template, EntityStatus entityStatus){
        LocalDateTime time = LocalDateTime.of(2018,1,1,1,1);
//        time.toEpochSecond()
        this.setId(id);
        this.setName(name);
        this.setPassword(password);
        this.setMaxHP(template.getMaxHP());
        this.setHP(template.getHP());
        this.setSTR(template.getSTR());
        this.setDEX(template.getDEX());
        this.setCON(template.getCON());
        this.setRoomLocation(template.getRoom());
        this.entityStatus = entityStatus;

    }
    @Override
    public void setHP(int hp){
        if (hp <=0){
            this.HP = 0;
            this.setEntityStatus(EntityStatus.KO);

        }else if (hp > this.maxHP){
            this.HP = this.maxHP;

        }else {
            this.HP = hp;

        }

        if (this.HP >0)
            this.setEntityStatus(EntityStatus.ACTIVE);

    }

    @Override
    public boolean isStatsBased() {
        return true;
    }
}
