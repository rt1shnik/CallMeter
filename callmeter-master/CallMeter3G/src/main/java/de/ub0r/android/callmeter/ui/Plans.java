/*
 * Copyright (C) 2009-2013 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.callmeter.ui;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TitlePageIndicator;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import de.ub0r.android.callmeter.Ads;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.TrackingUtils;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.ChangelogHelper;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Callmeter's Main {@link SherlockFragmentActivity}.
 *
 * @author flx
 */
public final class Plans extends TrackingSherlockFragmentActivity implements OnPageChangeListener {

    /**
     * Tag for output.
     */
    private static final String TAG = "Plans";

    /**
     * Ad's unit id.
     */
    private static final String AD_UNITID = "a14c185ce8841c6";

    /**
     * Ad's keywords.
     */
    public static final HashSet<String> AD_KEYWORDS = new HashSet<String>();

    static {
        AD_KEYWORDS.add("android");
        AD_KEYWORDS.add("mobile");
        AD_KEYWORDS.add("handy");
        AD_KEYWORDS.add("cellphone");
        AD_KEYWORDS.add("google");
        AD_KEYWORDS.add("htc");
        AD_KEYWORDS.add("samsung");
        AD_KEYWORDS.add("motorola");
        AD_KEYWORDS.add("market");
        AD_KEYWORDS.add("app");
        AD_KEYWORDS.add("report");
        AD_KEYWORDS.add("calls");
        AD_KEYWORDS.add("game");
        AD_KEYWORDS.add("traffic");
        AD_KEYWORDS.add("data");
        AD_KEYWORDS.add("amazon");
    }

    /**
     * {@link Message} for {@link Handler}: start background: LogMatcher.
     */
    public static final int MSG_BACKGROUND_START_MATCHER = 1;
    /**
     * {@link Message} for {@link Handler}: stop background: LogMatcher.
     */
    public static final int MSG_BACKGROUND_STOP_MATCHER = 2;
    /**
     * {@link Message} for {@link Handler}: start background: LogRunner.
     */
    public static final int MSG_BACKGROUND_START_RUNNER = 3;
    /**
     * {@link Message} for {@link Handler}: stop background: LogRunner.
     */
    public static final int MSG_BACKGROUND_STOP_RUNNER = 4;
    /**
     * {@link Message} for {@link Handler}: progress: LogMatcher.
     */
    public static final int MSG_BACKGROUND_PROGRESS_MATCHER = 5;

    /**
     * Delay for LogRunnerService to run.
     */
    private static final long DELAY_LOGRUNNER = 1500;

    /**
     * Display ads?
     */
    private static boolean prefsNoAds = true;

    private static TextView mTitle;

    private View mBackBtn;

    /**
     * {@link ViewPager}.
     */
    private static ViewPager pager;
    /**
     * {@link PlansFragmentAdapter}.
     */
    private PlansFragmentAdapter fadapter;
    private final static String IS_FIRST = "isFirstTime";
    private SharedPreferences myPref;

    public PlansFragmentAdapter getFragmentAdapter() {
        return fadapter;
    }

