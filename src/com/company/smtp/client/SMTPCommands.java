package com.company.smtp.client;

import java.io.PrintWriter;

public class SMTPCommands {

    private char SP = 32;
    private char CR = 13;
    private char LF = 10;

    private String domain = "local";

    //250 - success
    //500 - command unrecognized
    //501 - syntax error w argumencie
    //504 - command parameter not implemented
    //421 - Service not avaiable

}
