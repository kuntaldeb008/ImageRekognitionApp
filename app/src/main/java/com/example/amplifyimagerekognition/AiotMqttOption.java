package com.example.amplifyimagerekognition;

public class AiotMqttOption {
    private String username;
    private String password;
    private String clientId;

    public String getUsername()
    {
        username = Util.getProperty("USER");
        return this.username;
    }
    public String getPassword() {

        password = Util.getProperty("PASSWORD");
        return this.password;
    }
    public String getClientId() {

        clientId = Util.getProperty("CLIENTID");
        return this.clientId;
    }

}
