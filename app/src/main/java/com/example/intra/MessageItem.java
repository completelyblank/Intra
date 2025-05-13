package com.example.intra;

public interface MessageItem {
    class MessageWrapper implements MessageItem {
        private final Message message;

        public MessageWrapper(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }

        public boolean isSentByCurrentUser() {
            return message.isSentByCurrentUser();
        }

        public String getText() {
            return message.getText();
        }

        public String getFormattedTime() {
            return message.getFormattedTime();
        }

        public long getTimestamp() {
            return message.getTimestamp();
        }
    }

    class DateHeader implements MessageItem {
        private final String date;

        public DateHeader(String date) {
            this.date = date;
        }

        public String getDate() {
            return date;
        }
    }
}