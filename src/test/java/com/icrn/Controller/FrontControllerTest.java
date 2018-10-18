package com.icrn.Controller;

import com.icrn.model.MudUser;
import com.icrn.service.StateHandler;
import io.reactivex.Maybe;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FrontControllerTest {
    MudUser joe;
    FrontController controller;
    StateHandler stateHandler;

    @Before
    public void setup(){
    this.joe = MudUser.makeJoe();
    this.stateHandler = mock(StateHandler.class);

    when(stateHandler.getEntityByName("joe")).thenReturn(Maybe.just(MudUser.makeJoe()));

    this.controller = new FrontController(stateHandler);
    }


    @Test
    public void handleCorrectLogin(){
        val username = "joe";
        val password  = "JOE";

        this.controller.maybeGetUser(username,password)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(mudUser -> {
                    System.out.println(mudUser);
                    return mudUser.getName().equalsIgnoreCase("joe");
                });
    }

    @Test
    public void handleIncorrectLogin(){
        val username = "joe";
        val password = "bad_password";

        this.controller.maybeGetUser(username,password)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertNoValues();
    }

}