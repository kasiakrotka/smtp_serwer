package com.company.smtp.protocol;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Message {
    private LinkedList<String> headers, body, forwardPath, reversePath, recipients;
    private String sender;

    public Message() {
        recipients = new LinkedList<>();
        headers = new LinkedList<>();
        body = new LinkedList<>();
        forwardPath = new LinkedList<>();
        reversePath = new LinkedList<>();
    }

    public Message(LinkedList<String> headers, LinkedList<String> body, String recipient, String sender) {
        recipients = new LinkedList<>();
        this.headers = headers;
        this.body = body;
        this.recipients.push(recipient);
        this.sender = sender;
    }


    public String getSubject() {
        if (headers == null)
            return null;
        else {
            ListIterator<String> listIterator = headers.listIterator();
            while (listIterator.hasNext()) {
                String header = listIterator.next();
                if ((header.split(" ", 2)[0]).contains("Subject"))
                {
                    return header.split(" ", 2)[1];
                }
            }
        }
        return null;
    }

    public Calendar getDate() {
        Calendar cal = Calendar.getInstance();

        if (headers == null) {
            cal.setTime(new Date());
            return cal;
        } else {
            ListIterator<String> listIterator = headers.listIterator();
            while (listIterator.hasNext()) {
                String header = listIterator.next();
                if ((header.split(" ", 2)[0]).contains("Date"))
                {
                    String date_string = header.split(" ", 2)[1];
                    date_string = date_string.substring(0, 25);
                    //Thu, 17 Dec 2020 00:40:40 +0100 (CET)
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                    try {
                        Date date = sdf.parse(date_string);
                        cal.setTime(sdf.parse(date_string));
                        return cal;
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        cal.setTime(new Date());
        return cal;
    }

    public LinkedList<String> getHeaders() {
        return headers;
    }

    public void setHeaders(LinkedList<String> headers) {
        this.headers = headers;
    }

    public LinkedList<String> getBody() {
        return body;
    }

    public void setBody(LinkedList<String> body) {
        this.body = body;
    }

    public LinkedList<String> getForwardPath() {
        return forwardPath;
    }

    public void setForwardPath(LinkedList<String> forwardPath) {
        this.forwardPath = forwardPath;
    }

    public LinkedList<String> getReversePath() {
        return reversePath;
    }

    public void setReversePath(LinkedList<String> reversePath) {
        this.reversePath = reversePath;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipient(LinkedList<String> recipients) {
        this.recipients = recipients;
    }

    public void addRecipient(String recipient){
        this.recipients.push(recipient);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void resetMessage() {
        this.headers = new LinkedList<>();
        this.body = new LinkedList<>();
        this.forwardPath = new LinkedList<>();
        this.reversePath = new LinkedList<>();
        this.recipients =  new LinkedList<>();
        this.sender = null;
    }
}
