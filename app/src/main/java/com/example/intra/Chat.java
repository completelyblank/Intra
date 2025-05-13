package com.example.intra;

public class Chat {
    private String chatID; // Firestore document ID
    private String contactName;
    private String lastMessage;
    private String lastSenderId;
    private boolean isOnline;
    private long lastMessageTimestamp;
    private String userId;
    private String profilePicURL;

    public Chat() {
        // Needed for Firestore deserialization
    }

    public Chat(String contactName, String lastMessage, String lastSenderId, long lastMessageTimestamp, boolean isOnline, String userId, String profilePicURL, String chatID) {
        this.contactName = contactName;
        this.lastMessage = lastMessage;
        this.lastSenderId = lastSenderId;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.isOnline = isOnline;
        this.userId = userId;
        this.profilePicURL = profilePicURL;
        this.chatID = chatID;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public boolean isUserOnline() {
        return isOnline;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getOtherUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfilePicURL() {
        return profilePicURL;
    }

    public void setProfilePicURL(String profilePicURL) {
        this.profilePicURL = profilePicURL;
    }
}