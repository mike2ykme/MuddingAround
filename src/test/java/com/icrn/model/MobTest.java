package com.icrn.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MobTest {

    @Test
    public void testMakeGlork(){
        assertTrue(Mob.makeGlork() != null);
    }

}