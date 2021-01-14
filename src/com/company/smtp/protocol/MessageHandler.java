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
        LinkedList<String> addresses = message.getRecipients();
        for(int i = 0; i < addresses.size(); i++){
            int at = addresses.get(i).indexOf('@');
            if (at == -1)
                sendErrorMessage(message, i);
            String domain = addresses.get(i).substring(at + 1);
            if (domain.equals("ghost.com")) {
                MDA mda = new MDA();
                int matching = mda.checkIfExists(addresses.get(i));
                if (matching > 0)
                    sendMessageByMDA(message, i);
                else
                    sendErrorMessage(message, i);
            }
            else {
                relayMessage(message, domain, i);
            }
        }
    }

    private void sendMessageByMDA(Message message, int index){
        MDA MDA = new MDA();
        MDA.InsertMessage(message, index);
    }

    private void sendErrorMessage(Message message, int i){
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        String date = sdf.format(new Date());
        Message errorMessage = new Message();
        errorMessage.addRecipient(message.getSender());
        errorMessage.setSender("");
        errorMessage.getHeaders().add("Subject: Wiadomość nie została dostarczona\r\n");
        errorMessage.getHeaders().add("Date: "+date+"\r\n");
        errorMessage.getHeaders().add("To: "+message.getSender()+"\r\n");
        errorMessage.getHeaders().add("From: \r\n");
        errorMessage.getHeaders().add("Content-Type: text/html; charset=UTF-8\r\n");
        errorMessage.getBody().add("Wiadomość wysłana na adres: "+message.getRecipients().get(i)+" nie została dostarczona\r\n");
        errorMessage.getBody().add("Adres nie istnieje lub wystrąpił wewnętrzny błąd serwera poczty\r\n");

        int at = message.getSender().indexOf('@');
        String domain = message.getSender().substring(at + 1);
        if (domain.equals("ghost.com"))
            sendMessageByMDA(errorMessage, 0);
        else
            relayMessage(errorMessage, domain, i);
    }

    private void relayMessage(Message message, String domain, int i){
        SMTPClient smtpClient = new SMTPClient(message, domain, i);
        smtpClient.run();
    }
}
