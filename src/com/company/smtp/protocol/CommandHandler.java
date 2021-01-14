package com.company.smtp.protocol;

import com.company.smtp.MDA.MDA;
import com.company.smtp.server.SMTPServer;
import com.company.smtp.server.Session;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler {

    private final String[] commands = {"HELO", "MAIL", "RCPT", "DATA", "RSET", "NOOP", "VRFY", "QUIT"};
    private final ArrayList<String> commandsArray;
    private final SMTPServer parentServer;
    private final SMTPResponses responses;
    private final String domainRegex = "((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
    private final String atDomainRegex = "((?!-)@[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
    private final String emailRegex = "(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})";
    private final String pathRegex = atDomainRegex + "(," + atDomainRegex + ")*+" + ":" + emailRegex;

    public CommandHandler(SMTPServer server) {
        parentServer = server;
        responses = new SMTPResponses();
        commandsArray = new ArrayList<>(Arrays.asList(commands));
    }

    public String getCommandName(String command) {
        if (command != null) {
            if (command.length() == 4)
                return command.toUpperCase();
            else {
                String commandName = command.substring(0, 4);
                return commandName.toUpperCase();
            }
        }
        return null;
    }

    public String getPathString(String command) {
        int start = command.indexOf('<');
        int end = command.indexOf('>');
        return command.substring(start + 1, end);
    }

    public Boolean checkCommandExists(String commandName) {
        return commandsArray.contains(commandName);
    }

    //including command word and <CRLF>
    public Boolean checkCommandLength(String command) {
        return (command.length() > 512);
    }

    public Boolean checkUsernameLength(String username) {
        return (username.length() > 64);
    }

    public Boolean checkDomainLength(String domain) {
        return (domain.length() > 64);
    }

    //inluding punctuation and separators
    public Boolean checkPathLength(String path) {
        return (path.length() > 256);
    }

    //including replay code and <CRLF>
    public Boolean chceckReplayLength(String replay) {
        return (replay.length() > 512);
    }

    public Boolean checkCommandSyntax(String command, String commandName) {

        if (commandName.equals("NOOP") || commandName.equals("QUIT") || commandName.equals("RSET") || commandName.equals("DATA")) {
            return true;
        }

        if (command.length() < 4)
            return false;

        String arguments = command.substring(4);

        if (arguments.charAt(0) == ' ')
            arguments = arguments.substring(1);
        else
            return false;

        switch (commandName) {
            case "HELO":
                return (arguments.matches("^" + domainRegex + "$"));
            case "MAIL":
                if (arguments.matches("^FROM:( )*+<(" + pathRegex + ")?+>$"))
                    return true;
                return arguments.matches("^FROM:<" + emailRegex + ">$");
            case "RCPT":
                if (arguments.matches("^TO:( )*+<" + pathRegex + ">$"))
                    return true;
                return arguments.matches("^TO:<" + emailRegex + ">$");
            case "VRFY":
                if (arguments.matches("^" + emailRegex + "$"))
                    return true;
                else return arguments.matches("^.*<" + emailRegex + ">$");
            default:
                return true;
        }
    }

    public LinkedList<String> getPath(String pathString) {

        LinkedList<String> path = new LinkedList<>();

        Pattern domainPattern = Pattern.compile(this.atDomainRegex);
        Pattern emailPattern = Pattern.compile(this.emailRegex);
        Matcher matcher;
        int start;
        int end;
        int colon = pathString.indexOf(':');

        if (colon == -1) {
            matcher = emailPattern.matcher(pathString);
            if (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
                path.add(pathString.substring(start, end));
            }
            return path;
        }

        String client = pathString.substring(colon + 1);
        String domainPath = pathString.substring(0, colon);

        matcher = domainPattern.matcher(domainPath);
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            //System.out.println(domainPath.substring(start, end));
            path.add(domainPath.substring(start, end));
            domainPath = domainPath.substring(end);
            matcher = domainPattern.matcher(domainPath);
        }
        matcher = emailPattern.matcher(client);
        if (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            path.add(client.substring(start, end));
        }

        return path;
    }

    public void handleCommand(Session session, String commandLine) throws IOException {
        String commandName = this.getCommandName(commandLine);

        if (!checkCommandExists(commandName)) {
            session.sendResponse(SMTPResponses.commandDoesNotExist());
            session.sendResponse(responses.UnknownCommand());
            return;
        }
        if (!checkCommandLength(commandLine)) {
            session.sendResponse(responses.LengthError());
            return;
        }
        if (!checkCommandSyntax(commandLine, commandName)) {
            session.sendResponse(responses.SyntaxError());
            return;
        }

        switch (commandName) {
            case "HELO":
                heloCommand(session);
                break;
            case "MAIL":
                mailCommand(session, commandLine);
                break;
            case "RCPT":
                rcptCommand(session, commandLine);
                break;
            case "DATA":
                dataCommand(session);
                break;
            case "RSET":
                rsetCommand(session);
                break;
            case "VRFY":
                vrfyCommand(session, commandLine);
                break;
            case "NOOP":
                break;
            case "QUIT":
                quitCommand(session);
                break;
            default:
        }

    }


    public void greetings(Session parentSession){
        parentSession.sendResponse(responses.serverGreetings(true, parentServer.getHostName()));
    }


    //trzeba jeszcze dopisać coś żeby zmieniały sie stany w ktorych jest server i w przypadku gdyby
    //był w złym stanie to żeby się resetował.
    private void heloCommand(Session parentSession){
        if (parentSession.getActualState() == Session.State.IDLE) {
            parentSession.sendResponse(responses.HELO(parentServer.getHostName()));
            parentSession.setActualState(Session.State.INITIAL);
        } else
            parentSession.sendResponse(responses.BadSequence());
    }

    private void vrfyCommand(Session parentSession, String commandLine){
        String temp = commandLine.substring(4);
        Pattern emailPattern = Pattern.compile(this.emailRegex);
        Matcher matcher;
        int start, end;
        String address;
        matcher = emailPattern.matcher(temp);
        if (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            address = temp.substring(start, end);
            int resp = checkUserExistence(address);
            if (resp == -1)
                //don't exists 500
                parentSession.sendResponse(responses.SyntaxError());
            if (resp == 0)
                //don't know 252
                parentSession.sendResponse(responses.CannotVRFY());
            if (resp == 1)
                //exists 250
                parentSession.sendResponse(responses.ActionSuccess());
        } else {
            parentSession.sendResponse(responses.SyntaxError());
        }
    }

    private int checkUserExistence(String address) {
        int at = address.indexOf('@');
        if (at == -1)
            return -1;
        String domain = address.substring(at + 1);
        if (domain.equals("ghost.com")) {
            MDA mda = new MDA();
            int matching = mda.checkIfExists(address);
            if (matching > 0)
                //address found
                return 1;
        } else {
            //don't know if exists but try to relay
            return 0;
        }
        //no address found
        return -1;
    }

    private void mailCommand(Session parentSession, String commandLine){
        if (parentSession.getActualState() == Session.State.INITIAL) {
            LinkedList<String> reversePath = getPath(getPathString(commandLine));
            String sender = reversePath.getLast();
            parentSession.getMessage().setReversePath(reversePath);
            parentSession.getMessage().setSender(sender);

            parentSession.setActualState(Session.State.TRANSACTION_STARTED);
            parentSession.sendResponse(responses.ActionSuccess());
        } else
            parentSession.sendResponse(responses.BadSequence());

    }

    private void rcptCommand(Session parentSession, String commandLine){
        if (parentSession.getActualState() == Session.State.TRANSACTION_STARTED) {
            LinkedList<String> forwardPath = getPath(getPathString(commandLine));
            String recipient = forwardPath.getLast();
            parentSession.getMessage().setForwardPath(forwardPath);
            parentSession.getMessage().addRecipient(recipient);

            parentSession.sendResponse(responses.ActionSuccess());
        } else
            parentSession.sendResponse(responses.BadSequence());
    }

    private void dataCommand(Session parentSession) throws IOException {
        if (parentSession.getActualState() == Session.State.TRANSACTION_STARTED) {
            parentSession.setActualState(Session.State.DATA_TRANSFER);
            parentSession.sendResponse(responses.DATA());
            MessageHandler messageHandler = new MessageHandler();

            if (messageHandler.handleMessage(parentSession) == MessageHandler.State.RECEIVED) {
                parentSession.sendResponse(responses.ActionSuccess());
                messageHandler.sendMessage(parentSession.getMessage());
            } else parentSession.sendResponse(responses.ActionAborted());
        } else
            parentSession.sendResponse(responses.BadSequence());

    }

    private void rsetCommand(Session parentSession){
        parentSession.setActualState(Session.State.INITIAL);
        parentSession.sendResponse(responses.ActionSuccess());
        parentSession.resetSession();
    }

    private void quitCommand(Session parentSession){
        parentSession.sendResponse(responses.Quit());
        parentSession.quit();
    }
}
