package com.icrn.model;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.*;

public class MudUserTest {

    @Test
    public void verifyTemplate(){
        val template = StatsBasedEntityTemplate
                .builder()
                .room(0L)
                .CON(10)
                .DEX(10)
                .HP(10)
                .maxHP(20)
                .STR(10)
                .build();

        val mike = new MudUser(1L,"Mike","MIKE",template,EntityStatus.ACTIVE);
        assertTrue(mike.getMaxHP() > 0 && mike.getHP() <= mike.getMaxHP());


    }

}