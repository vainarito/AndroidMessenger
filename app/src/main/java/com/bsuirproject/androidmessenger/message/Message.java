package com.bsuirproject.androidmessenger.message;

public class Message {

    private String id, ownerId, text, date, chatId; // Добавлено поле chatId

    public Message(String id, String ownerId, String text, String date, String chatId) {
        this.id = id;
        this.ownerId = ownerId;
        this.text = text;
        this.date = date;
        this.chatId = chatId; // Инициализация chatId
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getChatId() {
        return chatId; // Геттер для chatId
    }

    public void setChatId(String chatId) {
        this.chatId = chatId; // Сеттер для chatId
    }
}
