package org.example;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class DbConfig {
    private String host;
    private String port;

    public DbConfig(String host, String port, String user, String pwd, String db) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
        this.db = db;
    }

    private String user;

    public DbConfig(String host, String port, String user, String pwd) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }

    private String pwd;
    private String db;

    public DbConfig(String path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(new File(path));
        Yaml yaml = new Yaml();
        Map<String, String> data = yaml.load(inputStream);
        this.host = data.get("host");
        this.port = data.get("port");
        this.user = data.get("user");
        this.pwd = data.get("pwd");
        this.db = data.get("db");
        //read config from path and set config
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }
}
