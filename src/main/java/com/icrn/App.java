package com.icrn;

import com.icrn.io.TelnetServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Hello world!
 *
 */
@Slf4j
public class App 
{

    Executor executors = Executors.newCachedThreadPool();

    public static void main( String[] args )
    {
        log.debug("TEST");
        System.out.println( "Hello World!" );
    }

//    TelnetServer telnetServer = new TelnetServer(executors,1,1);
}
