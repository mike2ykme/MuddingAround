package com.icrn.service;

import com.icrn.dao.EntityDao;
import com.icrn.exceptions.CannotFindEntity;
import com.icrn.exceptions.CannotPerformAction;
import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.reactivex.*;
import io.reactivex.Observable;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j @Data
public class StateHandler {
    @NonNull Map<Long,Entity> entities;
    private final HashMap<Long,ChannelHandlerContext> communicationMap = new HashMap<>();
    static final String RETURN_CHARS = "\r\n";
    EntityDao entityDao = null;


    public  StateHandler(){
        this.entities = new ConcurrentHashMap<>();

    }

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
    public Observable<Entity> getAllOnlineUsers() {

        return Observable.create(observableEmitter -> {
           this.entities.entrySet().stream()
           .map(Map.Entry::getValue)
           .filter(entity -> entity.getType() == EntityType.USER)
           .map(entity -> (MudUser)entity)
           .filter(MudUser::isOnline)
           .forEach(observableEmitter::onNext);

           observableEmitter.onComplete();
        });
    }

    public Single<Entity> saveEntityState(Entity entity){
        log.info("saving Entity state for " + entity.getName());
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
                log.info("saving Entity state for " + e.getName());
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
        return Observable.create(observableEmitter -> {
            this.entities.entrySet().stream()
                    .map(longEntityEntry -> longEntityEntry.getValue())
                    .filter(entity -> entity.getRoomLocation() == roomId)
                    .peek(entity -> log.info("Found entity: " + entity.getName() + " in roomId: " + roomId))
                    .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
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
                log.debug("Found entity by FULL match: \n" + entityByName.get().toString());
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

    public Completable sendUserMessage(MudUser user, String msg) {
        log.info("Trying to send user message: " + user.getName());
        return Completable.create(completableEmitter -> {
            val ctx = this.communicationMap.get(user.getId());
            if (ctx != null){
                log.debug("Found context for user " + user.getName());
                val chFut = ctx.writeAndFlush(msg + RETURN_CHARS);
                chFut.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        log.info("inside operationComplete()");
                        if (chFut.isSuccess()){
                            log.info("Was able to send " + user.getName() + " a message: " + msg);
                            completableEmitter.onComplete();
                        } else {
                            log.error("Unable to send " + user.getName() + " a message: " + msg);
                            completableEmitter.onError(CannotPerformAction.of("Cannot write data to user"));
                        }
                    }
                });

            }else {
                log.error("No communication channel for userName: " + user.getName() + " id:" + user.getId());
                completableEmitter.onError(CannotFindEntity.foundNone("User does not have ctx stored to send message"));
            }
        });
    }

    public Completable sendUserMessage(Long userId, String msg){
        val mudUser = (MudUser)this.entities.get(userId);
        return sendUserMessage(mudUser,msg);

    }

    public Completable registerUserOnline(MudUser mudUser, ChannelHandlerContext ctx){
        log.debug("Trying to register user online");
        if (mudUser.isOnline()){
            log.warn("User is showing as online already" + mudUser.getName());
        }
        return Completable.create(completableEmitter -> {
            if (!completableEmitter.isDisposed()){
                val possibleUserContext = this.communicationMap.put(mudUser.getId(),ctx);
                if (possibleUserContext != null){
                    log.info("User might have disconnected as I'm getting a login for the same user: " + mudUser.getName());
                    try {
                        possibleUserContext.writeAndFlush("CLOSING CONNECTION AS NEW CONNECTION FOR THIS USER DETECTED\r\n");
                    } catch (Exception e) {
                        log.error("ERROR trying to write message to duplicate connection");
                        log.error(e.getMessage());
//                        completableEmitter.onError(e);
                        throw e;
                    }
                    try {
                        possibleUserContext.channel().close().sync();
                    } catch (InterruptedException e) {
                        log.error("ERROR trying to close duplicate connection");
                        log.error(e.getMessage());
//                        completableEmitter.onError(e);
                        throw e;
                    }
                }

                MudUser user = (MudUser)this.entities.get(mudUser.getId());

                if ((null != user)){
                    log.info("User was not null, trying to register ctx of user");
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
        log.debug("Trying to register user offline");
        if (!mudUser.isOnline()){
            log.warn("User is showing as offline already" + mudUser.getName());
        }
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
                completableEmitter.onError(CannotPerformAction.of("Unable to find a user to put offline"));
            }
        });
    }

    public Single<Entity> getEntityById(long id) {
        log.info("Trying to find an ENTITY with id: " + id);
        log.info("Do we have this in the state?: " + this.entities.containsKey(id));
        return Single.create(singleEmitter -> {
           if (this.entities.containsKey(id)){
               log.info("We've found this userID in the state");
               singleEmitter.onSuccess(this.entities.get(id));
           }else {
               singleEmitter.onError(CannotFindEntity.foundNone());
           }
        });

    }

    public Single<Entity> createNewEntity(Entity entity) {
        // NOTE this will overwrite an ID, but it won't do any copying so it'll just point to an existing element
        return Single.create(singleEmitter -> {
            if (entity.getId() != null && this.entities.containsKey(entity.getId())) {
                singleEmitter.onError(CannotPerformAction.of("Entity with this ID already exists."));
            }else {
                val generator = new Random();
                var rand = generator.nextLong() & Long.MAX_VALUE; //We only want positive number IDs

                while (this.entities.containsKey(rand)){
                    rand = generator.nextLong();
                }
                entity.setId(rand);

                this.entities.put(rand,entity);

                singleEmitter.onSuccess(entity);
            }
        });
    }

    public Single<Map<Long,Entity>> getRoomEntityMap(Long roomId) {
        return Single.create(singleEmitter -> {
            val map = new HashMap<Long,Entity>();

            entities.entrySet().stream()
                    .filter(longEntityEntry -> longEntityEntry.getValue().getRoomLocation() == roomId)
                    .map(entry -> entry.getValue())
                    .forEach(entity -> {
                        if (!singleEmitter.isDisposed()){
                            map.put(entity.getId(),entity);
                        }
                    });

            singleEmitter.onSuccess(Collections.unmodifiableMap(map));

        });
    }
}
