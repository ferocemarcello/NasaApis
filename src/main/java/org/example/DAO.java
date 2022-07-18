package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DAO {
    private final DbConfig dbConfig;
    private boolean isConnected;
    private Connection connection;

    public DAO(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
        isConnected = connectToDb();
    }

    public boolean connectToDb() {
        String url = "jdbc:postgresql://"+this.dbConfig.getHost()+":"+this.dbConfig.getPort()
                +"/"+this.dbConfig.getDb();
        try {
            this.connection = DriverManager.getConnection(url, this.dbConfig.getUser(),
                    this.dbConfig.getPwd());
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void putManyInDb(Set<Map.Entry<String, String>> set) {//set of entries key-value
        //connect to db
        //insert if not exists
    }

    public boolean contains(String table, String key) {
        return false;//hardcoded
    }

    public String get(String yearTable, String year) {
        return "";
    }
}
