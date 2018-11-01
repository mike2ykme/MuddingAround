package com.icrn.controller;

import com.icrn.exceptions.CannotFindEntity;
import com.icrn.exceptions.CannotPerformAction;
import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.RestHandler;
import com.icrn.service.StateHandler;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Data
@AllArgsConstructor
public class FrontController {
    private final StateHandler stateHandler;
    private AttackHandler attackHandler;
    private RestHandler restHandler;

    public Maybe<MudUser> maybeGetUser(String username, String password) {
        return Maybe.create(maybeEmitter -> {
            this.stateHandler.getEntityByName(username)
                    .filter(entity -> entity.getType() == EntityType.USER)
                    .map(entity -> (MudUser) entity)
                    .filter(mudUser -> mudUser.getPassword().equals(password))
                    .subscribe(mudUser ->{
                            log.debug("maybeGetUser() was able to find user: " + mudUser.getName() + " for username: " + username);
                            maybeEmitter.onSuccess(mudUser);
                        },maybeEmitter::onError,() -> {
                            log.debug("maybeGetUser() completed for user: " + username );
                            maybeEmitter.onComplete();
                    });
            maybeEmitter.onComplete();
        });
    }

    public Single<ActionResult> registerUserOnline(MudUser mudUser, ChannelHandlerContext ctx) {
        return Single.create(singleEmitter -> {
            this.stateHandler.registerUserOnline(mudUser, ctx)
                    .subscribe(() ->
                            singleEmitter.onSuccess(ActionResult.success("User has been logged in",mudUser)),
                    throwable -> {
                            if (throwable instanceof NoUserToDisconnect){
                                singleEmitter.onSuccess(ActionResult.failure(throwable.getMessage(),mudUser));
                            }else {
                                singleEmitter.onError(throwable);
                            }
                    });
        });
    }

    public Single<ActionResult> registerUserOffline(MudUser mudUser) {
        return Single.create(singleEmitter -> {
            this.stateHandler.registerUserOffline(mudUser)
                    .subscribe(() ->
                            singleEmitter.onSuccess(ActionResult.success("Logout successful",mudUser))
                    ,throwable -> {
                            if (throwable instanceof NoUserToDisconnect){
                                singleEmitter.onSuccess(ActionResult.failure(throwable.getMessage(),mudUser));
                            }else {
                                singleEmitter.onError(throwable);
                            }
                    });
        });
    }

    public Single<ActionResult> handleUserMove(MudUser mudUser, Movement direction) {
        log.debug("inside handleUserMove()");
        log.debug("DIRECTION: " + direction.toString());
        return Single.create(singleEmitter -> {
            log.debug("Trying to find room from user's room location: " + mudUser.getRoomLocation());
            this.stateHandler.getEntityById(mudUser.getRoomLocation())
                    .map(entity -> (Room)entity)
                    .subscribe(room -> {
                        log.debug("Found room: " + room.getName());
                        if (room.allowsMovement(direction)){
                            log.debug("ROOM allows movement in this direction: " + direction);
                            mudUser.setRoomLocation(room.getRoomIdFromDirection(direction));
                            mudUser.performedAction();

                            this.stateHandler.saveEntityState(mudUser)
                                    .subscribe(entity -> {
                                        log.debug("user moved successfully and state was updated");
                                        singleEmitter.onSuccess(ActionResult.success("User was able to move in that direction", mudUser));
                                    },throwable -> {
                                        log.error("Unable to save the entity: " + mudUser.getName());
                                        singleEmitter.onSuccess(ActionResult.failure("Unable to update your character",mudUser));
                                    });

                        }else {
                            log.debug("ROOM does NOT allow movement in this direction: " + direction);
                            singleEmitter.onSuccess(ActionResult.failure("User is unable to move in that direction", mudUser));
                        }
                    },throwable -> {
                        log.error("Exeception received from getEntityById: " + throwable.getMessage());
                        singleEmitter.onSuccess(ActionResult.failure("You were unable to move, there is no room in that direction",mudUser));
                    });
        });
    }

