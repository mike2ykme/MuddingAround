package com.icrn.service;

import com.icrn.dao.EntityDao;
import com.icrn.exceptions.CannotFindUser;
import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.Entity;
import com.icrn.model.EntityType;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


@Slf4j @Data
public class StateHandler {
    @NonNull Map<Long,Entity> entities;
    private final HashMap<Long,ChannelHandlerContext> communicationMap = new HashMap<>();
    EntityDao entityDao = null;

    public StateHandler(Map<Long,Entity> entities){
        this.entities = entities;
        log.info("Entities assigned from HashMap<>()");

    }

    public StateHandler(EntityDao entityDao){
        this.entityDao = entityDao;
        this.entities = new ConcurrentHashMap<>();

        this.entityDao.getAllEntities()
                .subscribe(entity -> {
                    this.entities.put(entity.getId(),entity);

                },
                    throwable -> {
                    throw new RuntimeException(throwable);

                },
                    () -> log.info("All Entities loaded from EntityDao")

                );

    }

    public Observable<Entity> getAllEntities(){
        return Observable.create(observableEmitter -> {
            this.entities.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
    }
    public Observable<Entity> getAllOnlineEntities() {
//        return Observable.create(observableEmitter -> {
//            entities.entrySet().stream()
//                .map(Map.Entry::getValue)
//                .filter(Entity::isOnline)
//            .forEach(observableEmitter::onNext);
//
//            observableEmitter.onComplete();
//        });
//        return this.getAllEntities()
//                .filter(Entity::isOnline);
        return null;
    }

    public Single<Entity> saveEntityState(Entity entity){
        return Single.create(singleEmitter -> {
            entities.put(entity.getId(),entity);
            singleEmitter.onSuccess(entity);
        });
    }

    public Observable<Entity> saveEntityState(Entity... entities){
        return Observable.create(observableEmitter -> {
            for (Entity e: entities){
                if (observableEmitter.isDisposed())
                    return;
                this.entities.put(e.getId(),e);
                observableEmitter.onNext(e);
            }
            observableEmitter.onComplete();
        });
    }

    public Single<Entity> updateEntityState(Long id, Function<Entity,Entity> function){
        return Single.create(singleEmitter -> {
            final Entity entity = entities.get(id);
            if (entity == null)
                singleEmitter.onError(new RuntimeException("Unable to find user"));

            entities.put(id,function.apply(entity));
            singleEmitter.onSuccess(entities.get(id));
        });
    }

    public Observable<Entity> getAllEntitiesByRoom(long roomId) {
//        return Observable.create(observableEmitter -> {
//            this.entities.entrySet()
//                    .stream()
//                    .map(Map.Entry::getValue)
//                    .filter(entity -> entity.getRoomLocation() == roomId)
//                    .forEach(observableEmitter::onNext);
//
//            observableEmitter.onComplete();
//        });
        return null;
    }

    public Observable<Room> getAllRooms(){
        return Observable.create(observableEmitter -> {
            this.entities.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(entity -> entity.getType() == EntityType.ROOM )
                    .map(entity -> (Room)entity)
                    .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
    }

    public Single<Room> getRoomById(long id) {
        if (log.isDebugEnabled()){
            log.debug("PRINTING ALL ENTITIES from getRoomById()");
            this.entities.forEach((aLong, entity) -> log.debug(entity.toString()));

        }
        return Single.create(singleEmitter -> {
            Entity entity = this.entities.get(id);
            log.debug("FOUND ENTITY " + entity.toString());

            if (entity instanceof Room)
                singleEmitter.onSuccess((Room) entity);

            else
                singleEmitter.onError(new RuntimeException("Unable to find by ID: " + id));

        });
    }

    public Single<MudUser> getUserById(Long id) {
        return Single.create(singleEmitter -> {
           Entity entity = this.entities.get(id);
           if (entity != null && entity instanceof MudUser){
                singleEmitter.onSuccess((MudUser) entity);
           }else
               singleEmitter.onError(new RuntimeException("Unable to find user by ID: " + id));
        });
    }

    public Maybe<Entity> getEntityByName(String name) {
        return Maybe.create(maybeEmitter -> {

            Optional<Entity> entityByName = this.entities.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(entity -> entity.getName().equalsIgnoreCase(name))
                    .findFirst();

            if (entityByName.isPresent()){
                log.debug("Found entity by FULL match: " + entityByName.get().toString());
                entityByName.ifPresent(entity -> maybeEmitter.onSuccess(entity));
            }else {
                entityByName = this.entities.entrySet()
                        .stream()
                        .map(Map.Entry::getValue)
                        .filter(entity -> entity.getName().toUpperCase().contains(name.toUpperCase()))
                        .findFirst();
                entityByName.ifPresent(entity -> {
                    log.debug("Found entity by PARTIAL match: " + entity.toString());
                    maybeEmitter.onSuccess(entity);
                });
            }

            if (!entityByName.isPresent()){
                log.info("Unable to find entity");
                maybeEmitter.onComplete();
            }

        });

    }

    public Single<Boolean> sendUserMessage(MudUser user, String msg) {
        return Single.create(singleEmitter -> {
            val ctx = this.communicationMap.get(user.getId());
            if (ctx != null){
                ctx.writeAndFlush(msg);
            }else {
                log.info("No communication channel for userName: " + user.getName() + " id:" + user.getId());
                singleEmitter.onError(new RuntimeException("Unable to find user in communication map. User might have disconnected"));
            }
        });
    }

    public Completable registerUserOnline(MudUser mudUser, ChannelHandlerContext ctx){
        return Completable.create(completableEmitter -> {
            if (!completableEmitter.isDisposed()){
                val possibleUserKey = this.communicationMap.put(mudUser.getId(),ctx);

                if (possibleUserKey != null)
                    log.info("User might have disconnected as I'm getting a login for the same user: " + mudUser.getName());

                MudUser user = (MudUser)this.entities.get(mudUser.getId());

                if ((null != user)){
                   user.setOnline(true);
                   this.saveEntityState(user)
                           .subscribe(ignore ->{
                               log.info("User: " + user.getName() + " is now online.");
                                   completableEmitter.onComplete();
                           },completableEmitter::onError);

               }else {
                   completableEmitter.onError(new RuntimeException("Unable to find a user to put online"));
               }
            }
        });
    }

    public Completable registerUserOffline(MudUser mudUser){
        return Completable.create(completableEmitter -> {
            val possibleUserKey = this.communicationMap.remove(mudUser.getId());

            MudUser user = (MudUser)this.entities.get(mudUser.getId());
            if (null != user){
                user.setOnline(false);
                this.saveEntityState(user)
                        .subscribe(ignore ->{
                            if (possibleUserKey == null){
                                log.info("I have tried to remove a user comm function when none existed. Should not disconnect logged out users again");
                                completableEmitter.onError(NoUserToDisconnect.foundNone());
                            }else {
                                completableEmitter.onComplete();
                            }
                        },completableEmitter::onError);
            }else {
                completableEmitter.onError(new RuntimeException("Unable to find a user to put offline"));
            }
        });
    }

    public Single<Entity> getEntityById(long id) {
        return Single.create(singleEmitter -> {
           if (this.entities.containsKey(id)){
               singleEmitter.onSuccess(this.entities.get(id));
           }else {
               singleEmitter.onError(CannotFindUser.foundNone());
           }
        });

    }
}
