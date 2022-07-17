package org.example;

import java.util.Map;
import java.util.Set;

public class DAO {
    private final DbConfig dbConfig;

    public DAO(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public boolean connectToDb() {
        return true;//hardcoded
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
