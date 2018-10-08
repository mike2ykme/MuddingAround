package com.icrn.model;

import com.icrn.exceptions.TO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@ToString
public class MudUser implements Entity {
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


    public LocalDateTime performAction(){
        this.lastActionPerformedTime = LocalDateTime.now();
        return lastActionPerformedTime;
    }
    public boolean canPerformAction() {
        int lastDoY = lastActionPerformedTime.getDayOfYear();
        int nowDoY = LocalDateTime.now().getDayOfYear();

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
                10,10,10,true,EntityType.USER,0L,UserStatus.ACTIVE);
    }

    public void setHP(int hp){
        if (hp <=0){
            this.HP = 0;
            this.setUserStatus(UserStatus.KO);
        }else {
            this.HP = hp;
        }
    }
    public int attack(Entity entity) {
//        TO.DO();
//        TODO
        entity.setHP(entity.getHP()-this.getSTR());
        return this.getSTR();
    }

//    public void setStatus(UserStatus status) {
//        this.status = status;
//    }
//
//    public UserStatus getStatus() {
//        return status;
//    }
}
