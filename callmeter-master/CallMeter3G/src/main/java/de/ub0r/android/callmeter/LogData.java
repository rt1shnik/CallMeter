package de.ub0r.android.callmeter;

/**
 * Created by morrow on 11.11.2014.
 */
public class LogData {
    private long billPeriodStart;
    private long billPeriodEnd;

    private String incomingCallsDuration;
    private int incomingCallsCount;

    private String outcomingCallsDuration;
    private int outcomingCallsCount;

    private int smsReceivedCount;
    private int smsSentCount;
    private String internetValue;
    private String internetUnits;

    public void setBillPeriodStart(long billPeriodStart) {
        this.billPeriodStart = billPeriodStart;
    }

    public void setBillPeriodEnd(long billPeriodEnd) {
        this.billPeriodEnd = billPeriodEnd;
    }

    public void setIncomingCallsDuration(String incomingCallsDuration) {
        this.incomingCallsDuration = incomingCallsDuration;
    }

    public void setIncomingCallsCount(int incomingCallsCount) {
        this.incomingCallsCount = incomingCallsCount;
    }

    public void setOutcomingCallsDuration(String outcomingCallsDuration) {
        this.outcomingCallsDuration = outcomingCallsDuration;
    }

    public void setOutcomingCallsCount(int outcomingCallsCount) {
        this.outcomingCallsCount = outcomingCallsCount;
    }

    public void setSmsReceivedCount(int smsReceivedCount) {
        this.smsReceivedCount = smsReceivedCount;
    }

    public void setSmsSentCount(int smsSentCount) {
        this.smsSentCount = smsSentCount;
    }

    public void setInternetValue(String internetValue) {
        this.internetValue = internetValue;
    }

    public void setInternetUnits(String internetUnits) {
        this.internetUnits = internetUnits;
    }
}
