package com.icrn.controller;

import com.icrn.exceptions.NoUserToDisconnect;
import com.icrn.model.*;
import com.icrn.service.StateHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.mockito.Mockito.mock;

public class FrontControllerTest {
    MudUser joe;
    FrontController controller;
    StateHandler stateHandler;
    ChannelHandlerContext mockCtx;

    @Before
    public void setup(){
        this.joe = MudUser.makeJoe();
        HashMap<Long, Entity> map = new HashMap<>();
        map.put(joe.getId(),joe);

        this.stateHandler = new StateHandler(map);
        this.controller = new FrontController(stateHandler);
        this.mockCtx = mock(ChannelHandlerContext.class);

        Room room = new Room(0L);
        room.addRoomDirection(Movement.NORTH,10L);
        Room room10 = new Room(10L);
        room10.addRoomDirection(Movement.SOUTH,0L);

        this.stateHandler.saveEntityState(room).blockingGet();
        this.stateHandler.saveEntityState(room10).blockingGet();
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
                    System.out.println(mudUser);
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
                .assertNoErrors();
    }

    @Test
    public void registerOfflineUserOffline(){ //We can't make someone offline offline so we just return a NoUserToDisconnect exception as a message saying we did it but
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        this.controller.registerUserOffline(MudUser.makeJoe())
            .test()
            .assertError(NoUserToDisconnect.class);
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
        System.out.println(joe);
        this.controller.singleMoveUser(this.joe, Movement.of("N"))
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(actionResult -> actionResult.getStatus() == true);

        this.stateHandler.getAllEntitiesByRoom(10).subscribe(System.out::println);
    }
}