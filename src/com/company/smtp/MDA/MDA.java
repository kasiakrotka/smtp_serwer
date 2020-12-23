package com.company.smtp.MDA;

import com.company.smtp.protocol.Message;

import java.sql.*;
import java.util.stream.Collectors;

public class MDA {

    private final String url = "jdbc:postgresql://localhost:5432/postgres";
    private final String user = "postgres";
    private final String password = "password";
    private static final String FIND_MAILBOX_ID = "SELECT * FROM mailbox WHERE address = ?;";
    private static final String INSERT_MESSAGE = "INSERT INTO message (body, date, seen, sender, subject, mailbox_id) VALUES (?, ?, ?, ?, ?, ?);";
    private static final String DELETE_ACC = "DELETE FROM mailbox WHERE end_date < CURRENT_TIMESTAMP;";
    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public int checkIfExists(String address) {
        int count = 0;
        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(FIND_MAILBOX_ID);
            preparedStatement.setString(1, address);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next())
                ++count;

            return count;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return count;
    }

    public void InsertMessage(Message message) {
        long mailbox_id = 0;
        try {
            message.getDate();
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(FIND_MAILBOX_ID);
            preparedStatement.setString(1, message.getRecipient());
            preparedStatement.setMaxRows(1);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next())
                mailbox_id = resultSet.getLong("id");


            if(mailbox_id != 0) {
                preparedStatement = conn.prepareStatement(INSERT_MESSAGE);
                preparedStatement.setString(1, message.getBody().stream().collect(Collectors.joining()));
                //"yyyy-MM-dd HH:mm:ss"
                preparedStatement.setTimestamp(2, new Timestamp(message.getDate().getTimeInMillis()) ,message.getDate());
                preparedStatement.setBoolean(3, false);
                preparedStatement.setString(4, message.getSender());
                preparedStatement.setString(5, message.getSubject());
                preparedStatement.setLong(6, mailbox_id);
                System.out.println(preparedStatement);
                preparedStatement.executeUpdate();
            }
            conn.close();

        } catch (SQLException e) {
            printSQLException(e);
        }
    }

    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }

    public void clearAccounts() {
        try {
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(DELETE_ACC);
            int result = preparedStatement.executeUpdate();
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}