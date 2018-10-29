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
            this.processMonstersInAllRooms()
                    .subscribe(() -> {
                        log.info("Finished processing all mobs in all rooms tick");
                    },completableEmitter::onError);

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
                                .subscribe(entitiesInRoom -> {
                                    log.debug("Inside getRoomEntityMap for room ID: " + room.getId());

                                        entitiesInRoom.entrySet().stream()
                                                .filter(longEntityEntry -> longEntityEntry.getValue().getType() == EntityType.MOB)
                                                .map(longEntityEntry -> (Mob)longEntityEntry.getValue())
                                                .filter(Mob::canPerformAction)
                                                .forEach(mob -> {

                                                    if (mob.getLastAttackedById().isPresent()){
                                                        val userId = mob.getLastAttackedById().get();

                                                        if (entitiesInRoom.containsKey(userId)) {
                                                            this.processMonsterAttack(mob,(StatsBasedEntity)entitiesInRoom.get(userId))
                                                                    .subscribe(() -> {
                                                                        log.info("mob ID: " + mob.getId() + " retaliated against userID " + userId + " for previous attack");
                                                                    },completableEmitter::onError);
                                                        }
                                                    }else {
                                                        val random = new Random();
                                                        val rint = random.nextInt(99);
                                                        if (mob.getAggressionIndex() > rint){
                                                            log.info("The aggression index is greater than random integer");
                                                            val user = entitiesInRoom.entrySet().stream()
                                                                    .filter(longEntityEntry -> longEntityEntry.getValue().getType() == EntityType.USER)
                                                                    .map(longEntityEntry -> (MudUser)longEntityEntry.getValue())
                                                                    .sorted((o1, o2) -> {
                                                                        return Integer.compare(o2.getSTR(),o1.getSTR());
                                                                    })
                                                                    .findFirst();

                                                            user.ifPresent(mudUser ->{
                                                                log.info("User was present: " + mudUser.getId() + " == " + mudUser.getName());
                                                                this.processMonsterAttack(mob,mudUser)
                                                                        .subscribe(() -> {
                                                                            log.info("mob ID " + mob.getId() + " attacked: " + mudUser.getId());
                                                                        },completableEmitter::onError);
                                                            });
                                                        }
                                                    }
                                                    mob.performedAction();
                                                });

                                },completableEmitter::onError);

                    },throwable -> {
                        throw new RuntimeException(throwable);
                    });
            log.info("Done processing all entities in all Rooms for attacks");
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
                            .filter(entity -> ((Mob)entity).getEntityStatus() == EntityStatus.ACTIVE)
                            .count()
                            .subscribe(mobCount -> {
                                var currentCount = mobCount;
                                log.info("ROOM: " + room.getName() + " EXPECTED MOB COUNT IS: " + mobCount + " CURRENT COUNT: " + currentCount);

                                while (currentCount < roomMobInfo.getMobCount()){
                                    log.info("Room: " + room.getName() + " needs mobs generated");
                                    val random = new Random();

                                    Mob mob = Mob.fromTemplate(null,roomMobInfo.getMobName,random.nextInt(110),roomMobInfo.getStatsBasedEntityTemplate());
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
