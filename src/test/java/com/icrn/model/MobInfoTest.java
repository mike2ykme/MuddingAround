package com.icrn.model;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class MobInfoTest {

    @Test
    public void assertAbleToBeCreated(){

        val mockStat = mock(StatsBasedEntityTemplate.class);
        val mobInfo = new MobInfo();

        mobInfo.setMobName("TEST");
        mobInfo.setMobCount(2L);
        mobInfo.setStatsBasedEntityTemplate(mockStat);

        assertTrue(mobInfo.getMobName().equals("TEST"));
        assertTrue(mobInfo.getMobCount() ==2L);
        assertTrue(mobInfo.getStatsBasedEntityTemplate() == mockStat);

    }

    @Test
    public void assertStaticOfMethod(){
        String NAME = "NAME";
        long COUNT = 2l;
        val mockStats = mock(StatsBasedEntityTemplate.class);

        val test = MobInfo.of(COUNT,NAME,mockStats);

        assertTrue(test.getMobName().equals(NAME)
                    && test.getMobCount() == COUNT
                    && test.getStatsBasedEntityTemplate() == mockStats);

    }

}