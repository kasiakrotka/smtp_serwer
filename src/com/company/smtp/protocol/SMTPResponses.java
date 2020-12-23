package com.company.smtp.protocol;

import java.util.ArrayList;

public class SMTPResponses {

    private char SP = 32;
    private char CR = 13;
    private char LF = 10;

    public static String commandDoesNotExist() {
        return null;
    }

    public static void commandTooLong() {
    }

    public static void commandBadSyntax() {
    }

    public String serverGreetings(boolean connection, String domain) {
        if (connection) {
            return "220 " + domain + " SMTP service ready" + CR + LF;
        } else {
            return "421 " + domain + " " + CR + LF;
        }
    }

    public String HELO(String domain) {
        return "250 "+domain+CR+LF;
    }

    public String ActionSuccess() {
        return "250 OK" + CR + LF;
    }

    public String Quit(){
        return "221 Service closing transmission channel" + CR + LF;
    }

    public String UserNotFound(ArrayList<String> forwardPath){
        String forwardPathString;
        forwardPathString = forwardPath.toString();
        return "251 User not local; will forward to "+forwardPathString + CR + LF;
    }

    public String DATA() {
        return "354 Start mail input"+ CR + LF;
    }

    //moze byc odpowiedzia na jakakolwiek komende, jesli serwer wie że sie zamyka
    public String ClosingTransmission(String domain) {
        return "421 "+ domain + " service not available, closing transmission channel"+ CR + LF;
    }

    public String MailboxUnavailable() {
        return "450 Requested mail action not taken: mailbox unavailable" + CR + LF;
    }

    public String ActionAborted() {
        return "451 Requested action aborted: local error in processing" + CR + LF;
    }

    public String fullStorage() {
        return "452 Requested action not taken: insufficient system storage" + CR + LF;
    }

    public String UnknownCommand() {
        return "500 Syntax error, command unrecognized" + CR + LF;
    }

    public String SyntaxError() {

        return "501 Syntax error in parameters or arguments" + CR + LF;
    }

    public String LengthError() {

        return "501 Syntax error - command too long" + CR + LF;
    }

    public  String CannotVRFY(){
        return "252  Cannot VRFY user, but will accept message and attempt delivery" +CR+LF;
    }
    public String BadSequence() {
        return "503 Bad sequence of commands" + CR + LF;
    }

    public String UserNotLocal(String forwardPath) {
        return "551 User not local; please try " + forwardPath + + CR + LF;
    }
}
