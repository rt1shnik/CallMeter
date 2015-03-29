package de.ub0r.android.callmeter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.ub0r.android.callmeter.ui.Common;

/**
 * Created by morrow on 11.11.2014.
 */
public class LogData {
    private long creationTime;

    private long billPeriodStart;
    private long billPeriodEnd;

    private String incomingCallsDuration;
    private long incomingCallsDurationLong;
    private int incomingCallsCount;

    private String outcomingCallsDuration;
    private long outgoingCallsDurationLong;
    private int outcomingCallsCount;

    private int smsReceivedCount;
    private int smsSentCount;
    private String internetValue;
    private String internetUnits;

    public LogData() {
        creationTime = getCurrTime();
    }

    public void setBillPeriodStart(long billPeriodStart) {
        this.billPeriodStart = billPeriodStart;
    }

    public void setBillPeriodEnd(long billPeriodEnd) {
        this.billPeriodEnd = billPeriodEnd;
    }

    public void setIncomingCallsDuration(String incomingCallsDuration) {
        this.incomingCallsDuration = incomingCallsDuration;
    }


    public void setIncomingCallsDuration(long value) {
        incomingCallsDurationLong = value;
    }

    public void setOutgoingCallsDuration(long value) {
        outgoingCallsDurationLong = value;
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

    private long getCurrTime() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    public String getFileName() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(billPeriodStart);
        Date date = calendar.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM");
        String fileName = simpleDateFormat.format(date);

        return fileName;
    }

    private String getCreationTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(creationTime);
        Date date = calendar.getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:HH dd-MM-yyyy");
        String creationTime = simpleDateFormat.format(date);
        return creationTime;
    }

    private String getBillPeriod(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(billPeriodStart);
        String billPeriodStart = simpleDateFormat.format(calendar.getTime());

        calendar.setTimeInMillis(billPeriodEnd);
        String billPeriodEnd = simpleDateFormat.format(calendar.getTime());

        String result = billPeriodStart + " -- " + billPeriodEnd;
        return result;
    }

    private String getIncomingCallsStat(){
        return incomingCallsDuration + "(" + String.valueOf(incomingCallsCount) + ")";
    }

    private String getOutcomingCallsStat(){
        return outcomingCallsDuration + "(" + String.valueOf(outcomingCallsCount) + ")";
    }

    private String getCombinedCallsDuration() {
        return String.valueOf(incomingCallsDurationLong + outgoingCallsDurationLong);
    }

    @Override
    public String toString() {
        StringBuilder initiaalLog = new StringBuilder();
        initiaalLog.append("Creation time:\t").append(getCreationTime());
        initiaalLog.append("\nBill period:\t").append(getBillPeriod());
        initiaalLog.append("\nIncoming calls:\t").append(getIncomingCallsStat());
        initiaalLog.append("\nOutgoing calls:\t").append(getOutcomingCallsStat());
        initiaalLog.append("\nCalls duration:\t").append(getCombinedCallsDuration());
        initiaalLog.append("\nSms received:\t").append(smsReceivedCount);
        initiaalLog.append("\nSms sent:\t").append(smsSentCount);
        initiaalLog.append("\nInternet:\t").append(internetValue).append(internetUnits);
        return initiaalLog.toString();
    }
}
