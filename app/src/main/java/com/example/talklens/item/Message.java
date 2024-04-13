package com.example.talklens.item;

import com.example.talklens.util.SenderType;

public class Message {
    private String message;
    private SenderType senderType;

    public Message(String message, SenderType senderType) {
        this.message = message;
        this.senderType = senderType;
    }

    public String getMessage() {
        return message;
    }

    public SenderType getSenderType() {
        return senderType;
    }
}
