package com.skdev.guardianai.chatbot;

import java.io.Serializable;

/**
 * Model representing a chat message in the AI Safety Assistant.
 */
public class ChatMessage implements Serializable {
    private final String id;
    private final String message;
    private final boolean isUser;
    private final String timestamp;
    private final String actionPayload;

    public ChatMessage(String id, String message, boolean isUser, String timestamp, String actionPayload) {
        this.id = id;
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.actionPayload = actionPayload;
    }

    public String getId() { return id; }
    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
    public String getTimestamp() { return timestamp; }
    public String getActionPayload() { return actionPayload; }
}
