package com.company.smtp.client;

import com.company.smtp.protocol.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class SMTPClient implements Runnable{

    private Socket socket;
    private BufferedReader reader;
    private OutputStream outStream;
    private PrintWriter writer;
    private Message message;
    private boolean connected = false;
    private final int connectionTimeout = 1000 * 120;
    private final int resposnseTimeout = 1000 * 60;

    public SMTPClient() {
    }

    public SMTPClient(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void connect(String host, int port) {
        if (connected) {
            System.out.println("Already connected");
            return;
        }
        try {

            this.socket = new Socket();
            this.socket.setSoTimeout(this.resposnseTimeout);
            InetAddress ip1 = InetAddress.getByName(host);
            System.out.println(ip1);
            this.socket.connect(new InetSocketAddress(ip1, port), this.connectionTimeout);

            this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.outStream = this.socket.getOutputStream();
            this.writer = new PrintWriter(this.outStream, true);
            writer.println("HELO ISI-VAXA.ARPA\r\n");
            writer.println("VRFY admi\r\n");

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {

            try {
                terminate();
            } catch (IOException ioException) {
                System.out.println("Problem with closing connection");
            }
            e.printStackTrace();
        }
    }

    private void terminate() throws IOException {
        connected = false;
        if(!this.socket.isClosed() && this.socket != null){
            this.socket.close();
        }
    }

    @Override
    public void run() {

    }
}