    public Single<ActionResult> handleCommands(String command, long userId) {
        log.debug("Trying to handle command from userId: " + userId);
        return Single.<ActionResult>create(singleEmitter -> {
            this.stateHandler.getEntityById(userId)
                    .map(entity -> (MudUser) entity)
                    .subscribe( user-> {
                        log.debug("found user for command: " + user.getName());
                        val parsedCommand = MudCommand.parse(command,user);
                        log.debug("successfully parsed command from user");
                        user.setLastCommand(parsedCommand.getType());

                        val canUserPerformAction = user.canPerformAction();
                        if (!canUserPerformAction &&
                            parsedCommand.getType() != Actions.TALK &&
                            parsedCommand.getType() != Actions.WHISPER){
                            log.info("User can't perform action right now. User: " + user.getName());
                            singleEmitter.onSuccess(ActionResult.failure("You're too tired to act",user));
                        }else if (canUserPerformAction
                                || parsedCommand.getType() == Actions.TALK
                                || parsedCommand.getType() == Actions.WHISPER ){
                            log.debug("User can perform action");
                            if (parsedCommand.getType() != Actions.TALK &&
                            parsedCommand.getType() != Actions.WHISPER){
//                                user.setLastActionPerformedTime(LocalDateTime.now());
                                user.performedAction();
                            }

                            switch (parsedCommand.getType()){
                                case BADCOMMAND:
                                    singleEmitter.onSuccess(
                                        ActionResult.failure("BAD COMMAND. I'm sorry DAVE I can't do this",user)
                                    );
                                    break;
                                case ATTACK:
//                                    singleEmitter.onSuccess(ActionResult.failure("SORRY I'm NOT READY",user));
                                    this.handleUserAttack(user,parsedCommand)
                                            .subscribe(actionResult -> {
                                                singleEmitter.onSuccess(ActionResult.success(actionResult.getMessage(),user));
                                            },throwable -> {
                                                log.error(throwable.getMessage());
                                                singleEmitter.onSuccess(ActionResult.failure("Unable to attack other user",user));
                                            });
                                    break;
                                case DEFEND:
                                    this.handleUserDefend(user,parsedCommand)
                                            .subscribe(() -> {
                                                log.debug("able to defend");
                                                singleEmitter.onSuccess(ActionResult.success("You have defended",user));
                                            },throwable ->{
                                                log.error(throwable.getMessage());
                                                singleEmitter.onSuccess(ActionResult.failure("You were unable to defend",user));
                                            });
                                    break;
                                case REST:
                                    log.debug("REST reached");
                                    this.handUserRest(user,parsedCommand)
                                            .subscribe(singleEmitter::onSuccess
                                                    ,throwable -> {
                                                log.error("Error when trying to rest");
                                                singleEmitter.onSuccess(
                                                        ActionResult.failure("You were unable to rest",user));
                                            });
                                    break;
                                case MOVE:
                                    try {
                                        log.debug("TARGET DIRECTION: " + parsedCommand.getTarget().get());
                                        this.handleUserMove(user,Movement.of(parsedCommand.getTarget().get()))
                                                .subscribe(singleEmitter::onSuccess,throwable -> {
                                                    log.error("Error received from handleUserMove(): " +
                                                            throwable.getMessage());
                                                    singleEmitter.onSuccess(ActionResult.failure("Unable to handle move",user));
                                                });
                                    } catch (Exception e) {
                                        log.error("Exception caught, probably due to bad direction being given" +
                                                e.getMessage());
                                        singleEmitter.onSuccess(ActionResult.failure("Unable to move in that direction",user));
                                    }
                                    break;
                                case TALK:
                                    log.debug(user.getName() + "sent command TALK");
                                    this.handleUserTalk(user,parsedCommand.getTarget())
                                            .subscribe(singleEmitter::onSuccess,singleEmitter::onError);
                                    break;
                                case WHISPER:
                                    log.debug(user.getName() + "sent command WHISPER");
                                    this.handleUserWhisper(user,parsedCommand.getTarget())
                                            .subscribe(singleEmitter::onSuccess,throwable ->{
                                                log.error(throwable.getMessage());
                                                singleEmitter.onSuccess(ActionResult.failure("Unable to whisper",user));
                                            });

                                    break;
                                case STATUS:
                                    log.debug("The STATUS branch");
                                    singleEmitter.onSuccess(ActionResult.success(user.toString(),user));
                                    break;
                                default:
                                    singleEmitter.onSuccess(
                                            ActionResult.failure("I don't know what you're trying to do", user));
                            }
                        }

                    },throwable -> {
                        singleEmitter.onError(throwable);
                    });
        }).subscribeOn(Schedulers.single());
    }

    private Single<ActionResult> handUserRest(MudUser user, MudCommand parsedCommand) {
        log.debug("inside handleUserRest()");
        log.debug("target: " + parsedCommand.getTarget());
        log.debug("CMD: " + parsedCommand.getType() + " target: " + parsedCommand.getTarget().get());
        return Single.create(singleEmitter -> {
            this.restHandler.restStatsEntity(user)
                    .subscribe(statsBasedEntity -> {
                        log.debug(statsBasedEntity.toString());
                        this.stateHandler.saveEntityState(statsBasedEntity)
                                .subscribe(entity -> {
                                    singleEmitter.onSuccess(
                                        ActionResult.success("You have rested",user));
                                },singleEmitter::onError);
                    },singleEmitter::onError);
        });
    }

    private Completable handleUserDefend(MudUser user, MudCommand parsedCommand) {
        System.out.println(user.getLastCommand().isPresent());
        System.out.println(user.getLastCommand().get());
        return Completable.create(completableEmitter -> {
           this.stateHandler.saveEntityState(user)
                .subscribe(entity -> {
                    log.debug(entity.getName() + " state has been saved");
                    completableEmitter.onComplete();
                },completableEmitter::onError);
        });
    }

