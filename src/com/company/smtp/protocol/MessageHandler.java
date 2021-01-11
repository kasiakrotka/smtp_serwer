package com.company.smtp.protocol;

import com.company.smtp.MDA.MDA;
import com.company.smtp.client.SMTPClient;
import com.company.smtp.server.Session;
import com.company.utils.CRLFTerminatedReader;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;


public class MessageHandler {

    CRLFTerminatedReader reader;
    LinkedList<String> headers;
    LinkedList<String> body;
    State state;
    MDA MDA;

    public MessageHandler () {
        state = State.IDLE;
        headers = new LinkedList<String>();
        body = new LinkedList<String>();
        MDA = new MDA();
    }

    public enum State {
        IDLE, HEADERS, BODY, RECEIVED
    }

    public State handleMessage(Session parentSession) throws IOException {
        InputStream inputStream = parentSession.getInput();
        CRLFTerminatedReader reader = new CRLFTerminatedReader(inputStream);
        String inputLine = null;
        String parsedLine = null;
        state = State.HEADERS;
        while(!(inputLine = reader.readLine()).equals("")){
            headers.add(inputLine);
            System.out.println(inputLine);
        }
        state = State.BODY;

        while(!(inputLine = reader.readLine()).equals(".")){
            if (!inputLine.equals(""))
                if (inputLine.charAt(0) == '.' && inputLine.length() > 1) {
                    parsedLine = inputLine.substring(1);
                } else {
                    parsedLine = inputLine;
                }
            else
                parsedLine = inputLine;
            parsedLine = parsedLine.concat("\r\n");
            System.out.println(parsedLine);
            body.add(parsedLine);
        };
        System.out.println("End of the message");
        parentSession.getMessage().setBody(body);
        parentSession.getMessage().setHeaders(headers);
        state = State.RECEIVED;
        return state;
    }

    public void sendMessage(Message message){
        String address = message.getRecipient();
        int at = address.indexOf('@');
        if (at == -1)
            sendErrorMessage(message);
        String domain = address.substring(at + 1);
        if (domain.equals("ghost.com")) {
            MDA mda = new MDA();
            int matching = mda.checkIfExists(address);
            if (matching > 0)
                sendMessageByMDA(message);
            else
                sendErrorMessage(message);
        }
        else {
            relayMessage(message, domain);
        }
    }

    private void sendMessageByMDA(Message message){
        MDA MDA = new MDA();
        MDA.InsertMessage(message);
    }

    private void sendErrorMessage(Message message){
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        String date = sdf.format(new Date());
        Message errorMessage = new Message();
        errorMessage.setRecipient(message.getSender());
        errorMessage.setSender("");
        errorMessage.getHeaders().add("Subject: Wiadomość nie została dostarczona\r\n");
        errorMessage.getHeaders().add("Date: "+date+"\r\n");
        errorMessage.getHeaders().add("To: "+message.getRecipient()+"\r\n");
        errorMessage.getHeaders().add("From: \r\n");
        errorMessage.getHeaders().add("Content-Type: text/html; charset=UTF-8\r\n");
        errorMessage.getBody().add("Wiadomość wysłana na adres: "+message.getRecipient()+" nie została dostarczona\r\n");
        errorMessage.getBody().add("Adres nie istnieje lub wystrąpił wewnętrzny błąd serwera poczty\r\n");

        int at = message.getSender().indexOf('@');
        String domain = message.getSender().substring(at + 1);
        if (domain.equals("ghost.com"))
            sendMessageByMDA(errorMessage);
        else
            relayMessage(errorMessage, domain);
    }

    private void relayMessage(Message message, String domain){
        SMTPClient smtpClient = new SMTPClient(message, domain);
        smtpClient.run();
    }
}
