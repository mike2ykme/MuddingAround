package com.icrn.dao;

import com.icrn.model.Actions;
import com.icrn.model.Entity;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.time.LocalDateTime;

public interface EntityDao {
    Entity getEntityById(Long id);
    MudUser getUserById(Long id);
    Room getRoomById(Long id);
    Completable updateEntity(MudUser user);
    Maybe<Long> getEntityByName(String name);
    Observable<Entity> getAllEntities();
}
