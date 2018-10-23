package com.icrn.service;

import com.icrn.model.MudUser;
import com.icrn.model.StatsBasedEntity;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleAttackHandlerTest {

    private AttackHandler attackHandler;

    @Before
    public void setup(){
        this.attackHandler = new SimpleAttackHandler();
    }

    @Test
    public void processAttack() {
        val joe = MudUser.makeJoe();
        val mike = MudUser.makeJoe();
        val joeHP = joe.getHP();

        System.out.println(joe);
        mike.setName("Mike");
        mike.setId(1L);

        this.attackHandler.processAttack(mike,joe)
                .test()
                .assertNoErrors()
                .assertComplete()
                .assertValue(attackResult -> {
                    attackResult.getMessageLog().forEach(System.out::println);
                    return attackResult.getDefender().getLastAttackedById().get() == attackResult.getAttacker().getId()
                           && attackResult.getDefender().getHP() < joeHP;

                });
    }
}