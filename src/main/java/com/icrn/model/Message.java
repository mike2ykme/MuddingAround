package com.icrn.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Message {
    @Getter public final String recipient;
    @Getter public final String sender;
    @Getter public final String message;

    public static Message of(String recipient, String sender, String Message){
        return new Message(recipient,sender,Message);
    }

}
