package de.ub0r.android.callmeter.ui;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.ub0r.android.callmeter.TrackingUtils;

/**
 * @author flx
 */
public class TrackingSherlockFragmentActivity extends SherlockFragmentActivity {

    @Override
    public void onStart() {
        super.onStart();
        TrackingUtils.startTracking(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        TrackingUtils.stopTracking(this);
    }
}
