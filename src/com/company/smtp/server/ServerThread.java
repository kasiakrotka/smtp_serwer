package com.company.smtp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class ServerThread extends Thread{

    private final SMTPServer parentServer;
    private final ServerSocket serverSocket;

    private final int availableConnections;
    private volatile boolean shutDown = false;
    //Semafory blokujące dostęp do wątku jeśli liczba połączeń osiągnie maksimum
    private final Semaphore connectionSemaphore;
    private final Set<Session> sessionSet;


    public ServerThread(SMTPServer smtpServer, ServerSocket serverSocket) {
        this.parentServer = smtpServer;
        this.serverSocket = serverSocket;
        this.availableConnections = this.parentServer.getMaxConnections() + 10;
        this.connectionSemaphore = new Semaphore(availableConnections);
        this.sessionSet = new HashSet<>();
    }

    public void run() {

        try{
            lookingForConnection();
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        }
    }

    private void lookingForConnection() {
        while(!this.shutDown) {
            try{
                connectionSemaphore.acquire();
            } catch (InterruptedException e) {
                continue;
            }

            Socket socket;

            try {
                socket = this.serverSocket.accept();
            } catch (IOException e) {

                connectionSemaphore.release();
                if(!this.shutDown) {
                    try{ Thread.sleep(1000);}
                    catch(InterruptedException e1){
                        e1.printStackTrace();
                    }
                }
                continue;
            }
            Session session;
            try {
                session = new Session(parentServer, this, socket);
            } catch (IOException e) {
                connectionSemaphore.release();
                System.out.println("Error while connection ");
                try {
                    socket.close();
                } catch (IOException ioException) {
                    System.out.println("Can't close socket");
                }
                continue;
            }
            synchronized (this) {
                this.sessionSet.add(session);
            }
            try{
                parentServer.getExecutorService().execute(session);
            }catch(RejectedExecutionException e){
                connectionSemaphore.release();
                synchronized (this) {
                    this.sessionSet.remove(session);
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        System.out.println("Cannot close socket after exception");
                    }
                    continue;
                }
            }
        }
    }

    public void shutdown() {
        shutDown = true;
        //shutting down socket
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing server socket");
        }
        interrupt();
        try {
            join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //shutting down session
        ArrayList<Session> sessionToBeClosed;
        synchronized (this) {
            sessionToBeClosed = new ArrayList<>(sessionSet);
        }
        for(Session sessionThread: sessionToBeClosed){
            sessionThread.quit();
        }
        parentServer.getExecutorService().shutdown();
        try{
            parentServer.getExecutorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean maxConnectionsAcquired() {
        return sessionSet.size() > parentServer.getMaxConnections();
    }
    public synchronized int getNumberOfConnections(){
        return  sessionSet.size();
    }

    public void sessionEnded(Session session) {
        synchronized (this)
        {
            sessionSet.remove(session);
        }
        connectionSemaphore.release();
    }
}
