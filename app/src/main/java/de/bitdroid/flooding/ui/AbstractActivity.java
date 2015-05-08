package de.bitdroid.flooding.ui;

import android.content.Intent;

import javax.inject.Inject;

import de.bitdroid.flooding.auth.LoginManager;
import de.bitdroid.flooding.auth.RestrictedResource;
import roboguice.activity.RoboActionBarActivity;

/**
 * Base activity class.
 */
public class AbstractActivity extends RoboActionBarActivity implements RestrictedResource {

	@Inject private LoginManager loginManager;

	@Override
	public void logout() {
		loginManager.clearToken();
		loginManager.clearAccountName();
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}

}
