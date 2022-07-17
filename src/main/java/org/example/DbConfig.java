package org.example;

public class DbConfig {
    private String host;
    private String port;
    private String user;
    private String pwd;
    private String db;

    public DbConfig(String arg) {
        //read config from path and set config
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPwd() {
        return pwd;
    }

    public String getDb() {
        return db;
    }
}
