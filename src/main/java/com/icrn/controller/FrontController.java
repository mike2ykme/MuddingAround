package com.icrn.controller;

import com.icrn.exceptions.CannotPerformAction;
import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.*;
import com.icrn.service.StateHandler;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.swing.*;
import java.util.function.BiPredicate;

@Slf4j
@Data
@AllArgsConstructor
public class FrontController {
    private final StateHandler stateHandler;

    public Maybe<MudUser> maybeGetUser(String username, String password) {
        return Maybe.create(maybeEmitter -> {
            this.stateHandler.getEntityByName(username)
                    .filter(entity -> entity.getType() == EntityType.USER)
                    .map(entity -> (MudUser) entity)
                    .filter(mudUser -> mudUser.getPassword() == password)
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
        return Single.create(singleEmitter -> {
            this.stateHandler.getEntityById(mudUser.getRoomLocation())
                    .filter(entity -> entity.getType() == EntityType.ROOM)
                    .map(entity -> (Room)entity)
                    .subscribe(room -> {
                        if (room.allowsMovement(direction)){
                            mudUser.setRoomLocation(room.getRoomIdFromDirection(direction));
                            this.stateHandler.saveEntityState(mudUser)
                                    .subscribe(entity -> {
                                        singleEmitter.onSuccess(ActionResult.success("User was able to move in that direction", mudUser));
                                    },singleEmitter::onError);
                        }else {
                            singleEmitter.onSuccess(ActionResult.failure("User is unable to move in that direction", mudUser));
                        }
                    },singleEmitter::onError);
        });
    }

    public Single<ActionResult> handleCommands(String command, long userId) {
        return Single.create(singleEmitter -> {
            this.stateHandler.getEntityById(userId)
                    .filter(entity -> entity.getType() == EntityType.USER)
                    .map(entity -> (MudUser) entity)
                    .subscribe(user -> {
                        val parsedCommand = MudCommand.parse(command,user);
                        if (user.canPerformAction()
                                || parsedCommand.getType() == Actions.TALK
                                || parsedCommand.getType() == Actions.WHISPER ){
                            switch (parsedCommand.getType()){
                                case BADCOMMAND:
                                    singleEmitter.onSuccess(
                                        ActionResult.failure("BAD COMMAND. I'm sorry DAVE I can't do this",user)
                                    );
                                case ATTACK:
                                    break;
                                case DEFEND:
                                    break;
                                case WAIT:
                                    break;
                                case MOVE:
                                    this.handleUserMove(user,Movement.of(parsedCommand.getTarget().get()))
                                            .subscribe(singleEmitter::onSuccess,singleEmitter::onError);
                                case TALK:
                                    break;
                                case WHISPER:
                                    break;
                                default:
                                    singleEmitter.onSuccess(
                                            ActionResult.failure("I don't know what you're trying to do", user));
                            }
                        }

                    },throwable -> {
                        singleEmitter.onError(throwable);
                    });

        });
    }
}
