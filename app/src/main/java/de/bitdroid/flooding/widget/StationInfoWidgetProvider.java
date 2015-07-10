package de.bitdroid.flooding.widget;

import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.google.common.base.Optional;
import com.google.inject.Injector;

import java.util.Date;

import javax.inject.Inject;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.network.DefaultErrorAction;
import de.bitdroid.flooding.network.ErrorActionBuilder;
import de.bitdroid.flooding.network.NetworkUtils;
import de.bitdroid.flooding.ods.OdsManager;
import de.bitdroid.flooding.ods.Station;
import de.bitdroid.flooding.ods.StationMeasurements;
import roboguice.RoboGuice;
import roboguice.receiver.RoboAppWidgetProvider;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * {@link android.appwidget.AppWidgetProvider} for a station info
 * widget.
 */
public class StationInfoWidgetProvider extends RoboAppWidgetProvider {

	@Inject private OdsManager odsManager;
	@Inject private NetworkUtils networkUtils;
	@Inject private WidgetDataManager widgetDataManager;


	@Override
	public void onHandleUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (final int widgetId: appWidgetIds) {
			// get stored gauge id
			final Optional<String> gaugeId = widgetDataManager.fetchGaugeId(widgetId);
			if (!gaugeId.isPresent()) continue;

			// load data and set view
			final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_station_info);
			odsManager.getStationByGaugeId(gaugeId.get())
					.flatMap(new Func1<Optional<Station>, Observable<StationMeasurements>>() {
						@Override
						public Observable<StationMeasurements> call(Optional<Station> stationOptional) {
							if (!stationOptional.isPresent()) {
								return Observable.error(new IllegalArgumentException("station with gauge id " + gaugeId + " not found"));
							}
							return odsManager.getMeasurements(stationOptional.get());
						}
					})
					.compose(networkUtils.<StationMeasurements>getDefaultTransformer())
					.subscribe(new Action1<StationMeasurements>() {
						@Override
						public void call(StationMeasurements measurements) {
							remoteViews.setTextViewText(R.id.station_name, measurements.getStation().getStationName());
							remoteViews.setTextViewText(R.id.value, measurements.getLevel().getValue() + " " + measurements.getLevel().getUnit());
							remoteViews.setTextViewText(R.id.date, DateFormat.getDateFormat(context).format(new Date(measurements.getLevelTimestamp())));
							appWidgetManager.updateAppWidget(widgetId, remoteViews);
						}
					}, new ErrorActionBuilder()
							.add(new DefaultErrorAction(context, "failed to fetch measurements"))
							.build());

			// setup click to update
			Intent intent = new Intent(context, StationInfoWidgetProvider.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.container, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
	}


	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// setup injection
		final Injector injector = RoboGuice.getOrCreateBaseApplicationInjector((Application) context.getApplicationContext());
		injector.injectMembers(this);

		// clear stored data
		for (int widgetId : appWidgetIds) {
			widgetDataManager.clearGaugeId(widgetId);
		}
	}


}