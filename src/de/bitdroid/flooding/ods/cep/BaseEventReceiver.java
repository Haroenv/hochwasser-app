package de.bitdroid.flooding.ods.cep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public abstract class BaseEventReceiver extends BroadcastReceiver {

	protected final static String ACTION_EVENT_RECEIVED = "de.bitdroid.flooding.ods.cep.ACTION_EVENT_RECEIVED";
	protected final static String EXTRA_EVENTID = "event";


	@Override
	public final void onReceive(Context context, Intent intent) {
		String eventId = intent.getStringExtra(EXTRA_EVENTID);
		onReceive(context, eventId);
	}


	protected abstract void onReceive(Context context, String eventId);

}