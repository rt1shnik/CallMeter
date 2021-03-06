package de.ub0r.android.callmeter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.Common;
import de.ub0r.android.logg0r.Log;

public class LogCallStatsService extends Service {
    protected final static Object lock = new Object();
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128));

    private static final long MIN = 1000 * 60;
    private static final long HOUR = MIN * 60;
    private static final long DAY = 24 * HOUR;

    private static LogCallStatsService mInstance;

    private static PendingIntent mUpdatePendingIntent;
    private static HashMap<Long, LogData> mLogDataHashMap = new HashMap<Long, LogData>();

    public static class OnMonthInfoLoadComliteListenner implements Loader.OnLoadCompleteListener<Cursor> {
        private long now;

        public OnMonthInfoLoadComliteListenner(long now) {
            this.now = now;
        }

        private OnMonthInfoLoadComliteListenner() {

        }

        @Override
        public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                if (now < 0L && data.getColumnIndex(DataProvider.Plans.SUM_COST) > 0) {
                    StringBuilder sb = new StringBuilder(DataProvider.Plans.ID + " in (-1");
                    try {
                        if (!data.isClosed() && data.moveToFirst()) {
                            do {
                                sb.append(",").append(data.getLong(DataProvider.Plans.INDEX_ID));
                            } while (data.moveToNext());
                        }
                        sb.append(")");
//                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
//                                .putString("dummy_where", sb.toString()).commit();
                    } catch (IllegalStateException ex) {
//                        Log.e(TAG, "could not walk through cursor to save shown plans", ex);
                    }
                }
            }

            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(LogCallStatsService.getInstance());
            data.moveToFirst();
            String catname;
            LogData logData = new LogData();
            while (data.isAfterLast() == false) {

                DataProvider.Plans.Plan plan = null;
                if (data.getColumnIndex(DataProvider.Plans.SUM_COST) > 0) {
                    plan = new DataProvider.Plans.Plan(data);
                } else {
                    plan = new DataProvider.Plans.Plan(data, p);
                }
//            if(plan.type == DataProvider.)

                if (plan.id == 12) {
                    long billPeriodStart = plan.billday;
                    long billPeriodEnd = plan.nextbillday;
                    logData.setBillPeriodStart(billPeriodStart);
                    logData.setBillPeriodEnd(billPeriodEnd);
                }

                if (plan.id == 15) {
                    String text = Common.formatAmount(plan.type, plan.bpBa, true);
//                    text = text + "(" + String.valueOf(plan.bpCount) + ")";
                    logData.setIncomingCallsDuration((long) plan.bpBa);
                    logData.setIncomingCallsDuration(text);
                    logData.setIncomingCallsCount(plan.bpCount);
//                mIncomingCalls.setText(text);
                }
                if (plan.id == 16) {
                    String text = Common.formatAmount(plan.type, plan.bpBa, true);
//                    text = text + "(" + String.valueOf(plan.bpCount) + ")";
                    logData.setOutgoingCallsDuration((long) plan.bpBa);
                    logData.setOutcomingCallsDuration(text);
                    logData.setOutcomingCallsCount(plan.bpCount);
//                mOutcomingCalls.setText(text);
                }

                if (plan.id == 19) {
                    logData.setSmsReceivedCount(plan.bpCount);
//                mSmsReceived.setText(String.valueOf(plan.bpCount));
                }
                if (plan.id == 20) {
                    logData.setSmsSentCount(plan.bpCount);
//                mSmsSent.setText(String.valueOf(plan.bpCount));
                }

                if (plan.id == 23) {
//                mInternet.setText(Common.getSize(plan.bpBa));
//                mInternetUnits.setText(Common.getUnits(plan.bpBa));
                    logData.setInternetValue(Common.getSize(plan.bpBa));
                    logData.setInternetUnits(Common.getUnits(plan.bpBa));
                }

                data.moveToNext();

            }


            LogCallStatsService.getInstance().writeToFile(logData);

            cursorLoader.unregisterListener(this);
            cursorLoader.stopLoading();
        }
    }

    private static LogCallStatsService getInstance() {
        return mInstance;
    }

    public LogCallStatsService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        registerNextUpdate(1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerNextUpdate(long delayInMillSec) {
        System.out.println("registerUpdate()");
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, OnUpdateReceiver.class);
        mUpdatePendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayInMillSec, mUpdatePendingIntent);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static class OnUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Long[] billDays = LogCallStatsService.getInstance().getBillDays();

            for (int i = 0; i < billDays.length; i++) {
                LogCallStatsService.getInstance().createAndStartLoader(billDays[i], i);
            }
            syncWithKloudCenter();
            LogCallStatsService.getInstance().registerNextUpdate(DAY);
        }
    }

    private static final String KLOUD_CENTER_ACTIONS = "KloudCenter.launcher.actions";
    private static final String KLOUD_CENTER_ACTION = "action";
    private static final String KLOUD_CENTER_ACTION_SYNC_CALLSTATS = "sync_callstats";

    private static void syncWithKloudCenter() {
        Intent intent = new Intent(KLOUD_CENTER_ACTIONS);
        intent.putExtra(KLOUD_CENTER_ACTION, KLOUD_CENTER_ACTION_SYNC_CALLSTATS);
        mInstance.sendBroadcast(intent);
    }

    private void createAndStartLoader(long now, int id) {
        CursorLoader cursorLoader = new CursorLoader(this, DataProvider.Plans.CONTENT_URI_SUM
                .buildUpon()
                .appendQueryParameter(DataProvider.Plans.PARAM_DATE, String.valueOf(now))
                .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_ZERO,
                        String.valueOf(false))
                .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_NOCOST,
                        String.valueOf(false))
                .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_TODAY,
                        String.valueOf(!false || now >= 0L))
                .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_ALLTIME,
                        String.valueOf(!false)).build(), DataProvider.Plans.PROJECTION_SUM,
                null, null, null);

        cursorLoader.registerListener(id, new OnMonthInfoLoadComliteListenner(now));
        cursorLoader.startLoading();
    }


    private Long[] getBillDays() {

        Long[] positions;

        Long[] billDays;

        ContentResolver cr = this.getContentResolver();
        Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
                new String[]{DataProvider.Logs.DATE}, null, null, DataProvider.Logs.DATE
                        + " ASC LIMIT 1");
        if (c == null || !c.moveToFirst()) {
            positions = new Long[]{-1L};
            billDays = positions;
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } else {
            final long minDate = c.getLong(0);
            c.close();
            c = cr.query(
                    DataProvider.Plans.CONTENT_URI,
                    DataProvider.Plans.PROJECTION,
                    DataProvider.Plans.TYPE + "=? and " + DataProvider.Plans.BILLPERIOD + "!=?",
                    new String[]{String.valueOf(DataProvider.TYPE_BILLPERIOD),
                            String.valueOf(DataProvider.BILLPERIOD_INFINITE)},
                    DataProvider.Plans.ORDER + " LIMIT 1");
            if (minDate < 0L || c == null || !c.moveToFirst()) {
                positions = new Long[]{-1L};
                billDays = positions;
                if (c != null) {
                    c.close();
                }
            } else {
                ArrayList<Long> list = new ArrayList<Long>();
                int bptype = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
                ArrayList<Long> bps = DataProvider.Plans.getBillDays(bptype,
                        c.getLong(DataProvider.Plans.INDEX_BILLDAY), minDate, -1);
                Log.d(getPackageName(), "bill periods: ", bps.size());
                if (!bps.isEmpty()) {
                    bps.remove(bps.size() - 1);
                    if(bps.isEmpty() == false) {
                        bps.remove(bps.size() - 1);
                    }
                    list.addAll(bps);
                }
                c.close();
                list.add(-1L); // current time
                positions = list.toArray(new Long[list.size()]);
                int l = positions.length;
                Arrays.sort(positions, 0, l - 1);

                billDays = new Long[l];
                for (int i = 0; i < l; i++) {
                    long pos = positions[i];
                    billDays[i] = DataProvider.Plans.getBillDay(bptype, pos + 1L, pos,
                            false).getTimeInMillis();
                }
            }
        }
        Common.setDateFormat(this);
        return positions;
    }

    private void writeToFile(LogData logData) {
        File logFolder = new File(getExternalFilesDir(null).getAbsolutePath());
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        File logFile = new File(logFolder.getPath(), logData.getFileName() + ".txt");

        try {
            Write(logFile, logData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Write(File file, LogData log) throws Exception {

        CallLogWriteHandler writeHandler = new CallLogWriteHandler(log, file);
        EXECUTOR.execute(writeHandler);
    }

    class CallLogWriteHandler implements Runnable {
        private LogData logData;
        private File logFile = null;

        public CallLogWriteHandler(LogData logData, File gpxFile) {
            this.logData = logData;
            this.logFile = gpxFile;
        }


        @Override
        public void run() {
            synchronized (LogCallStatsService.lock) {

                try {
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    } else {
                        logFile.delete();
                        logFile.createNewFile();
                    }

                    FileOutputStream initialWriter = new FileOutputStream(logFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    System.out.println(logData.toString());
                    initialOutput.write(logData.toString().getBytes());
                    initialOutput.flush();
                    initialOutput.close();


                } catch (Exception e) {
                    Log.d("error", e.getMessage());
                }

            }

        }
    }
}
