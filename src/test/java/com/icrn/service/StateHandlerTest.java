package com.icrn.service;

import com.icrn.exceptions.CannotPerformAction;
import com.icrn.model.Entity;
import com.icrn.model.Mob;
import com.icrn.model.MudUser;
import com.icrn.model.Room;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class StateHandlerTest {
    private MudUser joe;
    private ChannelHandlerContext mockCtx;

    StateHandler stateHandler;
    HashMap<Long, Entity> map;

    @Before
    public void init() {
        this.map = new HashMap<>();
        this.stateHandler = new StateHandler(map);
        this.joe = MudUser.makeJoe();

        this.mockCtx = mock(ChannelHandlerContext.class);
    }


    @Test
    public void saveEntitiesState() {

        val joe = MudUser.makeJoe();

        val mike = MudUser.makeJoe();
        mike.setName("Mike");
        mike.setId(2L);

        this.stateHandler.saveEntityState(joe, mike)
                .test()
                .assertValueCount(2)
                .assertValues(joe, mike)
                .assertComplete();
    }

    @Test
    public void getAllOnlineEntities() {
        this.stateHandler.saveEntityState(MudUser.makeJoe()).blockingGet();

        stateHandler.getAllOnlineUsers()
                .test()
                .assertValue(entity -> entity.getName().equalsIgnoreCase("joe") &&
                        entity.getId() == 1L)
                .assertComplete();

    }

    @Test
    public void makeSureWeUpdateThenSaveonUpdateEntityStateFunction() {
        val joe = MudUser.makeJoe();
        this.stateHandler.saveEntityState(joe).blockingGet();

        this.stateHandler.updateEntityState(1L, entity -> {
            ((MudUser) entity).setOnline(false);
            ((MudUser) entity).setPassword("ABC");
            return entity;
        }).test()
                .assertComplete()
                .assertValueCount(1)
                .assertValue(entity -> entity.getId() == 1L
                        && entity.getName().equalsIgnoreCase("joe")
                        && ((MudUser) entity).isOnline() == false
                        && ((MudUser) entity).getPassword().equalsIgnoreCase("ABC"));
    }

    @Test
    public void registerOfflineUserOffline() {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        // We shouldn't be disconnecting a user that has is not there, so we'll return a status failure for user
        this.stateHandler.registerUserOffline(MudUser.makeJoe())
                .test()
                .assertError(CannotPerformAction.class);
//                .assertValue(actionResult -> !actionResult.get());
    }

//    @Test
//    public void registerOnlineUserOnline(){
//        this.joe.setOnline(true);
//        this.stateHandler.getEntities().put(joe.getId(),this.joe);
//        this.stateHandler.getCommunicationMap().put(MudUser.makeJoe().getId(),mockCtx);
//
//        this.stateHandler.registerUserOnline(MudUser.makeJoe(),mockCtx)
//                .test()
//                .assertComplete()
//                .assertNoErrors();
//    }

    @Test
    public void registerOnlineUserOffline() {
        this.joe.setOnline(true);
        this.stateHandler.getEntities().put(joe.getId(), this.joe);
        this.stateHandler.getCommunicationMap().put(MudUser.makeJoe().getId(), mockCtx);

        this.stateHandler.registerUserOffline(MudUser.makeJoe())
                .test()
                .assertComplete()
                .assertNoErrors();
    }

    @Test
    public void verifyWeGetAllSavedRooms() {
        val room0 = new Room(0L);
        val room1 = new Room(10L);
        val room2 = new Room(20L);
        val room3 = new Room(30L);

        this.stateHandler.saveEntityState(room0, room2, room3, room1)
                .blockingForEach(ignore -> {
                });

        this.stateHandler.getAllRooms()
                .test()
                .assertValueCount(4)
                .assertValues(room0, room2, room1, room3)
                .assertComplete()
        ;

    }

    @Test
    public void ensureWeGetCorrectRoom() {
        val room0 = new Room(0L);
        val room1 = new Room(10L);
        val room2 = new Room(20L);
        val room3 = new Room(30L);

        this.stateHandler.saveEntityState(room0, room2, room3, room1)
                .blockingForEach(ignore -> {
                });

        this.stateHandler.getRoomById(10L)
                .test()
                .assertComplete()
                .assertValue(room1)
                .assertValueCount(1);
    }

    @Test
    public void getAUserById() {
        val joe = MudUser.makeJoe();

        val mike = MudUser.makeJoe();
        mike.setName("Mike");
        mike.setId(2L);

        this.stateHandler.saveEntityState(joe, mike).blockingForEach(entity -> {
        });

        this.stateHandler.getUserById(1L)
                .test()
                .assertValueCount(1)
                .assertComplete()
                .assertValue(joe);

        this.stateHandler.getUserById(2L)
                .test()
                .assertValueCount(1)
                .assertComplete()
                .assertValue(mike);

        this.stateHandler.getEntityById(1L)
                .test()
                .assertValueCount(1)
                .assertComplete()
                .assertValue(joe);
    }

    @Test
    public void getAllEntities() {
        stateHandler.getAllEntities()
                .test()
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void getAllEntitiesInRoom() {
        long roomId = 3L;
        stateHandler.getAllEntitiesByRoom(0L)
                .test()
                .assertNoErrors()
                .assertNoValues()
                .assertComplete();
    }

    @Test
    public void verifyGetFullNameFirstBySearches() {
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
    }


    //    @Test
//    public void verifySendUserMessage(){
//        val joe = MudUser.makeJoe();
//        joe.setOnline(false);
//        this.stateHandler.registerUserOnline(joe,this.mockCtx)
//                .blockingGet();
//
//        val chFut = mock(ChannelFuture.class);
//
//        when(this.mockCtx.writeAndFlush(any())).thenReturn(chFut);
//
//        when(chFut.addListener(any())).thenReturn(null);
//        when(chFut.isSuccess()).thenReturn(true);
//
//        this.stateHandler.sendUserMessage(joe,"TEST")
//                .test()
//                .assertComplete();
//    }
    @Test
    public void registerUserOnlineWithExistingUser() throws Exception {


        val mockChannel = mock(Channel.class);

        when(this.mockCtx.channel()).thenReturn(mockChannel);

        val mockChFut = mock(ChannelFuture.class);
        when(mockChannel.close()).thenReturn(mockChFut);

        try {
            when(mockChFut.sync()).thenReturn(null);
        } catch (InterruptedException e) {
            System.out.println("YEP got expected exception");
//            e.printStackTrace();
        }

        when(this.mockCtx.writeAndFlush(any())).thenReturn(mockChFut);

        this.joe.setOnline(false);
        this.joe = (MudUser) this.stateHandler.saveEntityState(joe).blockingGet();
        this.stateHandler.registerUserOnline(joe, mockCtx)
                .test()
                .assertComplete();


        this.stateHandler.registerUserOnline(MudUser.makeJoe(), mockCtx)
                .test()
                .assertComplete();

        try {
            when(mockChFut.sync()).thenThrow(InterruptedException.class);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            System.out.println("YEP got expected exception");
        }
        this.stateHandler.registerUserOnline(MudUser.makeJoe(), mockCtx)
                .test()
                .assertError(InterruptedException.class);


        when(mockCtx.writeAndFlush(any())).thenThrow(new RuntimeException("TEST"));
        this.stateHandler.registerUserOnline(MudUser.makeJoe(), mockCtx)
                .test()
                .assertError(RuntimeException.class);
    }

    @Test
    public void testCreateNewEntity() {
        val joe = MudUser.makeJoe();
        joe.setName(null);
        this.stateHandler.createNewEntity(joe)
                .test()
                .assertComplete()
                .assertValue(entity -> {
                    System.out.println(entity);
                    return entity.getId() != null && entity.getId() != 1;
                });
    }

    @Test
    public void ensureSameNameIsOK(){
        //This is really going to be useful when dealing with mobs that have the same name

        val joe = MudUser.makeJoe();
        val joe2 = MudUser.makeJoe();

        this.stateHandler.saveEntityState(joe,joe2).blockingForEach(System.out::println);

        this.stateHandler.getEntityByName("joe")
                .test()
                .assertValueCount(1)
                .assertComplete();

    }

    @Test
    public void verifyWeCanGetRoomMap(){
        val joe = MudUser.makeJoe();
        val joe2 = MudUser.makeJoe();
        val glork = Mob.makeGlork();
        val glork2 = Mob.makeGlork();
        joe.setId(1L);
        joe2.setId(2L);
        glork.setId(3L);
        glork2.setId(4L);

        glork2.setRoomLocation(11L);
        glork.setRoomLocation(11L);
        joe.setRoomLocation(11L);
        joe2.setRoomLocation(10L);

        this.stateHandler.saveEntityState(joe,joe2,glork,glork2).blockingForEach(entity -> {});

        HashMap<Long,Entity> map = new HashMap<>();
        map.put(joe.getId(),joe);
        map.put(glork.getId(),glork);
        map.put(glork2.getId(),glork2);

        this.stateHandler.getRoomEntityMap(11L)
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .assertValue(map);
    }
}