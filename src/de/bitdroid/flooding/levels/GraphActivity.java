package de.bitdroid.flooding.levels;

import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_HTHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_HTHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTHW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTHW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MTNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_MW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_NTNW_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_CHARVALUES_NTNW_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_TIMESTAMP;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_TYPE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_ZERO_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_ZERO_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_KM;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_NAME;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_WATER_NAME;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.androidplot.xy.XYPlot;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.map.MapActivity;
import de.bitdroid.flooding.monitor.SourceMonitor;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;
import de.bitdroid.flooding.utils.AbstractLoaderCallbacks;

public class GraphActivity extends Activity {
	
	public static final String EXTRA_WATER_NAME = "waterName";

	private static final int LOADER_ID = 44;

	private WaterGraph graph;
	private boolean showingRegularSeries = true;
	private boolean showingSeekbar = false;
	private Cursor levelData;
	private String waterName;

	private MonitorSourceLoader loader;
	private long currentTimestamp;
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.graph);

		// enable action bar back button
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// setup graph
		waterName = getIntent().getExtras().getString(EXTRA_WATER_NAME);
		XYPlot graphView = (XYPlot) findViewById(R.id.graph);
		this.graph = new WaterGraph(graphView, waterName, getApplicationContext());
		if (showingRegularSeries) graph.setSeries(getRegularSeries());
		else graph.setSeries(getNormalizedSeries());

		// setup seekbar
		SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
		final List<Long> timestamps = SourceMonitor
			.getInstance(getApplicationContext())
			.getAvailableTimestamps(PegelOnlineSource.INSTANCE);
		Collections.sort(timestamps);
		seekbar.setMax(timestamps.size() - 1);
		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				if (fromUser) setTimestamp(timestamps.get(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekbar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekbar) { }
		});


		// get latest timestamp
		currentTimestamp = Collections.max(timestamps);

		AbstractLoaderCallbacks loaderCallbacks = new AbstractLoaderCallbacks(LOADER_ID) {

			@Override
			protected void onLoadFinishedHelper(Loader<Cursor> loader, Cursor cursor) {
				GraphActivity.this.levelData = cursor;
				graph.setData(cursor, currentTimestamp);
			}

			@Override
			protected void onLoaderResetHelper(Loader<Cursor> loader) {
				GraphActivity.this.levelData = null;
			}

			@Override
			protected Loader<Cursor> getCursorLoader() {

				return new MonitorSourceLoader(
						getApplicationContext(),
						PegelOnlineSource.INSTANCE,
						new String[] { 
							COLUMN_STATION_NAME,
							COLUMN_STATION_KM,
							COLUMN_LEVEL_TIMESTAMP,
							COLUMN_LEVEL_VALUE,
							COLUMN_LEVEL_UNIT,
							COLUMN_LEVEL_ZERO_VALUE,
							COLUMN_LEVEL_ZERO_UNIT,
							COLUMN_CHARVALUES_MW_VALUE,
							COLUMN_CHARVALUES_MW_UNIT,
							COLUMN_CHARVALUES_MHW_VALUE,
							COLUMN_CHARVALUES_MHW_UNIT,
							COLUMN_CHARVALUES_MNW_VALUE,
							COLUMN_CHARVALUES_MNW_UNIT,
							COLUMN_CHARVALUES_MTHW_VALUE,
							COLUMN_CHARVALUES_MTHW_UNIT,
							COLUMN_CHARVALUES_MTNW_VALUE,
							COLUMN_CHARVALUES_MTNW_UNIT,
							COLUMN_CHARVALUES_HTHW_VALUE,
							COLUMN_CHARVALUES_HTHW_UNIT,
							COLUMN_CHARVALUES_NTNW_VALUE,
							COLUMN_CHARVALUES_NTNW_UNIT
						}, COLUMN_WATER_NAME + "=? AND " 
							+ COLUMN_LEVEL_TYPE + "=?",
						new String[] { waterName, "W" },
						null,
						currentTimestamp);
			}
		};

		getLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks);
		Loader<Cursor> cursorLoader = getLoaderManager().getLoader(LOADER_ID);
		this.loader = (MonitorSourceLoader) cursorLoader;
    }


	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_enter_from_left, R.anim.slide_exit_to_right);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.graph_menu, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (showingRegularSeries) 
			menu.findItem(R.id.normalize).setTitle(getString(R.string.menu_graph_normalize));
		else 
			menu.findItem(R.id.normalize).setTitle(getString(R.string.menu_graph_regular));
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case android.R.id.home:
				finish();
				overridePendingTransition(R.anim.slide_enter_from_left, R.anim.slide_exit_to_right);
				return true;

			case R.id.select_series:
				List<String> seriesKeys = graph.getSeriesKeys();
				final String[] items = seriesKeys.toArray(new String[seriesKeys.size()]);
				final boolean[] selectedItems = new boolean[seriesKeys.size()];
				int i = 0;
				for (String item : items) {
					if (graph.isVisible(item)) selectedItems[i] = true;
					i++;
				}

				new AlertDialog.Builder(this)
					.setTitle(getString(R.string.series_select_dialog_title))
					.setMultiChoiceItems(
							items,
							selectedItems, 
							new DialogInterface.OnMultiChoiceClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int idx, boolean checked) {
							selectedItems[idx] = checked;
						}
					})
					.setNegativeButton(getString(R.string.btn_cancel), null)
					.setPositiveButton(getString(R.string.btn_ok) , new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							Set<String> visibleSeries = new HashSet<String>();
							for (int i = 0; i < selectedItems.length; i++) 
								if (selectedItems[i]) visibleSeries.add(items[i]);
							graph.setVisibleSeries(visibleSeries);
							
						}
					}).create().show();
				return true;

			case R.id.normalize:
				if (showingRegularSeries) graph.setSeries(getNormalizedSeries());
				else graph.setSeries(getRegularSeries());
				this.showingRegularSeries = !showingRegularSeries;
				if (levelData != null) graph.setData(levelData, currentTimestamp);
				return true;

			case R.id.timestamp:
				final List<Long> timestamps = SourceMonitor
					.getInstance(getApplicationContext())
					.getAvailableTimestamps(PegelOnlineSource.INSTANCE);
				Collections.sort(timestamps);

				SimpleDateFormat formatter = new SimpleDateFormat("dd/M/yyyy hh:mm a");
				List<String> stringTimestamps = new LinkedList<String>();
				for (long time : timestamps) {
					stringTimestamps.add(formatter.format(new Date(time)));
				}

				final long originalTimestamp = currentTimestamp;
				int selectedTimestamp = timestamps.indexOf(currentTimestamp);

				new AlertDialog.Builder(this)
					.setTitle(getString(R.string.series_monitor_dialog_title))
					.setSingleChoiceItems(
							stringTimestamps.toArray(new String[timestamps.size()]), 
							selectedTimestamp,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int idx) {
									setTimestamp(timestamps.get(idx));
									updateScrollbar();
								}
							})
					.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int idx) {
							setTimestamp(originalTimestamp);
							updateScrollbar();
						}
					})
					.setPositiveButton(R.string.btn_ok, null)
					.create().show();

				return true;

			case R.id.seekbar:
				showingSeekbar = !showingSeekbar;
				SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
				if (showingSeekbar) seekbar.setVisibility(View.VISIBLE);
				else seekbar.setVisibility(View.GONE);
				return true;

			case R.id.map:
				Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
				mapIntent.putExtra(MapActivity.EXTRA_WATER_NAME, waterName);
				startActivity(mapIntent);
				return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}

	
	private static final String 
		EXTRA_SHOWING_REGULAR_SERIES = "EXTRA_SHOWING_REGULAR_SERIES",
		EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP",
		EXTRA_SHOWING_SEEKBAR = "EXTRA_SHOWING_SEEKBAR";

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putBoolean(EXTRA_SHOWING_REGULAR_SERIES, showingRegularSeries);
		state.putLong(EXTRA_TIMESTAMP, currentTimestamp);
		state.putBoolean(EXTRA_SHOWING_SEEKBAR, showingSeekbar);
		graph.saveState(state);
	}


	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);

		// restore series
		this.showingRegularSeries = state.getBoolean(EXTRA_SHOWING_REGULAR_SERIES);
		if (!showingRegularSeries) {
			graph.setSeries(getNormalizedSeries());
			if (levelData != null) graph.setData(levelData, currentTimestamp);
		}
		graph.restoreState(state);

		// restore timestamp
		currentTimestamp = state.getLong(EXTRA_TIMESTAMP);
		if (loader != null) loader.setTimestamp(currentTimestamp);

		// restore seekbar
		showingSeekbar = state.getBoolean(EXTRA_SHOWING_SEEKBAR);
		if (showingSeekbar) findViewById(R.id.seekbar).setVisibility(View.VISIBLE);
	}




	private List<Pair<AbstractSeries, Integer>> getRegularSeries() {
		// regular water level (relative values)
		List<Pair<AbstractSeries, Integer>> seriesList 
				= new ArrayList<Pair<AbstractSeries, Integer>>();
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_water_levels),
						COLUMN_STATION_KM, 
						COLUMN_LEVEL_VALUE, 
						COLUMN_LEVEL_UNIT),
					R.xml.series_water_levels));

		// add characteristic values
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_mw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_MW_VALUE,
						COLUMN_CHARVALUES_MW_UNIT),
					R.xml.series_average_levels));
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_mhw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_MHW_VALUE,
						COLUMN_CHARVALUES_MHW_UNIT),
					R.xml.series_average_levels));
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_mnw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_MNW_VALUE,
						COLUMN_CHARVALUES_MNW_UNIT),
					R.xml.series_average_levels));

		// add tild series
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_mthw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_MTHW_VALUE,
						COLUMN_CHARVALUES_MTHW_UNIT),
					R.xml.series_average_tide_values));
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_mtnw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_MTNW_VALUE,
						COLUMN_CHARVALUES_MTNW_UNIT),
					R.xml.series_average_tide_values));
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_hthw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_HTHW_VALUE,
						COLUMN_CHARVALUES_HTHW_UNIT),
					R.xml.series_extreme_tide_values));
		seriesList.add(new Pair<AbstractSeries, Integer>(
					new SimpleSeries(
						getString(R.string.series_ntnw),
						COLUMN_STATION_KM,
						COLUMN_CHARVALUES_NTNW_VALUE,
						COLUMN_CHARVALUES_NTNW_UNIT),
					R.xml.series_extreme_tide_values));
		
		return seriesList;
	}


	private List<Pair<AbstractSeries, Integer>> getNormalizedSeries() {
		List<Pair<AbstractSeries, Integer>> series 
				= new ArrayList<Pair<AbstractSeries, Integer>>();

		AbstractSeries normalizedSeries = new NormalizedSeries(
				getString(R.string.series_water_levels_normalized),
				COLUMN_STATION_KM,
				COLUMN_LEVEL_VALUE,
				COLUMN_LEVEL_UNIT,
				COLUMN_CHARVALUES_MHW_VALUE,
				COLUMN_CHARVALUES_MHW_UNIT,
				COLUMN_CHARVALUES_MW_VALUE,
				COLUMN_CHARVALUES_MW_UNIT,
				COLUMN_CHARVALUES_MNW_VALUE,
				COLUMN_CHARVALUES_MNW_UNIT);

		series.add(new Pair<AbstractSeries, Integer>(
					normalizedSeries,
					R.xml.series_water_levels));

		series.add(new Pair<AbstractSeries, Integer>(
					new ConstantSeries(getString(R.string.series_mhw), normalizedSeries, 1),
					R.xml.series_average_levels));

		series.add(new Pair<AbstractSeries, Integer>(
					new ConstantSeries(getString(R.string.series_mw), normalizedSeries, 0.5),
					R.xml.series_average_levels));

		series.add(new Pair<AbstractSeries, Integer>(
					new ConstantSeries(getString(R.string.series_mnw), normalizedSeries, 0),
					R.xml.series_average_levels));

		return series;
	}


	private void setTimestamp(long newTimestamp) {
		currentTimestamp = newTimestamp;
		loader.setTimestamp(currentTimestamp);
	}

	
	private void updateScrollbar() {
		final List<Long> timestamps = SourceMonitor
			.getInstance(getApplicationContext())
			.getAvailableTimestamps(PegelOnlineSource.INSTANCE);
		Collections.sort(timestamps);
		SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
		seekbar.setProgress(timestamps.indexOf(currentTimestamp));
	}

}
