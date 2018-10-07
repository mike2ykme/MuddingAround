package com.icrn.service;

import com.icrn.dao.EntityDao;
import com.icrn.model.Entity;
import com.icrn.model.EntityType;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;


@Slf4j
public class StateHandler {
    @NonNull Map<Long,Entity> entities;
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

    public Observable<Entity> getAllOnlineEntities() {
        return Observable.create(observableEmitter -> {
            entities.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(Entity::isOnline)
            .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
    }

    public Single<Entity> saveEntityState(Entity entity){
        return Single.create(singleEmitter -> {
            entities.put(entity.getId(),entity);

            singleEmitter.onSuccess(entity);
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
            this.entities.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(entity -> entity.getRoomLocation() == roomId)
                    .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
    }

    public Observable<Entity> getAllRooms(){
        return Observable.create(observableEmitter -> {
            this.entities.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(entity -> entity.getType() == EntityType.ROOM )
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
            Optional<Entity> optionalS = this.entities.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .filter(entity -> entity.getName().equalsIgnoreCase(name))
                    .findFirst();


            if (optionalS.isPresent()){
                log.debug("Found entity by FULL match: " + optionalS.get().toString());
                optionalS.ifPresent(entity -> maybeEmitter.onSuccess(entity));
            }else {
                optionalS = this.entities.entrySet()
                        .stream()
                        .map(Map.Entry::getValue)
                        .filter(entity -> entity.getName().toUpperCase().contains(name.toUpperCase()))
                        .findFirst();
                optionalS.ifPresent(entity -> {
                    log.debug("Found entity by PARTIAL match: " + entity.toString());
                    maybeEmitter.onSuccess(entity);
                });
            }

            if (!optionalS.isPresent()){
                log.info("Unable to find entity");
                maybeEmitter.onComplete();
            }

        });

    }
//    public Single<Entity> getEntityByName(String name) {
//        return getEntityByName(name,null);
//    }
}
