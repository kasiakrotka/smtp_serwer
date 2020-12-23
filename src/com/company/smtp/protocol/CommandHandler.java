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
    private ArrayList<String> commandsArray;
    private final SMTPServer parentServer;
    private final SMTPResponses responses;
    private final String domainRegex = "((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
    private final String atDomainRegex = "((?!-)@[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}";
    private final String CRLFRegex = "\\r\\n";
    private final String emailRegex = "(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})";
    private final String pathRegex = atDomainRegex + "(," + atDomainRegex + ")*+" + ":" + emailRegex;

    public CommandHandler(SMTPServer server) {
        parentServer = server;
        responses = new SMTPResponses();
        commandsArray = new ArrayList<String>(Arrays.asList(commands));
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
        String path = command.substring(start + 1, end);

        return path;
    }

    public Boolean checkCommandExists(String commandName) {
        if (commandsArray.contains(commandName))
            return true;
        else
            return false;
    }

    //including command word and <CRLF>
    public Boolean checkCommandLength(String command) {
        if (command.length() > 512)
            return false;
        else
            return true;
    }

    public Boolean checkUsernameLength(String username) {
        if (username.length() > 64)
            return false;
        else
            return true;
    }

    public Boolean checkDomainLength(String domain) {
        if (domain.length() > 64)
            return false;
        else
            return true;
    }

    //inluding punctuation and separators
    public Boolean checkPathLength(String path) {
        if (path.length() > 256)
            return false;
        else
            return true;
    }

    //including replay code and <CRLF>
    public Boolean chceckReplayLength(String replay) {
        if (replay.length() > 512)
            return false;
        else
            return true;
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
                if (arguments.matches("^" + domainRegex + "$"))
                    return true;
                else
                    return false;
            case "MAIL":
                if (arguments.matches("^FROM:( )*+<(" + pathRegex + ")?+>$"))
                    return true;
                if (arguments.matches("^FROM:<" + emailRegex + ">$"))
                    return true;
                return false;
            case "RCPT":
                if (arguments.matches("^TO:( )*+<" + pathRegex + ">$"))
                    return true;
                if (arguments.matches("^TO:<" + emailRegex + ">$"))
                    return true;
                return false;
            case "DATA":
                return true;
            case "VRFY":
                if (arguments.matches("^" + emailRegex + "$"))
                    return true;
                else if (arguments.matches("^.*<" + emailRegex + ">$"))
                    return true;
                else
                    return false;
            default:
                return false;
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
        Session parentSession = session;

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
                heloCommand(parentSession, commandLine);
                break;
            case "MAIL":
                mailCommand(parentSession, commandLine);
                break;
            case "RCPT":
                rcptCommand(parentSession, commandLine);
                break;
            case "DATA":
                dataCommand(parentSession, commandLine);
                break;
            case "RSET":
                rsetCommand(parentSession, commandLine);
                break;
            case "VRFY":
                vrfyCommand(parentSession, commandLine);
                break;
            case "NOOP":
                break;
            case "QUIT":
                quitCommand(parentSession, commandLine);
                break;
            default:
        }

    }


    public void greetings(Session parentSession) throws IOException {
        parentSession.sendResponse(responses.serverGreetings(true, parentServer.getHostName()));
    }

    private void ehloCommand(Session parentSession, String commandLine) {

    }

    //trzeba jeszcze dopisać coś żeby zmieniały sie stany w ktorych jest server i w przypadku gdyby
    //był w złym stanie to żeby się resetował.
    private void heloCommand(Session parentSession, String commandLine) throws IOException {
        if (parentSession.getActualState() == Session.State.IDLE) {
            parentSession.sendResponse(responses.HELO(parentServer.getHostName()));
            parentSession.setActualState(Session.State.INITIAL);
        } else
            parentSession.sendResponse(responses.BadSequence());
    }

    private void vrfyCommand(Session parentSession, String commandLine) throws IOException {
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
            int resp = checkUserExistance(address);
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

    private int checkUserExistance(String address) {
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

    private void mailCommand(Session parentSession, String commandLine) throws IOException {
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

    private void rcptCommand(Session parentSession, String commandLine) throws IOException {
        if (parentSession.getActualState() == Session.State.TRANSACTION_STARTED) {
            LinkedList<String> forwardPath = getPath(getPathString(commandLine));
            String recipient = forwardPath.getLast();
            parentSession.getMessage().setForwardPath(forwardPath);
            parentSession.getMessage().setRecipient(recipient);

            parentSession.sendResponse(responses.ActionSuccess());
        } else
            parentSession.sendResponse(responses.BadSequence());
    }

    private void dataCommand(Session parentSession, String commandLine) throws IOException {
        if (parentSession.getActualState() == Session.State.TRANSACTION_STARTED) {
            parentSession.setActualState(Session.State.DATA_TRANSFER);
            parentSession.sendResponse(responses.DATA());
            MessageHandler messageHandler = new MessageHandler();

            if (messageHandler.handleMessage(parentSession) == MessageHandler.State.RECEIVED) {
                parentSession.sendResponse(responses.ActionSuccess());
            } else parentSession.sendResponse(responses.ActionAborted());
        } else
            parentSession.sendResponse(responses.BadSequence());

    }

    private void sendMessageByMDA(){

    }

    private void sendErrorMessage(){}

    private void relayMessage(){

    }

    private void rsetCommand(Session parentSession, String commandLine) throws IOException {
        parentSession.setActualState(Session.State.INITIAL);
        parentSession.sendResponse(responses.ActionSuccess());
        parentSession.resetState();
    }

    private void quitCommand(Session parentSession, String commandLine) throws IOException {
        parentSession.sendResponse(responses.Quit());
        parentSession.quit();
    }


}
