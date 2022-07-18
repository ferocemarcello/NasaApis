package org.example;

import java.sql.*;
import java.util.Map;
import java.util.Set;

public class DAO {
    private final DbConfig dbConfig;
    private boolean isConnected;
    private Connection connection;

    public DAO(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public void connectToDb() {
        String url = "jdbc:postgresql://"+this.dbConfig.getHost()+":"+this.dbConfig.getPort()
                +"/"+this.dbConfig.getDb();
        try {
            this.connection = DriverManager.getConnection(url, this.dbConfig.getUser(),
                    this.dbConfig.getPwd());
            isConnected = true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean disconnect() {
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void putManyInDb(Set<Map.Entry<String, String>> set, Pair<String,Pair<String,String>> tableNameAndColumns) throws SQLException {//set of entries key-value
        if(set.size()<=0)return;
        String values = getSqlValues(set);
        String sql = "INSERT INTO "+tableNameAndColumns.getFirst() +" ("
                +tableNameAndColumns.getSecond().getFirst()+", "
                +tableNameAndColumns.getSecond().getSecond()+") VALUES "
                +values+" ON CONFLICT DO NOTHING";
        if(!isConnected)connectToDb();
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

    public void putInDb(String key, String val, Pair<String,Pair<String,String>> tableNameAndColumns) {

    }

    public boolean contains(String table, Pair<String,String> keyNameValue) throws SQLException {
        if(!isConnected)connectToDb();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM "+table+" WHERE "+keyNameValue.getFirst()+"='"
                +keyNameValue.getSecond()+"'");
        boolean isIn = rs.next();
        rs.close();
        stmt.close();
        return isIn;
    }

    public String get(String table, Pair<String,String> keyNameValue, String columnToRetrieve) throws SQLException {
        if(!isConnected)connectToDb();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM "+table+" WHERE "+
                keyNameValue.getFirst()+"='"+keyNameValue.getSecond()+"'");
        String val = rs.getString(1);
        rs.close();
        stmt.close();
        return val;
    }
}
