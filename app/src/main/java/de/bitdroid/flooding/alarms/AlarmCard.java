package de.bitdroid.flooding.alarms;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.levels.StationGraphActivity;
import de.bitdroid.flooding.ods.gcm.GcmStatus;
import de.bitdroid.flooding.utils.Assert;
import de.bitdroid.flooding.utils.Log;
import de.bitdroid.flooding.utils.StringUtils;
import it.gmariotti.cardslib.library.internal.Card;

final class AlarmCard extends Card {

	private final LevelAlarm alarm;
	private final AlarmManager manager;

	public AlarmCard(
			final Activity activity,
			final AlarmManager manager,
			final LevelAlarm alarm) {

		super(activity, R.layout.alarms_card);
		Assert.assertNotNull(manager, alarm);
		this.manager = manager;
		this.alarm = alarm;

		setSwipeable(true);
		setOnSwipeListener(new OnSwipeListener() {
			@Override
			public void onSwipe(Card card) {
				manager.unregister(alarm);
			}
		});
		setOnUndoSwipeListListener(new OnUndoSwipeListListener() {
			@Override
			public void onUndoSwipe(Card card) {
				manager.register(alarm);
			}
		});

		// retry registration on tap
		setOnClickListener(new OnCardClickListener() {
			@Override
			public void onClick(Card card, View view) {
				// tap to retry
				if (manager.getRegistrationStatus(alarm).equals(GcmStatus.UNREGISTERED)) {
					manager.register(alarm);

				// goto station graph
				} else if (manager.getRegistrationStatus(alarm).equals(GcmStatus.REGISTERED)) {
					Intent intent = new Intent(activity, StationGraphActivity.class);
					intent.putExtra(StationGraphActivity.EXTRA_WATER_NAME, alarm.getRiver());
					intent.putExtra(StationGraphActivity.EXTRA_STATION_NAME, alarm.getStation());
					activity.startActivity(intent);
					activity.overridePendingTransition(R.anim.slide_enter_from_right, R.anim.slide_exit_to_left);
				}
			}
		});
	}


	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {

		TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		text1.setText(getTitle());

		TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		String description;
		if (alarm.getAlarmWhenAbove()) {
			description = getContext().getString(R.string.alarms_description_above, alarm.getLevel());
		} else {
			description = getContext().getString(R.string.alarms_description_below, alarm.getLevel());
		}
		text2.setText(description);

		LinearLayout regView = (LinearLayout) view.findViewById(R.id.registration);
		TextView regStatusView = (TextView) view.findViewById(R.id.registration_status);

		GcmStatus regStatus = manager.getRegistrationStatus(alarm);
		if (regStatus.equals(GcmStatus.REGISTERED)) return;

		regView.setVisibility(View.VISIBLE);
		switch (regStatus) {
			case PENDING_REGISTRATION:
				view.findViewById(R.id.registration_pending).setVisibility(View.VISIBLE);
				regStatusView.setText(getContext().getString(R.string.alarms_registration_pending));
				break;
			case UNREGISTERED:
				view.findViewById(R.id.registration_error).setVisibility(View.VISIBLE);
				regStatusView.setText(getContext().getString(R.string.alarms_registration_error));
				break;
			case PENDING_UNREGISTRATION:
				Log.warning("Found alarm with PENDING_UNREGISTRATION");
			}

	}


	public Alarm getAlarm() {
		return alarm;
	}


	public String getTitle() {
		return StringUtils.toProperCase(alarm.getRiver()) + " - " + StringUtils.toProperCase(alarm.getStation());
	}

}
