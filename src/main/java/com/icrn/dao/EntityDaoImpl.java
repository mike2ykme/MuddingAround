package com.icrn.dao;
import com.icrn.model.Entity;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntityDaoImpl implements EntityDao {
    private EntityDaoImpl(){}
    private static class LazyHolder{ static final EntityDaoImpl INSTANCE = new EntityDaoImpl(); }
    public static EntityDao getInstance(){
        return LazyHolder.INSTANCE;
    }

    @Override
    public Entity getEntityById(Long id) {
        return null;
    }

    @Override
    public MudUser getUserById(Long id) {
        return null;
    }

    @Override
    public Room getRoomById(Long id) {
        return null;
    }

    @Override
    public Completable updateEntity(MudUser user) {
        return null;
    }

    @Override
    public Maybe<Long> getEntityByName(String name) {
        return null;
    }

    @Override
    public Observable<Entity> getAllEntities() {
        return null;
    }
}
