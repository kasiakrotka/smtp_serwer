package com.company.smtp.MDA;

import com.company.smtp.protocol.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MDA {

    private final String url = "jdbc:postgresql://localhost:5432/postgres";
    private final String user = "postgres";
    private final String password = "password";
    private static final String FIND_MAILBOX_ID = "SELECT * FROM mailbox WHERE address = ?;";
    private static final String INSERT_MESSAGE = "INSERT INTO message (body, date, seen, sender, subject, mailbox_id) VALUES (?, ?, ?, ?, ?, ?) RETURNING id;";
    private static final String DELETE_ACC = "DELETE FROM mailbox WHERE id = ?";
    private static final String SELECT_OLD_ACC = "SELECT id FROM mailbox WHERE end_date < CURRENT_TIMESTAMP;";
    private static final String SELECT_MESSAGES_TO_DELETE = "SELECT id FROM message WHERE mailbox_id = ?;";
    private static final String DELETE_HEADERS = "DELETE FROM header WHERE message_id = ?;";
    private static final String DELETE_MESSAGES = "DELETE FROM message WHERE mailbox_id = ?;";
    private static final String INSERT_HEADERS = "INSERT INTO header (content, message_id) VALUES (?,?)";

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

    public void InsertMessage(Message message, int index) {
        long mailbox_id = 0;
        try {
            message.getDate();
            Connection conn = connect();
            PreparedStatement preparedStatement = conn.prepareStatement(FIND_MAILBOX_ID);
            preparedStatement.setString(1, message.getRecipients().get(index));
            preparedStatement.setMaxRows(1);
            System.out.println(preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next())
                mailbox_id = resultSet.getLong("id");


            if (mailbox_id != 0) {
                preparedStatement = conn.prepareStatement(INSERT_MESSAGE);
                preparedStatement.setString(1, message.getBody().stream().collect(Collectors.joining()));
                //"yyyy-MM-dd HH:mm:ss"
                preparedStatement.setTimestamp(2, new Timestamp(message.getDate().getTimeInMillis()), message.getDate());
                preparedStatement.setBoolean(3, false);
                preparedStatement.setString(4, message.getSender());
                preparedStatement.setString(5, message.getSubject());
                preparedStatement.setLong(6, mailbox_id);
                System.out.println(preparedStatement);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    long message_id = resultSet.getInt("id");
                    System.out.println(message_id);
                    for (int i = 0; i < message.getHeaders().size(); i++) {
                        preparedStatement = conn.prepareStatement(INSERT_HEADERS);
                        preparedStatement.setString(1, message.getHeaders().get(i));
                        preparedStatement.setLong(2, message_id);
                        System.out.println(preparedStatement);
                        preparedStatement.executeUpdate();
                    }
                }
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
            PreparedStatement preparedStatement = conn.prepareStatement(SELECT_OLD_ACC);
            ResultSet mailbox_to_delete = preparedStatement.executeQuery(); //id skrzynek do usunięcia
            while (mailbox_to_delete.next()) {
                long mailbox_id = mailbox_to_delete.getLong("id"); //id skrzynki
                preparedStatement = conn.prepareStatement(SELECT_MESSAGES_TO_DELETE);
                preparedStatement.setLong(1, mailbox_id);
                ResultSet messages = preparedStatement.executeQuery(); //wiadomości przypisane do tej skrzynki
                while (messages.next()) {
                    preparedStatement = conn.prepareStatement(DELETE_HEADERS);
                    preparedStatement.setLong(1, messages.getLong("id"));
                    preparedStatement.executeUpdate(); //usunięcie nagłówków przypisanych do tej skrzynki
                }
                preparedStatement = conn.prepareStatement(DELETE_MESSAGES);
                preparedStatement.setLong(1, mailbox_to_delete.getLong("id"));
                preparedStatement.executeUpdate(); //usuniecie wiadomosci przypisanych do skrzynki
                preparedStatement = conn.prepareStatement(DELETE_ACC);
                preparedStatement.setLong(1, mailbox_to_delete.getLong("id"));
                preparedStatement.executeUpdate(); //usuniecie skrzynki na liscie do usuniecia
            }
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
