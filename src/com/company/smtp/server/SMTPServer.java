package com.company.smtp.server;

import com.company.smtp.MDA.AccountCleanerThread;
import com.company.smtp.protocol.CommandHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SMTPServer {

    private int port = 8081;
    private InetAddress inetAddress = null;
    private String hostName = "localhost";
    private int timeout = 60000; //60s
    private int maxConnections = 50;
    private boolean started= false;
    private boolean cleaning = true;

    private ServerThread serverThread;
    private ExecutorService executorService;
    private final CommandHandler commandHandler;
    private AccountCleanerThread accountCleanerThread;

    public SMTPServer(){
        this.commandHandler = new CommandHandler(this);
        this.executorService = Executors.newCachedThreadPool();
        try {
            this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            this.hostName = "localhost";
        }
    }

    public SMTPServer(int port){
        this.port = port;
        this.commandHandler = new CommandHandler(this);
        this.executorService = Executors.newCachedThreadPool();
        try {
            this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            this.hostName = "localhost";
        }
    }

    public synchronized void start() {

        if(this.started)
            throw new IllegalStateException("SMTP server is already running.");

        ServerSocket serverSocket;
        try{
            serverSocket = this.createNewSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("SMTP server started");
        this.serverThread = new ServerThread(this, serverSocket);
        this.accountCleanerThread = new AccountCleanerThread();
        //start of working server
        this.serverThread.start();

        if(this.cleaning)
        {
            accountCleanerThread.start();
        }
        this.started = true;
    }

    private ServerSocket createNewSocket() throws IOException {
        InetSocketAddress inetAddress;
        if(this.inetAddress == null) {
            inetAddress = new InetSocketAddress(this.port);
        }
        else {
            inetAddress = new InetSocketAddress(this.inetAddress, this.port);
        }

        ServerSocket socket = new ServerSocket();
        socket.bind(inetAddress);

        return socket;
    }

    public synchronized  void stop() {
        if(this.serverThread == null)
            return;

        this.serverThread.shutdown();
        this.serverThread = null;
        this.accountCleanerThread.shutdown();
        this.accountCleanerThread = null;

        System.out.println("SMTP server stopped");
    }

    public void setCleaning(boolean value){
        this.cleaning = value;
    }

    public void stopCleaning(){
        if(cleaning == true && accountCleanerThread != null)
            this.accountCleanerThread.shutdown();
    }

    public synchronized boolean isRunning(){

        return this.serverThread != null;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
