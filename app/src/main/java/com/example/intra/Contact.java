package com.example.intra;

public class Contact {
    private final String userId;
    private final String name;
    private final String email;
    private final String profilePicURL;

    public Contact(String userId, String name, String email, String profilePicURL) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePicURL = profilePicURL;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicURL() {
        return profilePicURL;
    }
}