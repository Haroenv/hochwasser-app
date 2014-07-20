package de.bitdroid.flooding.main;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.alarms.AlarmFragment;
import de.bitdroid.flooding.dataselection.RiverSelectionFragment;
import de.bitdroid.flooding.levels.StationListActivity;
import de.bitdroid.flooding.monitor.SourceMonitor;
import de.bitdroid.flooding.news.NewsFragment;
import de.bitdroid.flooding.ods.data.OdsSource;
import de.bitdroid.flooding.ods.data.OdsSourceManager;
import de.bitdroid.flooding.ods.gcm.GcmStatus;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;

public class MainActivity extends FragmentActivity {

	public static String ACTION_NAVIGATE = "de.bitdroid.flooding.MainActivity.ACTION_NAVIGATE";
	public static String EXTRA_POSITION = "EXTRA_POSITION";

	private final String PREFS_KEY_FIRST_START = "FIRST_START";

	private DrawerLayout drawerLayout;
	private LinearLayout drawerMenu;
	private ListView drawerMenuList;
	private ActionBarDrawerToggle drawerToggle;
	private CharSequence drawerTitle, fragmentTitle;
	private TextView titleView;

	private NavItem[] navItems;
	private int currentNavItem = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
		drawerMenu = (LinearLayout) findViewById(R.id.menu);
		drawerMenuList = (ListView) findViewById(R.id.menu_entries);
		titleView = (TextView) findViewById(Resources.getSystem().getIdentifier("action_bar_title", "id", "android"));

		String[] navTitles = getResources().getStringArray(R.array.nav_titles);
		TypedArray navIcons = getResources().obtainTypedArray(R.array.nav_icons);
		fragmentTitle = navTitles[currentNavItem];
		drawerTitle = getTitle();

		navItems = new NavItem[navTitles.length];
		for (int i = 0; i < navItems.length; i++) {
			navItems[i] = new NavItem(navTitles[i], navIcons.getResourceId(i, -1));
		}

		// set drawer shadow
		drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// set nav items
		drawerMenuList.setAdapter(new NavListAdapter(getApplicationContext(), navItems));
		drawerMenuList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				navigateTo(position);
			}
		});

		// app icon toggles drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// drawer listener for options menus and title
		drawerToggle = new ActionBarDrawerToggle(
				this,
				drawerLayout,
				R.drawable.ic_drawer,
				R.string.nav_open,
				R.string.nav_close) {
			@Override
			public void onDrawerClosed(View view) {
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerSlide(View drawerView, float offset) {
				super.onDrawerSlide(drawerView, offset);
				CharSequence title = null;
				int alpha = 0;
				if (offset <= 0.5) {
					alpha = (int) ((0.5 - offset) * 2 * 255);
					title = fragmentTitle;
				} else {
					alpha = (int) ((offset - 0.5) * 2 * 255);
					title = drawerTitle;
				}

				titleView.setText(title);
				titleView.setTextColor(titleView.getTextColors().withAlpha(alpha));
			}
		};
		drawerLayout.setDrawerListener(drawerToggle);

		// move to home screen
		navigateTo(currentNavItem);

		// load default pref values
		PreferenceManager.setDefaultValues(
				getApplicationContext(),
				R.xml.preferences,
				false);
		SharedPreferences prefs = PreferenceManager
			.getDefaultSharedPreferences(getApplicationContext());


		// set ODS server name
		OdsSourceManager sourceManager = OdsSourceManager.getInstance(getApplicationContext());
		if (sourceManager.getOdsServerName() == null) {
			sourceManager.setOdsServerName(
					prefs.getString(getString(R.string.prefs_ods_servername_key), null));
		}


		// monitor setup
		OdsSource source = PegelOnlineSource.INSTANCE;
		boolean enabled = prefs.getBoolean(getString(R.string.prefs_ods_monitor_key), false);
		SourceMonitor monitor = SourceMonitor.getInstance(getApplicationContext());
		if (enabled && !monitor.isBeingMonitored(source)) {
			monitor.startMonitoring(source);
		}

		OdsSourceManager manager = OdsSourceManager.getInstance(getApplicationContext());
		GcmStatus status = manager.getPushNotificationsRegistrationStatus(source);
		if (!status.equals(GcmStatus.REGISTERED)) {
			manager.startPushNotifications(source);
		}

	
		// start first manual sync
		boolean firstStart = prefs.getBoolean(PREFS_KEY_FIRST_START, true);
		if (firstStart) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PREFS_KEY_FIRST_START, false);
			editor.commit();

			manager.startManualSync(source);
		}
	}


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = drawerLayout.isDrawerOpen(drawerMenu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			item.setVisible(!drawerOpen);
		}
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onResume() {
		super.onResume();
		if (!arePlayServicesAvailable()) finish();
		registerReceiver(navigationBroadcastReceiver, new IntentFilter(ACTION_NAVIGATE));
	}


	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(navigationBroadcastReceiver);
	}


	private static final String EXTRA_CURRENT_NAV_ITEM = "EXTRA_CURRENT_NAV_ITEM";

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(EXTRA_CURRENT_NAV_ITEM, currentNavItem);
	}
	

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		currentNavItem = state.getInt(EXTRA_CURRENT_NAV_ITEM);
		navigateTo(currentNavItem);
	}


	private void navigateTo(int position) {
		currentNavItem = position;

		Fragment fragment = null;
		if (position == 0) fragment = new NewsFragment();
		else if (position == 1) fragment = new AlarmFragment();
		else if (position == 2) fragment = RiverSelectionFragment.newInstance(
				StationListActivity.class, 
				R.anim.slide_enter_from_right,
				R.anim.slide_exit_to_left);
		else if (position == 3) fragment = new SettingsFragment();
		getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
		drawerMenuList.setItemChecked(position, true);
		fragmentTitle = navItems[position].getTitle();
		drawerLayout.closeDrawer(drawerMenu);
		setTitle(fragmentTitle);
	}


	private boolean arePlayServicesAvailable() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (status == ConnectionResult.SUCCESS) return true;

		if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, 42);
			dialog.show();
		}

		return false;
	}


	private BroadcastReceiver navigationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int pos = intent.getIntExtra(EXTRA_POSITION, -1);
			navigateTo(pos);
		}
	};

}