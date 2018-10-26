package com.icrn.controller;

import com.icrn.model.EntityType;
import com.icrn.model.Mob;
import com.icrn.model.StatsBasedEntity;
import com.icrn.service.AttackHandler;
import com.icrn.service.StateHandler;
import io.reactivex.Completable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.Random;

@Slf4j
@Data
public class NpcController {
    private StateHandler stateHandler;
    private AttackHandler attackHandler;

    public NpcController(StateHandler stateHandler, AttackHandler attackHandler) {
        this.stateHandler = stateHandler;
        this.attackHandler = attackHandler;

    }

    public Completable processTick(){
//        return Completable.complete();
        return Completable.create(completableEmitter -> {
           this.spawnMonsters();



           completableEmitter.onComplete();
        });
    }

    public Completable spawnMonsters() {
        log.debug("Spawning Monsters");
        return Completable.create(completableEmitter -> {
            this.stateHandler.getAllRooms()
                .subscribe(room -> {
                    val roomMobInfo = room.getMobInfo();

                    this.stateHandler.getAllEntitiesByRoom(room.getId())
                            .filter(entity ->  entity.getType() == EntityType.MOB)
                            .count()
                            .subscribe(mobCount -> {
                                var currentCount = mobCount;
                                log.info("ROOM: " + room.getName() + " EXPECTED MOB COUNT IS: " + mobCount + " CURRENT COUNT: " + currentCount);

                                while (currentCount < roomMobInfo.getMobCount()){
                                    log.info("Room: " + room.getName() + " needs mobs generated");
                                    val random = new Random();

                                    Mob mob = Mob.fromTemplate(null,roomMobInfo.getMobName,random.nextInt(110),roomMobInfo.getTemplate());
                                    this.stateHandler.createNewEntity(mob)
                                            .subscribe(entity -> {
                                                log.info("ENTITY GENERATED: " + entity.getName());
                                            },completableEmitter::onError);

                                    currentCount++;
                                }

                            },completableEmitter::onError);
                    log.info("Done spawning mobs for: " + room.getName());
                    completableEmitter.onComplete();
                },completableEmitter::onError);

        });
    }

    public Completable processMonsterAttack(Mob attacker, StatsBasedEntity defender) {
        return Completable.create(completableEmitter -> {
            if (!attacker.canPerformAction())
                completableEmitter.onComplete();
            else {
                val dName= defender.getName();
                val dId = defender.getId();

                attacker.performedAction();
                this.attackHandler.processAttack(attacker,defender)
                    .subscribe(attackResult -> {
                        log.info("Mob id: " +attacker.getId() + " attacked: " + dId + " -- " + dName);

                        this.stateHandler.saveEntityState(attacker,defender)
                                .subscribe(entity -> {
                                    log.info("Entity " + entity.getName() + " -- " + entity.getId() + " was saved");
                                },completableEmitter::onError);

                        this.stateHandler.sendUserMessage(dId,attackResult.getMessageLogString())
                                .subscribe(() -> {
                                    log.info("User ID: " + dId + " -- " + dName + " was sent a message after the attack");
                                },completableEmitter::onError);

                    },completableEmitter::onError);
            }
            completableEmitter.onComplete();
        });
    }
}
