package org.messenger.data;

import java.sql.*;

public class DataBaseConnection {

    public static ThreadLocal<Connection> INSTANSE = null;

    public DataBaseConnection(){
        if(INSTANSE == null){
            String jdbcURL = "jdbc:postgresql://127.0.0.1/postgres";
            try {
                Connection connection = DriverManager.getConnection(jdbcURL, "postgres","password");
                INSTANSE = new ThreadLocal<>();
                INSTANSE.set(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public ResultSet executeQuery(String query) throws SQLException{
        ResultSet resultSet = null;
        Statement statement = INSTANSE.get().createStatement();
        resultSet = statement.executeQuery(query);
        return resultSet;
    }
    public void execute(String query) throws SQLException{
        Statement statement = INSTANSE.get().createStatement();
        statement.execute(query);
    }

}