    private Single<ActionResult> handleUserAttack(MudUser attacker, MudCommand parsedCommand) {
        log.debug("Inside the handleUserAttack() function");
        return Single.create(singleEmitter -> {
            log.debug(parsedCommand.toString());

            if (!parsedCommand.getTarget().isPresent()) {
                singleEmitter.onError(CannotPerformAction.of("No target to perform action"));

            }else {
                val defenderName = parsedCommand.getTarget().get();

                log.debug("The target was present");
                log.debug("Trying to get Entity: " + defenderName);

                this.stateHandler.getEntityByName(defenderName)
                    .filter(entity -> entity instanceof StatsBasedEntity)
                    .map(entity -> (StatsBasedEntity) entity)
                    .filter(StatsBasedEntity::isOnline)
                    .subscribe(defender -> {
                        log.debug("found entity matching: " + defender.getName());
                        log.debug("Trying to use the attackHandler processAttack()");
                        this.attackHandler.processAttack(attacker,defender)
                            .subscribe(attackResult -> {
                                log.debug("Now trying to attack with attacker: " + attacker.getName() + " defender: " + defenderName);
                                val strings = String.join("\n",attackResult.getMessageLog());

                                this.stateHandler.saveEntityState(attackResult.getAttacker(), attackResult.getDefender())
                                    .subscribe(savedEntity -> {
                                        log.debug("Successfully saved: " + savedEntity.getName());
                                        log.debug(strings);
                                        System.out.println(strings);

                                        if (savedEntity instanceof MudUser){
                                            if (savedEntity.getId() != attacker.getId()) {
                                                this.stateHandler.sendUserMessage(savedEntity.getId(), strings)
                                                    .subscribe(() -> {
                                                        log.debug("User " + savedEntity.getName() + " was sent message");
                                                    }, throwable -> log.error(throwable.getMessage()));
                                            }
                                        }
                                    },singleEmitter::onError
                                    ,() -> {
                                            singleEmitter.onSuccess(ActionResult.success(strings,attacker));
                                    });
                            },singleEmitter::onError);

                    },singleEmitter::onError
                    ,() -> singleEmitter.onError(CannotFindEntity.foundNone(defenderName)));
            }
        });
    }

    private Single<ActionResult> handleUserWhisper(MudUser user, Optional<String> message) {
        log.debug("Inside handleUserWhisper()");
        if (user == null || message == null)
            throw new IllegalArgumentException("arguments are null");

        val username = user.getName();

        if (!message.isPresent()){
            log.debug(username + " did not send anything to say");
            return Single.just(ActionResult.failure("Must have something to say",user));

        }else {
            val msgContents =  Arrays.asList(message.get().split("\\s+"));

            return Single.create(singleEmitter -> {
                this.stateHandler.getEntityByName(msgContents.get(0))
                        .subscribe(entity -> {
                            if (entity.getType() == EntityType.USER){

                                StringBuilder builder = new StringBuilder();
                                for (int i =1; i <msgContents.size(); i++){
                                    builder.append(msgContents.get(i) + " ");

                                }
                                System.out.println(builder.toString());
                                this.stateHandler.sendUserMessage((MudUser)entity,username + "<WHISPERS>: " +builder.toString().trim())
                                        .subscribe(() ->{
                                            singleEmitter.onSuccess(ActionResult.success("You were able to whisper to " + entity.getName(),user));
                                        } ,throwable -> singleEmitter.onError(throwable));

                            }else {
                                log.warn("We can find an entity by name, but it's not a user so we can't talk to it.");
                                singleEmitter.onSuccess(
                                        ActionResult.failure("You can't talk to " + entity.getName(),user));

                            }
                        },throwable -> {
                            log.error("Received an error trying to get the Entity by name " + throwable.getMessage());
//                            singleEmitter.onSuccess(ActionResult.failure("Unable to find the target",user));
                            singleEmitter.onError(throwable);

                        },() -> {
                            log.debug("We didn't find a user, but it ran to completion");
                            singleEmitter.onSuccess(ActionResult.failure("Unable to whisper, maybe your friend isn't online", user));

                        });
            });
        }
    }

    private Single<ActionResult> handleUserTalk(MudUser user, Optional<String> message) {
        log.debug("Inside handleUserTalk()");
        val username = user.getName();
        if (!message.isPresent()){
            log.debug(username + " did not send anything to say");
            return Single.just(ActionResult.failure("Must have something to say",user));
        }
        else {
            return Single.create(singleEmitter -> {
                this.stateHandler.getAllEntitiesByRoom(user.getRoomLocation())
                        .filter(entity -> entity.getType() == EntityType.USER)
                        .filter(entity -> ((MudUser)entity).isOnline())
                        .map(entity -> entity.getId())
                        .subscribe(userId -> {
                            log.debug("FOUND userId: " + userId + " in room: " + user.getRoomLocation());
                            val messageToSend = user.getName() + ": " +message.get();
                            this.stateHandler.sendUserMessage(userId, messageToSend)
                                    .subscribe(() ->{
                                        log.debug( username + "was able to talk to userID: " + userId);
                                    } ,singleEmitter::onError);
                        });
                singleEmitter.onSuccess(ActionResult.success("You talked to everyone in the room",user));
            });
        }
    }

}
