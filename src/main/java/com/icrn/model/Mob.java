package com.icrn.model;

import lombok.Data;
import lombok.val;

import java.time.LocalDateTime;
import java.util.Optional;

@Data
public class Mob implements StatsBasedEntity {
    private Long id;
    private static int SECONDS_TO_COMPARE = 20;
    private String name;
    private LocalDateTime lastActionPerformedTime;
    private int maxHP;
    private int HP;
    private int STR;
    private int DEX;
    private int CON;
    private boolean online;
    private long roomLocation;
    private EntityStatus entityStatus;
    private Long lastAttackedById;
    private Actions lastCommand;
    private Integer aggressionIndex;

    public static Mob fromTemplate(Long id, String name,Integer aggressionIndex, StatsBasedEntityTemplate template)
    {
        val mob = new Mob(id,name);
        mob.setCON(template.getCON());
        mob.setDEX(template.getDEX());
        mob.setHP(template.getHP());
        mob.setMaxHP(template.getMaxHP());
        mob.setSTR(template.getSTR());
        mob.setRoomLocation(template.getRoom());
        return mob;
    }

    public Mob(Long id,String name){
        setId(id);
        setName(name);
    }

    public static Mob makeGlork(){
        val builder= StatsBasedEntityTemplate.builder();
        val template =
                builder.CON(10)
                .DEX(10)
                .HP(10)
                .maxHP(25)
                .STR(10)
                .room(100L)
                .build();
        return Mob.fromTemplate(55L,"Glork", 50,template);

    }
    @Override
    public Optional<Long> getLastAttackedById() {
        if (this.getLastAttackedById() == null)
            return Optional.of(this.lastAttackedById);
        else
            return Optional.empty();
    }

    @Override
    public Optional<Actions> getLastCommand() {
        return Optional.empty();
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public EntityType getType() {
        return EntityType.MOB;
    }


    public boolean canPerformAction() {
        if (lastActionPerformedTime
                .plusSeconds((SECONDS_TO_COMPARE/this.DEX))
                .isBefore(LocalDateTime.now())){
            return true;
        }
        return false;
    }

    public LocalDateTime performedAction(){
        val oldTime = this.lastActionPerformedTime;
        this.lastActionPerformedTime = LocalDateTime.now();
        return oldTime;
    }
}
