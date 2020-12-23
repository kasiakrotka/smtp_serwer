package com.company;

import com.company.smtp.MDA.MDA;
import com.company.smtp.client.MXLookup;
import com.company.smtp.client.SMTPClient;
import com.company.smtp.server.SMTPServer;
import com.company.utils.CRLFTerminatedReader;

import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
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
