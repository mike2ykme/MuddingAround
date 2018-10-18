package com.icrn.model;

import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MudCommandTest {

    @Before
    public void setup(){

    }

    @Test
    public void handleCorrectlyTypedCommand(){
        val cmdString = "move north";
        val mudCmd = MudCommand.parse(cmdString,null);
        System.out.println(mudCmd);

        val mudCmd2 = MudCommand.of(Actions.TALK,null,null);
        System.out.println(mudCmd2);
    }

    @Test
    public void handleIncorrectlyTypedCommand(){
        val badString = "bad command";
        val mudCmd = MudCommand.parse(badString,null);
        System.out.println(mudCmd);

        val mudCmd2 = MudCommand.of(null,null,null);
        System.out.println(mudCmd2);

    }
}