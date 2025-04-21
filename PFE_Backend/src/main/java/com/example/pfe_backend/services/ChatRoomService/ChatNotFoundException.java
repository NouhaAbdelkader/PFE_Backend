package com.example.pfe_backend.services.ChatRoomService;

public class ChatNotFoundException extends Throwable {
    public ChatNotFoundException() {
        super("Chat not found");
    }


}
