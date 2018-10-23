package com.icrn.controller;

import com.icrn.exceptions.CannotFindEntity;
import com.icrn.exceptions.CannotPerformAction;
import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.StateHandler;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Maybe;
import io.reactivex.Single;
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

    public Maybe<MudUser> maybeGetUser(String username, String password) {
        return Maybe.create(maybeEmitter -> {
            this.stateHandler.getEntityByName(username)
                    .filter(entity -> entity.getType() == EntityType.USER)
                    .map(entity -> (MudUser) entity)
                    .filter(mudUser -> mudUser.getPassword().equals(password))
                    .subscribe(mudUser ->{
                            log.info("maybeGetUser() was able to find user: " + mudUser.getName() + " for username: " + username);
                            maybeEmitter.onSuccess(mudUser);
                        },maybeEmitter::onError,() -> {
                            log.info("maybeGetUser() completed for user: " + username );
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
        log.info("inside handleUserMove()");
        log.info("DIRECTION: " + direction.toString());
        return Single.create(singleEmitter -> {
            log.info("Trying to find room from user's room location: " + mudUser.getRoomLocation());
            this.stateHandler.getEntityById(mudUser.getRoomLocation())
                    .map(entity -> (Room)entity)
                    .subscribe(room -> {
                        log.info("Found room: " + room.getName());
                        if (room.allowsMovement(direction)){
                            log.info("ROOM allows movement in this direction: " + direction);
                            mudUser.setRoomLocation(room.getRoomIdFromDirection(direction));
                            mudUser.performAction();

                            this.stateHandler.saveEntityState(mudUser)
                                    .subscribe(entity -> {
                                        log.info("user moved successfully and state was updated");
                                        singleEmitter.onSuccess(ActionResult.success("User was able to move in that direction", mudUser));
                                    },throwable -> {
                                        log.error("Unable to save the entity: " + mudUser.getName());
                                        singleEmitter.onSuccess(ActionResult.failure("Unable to update your character",mudUser));
                                    });

                        }else {
                            log.info("ROOM does NOT allow movement in this direction: " + direction);
                            singleEmitter.onSuccess(ActionResult.failure("User is unable to move in that direction", mudUser));
                        }
                    },throwable -> {
                        log.error("Exeception received from getEntityById: " + throwable.getMessage());
                        singleEmitter.onSuccess(ActionResult.failure("You were unable to move, there is no room in that direction",mudUser));
                    });
        });
    }

    public Single<ActionResult> handleCommands(String command, long userId) {
        log.info("Trying to handle command from userId: " + userId);
        return Single.<ActionResult>create(singleEmitter -> {
            this.stateHandler.getEntityById(userId)
                    .map(entity -> (MudUser) entity)
                    .subscribe( user-> {
                        log.info("found user for command: " + user.getName());
                        val parsedCommand = MudCommand.parse(command,user);
                        log.debug("successfully parsed command from user");
                        user.setLastCommand(parsedCommand.getType());
                        if (user.canPerformAction()
                                || parsedCommand.getType() == Actions.TALK
                                || parsedCommand.getType() == Actions.WHISPER ){
                            user.setLastActionPerformedTime(LocalDateTime.now());
                            switch (parsedCommand.getType()){
                                case BADCOMMAND:
                                    singleEmitter.onSuccess(
                                        ActionResult.failure("BAD COMMAND. I'm sorry DAVE I can't do this",user)
                                    );
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
//                                case DEFEND:
//                                    break;
//                                case WAIT:
//                                    break;
                                case MOVE:
                                    try {
                                        log.info("TARGET DIRECTION: " + parsedCommand.getTarget().get());
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
                                default:
                                    singleEmitter.onSuccess(
                                            ActionResult.failure("I don't know what you're trying to do", user));
                            }
                        }

                    },throwable -> {
                        singleEmitter.onError(throwable);
                    });

//        }).subscribeOn(Schedulers.io());
        });
    }

    private Single<ActionResult> handleUserAttack(MudUser user, MudCommand parsedCommand) {
        log.debug("Inside the handleUserAttack() function");
        return Single.create(singleEmitter -> {
            log.debug(parsedCommand.toString());
                if (parsedCommand.getTarget().isPresent()){
                    log.debug("The target was present");
                    this.stateHandler.getEntityByName(parsedCommand.getTarget().get())
                            .subscribe(entity -> {
                                log.debug("found entity matching: " + entity.getName());
                                if (entity.getType() == EntityType.USER){
                                    val defender = (StatsBasedEntity)entity;
                                    this.attackHandler.processAttack(user,defender)
                                            .subscribe(attackResult -> {
                                                this.stateHandler.saveEntityState(attackResult.getAttacker(),attackResult.getDefender())
                                                        .subscribe(savedEntity -> {
                                                                log.info("Successfully saved: " + savedEntity.getName());
                                                        },
                                                                singleEmitter::onError
                                                        ,() -> {
                                                                singleEmitter.onSuccess(
                                                                        ActionResult.success(
                                                                                String.join("\n",attackResult.getMessageLog())
                                                                                ,user));
                                                        });
                                            },throwable -> {
                                                log.error(throwable.getMessage());
                                                singleEmitter.onError(CannotPerformAction.of(throwable.getMessage()));
                                            });

                                }else {
                                    singleEmitter.onError(CannotPerformAction.of("Unable to attack that: " + entity.getName()));
                                }

                            },throwable -> {
                                singleEmitter.onError(CannotFindEntity.foundNone());
                            });

                }else {
                    singleEmitter.onError(CannotPerformAction.of("No target to perform action"));
                }
        });
    }

    private Single<ActionResult> handleUserWhisper(MudUser user, Optional<String> message) {
        log.debug("Inside handleUserWhisper()");
        if (user == null || message == null)
            throw new IllegalArgumentException("arguments are null");

        val username = user.getName();

        if (!message.isPresent()){
            log.info(username + " did not send anything to say");
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
                                this.stateHandler.sendUserMessage((MudUser)entity,username + ": " +builder.toString().trim())
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
                            log.info("We didn't find a user, but it ran to completion");
                            singleEmitter.onSuccess(ActionResult.failure("Unable to whisper", user));

                        });
            });
        }
    }

    private Single<ActionResult> handleUserTalk(MudUser user, Optional<String> message) {
        log.debug("Inside handleUserTalk()");
        val username = user.getName();
        if (!message.isPresent()){
            log.info(username + " did not send anything to say");
            return Single.just(ActionResult.failure("Must have something to say",user));
        }
        else {
            return Single.create(singleEmitter -> {
                this.stateHandler.getAllEntitiesByRoom(user.getRoomLocation())
                        .filter(entity -> entity.getType() == EntityType.USER)
                        .filter(entity -> ((MudUser)entity).isOnline())
                        .map(entity -> entity.getId())
                        .subscribe(userId -> {
                            log.info("FOUND userId: " + userId + " in room: " + user.getRoomLocation());
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
