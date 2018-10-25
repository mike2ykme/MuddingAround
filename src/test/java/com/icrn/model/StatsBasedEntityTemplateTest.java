package com.icrn.model;

import lombok.val;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.matchers.Null;

import static org.junit.Assert.*;

public class StatsBasedEntityTemplateTest {

    @Test
    public void testBuilder(){
        val builder = new StatsBasedEntityTemplate.StatsBasedEntityTemplateBuilder();
        val template = builder.CON(10).DEX(10).HP(10).maxHP(20).STR(10).room(0L).build();
        assertTrue(template instanceof StatsBasedEntityTemplate);
    }

    @Test
    public void testBuilderUsingMethod(){
        val builder = StatsBasedEntityTemplate.builder();
        val template = builder.CON(10).DEX(10).HP(10).maxHP(20).STR(10).room(0L).build();
        assertTrue(template instanceof StatsBasedEntityTemplate);
    }

    @Test(expected=NullPointerException.class)
    public void testBuilderNulllPointerException(){
        val builder = StatsBasedEntityTemplate.builder();
        val template = builder.CON(10).DEX(10).HP(10).maxHP(20).STR(null).room(0L).build();
        assertTrue(template instanceof StatsBasedEntityTemplate);
    }

//    @Test(expected=NullPointerException.class)
//    public void testBuilderNulllPointerException(){
//        val builder = StatsBasedEntityTemplate.builder();
//        val template = builder.CON(10).DEX(10).HP(10).maxHP(20).STR(null).room(0L).build();
//        assertTrue(template instanceof StatsBasedEntityTemplate);
//    }
}