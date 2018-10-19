package com.icrn.controller;

import com.icrn.model.*;
import com.icrn.service.StateHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class MudderTest {

    Mudder mudder = null;
//    EntityDao entityDao = null;
    StateHandler stateHandler = null;
    @Before
    public void setup(){
        MudUser joe = MudUser.makeJoe();
        joe.setId(1L);
        MudUser joe2 = MudUser.makeJoe();
        joe2.setId(2L);
        joe2.setName("Moe");

        Room room = new Room(0L);
        room.setAllowedDirections(new HashMap<>());
        room.addRoomDirection(Movement.NORTH,10L);

        Room room10 = new Room(10L);
        room10.setAllowedDirections(new HashMap<>());
        room10.addRoomDirection(Movement.SOUTH,0L);

        final Map<Long, Entity> longEntityHashMap = new ConcurrentHashMap<>();

        this.stateHandler = new StateHandler(longEntityHashMap);
        this.stateHandler.saveEntityState(room).blockingGet();
        this.stateHandler.saveEntityState(room10).blockingGet();
        this.stateHandler.saveEntityState(joe).blockingGet();
        this.stateHandler.saveEntityState(joe2).blockingGet();

        this.mudder = new Mudder(this.stateHandler);

    }

    @Test
    public void handleWaitAction() {
        MudCommand cmd = new MudCommand(Actions.WAIT,Optional.empty(),this.stateHandler.getUserById(1L).blockingGet());

        this.mudder.HandleAction(cmd)
            .test()
            .assertComplete()
                .assertValue(mudResult -> (mudResult.isCompleted() == true
                        && mudResult.getMsg().contains(cmd.getRequester().getName())));
    }

    @Test
    public void handleWaitActionTooFast() {
        MudCommand cmd = new MudCommand(Actions.WAIT,Optional.empty(),this.stateHandler.getUserById(1L).blockingGet());

        int hp = cmd.getRequester().getHP();
        this.mudder.HandleAction(cmd)
                .test()
                .assertComplete()
                .assertValue(mudResult -> mudResult.isCompleted() == true);

        this.mudder.HandleAction(cmd)
                .test()
                .assertComplete()
                .assertValue(mudResult -> mudResult.isCompleted() == false);

        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            this.mudder.HandleAction(cmd)
                    .test()
                    .assertComplete()
                    .assertValue(mudResult -> mudResult.isCompleted() == true);
        }
        Assert.assertTrue(hp < cmd.getRequester().getHP());
    }

    @Test
    public void handleMovementOfUser(){
        MudUser user = this.stateHandler.getUserById(1L).blockingGet();
        MudCommand cmd = new MudCommand(Actions.MOVE,Optional.of("N"),user);

        this.mudder.HandleAction(cmd)
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(mudResult -> mudResult.isCompleted() == true);

        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.mudder.HandleAction(new MudCommand(Actions.MOVE,Optional.of("S"),user))
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(mudResult -> mudResult.isCompleted() == true);
    }

    @Test
    public void handleAttackInSameRoom(){
        MudUser joe = this.stateHandler.getUserById(1L).blockingGet();
        MudUser moe = this.stateHandler.getUserById(2L).blockingGet();

        this.stateHandler.getEntityByName("JOE")
                .subscribe(System.out::println,Throwable::printStackTrace);


        MudCommand command = MudCommand.of(Actions.ATTACK,"Moe",joe);
        int moeHp = moe.getHP();
        System.out.println(moeHp);
        this.mudder.HandleAction(command)
                .subscribe(System.out::println,Throwable::printStackTrace);

        System.out.println(this.stateHandler.getUserById(2L).blockingGet().getHP());
        Assert.assertTrue(moeHp > this.stateHandler.getUserById(2L).blockingGet().getHP());
    }

    @Test
    public void handleAttackInDifferentRoom(){
        MudUser joe = this.stateHandler.getUserById(1L).blockingGet();
        MudUser moe = this.stateHandler.getUserById(2L).blockingGet();
        moe.setRoomLocation(10L);
        this.stateHandler.saveEntityState(moe).blockingGet();

        MudCommand command = MudCommand.of(Actions.ATTACK,"Moe",joe);

        this.mudder.HandleAction(command)
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(mudResult ->
                        mudResult.isCompleted() == false && mudResult.getMsg().toUpperCase().contains("NOT"));
    }

    @Test
    public void verifyGetUser(){
        this.mudder.maybeGetUser("mike","password")
                .test()
                .assertValue(mudUser -> mudUser.getName().equalsIgnoreCase("joe"))
                .assertComplete();

        this.mudder.maybeGetUser("mike","password")
                .subscribe(System.out::println,Throwable::printStackTrace);
    }

    @Test
    public void joeCanTalkToMoeInSameRoom(){
        MudUser joe = this.stateHandler.getUserById(1L).blockingGet();
        MudUser moe = this.stateHandler.getUserById(2L).blockingGet();


        MudCommand cmd = MudCommand.of(Actions.TALK,null,joe); //Talking means saying it out loud in a room

        this.mudder.HandleAction(cmd)
                .test()
                .assertNoErrors()
                .assertComplete();

        cmd = MudCommand.of(Actions.TALK,"HELLO",joe);
        this.mudder.HandleAction(cmd)
                .test()
                .assertNoErrors()
                .assertComplete();
    }
}
