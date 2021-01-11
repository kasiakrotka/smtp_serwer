package com.company.smtp.MDA;

import com.company.smtp.MDA.MDA;
import com.company.smtp.server.SMTPServer;

public class AccountCleanerThread extends Thread {

    private MDA mda;
    private long period = 60000;
    private volatile boolean shutDown = false;

    public AccountCleanerThread()
    {
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
            sleep(period);
        }
    }

    public void shutdown() {
        this.shutDown = true;
    }

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }
}
