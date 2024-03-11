package org.messenger.data.entities;

import org.messenger.data.interfaces.DBSerializable;

public class User implements DBSerializable {
    private String userName;

    User(String userName){ this.userName = userName;}

    public String getUserName() {
        return userName;
    }
}
