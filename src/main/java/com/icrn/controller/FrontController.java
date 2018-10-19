package com.icrn.controller;

import com.icrn.exceptions.CannotPerformAction;
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

    public Completable registerUserOnline(MudUser mudUser, ChannelHandlerContext ctx) {
        return this.stateHandler.registerUserOnline(mudUser,ctx);
    }

    public Completable registerUserOffline(MudUser mudUser) {
        return this.stateHandler.registerUserOffline(mudUser);
    }

    public Single<ActionResult> singleMoveUser(MudUser mudUser, Movement direction) {
        return Single.create(singleEmitter -> {
            this.stateHandler.getEntityById(mudUser.getRoomLocation())
                    .filter(entity -> entity.getType() == EntityType.ROOM)
                    .map(entity -> (Room)entity)
                    .subscribe(room -> {
                        if (room.allowsMovement(direction)){
                            mudUser.setRoomLocation(room.getRoomIdFromDirection(direction));
                            this.stateHandler.saveEntityState(mudUser)
                                    .subscribe(entity -> {
                                        singleEmitter.onSuccess(ActionResult.success("User was able to move in that direction"));
                                    },singleEmitter::onError);
                        }else {
                            singleEmitter.onSuccess(ActionResult.failure("User is unable to move in that direction"));
                        }
                    },singleEmitter::onError);
        });
    }
}
