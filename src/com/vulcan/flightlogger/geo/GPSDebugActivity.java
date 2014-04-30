package com.vulcan.flightlogger.geo;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial;
import com.vulcan.flightlogger.FileBrowser;
import com.vulcan.flightlogger.R;
import com.vulcan.flightlogger.util.SystemUiHider;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class GPSDebugActivity extends Activity implements LocationListener {

	// used for identifying Activities that return results
	static final int LOAD_GPX_FILE = 10001;

	private final String LOGGER_TAG = GPSDebugActivity.class.getSimpleName();

	private LocationManager mLocationManager;
	private boolean mGpsEnabled;

	// need to get a better number in here to save battery life, once testing in
	// the air
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

	// need to revisit this guy, to see if we need more accuracy. Currently they
	// sample at 30 seconds
	private static final long MIN_TIME_BETWEEN_UPDATES = 1000 * 3;

	// refs to our ui controls, so we don't do a resource lookup each time
	TextView mLatTV, mLonTV, mAltTV, mSpeedTV, mAccuracyTV, mTimeTV;

	// Autohide stuff, generated by the Eclipse project. We may need to
	// extract it, it seems pretty messy to me
	private static final boolean AUTO_HIDE = true;
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
	private static final boolean TOGGLE_ON_CLICK = true;
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	private SystemUiHider mSystemUiHider;

	private static final String ACTION_USB_PERMISSION = "com.vulcan.flightlogger.USB_PERMISSION";
	PendingIntent mPermissionIntent;

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					// TODO - start the serial process here
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_gps_debug);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		initGps();

		bindUIControls();

		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		// Autohide the parent controls, to maximize monitor space when running
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

	}
	
	public void loadGPX(View view) {
	    // Do something in response to button click
		Log.d("Foo", "Foo");
		// load gpx
		Intent intent = new Intent(this, FileBrowser.class);
		this.startActivityForResult(intent, LOAD_GPX_FILE);
	}

	private void bindUIControls() {
		mLatTV = (TextView) findViewById(R.id.gpsVal1);
		mLonTV = (TextView) findViewById(R.id.gpsVal2);
		mAltTV = (TextView) findViewById(R.id.gpsVal3);
		mSpeedTV = (TextView) findViewById(R.id.gpsVal4);
		mTimeTV = (TextView) findViewById(R.id.gpsVal5);
		mAccuracyTV = (TextView) findViewById(R.id.gpsVal6);
	}

	/**
	 * GPS
	 */

	private void initGps() {
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// getting GPS status
		boolean isGPSEnabled = mLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		if (isGPSEnabled) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MIN_TIME_BETWEEN_UPDATES,
					MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
			Log.d(LOGGER_TAG, "GPS Enabled");
		} else {
			Log.d(LOGGER_TAG, "GPS not enabled");
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		mLatTV.setText(Location.convert(location.getLatitude(),
				Location.FORMAT_DEGREES));
		mLonTV.setText(Location.convert(location.getLongitude(),
				Location.FORMAT_DEGREES));
		mAltTV.setText(formatAltitude(location.getAltitude()));
		mSpeedTV.setText(String.valueOf(location.getSpeed()));
		mAccuracyTV.setText(String.valueOf(location.getAccuracy()));
		mTimeTV.setText(convertGPSTime(location.getTime()));
	}

	@SuppressLint("SimpleDateFormat")
	private String convertGPSTime(long gpsTime) {
		// TODO: see if SimpleDateFormat and Date are expensive
		Date date = new Date(gpsTime);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z");
		String text = sdf.format(date);
		return text;
	}

	private String formatAltitude(double altVal) {
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(altVal);
	}

	@Override
	public void onProviderDisabled(String arg0) {
		Log.d(LOGGER_TAG, "GPS provider disabled");

	}

	@Override
	public void onProviderEnabled(String arg0) {
		Log.d(LOGGER_TAG, "GPS provider enabled");
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		Log.d(LOGGER_TAG, "GPS status changed");

	}

	public boolean isGpsEnabled() {
		return mGpsEnabled;
	}

	public void setGpsEnabled(boolean isEnabled) {
		this.mGpsEnabled = isEnabled;
	}

	/**
	 * Hide status bars to maximize space
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	protected void onDestroy() {
		SlickUSB2Serial.cleanup(this);
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

}
