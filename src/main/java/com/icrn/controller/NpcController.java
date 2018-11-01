package com.icrn.controller;

import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.StateHandler;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.Map;
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
        return Completable.create(completableEmitter -> {
            log.info("Starting NPC Tick");

            this.spawnMonsters()
                    .subscribe(() -> {
                        log.debug("Finished round of spawning mobs");
                    },completableEmitter::onError);
            this.processMonstersInAllRooms()
                    .subscribe(() -> {
                        log.debug("Finished processing all mobs in all rooms tick");
                    },completableEmitter::onError);

           completableEmitter.onComplete();

           log.info("Finished processing NPC Tick");
        }).subscribeOn(Schedulers.single());
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
                                                                        log.debug("mob ID: " + mob.getId() + " retaliated against userID " + userId + " for previous attack");
                                                                    },completableEmitter::onError);
                                                        }
                                                    }else {
                                                        val random = new Random();
                                                        val rint = random.nextInt(99);
                                                        if (mob.getAggressionIndex() > rint){
                                                            log.debug("The aggression index is greater than random integer");
                                                            val user = entitiesInRoom.entrySet().stream()
                                                                    .filter(longEntityEntry -> longEntityEntry.getValue().getType() == EntityType.USER)
                                                                    .map(longEntityEntry -> (MudUser)longEntityEntry.getValue())
                                                                    .sorted((o1, o2) -> {
                                                                        return Integer.compare(o2.getSTR(),o1.getSTR());
                                                                    })
                                                                    .findFirst();

                                                            user.ifPresent(mudUser ->{
                                                                log.debug("User was present: " + mudUser.getId() + " == " + mudUser.getName());
                                                                this.processMonsterAttack(mob,mudUser)
                                                                        .subscribe(() -> {
                                                                            log.debug("mob ID " + mob.getId() + " attacked: " + mudUser.getId());
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
            log.debug("Done processing all entities in all Rooms for attacks");
            completableEmitter.onComplete();
        });
    }

    public Completable spawnMonsters() {
        log.debug("Spawning Monsters");
        return Completable.create(completableEmitter -> {
//            val count = this.stateHandler.getEntities().entrySet()
//                    .stream()
//                    .filter(longEntityEntry -> longEntityEntry.getValue().getType() == EntityType.ROOM)
//                    .map(Map.Entry::getValue)
//                    .map(entity -> (Room)entity)
//                    .count();

//            log.debug("COUNT: " + count);
            this.stateHandler.getAllRooms()
                .filter(room -> room.getMobInfo() != null)
                .subscribe(room -> {
                    val roomMobInfo = room.getMobInfo();
                    log.debug("inside getAllRooms() room id: " + room.getId() + " -- " + room.getName());
                    this.stateHandler.getAllEntitiesByRoom(room.getId())
                            .filter(entity ->  entity.getType() == EntityType.MOB)
                            .filter(entity -> ((Mob)entity).getEntityStatus() == EntityStatus.ACTIVE)
                            .count()
                            .subscribe(mobCount -> {
                                var currentCount = mobCount;
                                log.debug("mobCount: " + mobCount);
                                log.debug("ROOM MOB INFO: " + roomMobInfo.toString());
                                log.debug("ROOM: " + room.getName() + " EXPECTED MOB COUNT IS: " + roomMobInfo.getMobCount() + " CURRENT COUNT: " + currentCount);

                                while (currentCount < roomMobInfo.getMobCount()){
                                    log.debug("Room: " + room.getName() + " needs mobs generated");
                                    val random = new Random();

                                    Mob mob = Mob.fromTemplate(null,roomMobInfo.getMobName(),random.nextInt(110),roomMobInfo.getStatsBasedEntityTemplate());
                                    this.stateHandler.createNewEntity(mob)
                                            .subscribe(entity -> {
                                                log.debug("ENTITY GENERATED: " + entity.getName());
                                            },completableEmitter::onError);

                                    currentCount++;
                                }

                            },completableEmitter::onError);
                    log.debug("Done spawning mobs for: " + room.getName());
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
                        log.debug("Mob id: " +attacker.getId() + " attacked: " + dId + " -- " + dName);

                        this.stateHandler.saveEntityState(attacker,defender)
                                .subscribe(entity -> {
                                    log.debug("Entity " + entity.getName() + " -- " + entity.getId() + " was saved");
                                },completableEmitter::onError);

                        this.stateHandler.sendUserMessage(dId,attackResult.getMessageLogString())
                                .subscribe(() -> {
                                    log.debug("User ID: " + dId + " -- " + dName + " was sent a message after the attack");
                                },completableEmitter::onError);

                    },completableEmitter::onError);
            }
            completableEmitter.onComplete();
        });
    }
}
