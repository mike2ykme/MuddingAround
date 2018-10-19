package com.icrn.controller;

import static org.mockito.Mockito.*;

import com.icrn.dao.EntityDao;
import com.icrn.model.MudUser;
import com.icrn.service.StateHandler;
import org.junit.Before;
import org.mockito.Mockito;

import java.util.HashMap;

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

        this.mudder = new Mudder(this.stateHandler);
    }

}