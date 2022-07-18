package org.example;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

public class DAO {
    private final DbConfig dbConfig;
    private boolean isConnected;
    private Connection connection;

    public DAO(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void connectToDb() throws SQLException {
        if(dbConfig==null) return;
        String url = "jdbc:postgresql://"+this.dbConfig.getHost()+":"+this.dbConfig.getPort()
                +"/"+this.dbConfig.getDb();
        this.connection = DriverManager.getConnection(url, this.dbConfig.getUser(),
                this.dbConfig.getPwd());
        isConnected = true;
    }
    public boolean disconnect() {
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void putInDb(String key, String val, Pair<String,Pair<String,String>> tableNameAndColumns) {

    }
    public void putManyInDb(Set<Map.Entry<String, String>> set, Pair<String,Pair<String,String>> tableNameAndColumns) throws SQLException {
        if(!isConnected) {
            try {
                connectToDb();
            } catch (SQLException e) {
                return;
            }
        }
        if(set.size()<=0)return;
        String values = getSqlValues(set);
        String sql = "INSERT INTO "+tableNameAndColumns.getFirst() +" ("
                +tableNameAndColumns.getSecond().getFirst()+", "
                +tableNameAndColumns.getSecond().getSecond()+") VALUES "
                +values+" ON CONFLICT DO NOTHING";
        Statement st = connection.createStatement();
        st.execute(sql);
        st.close();
    }

    private String getSqlValues(Set<Map.Entry<String, String>> set) {
        StringBuilder values = new StringBuilder();
        for (Map.Entry<String,String> e: set
             ) {
            values.append("('").append(e.getKey()).append("','").append(e.getValue()).append("'),");
        }
        values.deleteCharAt(values.length()-1);
        return values.toString();
    }

    public boolean contains(String table, Pair<String,String> keyNameValue) throws SQLException {
        if(!isConnected) {
            try {
                connectToDb();
            } catch (SQLException e) {
                return false;
            }
        }
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM "+table+" WHERE "+keyNameValue.getFirst()+"='"
                +keyNameValue.getSecond()+"'");
        boolean isIn = rs.next();
        rs.close();
        stmt.close();
        return isIn;
    }

    public List<String> getFromKey(String table, Pair<String,String> keyNameValue, String columnToRetrieve) throws SQLException {
        if(!isConnected) {
            try {
                connectToDb();
            } catch (SQLException e) {
                return new ArrayList<>();
            }
        }
        List<String> values = new LinkedList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT "+columnToRetrieve+" FROM "+table+" WHERE "+
                keyNameValue.getFirst()+"='"+keyNameValue.getSecond()+"'");

        while (rs.next()){
            values.add(rs.getString(columnToRetrieve));
        }
        rs.close();
        stmt.close();
        return values;
    }
    public List<String> get(String table, String columnToRetrieve) throws SQLException {
        if(!isConnected) {
            try {
                connectToDb();
            } catch (SQLException e) {
                return new ArrayList<>();
            }
        }
        List<String> values = new LinkedList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT "+columnToRetrieve+" FROM "+table);

        while (rs.next()) values.add(rs.getString(columnToRetrieve));
        rs.close();
        stmt.close();
        return values;
    }
    public Map<String, String> filterDbToMap(String table, String[] keys, String keyName, String columnToRetrieve) throws SQLException {
        if(!isConnected) {
            try {
                connectToDb();
            } catch (SQLException e) {
                return new HashMap<>();
            }
        }
        Map<String, String> map = new HashMap<>();
        for (String key: keys
             ) {
            map.put(key,this.getFromKey(table, new Pair<>(keyName,key), columnToRetrieve).get(0));
        }
        return map;
    }
}
