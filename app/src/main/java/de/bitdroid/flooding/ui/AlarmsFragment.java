package de.bitdroid.flooding.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.ceps.Alarm;
import de.bitdroid.flooding.ceps.CepsManager;
import de.bitdroid.flooding.network.DefaultErrorAction;
import de.bitdroid.flooding.network.ErrorActionBuilder;
import de.bitdroid.flooding.network.HideSpinnerAction;
import de.bitdroid.flooding.network.NetworkUtils;
import de.bitdroid.flooding.ods.BodyOfWater;
import de.bitdroid.flooding.ods.Station;
import de.bitdroid.flooding.utils.StringUtils;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class AlarmsFragment extends AbstractFragment {

	private static final int
			REQUEST_ADD_ALARM = 42,
			REQUEST_SHOW_STATION_INFO = 43;

	@Inject private NetworkUtils networkUtils;
	@Inject private CepsManager cepsManager;

	@InjectView(R.id.button_add) private FloatingActionButton addButton;
	@InjectView(R.id.list) private RecyclerView recyclerView;
	@InjectView(R.id.empty) private View emptyView;
	private AlarmsAdapter adapter;


	public AlarmsFragment() {
		super(R.layout.fragment_alarms);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		analyticsUtils.onScreen("alarms screen");

		// setup list
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
		recyclerView.setLayoutManager(layoutManager);
		adapter = new AlarmsAdapter();
		recyclerView.setAdapter(adapter);

		// setup swipe to delete
		SwipeableRecyclerViewTouchListener swipeTouchListener = new SwipeableRecyclerViewTouchListener(recyclerView, new AlarmSwipeListener());
		recyclerView.addOnItemTouchListener(swipeTouchListener);

		// setup add button
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				analyticsUtils.onClick("add new alarm");
				startActivityForResult(new Intent(getActivity(), WaterSelectionActivity.class), REQUEST_ADD_ALARM);
			}
		});

		// load items
		showSpinner();
		loadAlarms();
	}


	private void loadAlarms() {
		compositeSubscription.add(cepsManager.getAlarms()
				.compose(networkUtils.<List<Alarm>>getDefaultTransformer())
				.subscribe(new Action1<List<Alarm>>() {
					@Override
					public void call(List<Alarm> alarms) {
						if (isSpinnerVisible()) hideSpinner();
						if (!alarms.isEmpty()) emptyView.setVisibility(View.GONE);
						else emptyView.setVisibility(View.VISIBLE);
						adapter.setAlarms(alarms);
					}
				}, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this.getActivity(), this, "failed to load data"))
						.add(new HideSpinnerAction(this))
						.build()));
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ADD_ALARM:
			case REQUEST_SHOW_STATION_INFO:
				loadAlarms();
				break;
		}
	}


	protected class AlarmsAdapter extends RecyclerView.Adapter<AlarmViewHolder> {

		private final List<Alarm> alarmList = new ArrayList<>();

		@Override
		public void onBindViewHolder(AlarmViewHolder holder, int position) {
			holder.setItem(alarmList.get(position));
		}

		@Override
		public int getItemCount() {
			return alarmList.size();
		}

		public void setAlarms(List<Alarm> alarmList) {
			this.alarmList.clear();
			this.alarmList.addAll(alarmList);
			notifyDataSetChanged();
		}

		public List<Alarm> getAlarms() {
			return new ArrayList<>(alarmList);
		}

		@Override
		public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_alarm, parent, false);
			return new AlarmViewHolder(view);
		}

	}


	protected class AlarmViewHolder extends RecyclerView.ViewHolder {

		private final View containerView;
		private final TextView stationView;
		private final TextView descriptionView;

		public AlarmViewHolder(View view) {
			super(view);
			this.containerView = view.findViewById(R.id.container);
			this.stationView = (TextView) view.findViewById(R.id.alarm_title);
			this.descriptionView = (TextView) view.findViewById(R.id.alarm_description);
		}

		public void setItem(final Alarm alarm) {
			// setup on click
			containerView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					analyticsUtils.onClick("show alarm details");
					Intent stationIntent = new StationSelection(alarm.getStation().getBodyOfWater(), alarm.getStation())
							.toIntent(getActivity(), StationInfoActivity.class);
					startActivityForResult(stationIntent, REQUEST_SHOW_STATION_INFO);
				}
			});

			// setup content
			stationView.setText(StringUtils.toProperCase(alarm.getStation().getStationName()) + " - "
					+ StringUtils.toProperCase(alarm.getStation().getBodyOfWater().getName()));
			String description;
			if (alarm.isAlarmWhenAboveLevel()) description = getString(R.string.alarms_description_above, alarm.getLevel());
			else description = getString(R.string.alarms_description_below, alarm.getLevel());
			descriptionView.setText(description);
		}

	}


	/**
	 * Handles swipe to delete on alarm items
	 */
	protected class AlarmSwipeListener implements SwipeableRecyclerViewTouchListener.SwipeListener {

		@Override
		public boolean canSwipe(int position) {
			return true;
		}

		@Override
		public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
			analyticsUtils.onSwipe("remove left swipe");
			removeItems(reverseSortedPositions);
		}

		@Override
		public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
			analyticsUtils.onSwipe("remove right swipe");
			removeItems(reverseSortedPositions);
		}

		private void removeItems(int[] reverseSortedPositions) {
			List<Alarm> alarmsToRemove = new ArrayList<>();
			List<Alarm> alarmsToKeep = adapter.getAlarms();
			for (int position : reverseSortedPositions) {
				alarmsToRemove.add(alarmsToKeep.remove(position));
			}
			adapter.setAlarms(alarmsToKeep);

			showSpinner();
			compositeSubscription.add(Observable.from(alarmsToRemove)
					.flatMap(new Func1<Alarm, Observable<Void>>() {
						@Override
						public Observable<Void> call(Alarm alarm) {
							return cepsManager.removeAlarm(alarm);
						}
					})
					.toList()
					.compose(networkUtils.<List<Void>>getDefaultTransformer())
					.subscribe(
							new Action1<List<Void>>() {
								@Override
								public void call(List<Void> nothing) {
									showSpinner();
									loadAlarms();
								}
							}, new ErrorActionBuilder()
									.add(new DefaultErrorAction(AlarmsFragment.this.getActivity(), AlarmsFragment.this, "error removing alarm"))
									.add(new HideSpinnerAction(AlarmsFragment.this))
									.build()));
		}

	}


	/**
	 * The glue that is necessary kick of the station selection process and show new {@link NewAlarmActivity}
	 * when a station has been selected.
	 */
	public static class WaterSelectionActivity extends AbstractWaterSelectionActivity {

		@Override
		protected void onWaterSelected(BodyOfWater water) {
			startActivity(new StationSelection(water).toIntent(this, StationSelectionActivity.class));
		}

		@Override
		protected Class<? extends AbstractMapSelectionActivity> getMapSelectionClass() {
			return MapSelectionActivity.class;
		}

	}


	public static class StationSelectionActivity extends AbstractStationSelectionActivity {

		public StationSelectionActivity() {
			super(false);
		}

		@Override
		protected void onAllStationsSelected() { }

		@Override
		protected void onStationSelected(Station station) {
			AlarmsFragment.onStationSelected(this, station);
		}

		@Override
		protected Class<? extends AbstractMapSelectionActivity> getMapSelectionClass() {
			return MapSelectionActivity.class;
		}

	}


	public static class MapSelectionActivity extends AbstractMapSelectionActivity {

		@Override
		public void onStationSelected(Station station) {
			AlarmsFragment.onStationSelected(this, station);
		}

	}


	private static void onStationSelected(Context context, Station station) {
		Intent intent = new StationSelection(station.getBodyOfWater(), station).toIntent(context, NewAlarmActivity.class);
		intent.putExtra(NewAlarmActivity.EXTRA_START_MAIN_ACTIVITY_ON_FINISH, true);
		context.startActivity(intent);
	}

}

