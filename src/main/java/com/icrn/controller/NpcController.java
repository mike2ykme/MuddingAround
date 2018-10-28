package com.icrn.controller;

import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.StateHandler;
import io.reactivex.Completable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.List;
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
            this.processMonstersInAllRooms();

           completableEmitter.onComplete();
        });
    }

    public Completable processMonstersInAllRooms(){
        return Completable.create(completableEmitter ->{

            log.debug("Processing monsters in all rooms");
            stateHandler.getAllRooms()
                    .filter(room -> !room.isSafeZone())
                    .subscribe(room -> {
                        log.debug("In NON-SAFE ROOM: " + room.getName() + " -- " + room.getId());

                        stateHandler.getRoomEntityMap(room.getId())
                                .subscribe(longEntityMap -> {

                                },completableEmitter::onError);

//                        stateHandler.getAllEntitiesByRoom(room.getId())
//                                .filter(entity -> entity.getType() == EntityType.MOB)
//                                .map(entity -> (Mob)entity)
//                                .subscribe(mob -> {
//                                    log.info("mob is considering attacking: " + mob.getId() + " -- " + mob.getName());
//                                    val random = new Random();
//                                    val randomCompare = random.nextInt(99);
//
//                                    if (mob.getAggressionIndex() >= randomCompare){ //Do we need to actually attack anyone
//                                        if (mob.getLastAttackedById().isPresent()){
//                                            val user = this.stateHandler.getEntityById(mob.getLastAttackedById().get()).blockingGet();
//                                            if (room.getId() == user.getId()){
//
//                                            }
//                                        }
////                                        stateHandler.getRandomUserByRoom(room.getId())
////                                                .subscribe()
//                                    }
//
//                                },throwable ->{
//                                    throw new RuntimeException(throwable);
//                                });
                    },throwable -> {
                        throw new RuntimeException(throwable);
                    });
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
                            .filter(entity -> ((Mob)entity).getEntityStatus() == EntityStatus.ACTIVE)
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
