package com.icrn.controller;

import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.SimpleHandlerImpl;
import com.icrn.service.StateHandler;
import io.netty.channel.*;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FrontControllerTest {
    MudUser joe;
    FrontController controller;
    StateHandler stateHandler;
    ChannelHandlerContext mockCtx;
    AttackHandler mockAttackHandler;
    SimpleHandlerImpl simpleHandlerImpl = new SimpleHandlerImpl();
    @Before
    public void setup(){
        this.joe = MudUser.makeJoe();
        HashMap<Long, Entity> map = new HashMap<>();
        map.put(joe.getId(),joe);

        this.mockAttackHandler = mock(AttackHandler.class);

        this.stateHandler = new StateHandler(map);
        this.controller = new FrontController(stateHandler, simpleHandlerImpl, simpleHandlerImpl);
        this.mockCtx = mock(ChannelHandlerContext.class);

        Room room = new Room(0L);
        Room room10 = new Room(10L);
        Room room20 = new Room(20L);
        room10.addRoomDirection(Movement.SOUTH,0L);
        room.addRoomDirection(Movement.NORTH,10L);
        room.addRoomDirection(Movement.SOUTH,20L);
        room20.addRoomDirection(Movement.NORTH,0L);



        this.stateHandler.saveEntityState(room).blockingGet();
        this.stateHandler.saveEntityState(room10).blockingGet();
    }

    @Test
    public void handleNoUsersAvailableForLogin(){
        val statehandler = new StateHandler(new HashMap<>());
        val controller = new FrontController(statehandler, null, null);
        val mockCtx = mock(ChannelHandlerContext.class);

        val username = "joe";
        val password  = "JOE";

        controller.maybeGetUser(username,password)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertNoValues();
    }

    @Test
    public void handleCorrectLogin(){
        val username = "joe";
        val password  = "JOE";

        this.controller.maybeGetUser(username,password)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(mudUser -> {
                    return mudUser.getName().equalsIgnoreCase("joe");
                });
    }

    @Test
    public void handleIncorrectLogin(){
        val username = "joe";
        val password = "bad_password";

        this.controller.maybeGetUser(username,password)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertNoValues();
    }

    @Test
    public void registerLoggedInUserOnline(){
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        this.controller.registerUserOnline(MudUser.makeJoe(),ctx)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(actionResult -> actionResult.getStatus());
    }

    @Test
    public void registerOfflineUserOffline(){
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        // We shouldn't be disconnecting a user that has is not there, so we'll return a status failure for user
        this.controller.registerUserOffline(MudUser.makeJoe())
            .test()
            .assertComplete()
            .assertValue(actionResult -> !actionResult.getStatus());
    }

    @Test
    public void registerOnlineUserOffline(){
        this.joe.setOnline(true);
        this.stateHandler.getEntities().put(joe.getId(),this.joe);
        this.stateHandler.getCommunicationMap().put(MudUser.makeJoe().getId(),mockCtx);

        this.controller.registerUserOffline(MudUser.makeJoe())
                .test()
                .assertComplete()
                .assertNoErrors();
    }

    @Test
    public void handleUserMove(){
        assertTrue(joe.getRoomLocation()==0L);
        this.controller.handleUserMove(this.joe, Movement.of("N"))
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(actionResult ->
                    actionResult.getStatus() && actionResult.getUser().getRoomLocation() == 10
                );
    }

    @Test
    public void verifyStringToCommandHandler(){
        val command = "Move S";


        this.controller.handleCommands(command,joe.getId())
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(actionResult ->
                   actionResult.getStatus() && actionResult.getUser().getRoomLocation() == 20L
                );
    }

    @Test
    public void verifyStringToCommandHandlerFailure(){
        val command = "Move S";


        val joe = MudUser.makeJoe();
        HashMap<Long, Entity> map = new HashMap<>();
        map.put(joe.getId(),joe);
        StateHandler stater = new StateHandler(map);
        FrontController controller = new FrontController(stater, null,null);

        controller.handleCommands(command,joe.getId())
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(actionResult ->{
                    System.out.println(actionResult);
                        return !actionResult.getStatus();}
                );
    }

    @Test
    public void allowUsersToAttackEachOther(){
        val joe = MudUser.makeJoe();
        val mike = MudUser.makeJoe();
        mike.setId(2L);
        mike.setName("Mike");
        this.stateHandler.saveEntityState(joe,mike).blockingForEach(entity -> {});

        this.controller.handleCommands("attack mike",joe.getId())
                .test()
                .assertNoErrors()
                .assertValue(actionResult ->{
                    System.out.println(actionResult);
                    return actionResult.getMessage().contains("attacks") && actionResult.getStatus();
                });
    }

    @Test
    public void testUserDefend(){
        val joe = MudUser.makeJoe();

        this.stateHandler.saveEntityState(joe).blockingGet();

        this.controller.handleCommands("defend",joe.getId())
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(actionResult ->
                        actionResult.getStatus()
                        && actionResult.getMessage().toLowerCase().contains("defend")
                );
        val updatedJoe = (MudUser) this.stateHandler.getEntityByName("joe").blockingGet();

        assertTrue(updatedJoe.getLastCommand().get() == Actions.DEFEND);
    }


    @Test
    public void testRestingUser(){
        val joe = MudUser.makeJoe();
        val prevHp = joe.getHP();
        val cmd = "rest";
        this.stateHandler.saveEntityState(joe).blockingGet();

        this.controller.handleCommands(cmd,joe.getId())
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(actionResult ->
                   actionResult.getStatus() &&
                   prevHp < actionResult.getUser().getHP() &&
                   actionResult.getMessage().toLowerCase().contains("rested")
                );
    }
}