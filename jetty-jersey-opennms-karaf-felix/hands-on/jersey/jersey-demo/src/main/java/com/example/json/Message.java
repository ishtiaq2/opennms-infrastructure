package com.example.json;

public class Message {
    private String text;
    private String recipient;
    private long timestamp;

    public Message() {}

    public Message(String text, String recipient) {
        this.text = text;
        this.recipient = recipient;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}