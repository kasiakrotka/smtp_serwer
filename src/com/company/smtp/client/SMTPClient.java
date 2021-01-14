package com.company.smtp.client;

import com.company.smtp.protocol.Message;
import com.company.utils.CRLFTerminatedReader;

import javax.naming.NamingException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

public class SMTPClient implements Runnable {

    private Socket socket;
    private InputStream input;
    private CRLFTerminatedReader reader;
    //raw output
    private OutputStream outStream;
    private PrintWriter writer;
    private Message message;
    private String domain;
    private String hostName;
    private boolean connected = false;
    private final int connectionTimeout = 1000 * 120;
    private final int resposnseTimeout = 1000 * 60;
    private final int address_index;

    public SMTPClient(Message message, String domain, int i) {
        try {
            this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            this.hostName = "localhost";
        }
        this.address_index = i;
        this.message = null;
        this.domain = null;
    }

    public SMTPClient(){
        this.address_index = 0;
        try {
            this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            this.hostName = "localhost";
        }
    }

    private class Response {
        public String message;
        public int code;

        public Response(String message, int code) {
            this.message = message;
            this.code = code;
        }

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

        this.socket = new Socket();
        try {
            this.socket.setSoTimeout(this.resposnseTimeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.out.println("Can't bind hostname to ip address");
            e.printStackTrace();
        }

        try {
            this.socket.connect(new InetSocketAddress(ip1, port), this.connectionTimeout);
        } catch (IOException e) {
            System.out.println("Problem with connecting");
            e.printStackTrace();
        }
        this.connected = true;

        try {
            this.input = this.socket.getInputStream();
            this.reader = new CRLFTerminatedReader(this.input);
            this.outStream = this.socket.getOutputStream();
            this.writer = new PrintWriter(this.outStream, true);

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
        if (!this.socket.isClosed() && this.socket != null) {
            this.socket.close();
        }
    }

    @Override
    public void run() {
        if (this.domain == null || this.message == null)
            return;
        MXLookup mxLookup = new MXLookup();
        try {
            boolean sent = false;
            ArrayList<String> smtpHosts = mxLookup.doLookup(this.domain);
            for (int i = 0; i < smtpHosts.size(); i++) {
                sent = this.startSession(smtpHosts.get(i), 25);
                if (sent) {
                    System.out.println("Session ended with success");
                    break;
                }
                else{
                    this.terminate();
                }
            }
            if (sent == false){
                System.out.println("Couldn't send message");
            }
        } catch (NamingException e) {
            System.out.println("No smtp server found for given domain");
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean startSession(String smtpHost, int port) {
        System.out.println("Starting client Session");
        Response resp;
        this.connect(smtpHost, port);
        if (connected) {
            try {
                resp = read();
                if (resp.code != 220)
                    return false;
                sendLine("HELO " + hostName);
                resp = read();
                if (resp.code != 250)
                    return false;
                sendLine("MAIL FROM: <" + message.getSender() + ">");
                resp = read();
                if (resp.code != 250) {
                    sendLine("QUIT");
                    return false;
                }
                sendLine("RCPT TO: <" + message.getRecipients().get(this.address_index) + ">");
                resp = read();
                if (resp.code != 250) {
                    sendLine("QUIT");
                    return false;
                }
                sendLine("DATA");
                resp = read();
                if (resp.code != 354) {
                    sendLine("QUIT");
                    return false;
                }
                sendMessage();
                resp = read();
                if(resp.code != 250)
                    return false;
                sendLine("QUIT");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void sendMessage() throws IOException {
        String parsedLine;
        for (int i = 0; i < this.message.getHeaders().size(); i++) {
            this.sendDataLine(this.message.getHeaders().get(i));
        }
        this.sendLine("");
        for (int i = 0; i < this.message.getBody().size(); i++) {
            String line = this.message.getBody().get(i);
            if (!line.equals(""))
                if (line.charAt(0) == '.') {
                    parsedLine = ".";
                    parsedLine = parsedLine.concat(line);
                } else {
                    parsedLine = line;
                }
            else
                parsedLine = line;
            this.sendDataLine(parsedLine);
        }
        this.sendLine("");
        this.sendLine(".");
    }

    private Response read() {
        try {
            String inputLine = null;
            try {
                inputLine = this.reader.readLine();
                System.out.println("Server: " + inputLine);
            } catch (SocketException e) {
                System.out.println(e);
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            if (inputLine == "" || inputLine == null) {
                System.out.println("Input line is null");
                return null;
            }

            int code = 0;
            String message = "";

            if(inputLine.length() > 3){
                code =  Integer.parseInt(inputLine.substring(0, 3));
                message = inputLine.substring(3);
            }
            Response response = new Response(message, code);
            return  response;

        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    private void sendDataLine(String line) {
        if (connected) {
            System.out.println("Client: " + line);
            this.writer.println(line);
            this.writer.flush();
        } else {
            throw new IllegalStateException("Not connected to server");
        }
    }

    private void sendLine(String line) {
        if (connected) {
            System.out.println("Client: " + line);
            this.writer.println(line + "\r\n");
            this.writer.flush();
        } else {
            throw new IllegalStateException("Not connected to server");
        }
    }
}
