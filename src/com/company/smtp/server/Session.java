package com.company.smtp.server;

import com.company.smtp.protocol.Message;
import com.company.smtp.protocol.SMTPResponses;
import com.company.utils.CRLFTerminatedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;

public class Session implements Runnable{

    private final SMTPServer parentServer;
    private final ServerThread parentServerThread;
    private final SMTPResponses responses;

    private Message message;

    //Communication with client
    private Socket socket;
    private InputStream input;
    private CRLFTerminatedReader reader;
    private PrintWriter writer;

    private volatile boolean quitting = false;

    public enum State {
        IDLE,
        INITIAL,
        TRANSACTION_STARTED,
        DATA_TRANSFER
    }
    State actualState = State.IDLE;

    public Session(SMTPServer server, ServerThread serverThread, Socket socket) throws IOException {
        this.message = new Message();
        this.parentServer = server;
        this.parentServerThread = serverThread;
        this.responses = new SMTPResponses();
        this.setSocket(socket);
    }

    private void setSocket(Socket socket) throws IOException{
        this.socket = socket;
        this.input = this.socket.getInputStream();
        this.reader = new CRLFTerminatedReader(this.input);
        this.writer = new PrintWriter(this.socket.getOutputStream());
        this.socket.setSoTimeout(this.parentServer.getTimeout());
    }

    public void run() {
        final String originalName = Thread.currentThread().getName();
        Thread.currentThread().setName(Session.class.getName()+"-"+socket.getInetAddress()+":"+socket.getPort());
        try {
            runCommandLoop();
        }
        catch (IOException e) {
            if(!this.quitting){
                    this.sendResponse(responses.ClosingTransmission(this.parentServer.getHostName()));
            }
        }
        catch (Throwable e) {
                this.sendResponse("421 4.3.0 Mail system failure, closing transmission channel");

            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw (Error) e;
        }
        finally {
            this.closeConnection();
            this.parentServerThread.sessionEnded(this);
            Thread.currentThread().setName(originalName);
        }
    }

    private void runCommandLoop() throws IOException {

        if(this.parentServerThread.maxConnectionsAcquired())
        {
            this.sendResponse("421 Too many connections, try again later");
            return;
        }

        this.parentServer.getCommandHandler().greetings(this);

        while(!this.quitting) {
             try {
                    String inputLine;
                 try {
                     inputLine  = this.reader.readLine();
                     System.out.println("Client: "+inputLine);
                 }catch (SocketException e) {
                     e.printStackTrace();
                     return;
                 }

                 if(inputLine == "" || inputLine == null) {
                     System.out.println("Input line is null");
                     return;
                 }

                 this.parentServer.getCommandHandler().handleCommand(this, inputLine);

             }
             catch (RejectedExecutionException e) {
                 this.sendResponse(responses.ClosingTransmission(parentServer.getHostName()));
                 return;
             }
             catch (CRLFTerminatedReader.TerminationException e) {
                 this.sendResponse(responses.SyntaxError());
                 return;
             }
             catch (CRLFTerminatedReader.MaxLineLengthException e) {
                 this.sendResponse(responses.LengthError());
                 return;
             }

        }
    }

    public void sendResponse(String response)
    {
        System.out.println(response);
        this.writer.print(response);
        this.writer.flush();
    }

    public void resetSession(){
        actualState = State.INITIAL;
        message.resetMessage();
    }

    public void closeConnection() {
        try {
            this.writer.close();
            this.input.close();
            if((this.socket != null) && this.socket.isBound() && !this.socket.isClosed())
                this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        this.quitting = true;
        this.closeConnection();
    }

    public Message getMessage() {
        return message;
    }

    public void resetState() {
        this.actualState = State.INITIAL;
    }

    public SMTPServer getParentServer() {
        return parentServer;
    }

    public State getActualState() {
        return actualState;
    }

    public void setActualState(State actualState) {
        this.actualState = actualState;
    }

    public InputStream getInput() {
        return input;
    }
}
