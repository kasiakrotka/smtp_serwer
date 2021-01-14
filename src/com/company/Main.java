package com.company;

import com.company.smtp.MDA.MDA;
import com.company.smtp.client.MXLookup;
import com.company.smtp.client.SMTPClient;
import com.company.smtp.protocol.Message;
import com.company.smtp.server.SMTPServer;
import com.company.utils.CRLFTerminatedReader;

import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
        String date = sdf.format(new Date());
        MDA mda = new MDA();
        Message message = new Message();
        LinkedList<String> body = new LinkedList();
        body.add("Pierwszy wiersz wiadomosci\r\n");
        body.add("Drugi wiersz wiadomości\r\n");
        LinkedList<String> headers = new LinkedList<>();
        headers.add("Subject: Wiadomość nie została dostarczona\r\n");
        headers.add("Date: "+date+"\r\n");
        headers.add("To: kasia@ghost.com\r\n");
        headers.add("From: \r\n");
        message.setHeaders(headers);
        message.setSender("jakis@gmail.com");
        message.addRecipient("kasia@ghost.com");
        message.setBody(body);
        //mda.InsertMessage(message);
/*
        MXLookup mx = new MXLookup();
        try {
            ArrayList res = mx.doLookup("gmail.com");
            for(int i = 0; i < res.size(); i++)
            System.out.println(res.get(i));
        } catch (NamingException e) {
            e.printStackTrace();
        }
*/
        SMTPServer server = new SMTPServer();
        server.start();

       // SMTPClient client = new SMTPClient();
        //client.connect("localhost",8081);
        return;
    }
}
