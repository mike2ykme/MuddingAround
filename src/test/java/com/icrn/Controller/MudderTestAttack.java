package com.icrn.Controller;

import static org.mockito.Mockito.*;

import com.icrn.dao.EntityDao;
import com.icrn.model.Actions;
import com.icrn.model.MudCommand;
import com.icrn.model.MudUser;
import com.icrn.service.StateHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;

public class MudderTestAttack {
    Mudder mudder = null;
    EntityDao entityDao = null;
    StateHandler stateHandler = null;
    @Before
    public void setup(){
        this.entityDao = Mockito.mock(EntityDao.class);
        when(entityDao.getEntityById(1L)).thenReturn(MudUser.makeJoe());
        when(entityDao.getEntityById(2L)).thenReturn(MudUser.makeJoe());
        this.stateHandler = new StateHandler(new HashMap<>());

//        this.mudder = new Mudder(this.entityDao,this.stateHandler);
        this.mudder = new Mudder(this.stateHandler);
    }

//    @Test
//    public void returns(){
//        MudCommand cmd = new MudCommand(Actions.ATTACK,Optional.of("Joe"),this.entityDao.getEntityById(1L));
//        this.mudder.HandleAction(cmd)
//                .test()
//                .assertComplete()
//                .assertValue(mudResult -> mudResult.isCompleted() );
//
//    }
//
//    @Test
//    public void cannotAttackSelf(){
//        MudCommand cmd = new MudCommand(Actions.ATTACK,Optional.of("Joe"),this.entityDao.getEntityById(1L));
//        this.mudder.HandleAction(cmd)
//                .test()
//                .assertComplete()
//                .assertValue(mudResult -> mudResult.isCompleted() == false);
//    }
}