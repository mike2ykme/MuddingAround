package com.icrn.controller;

import com.icrn.model.*;
import com.icrn.service.AttackHandler;
import com.icrn.service.SimpleHandlerImpl;
import com.icrn.service.StateHandler;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class NpcControllerTest {
    private NpcController controller;
    private NpcController controllerHasMock;
    private StateHandler stateHandler;
    private StateHandler mockStateHandler;
    private AttackHandler attackHandler;
    private AttackHandler mockAttackHandler;

    @Before
    public void setup(){
        this.mockStateHandler = mock(StateHandler.class);
        this.mockAttackHandler = mock(AttackHandler.class);
        this.attackHandler = new SimpleHandlerImpl();
        this.stateHandler = new StateHandler();
        this.controller = new NpcController(stateHandler, attackHandler);
        this.controllerHasMock = new NpcController(mockStateHandler, mockAttackHandler);
    }
    @Test
    public void test(){
        assertNotNull(new NpcController(mockStateHandler,attackHandler));
    }


    @Test
    public void processTick(){
        when(this.mockStateHandler.getAllRooms()).thenReturn(Observable.empty());

        this.controllerHasMock.processTick()
                .test()
                .assertComplete();
    }

    @Test
    public void verifyRoomsSpawnMonsters(){
        Room mockRoom = mock(Room.class);
        MobInfo mockMobInfo = mock(MobInfo.class);
        when(mockStateHandler.getAllRooms()).thenReturn(Observable.just(mockRoom));
        when(mockStateHandler.getAllEntitiesByRoom(0L)).thenReturn(Observable.empty());
        when(mockRoom.getMobInfo()).thenReturn(mockMobInfo);
        when(mockMobInfo.getMobCount()).thenReturn(0L);

        val mockTemplate = mock(StatsBasedEntityTemplate.class);

//        when(mockMobInfo.getStatsBasedEntityTemplate()).thenReturn(mockTemplate);
        when(mockMobInfo.getStatsBasedEntityTemplate()).thenReturn(mockTemplate);
        when(mockTemplate.getCON()).thenReturn(10);
        when(mockTemplate.getDEX()).thenReturn(10);
        when(mockTemplate.getHP()).thenReturn(10);
        when(mockTemplate.getMaxHP()).thenReturn(10);
        when(mockTemplate.getSTR()).thenReturn(10);
        when(mockTemplate.getRoom()).thenReturn(0L);



        when(mockRoom.getName()).thenReturn("I'M A FAKE!");

        this.controllerHasMock.spawnMonsters()
                .test()
                .assertComplete()
                .assertNoErrors();


        when(mockMobInfo.getMobCount()).thenReturn(1L);
        when(mockStateHandler.createNewEntity(any())).thenReturn(Single.just(MudUser.makeJoe()));

        this.controllerHasMock.spawnMonsters()
                .test()
                .assertComplete()
                .assertNoErrors();
    }

    @Test
    public void verifyMobsCanAttack(){
        val joe = MudUser.makeJoe();
        val glork = Mob.makeGlork();
        val angryGlork = Mob.makeGlork();

        val prevHp = joe.getHP();

        angryGlork.setName("angryGlork");
        angryGlork.setAggressionIndex(100);
        System.out.println(glork);
        System.out.println(joe);
        List<String> mockList = new ArrayList<>();
        mockList.add("TEST STRING");
        when(mockStateHandler.sendUserMessage(eq(1L),
                Matchers.contains("attack")
        ))
                .thenReturn(Completable.complete());

        when(mockStateHandler.saveEntityState(angryGlork,joe))
                .thenReturn(Observable.just((Entity)angryGlork,(Entity)joe));

        this.controllerHasMock.setAttackHandler(this.attackHandler);

        this.controllerHasMock.processMonsterAttack(angryGlork,joe)
                .test()
                .assertComplete()
                .assertNoErrors()
                ;
        assertTrue(prevHp > joe.getHP());
        assertTrue(joe.getLastAttackedById().get() == angryGlork.getId());
        assertFalse(angryGlork.canPerformAction());

    }


    @Test
    public void verifyProcessingAllMonstersInARoom(){
        // Before we do anything we're just going to look at each room and find if there are
        // any active Mobs in it
        val joe = MudUser.makeJoe();
        val joe2 = MudUser.makeJoe();
        val glork = Mob.makeGlork();
        val glork2 = Mob.makeGlork();
        joe.setId(1L);
        joe2.setId(2L);
        glork.setId(3L);
        glork.setLastAttackedById(joe.getId());
        glork2.setId(4L);

        glork2.setRoomLocation(11L);
        glork.setRoomLocation(11L);
        joe.setRoomLocation(11L);
        joe2.setRoomLocation(10L);

        HashMap<Long,Entity> map = new HashMap<>();
        map.put(joe.getId(),joe);
        map.put(glork.getId(),glork);
        map.put(glork2.getId(),glork2);

        HashMap<Long,Entity> map2 = new HashMap<>();
        map2.put(joe2.getId(),joe2);

        val mockRoom10 = mock(Room.class);
        val mockRoom11 = mock(Room.class);
        val mockRoom0 = mock(Room.class);

        when(mockRoom10.getId()).thenReturn(10L);
        when(mockRoom11.getId()).thenReturn(11L);

        when(mockRoom10.getName()).thenReturn("ROOM 10");
        when(mockRoom11.getName()).thenReturn("ROOM 11");

        when(mockRoom10.isSafeZone()).thenReturn(false);
        when(mockRoom11.isSafeZone()).thenReturn(false);
        when(mockRoom0.isSafeZone()).thenReturn(true);


        when(mockStateHandler.getRoomEntityMap(10L)).thenReturn(Single.just(map2));
        when(mockStateHandler.getRoomEntityMap(11L)).thenReturn(Single.just(map));

        List<String> list = new ArrayList<>();
        list.add("attack");

        when(this.mockAttackHandler.processAttack(any(),any())).thenReturn(Single.just(new AttackResult(glork,joe,list)));

        when(this.mockStateHandler.getAllRooms()).thenReturn(Observable.just(mockRoom11,mockRoom10));

        when(this.mockStateHandler.getAllEntitiesByRoom(0L)).thenReturn(null);

        when(this.mockStateHandler.saveEntityState(any(),any())).thenReturn(Observable.just(MudUser.makeJoe()));

        when(this.mockStateHandler.sendUserMessage(anyLong(),any())).thenReturn(Completable.complete());

        this.controllerHasMock.processMonstersInAllRooms()
                .test()
                .assertComplete()
                .assertNoErrors();
    }
}