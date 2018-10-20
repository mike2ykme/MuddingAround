package com.icrn.service;

import com.icrn.model.Entity;
import com.icrn.model.EntityType;
import com.icrn.model.MudUser;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


public class StateHandlerTest {
    StateHandler stateHandler;
    HashMap<Long,Entity> map;
    @Before
    public void init(){
        this.map = new HashMap<>();
        this.stateHandler = new StateHandler(map);
    }

    @Test
    public void getAllOnlineEntities(){
        stateHandler.getAllOnlineEntities()
                .test()
                .assertComplete();
    }

    @Test
    public void getAllOnlineUserEntities(){
        stateHandler.getAllOnlineEntities()
                .filter(entity -> entity.getType() == EntityType.USER)
                .test()
                .assertComplete();
    }

    @Test
    public void getAllEntitiesInRoom(){
        long roomId = 3L;
        stateHandler.getAllEntitiesByRoom(0L)
            .test()
                .assertNoErrors()
                .assertNoValues()
                .assertComplete();
    }
//    @Test
//    public void updateAndVerifyOnline(){
//        MudUser mudUser = MudUser.makeJoe();
//        mudUser.setOnline(false);
//        stateHandler.saveEntityState(mudUser)
//            .test()
//        .assertNoErrors()
//        .assertComplete();
//
//        stateHandler.getAllOnlineEntities()
//                .test()
//                .assertComplete()
//                .assertNoValues()
//                .assertNoErrors();
//
//        stateHandler.updateEntityState(mudUser.getId(), entity ->{
//                    entity.setOnline(true);
//                    return entity;
//                })
//                .test()
//                .assertNoErrors()
//                .assertComplete();
//
//        stateHandler.getAllOnlineEntities()
//                .test()
//                .assertComplete()
//                .assertNoErrors()
//                .assertValue(entity -> entity.isOnline() == true);
//
//        this.map.forEach((aLong, entity) -> System.out.println("ID: " + aLong));
//    }

    @Test
    public void verifyGetFullNameFirstBySearches(){
        MudUser joe = MudUser.makeJoe();
        MudUser joseph = MudUser.makeJoe();
        joseph.setId(2L);
        joseph.setName("Joseph");

        MudUser josephene = MudUser.makeJoe();
        josephene.setId(3L);
        josephene.setName("Josephene");

        this.stateHandler.saveEntityState(joe)
                .blockingGet();
        this.stateHandler.saveEntityState(joseph)
                .blockingGet();
        this.stateHandler.saveEntityState(josephene)
                .blockingGet();
//        this.stateHandler.getAllOnlineEntities()
//                .subscribe(System.out::println);

        this.stateHandler.getEntityByName("joe")
                .test()
                .assertComplete()
                .assertValue(entity -> entity.getId() == joe.getId());

        this.stateHandler.getEntityByName("joseph")
                .test()
                .assertComplete()
                .assertValue(entity -> entity.getId() == joseph.getId());


        this.stateHandler.getEntityByName("josephe")
                .test()
                .assertComplete()
                .assertValue(entity -> entity.getId() == josephene.getId());

        final Entity entity = this.stateHandler.getEntityByName("JOE")
                .blockingGet();
//        System.out.println(entity);

    }
}