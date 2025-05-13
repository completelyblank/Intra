package com.example.intra;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String text;
    private boolean isSentByCurrentUser;
    private long timestamp;

    public Message(String text, boolean isSentByCurrentUser, long timestamp) {
        this.text = text;
        this.isSentByCurrentUser = isSentByCurrentUser;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public boolean isSentByCurrentUser() {
        return isSentByCurrentUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}