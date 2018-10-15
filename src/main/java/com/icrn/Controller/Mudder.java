package com.icrn.Controller;

import com.icrn.dao.EntityDao;
import com.icrn.model.*;
import com.icrn.service.StateHandler;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Getter
@AllArgsConstructor
@Slf4j
public class Mudder {
//    @NonNull final private EntityDao entityDao;
    @NonNull final private StateHandler stateHandler;

    public static Single<MudUser> maybeGetUser(String username, String password){
        log.debug("Trying to get user with username: " + username);
//        return Single.just(MudUser.makeJoe());
        System.out.println("\tusername passed:\n" + username + "\n\tPassword passed:\n" + password);
        return Single.<MudUser>create(singleEmitter ->{
            if ("mike".equalsIgnoreCase(username) && "password".equalsIgnoreCase(password)){
                singleEmitter.onSuccess(MudUser.makeJoe());

            } else {
                singleEmitter.onError(new RuntimeException("Unable to find user"));

            }
        }).subscribeOn(Schedulers.io());
    }

    public static Completable maybeRegisterUser(MudUser mudUser, Consumer<String> f) {
        return Completable.complete();
    }

//    public static Completable maybeRegisterUser(MudUser mudUser) {
//        return Completable.complete();
//    }

    private Single<MudResult> handleMove(MudUser user, MudCommand cmd){
        return Single.create(singleEmitter -> {
            if (cmd.getTarget().isPresent()){ // If there is a direction to go to
                stateHandler.getRoomById(user.getRoomLocation()) // if we can find the room ID
                        .subscribe(room -> { // We should always have a room since the user can't exist 'nowhere'
                            String direction = cmd.getTarget().get();  // Where are we going
                            Movement movement = MovementDirection.of(direction);    //Wherre we're going in movment form
                            if (room.allowsMovement(movement)){ // If we can go there
                                user.setRoomLocation(room.getRoomFromDirection(movement));
                                user.performAction();
                                stateHandler.saveEntityState(user);
                                singleEmitter.onSuccess(MudResult.noActionSuccess("You have moved"));

                            }else {
                                singleEmitter.onSuccess(MudResult.noActionFailure("There is no room in that direction"));

                            }
                        },singleEmitter::onError);

            }else {
                singleEmitter.onSuccess(MudResult.noActionFailure("No direction specified"));

            }
        });
    }

    private Single<MudResult> handleAttack(MudUser user, MudCommand cmd){
        log.debug("HandleAttack() called. user:\n" + user.toString() +"\nCMD:\n"+cmd.toString());
        return Single.create(singleEmitter -> {
           user.performAction();
           log.info(user.getName() + " has had performAction() called");
           cmd.getTarget().ifPresent(otherUserName->{
               this.stateHandler.getEntityByName(otherUserName)
                       .subscribe(entity -> {
                           if (entity.getRoomLocation() == user.getRoomLocation()) {
                               log.info("other user existed and they were in the same room");
                               try {
                                   int dmg = user.attack(entity);
                                   this.stateHandler.saveEntityState(entity).blockingGet();

                                   singleEmitter.onSuccess(MudResult
                                           .noActionSuccess("You did "+ dmg + " damage"));

                               } catch (Exception e) {
                                   singleEmitter.onError(e);
                               }
                           } else {
                               log.info("other user was not in the same room. Other user: " + entity.getName());
                               singleEmitter.onSuccess(MudResult
                                       .noActionFailure("You're not in the same room as " + entity.getName()));

                           }
                       },singleEmitter::onError);

           });

           this.stateHandler.saveEntityState(user);
        });
    }

    private Single<MudResult> handleWait(MudUser user, MudCommand cmd){
        return Single.create(singleEmitter -> {
            user.rest();
            this.stateHandler.saveEntityState(user)
                    .subscribe(entity -> {
                        singleEmitter.onSuccess(MudResult.noActionSuccess(user.getName() + " was able to rest successfully"));
                    },singleEmitter::onError);

        });
    }

    private Single<MudResult> handleDefend(MudUser user, MudCommand cmd) {
        return null;
    }

    public Single<MudResult> HandleAction(MudCommand cmd){
        this.stateHandler.getAllOnlineEntities()
                .subscribe(entity -> log.debug(entity.toString()));

        log.debug("ID of requestor: " + cmd.getRequester().getId());
        MudUser user   = this.stateHandler.getUserById(cmd.getRequester().getId())
                .blockingGet();

        if (user.canPerformAction()
                || cmd.getType() == Actions.TALK
                || cmd.getType() == Actions.WHISPER ) {
            switch (cmd.getType()) {
                case ATTACK:
                    return this.handleAttack(user,cmd);

                case DEFEND:
                    return this.handleDefend(user,cmd);

                case WAIT:
                    return this.handleWait(user,cmd);

                case MOVE:
                    return this.handleMove(user,cmd);

                case TALK:
                    return this.handleTalk(user,cmd);

                case WHISPER:
                    return this.handleWhisper(user,cmd);

                default:
                    return Single.create(singleEmitter ->
                            singleEmitter.onError(new RuntimeException("Unable to find command")));

            }
        }else {
            return Single.create(singleEmitter ->
                    singleEmitter.onSuccess(MudResult.noActionFailure("You are too tired to " + cmd.getType())));

        }
    }

    private Single<MudResult> handleWhisper(MudUser user, MudCommand cmd) {
        return null;
    }

    private Single<MudResult> handleTalk(MudUser user, MudCommand cmd) {
        return Single.create(singleEmitter -> {
            if (cmd.getTarget().isPresent()) {
                String message = cmd.getTarget().get();

                this.stateHandler.getAllEntitiesByRoom(user.getRoomLocation())
                        .filter(entity -> entity.getType() == EntityType.USER)
                        .map(entity -> (MudUser) entity)
                        .subscribe(userInRoom -> {
                            this.stateHandler.sendUserMessage(userInRoom, message)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(aBoolean ->{
                                            String output = user.getName() + " said " + message + "to " + userInRoom;
                                            System.out.println(output);
                                            log.debug(output);
                                            },singleEmitter::onError);
                        }, singleEmitter::onError);

            } else {
                String msg = "Nothing said to everyone in the room";
                System.out.println(msg);
                log.debug(msg);
                singleEmitter.onSuccess(MudResult.noActionSuccess(msg));
            }
        });
    }
}
