package com.icrn.Controller;

import com.icrn.dao.EntityDao;
import com.icrn.model.*;
import com.icrn.service.StateHandler;
import io.reactivex.Completable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class Mudder {
//    @NonNull final private EntityDao entityDao;
    @NonNull final private StateHandler stateHandler;

    public static Single<MudUser> maybeGetUser(String username, String password){
        return Single.create(singleEmitter ->{
            if ("mike".equalsIgnoreCase(username) && "password".equalsIgnoreCase(password)){
                singleEmitter.onSuccess(MudUser.makeJoe());

            } else {
                singleEmitter.onError(new RuntimeException("Unable to find user"));

            }
        });
    }

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
        return null;
//        return Single.create(singleEmitter -> {
//           user.performAction();
//           stateHandler.getAllEntitiesByRoom(user.getRoomLocation())
//                   .fi
//
//
//           stateHandler.saveEntityState(user).subscribe(entity -> {
//               singleEmitter.onSuccess(MudResult.noActionSuccess("You attacked " + cmd.getTarget().get()));
//           },singleEmitter::onError);
//        });
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

        if (user.canPerformAction()) {
            switch (cmd.getType()) {
                case ATTACK:
                    return this.handleAttack(user,cmd);

                case DEFEND:
                    return this.handleDefend(user,cmd);

                case WAIT:
                    return this.handleWait(user,cmd);

                case MOVE:
                    return this.handleMove(user,cmd);

                default:
                    return Single.create(singleEmitter ->
                            singleEmitter.onError(new RuntimeException("Unable to find command")));

            }
        }else {
            return Single.create(singleEmitter ->
                    singleEmitter.onSuccess(MudResult.noActionFailure("You are too tired to " + cmd.getType())));

        }
    }


}
