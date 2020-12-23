package com.company.smtp.server;

import com.company.smtp.MDA.MDA;

public class AccountCleanerThread extends Thread {

    private SMTPServer parentServer;
    private MDA mda;
    private volatile boolean shutDown = false;

    public AccountCleanerThread(SMTPServer server)
    {
        this.parentServer = server;
        this.mda = new MDA();
    }

    public void run() {
        try {
            lookForOutdated();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void lookForOutdated() throws InterruptedException {
        while(!this.shutDown){
            System.out.println("Cleaning outdated accounts");
            mda.clearAccounts();
            sleep(60000);
        }
    }

    public void shutdown() {
        this.shutDown = true;
    }
}