    /**
     * {@link Handler} for handling messages from background process.
     */
    private final Handler handler = new Handler() {
        /** LogRunner running in background? */
        private boolean inProgressRunner = false;
        /** {@link ProgressDialog} showing LogMatcher's status. */
        private ProgressDialog statusMatcher = null;

        /** Is statusMatcher a {@link ProgressDialog}? */
        private boolean statusMatcherProgress = false;

        /** String for recalculate message. */
        private String recalc = null;

        @Override
        public synchronized void handleMessage(final Message msg) {
            Log.d(TAG, "handleMessage(", msg.what, ")");
            switch (msg.what) {
                case MSG_BACKGROUND_START_RUNNER:
                    inProgressRunner = true;
                case MSG_BACKGROUND_START_MATCHER:
                    statusMatcherProgress = false;
                    Plans.this.setInProgress(1);
                    break;
                case MSG_BACKGROUND_STOP_RUNNER:
                    inProgressRunner = false;
                    Plans.this.setInProgress(-1);
                    break;
                case MSG_BACKGROUND_STOP_MATCHER:
                    if (statusMatcher != null && statusMatcher.isShowing()) {
                        try {
                            statusMatcher.dismiss();
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "error dismissing dialog", e);
                        }
                        statusMatcher = null;
                    }

                    Plans.this.setInProgress(-1);
                    Boolean isFirst = myPref.getBoolean(IS_FIRST, true);
                    if(isFirst){
                        init();
                        myPref.edit().putBoolean(IS_FIRST, false).commit();
                    }
                    Fragment f = Plans.this.fadapter.getActiveFragment(Plans.this.pager,
                            Plans.this.fadapter.getHomeFragmentPos());
                    if (f != null && f instanceof PlansFragment) {
                        ((PlansFragment) f).requery(true);
//                        ((PlansFragment) f).requery(false);
                    }
                    break;
                case MSG_BACKGROUND_PROGRESS_MATCHER:
                    if (Plans.this.progressCount == 0) {
                        Plans.this.setProgress(1);
                    }
                    if (statusMatcher == null
                            || (!this.statusMatcherProgress || statusMatcher.isShowing())) {
                        Log.d(TAG, "matcher progress: ", msg.arg1);
                        if (statusMatcher == null || !this.statusMatcherProgress) {
                            final ProgressDialog dold = statusMatcher;
                            statusMatcher = new ProgressDialog(Plans.this);
                            statusMatcher.setProgressDrawable(getResources().getDrawable(R.drawable.progress_dialog));
                            statusMatcher.setCancelable(true);
                            if (recalc == null) {
                                recalc = Plans.this.getString(R.string.reset_data_progr3);
                            }
                            statusMatcher.setMessage(recalc);
                            statusMatcher.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            statusMatcher.setMax(msg.arg2);
                            statusMatcher.setIndeterminate(false);

                            statusMatcherProgress = true;
                            Log.d(TAG, "showing dialog..");
                            try {
                                statusMatcher.show();
                                if (dold != null) {
                                    dold.dismiss();
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "activity already finished?", e);
                            }
                        }
                        statusMatcher.setProgress(msg.arg1);
                    }
                    if (recalc == null) {
                        recalc = Plans.this.getString(R.string.reset_data_progr3);
                    }
                    statusMatcher.setMax(msg.arg2);
                    statusMatcher.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    statusMatcher.setProgress(msg.arg1);
                    statusMatcher.show();
                    break;
                default:
                    break;
            }

            if (inProgressRunner) {
                if (statusMatcher == null
                        || (msg.arg1 <= 0 && !statusMatcher.isShowing())) {
                    statusMatcher = new ProgressDialog(Plans.this);
//                    statusMatcher = (ProgressBar) Plans.this.findViewById(R.id.progressbar);
                    statusMatcher.setCancelable(true);
                    statusMatcher.setMessage(Plans.this.getString(R.string.reset_data_progr3));
                    statusMatcher.setIndeterminate(true);
                    statusMatcher.setProgressDrawable(getResources().getDrawable(R.drawable.progress_dialog));

                    statusMatcherProgress = false;
                    try {
                        statusMatcher.show();
                    } catch (Exception e) {
                        Log.w(TAG, "activity already finished?", e);
                    }
                }
            } else {
//                if (statusMatcher != null && statusMatcher.isShowing()) {
//                    try {
//                        statusMatcher.dismiss();
//                    } catch (IllegalArgumentException e) {
//                        Log.e(TAG, "error dismissing dialog", e);
//                    }
//                    statusMatcher = null;
//                }
            }
        }
    };

    /**
     * Number of background tasks.
     */
    private int progressCount = 0;

    /**
     * {@link Handler} for outside.
     */
    private static Handler currentHandler = null;

    /**
     * Show all {@link PlansFragment}s.
     *
     * @author flx
     */
    public static class PlansFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {
        /**
         * {@link FragmentManager} .
         */
        private final FragmentManager mFragmentManager;
        /**
         * List of positions.
         */
        private final Long[] positions;
        /**
         * List of bill days.
         */
        private final Long[] billDays;
        /**
         * List of titles.
         */
        private final String[] titles;
        /**
         * {@link Context}.
         */
        private final Context ctx;

        /**
         * Default constructor.
         *
         * @param context {@link Context}
         * @param fm      {@link FragmentManager}
         */
        public PlansFragmentAdapter(final Context context, final FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
            ctx = context;
            ContentResolver cr = context.getContentResolver();
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
                    Log.d(TAG, "bill periods: ", bps.size());
                    if (!bps.isEmpty()) {
                        bps.remove(bps.size() - 1);
                        bps.remove(bps.size() - 1);
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
            Common.setDateFormat(context);
            final int l = positions.length;
            titles = new String[l];
            titles[l - 1] = context.getString(R.string.title_curr);
//            titles[l - 2] = context.getString(R.string.title_prev);
        }

        /**
         * Get an active fragment.
         *
         * @param container {@link ViewPager}
         * @param position  position in container
         * @return null if no fragment was initialized
         */
        public Fragment getActiveFragment(final ViewPager container, final int position) {
            String name = makeFragmentName(container.getId(), position);
            return mFragmentManager.findFragmentByTag(name);
        }

//        public Fragment getCurFragment() {
//            String name = makeFragmentName(pager.getId(), pager.getCurrentItem());
//            return mFragmentManager.findFragmentByTag(name);
//        }


        /**
         * Get a {@link Fragment}'s name.
         *
         * @param viewId container view
         * @param index  position
         * @return name of {@link Fragment}
         */
        private static String makeFragmentName(final int viewId, final int index) {
            // this might change in underlying method!
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public int getIconResId(int i) {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return positions.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Fragment getItem(final int position) {
            return PlansFragment.newInstance(position, positions[position]);
        }


        /**
         * Get position of home {@link Fragment}.
         *
         * @return position of home {@link Fragment}
         */
        public int getHomeFragmentPos() {
            return positions.length - 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CharSequence getPageTitle(final int position) {
            String ret;
            if (titles[position] == null) {
                ret = Common.formatDate(ctx, billDays[position]);
                titles[position] = ret;
            } else {
                ret = titles[position];
            }
            return ret;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setTheme(Preferences.getTheme(this));
        Utils.setLocale(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.plans);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        if (p.getAll().isEmpty()) {
            // show intro
//            startActivity(new Intent(this, IntroActivity.class));
            // set date of recordings to beginning of last month
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 0);
            c.add(Calendar.MONTH, -1);
            Log.i(TAG, "set date of recording: " + c);
            p.edit().putLong(Preferences.PREFS_DATE_BEGIN, c.getTimeInMillis()).commit();
        }

        myPref = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        Boolean isFirst = myPref.getBoolean(IS_FIRST, true);
        if(!isFirst){
            init();
        }

//        ProgressDialog progressBar = new ProgressDialog(this);
//        progressBar.setMax(100);
//        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressBar.setMessage("fgasdfgsdfgadf");
//        progressBar.setProgress(50);
//        progressBar.show();
//        init();


    }

    private void init() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        //noinspection ConstantConditions
        if (p.getAll().isEmpty()) {
            // show intro
//            startActivity(new Intent(this, IntroActivity.class));
            // set date of recordings to beginning of last month
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 0);
            c.add(Calendar.MONTH, -1);
            Log.i(TAG, "set date of recording: " + c);
            p.edit().putLong(Preferences.PREFS_DATE_BEGIN, c.getTimeInMillis()).commit();
        }

        ChangelogHelper.showChangelog(this, getString(R.string.changelog_),
                getString(R.string.app_name), R.array.updates, R.array.notes_from_dev);
//        progressDialog = new ProgressDialog(this);
//          prefsNoAds = DonationHelper.hideAds(this);

        pager = (ViewPager) findViewById(R.id.pager);

        fadapter = new PlansFragmentAdapter(this, getSupportFragmentManager());
        pager.setAdapter(fadapter);

        CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.titles);
        indicator.setViewPager(pager);

//        pager.setCurrentItem();
        indicator.setCurrentItem(fadapter.getHomeFragmentPos());
        indicator.setOnPageChangeListener(this);

        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText(R.string.title_curr);

        mBackBtn = findViewById(R.id.bottomLayout);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Plans.this.finish();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        Utils.setLocale(this);
        currentHandler = handler;
        setInProgress(0);
        PlansFragment.reloadPreferences(this);

        // schedule next update
        LogRunnerReceiver.schedNext(this, DELAY_LOGRUNNER, LogRunnerService.ACTION_RUN_MATCHER);
        LogRunnerReceiver.schedNext(this, LogRunnerService.ACTION_SHORT_RUN);
        if (!prefsNoAds) {
            Ads.loadAd(this, R.id.ad, AD_UNITID, AD_KEYWORDS);
        } else {
            findViewById(R.id.ad).setVisibility(View.GONE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        currentHandler = null;
    }

    /**
     * Get the current {@link Handler}.
     *
     * @return {@link Handler}.
     */
    public static Handler getHandler() {
        return currentHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_main, menu);
        if (prefsNoAds) {
            menu.removeItem(R.id.item_donate);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(final int position, final float positionOffset,
                               final int positionOffsetPixels) {
        // nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(final int position) {
        Log.d(TAG, "onPageSelected(", position, ")");

        Fragment f = fadapter.getActiveFragment(pager, position);
        if (f != null && f instanceof PlansFragment) {
            ((PlansFragment) f).requery(false);
        }

        if (!prefsNoAds) {
            findViewById(R.id.ad).setVisibility(View.VISIBLE);
        }

        if (position > 0) {
            mTitle.setText(R.string.title_curr);
        } else {
            mTitle.setText(R.string.title_prev);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(final int state) {
        // nothing to do
    }

    /**
     * Set progress indicator.
     *
     * @param add add number of running tasks
     */
    public synchronized void setInProgress(final int add) {
        Log.d(TAG, "setInProgress(", add, ")");
        progressCount += add;

        if (progressCount < 0) {
            Log.w(TAG, "this.progressCount: " + progressCount);
            progressCount = 0;
        }

//        Log.d(TAG, "progressCount: ", progressCount);
//        if (progressCount == 0) {
//            setSupportProgressBarIndeterminateVisibility(false);
//        } else {
//            setSupportProgressBarIndeterminateVisibility(true);
//        }
    }
}
