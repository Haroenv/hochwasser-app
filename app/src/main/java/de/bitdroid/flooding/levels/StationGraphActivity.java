package de.bitdroid.flooding.levels;

import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_TYPE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_UNIT;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_LEVEL_VALUE;
import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_NAME;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.dataselection.Extras;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;
import de.bitdroid.flooding.utils.StringUtils;

// TODO include graph at some point
public class StationGraphActivity extends BaseActivity 
	implements LoaderManager.LoaderCallbacks<Cursor>, Extras {
	
	private static final int LOADERID = 46;

	private TextView levelView;
	private String stationName;
	private String waterName;

	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.data_station);
		stationName = getIntent().getExtras().getString(EXTRA_STATION_NAME);
		waterName = getIntent().getExtras().getString(EXTRA_WATER_NAME);

		levelView = (TextView) findViewById(R.id.level);

		getActionBar().setTitle(StringUtils.toProperCase(stationName));
		getActionBar().setSubtitle(waterName);

    }


	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().initLoader(LOADERID, null, this);
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		return new CursorLoader(
				getApplicationContext(),
				PegelOnlineSource.INSTANCE.toUri(),
				new String[] { COLUMN_LEVEL_VALUE, COLUMN_LEVEL_UNIT },
				COLUMN_STATION_NAME + "=? AND " + COLUMN_LEVEL_TYPE + "=?",
				new String[] { stationName, "W" },
				null);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() != LOADERID) return;

		if (cursor.getCount() <= 0) return;

		int valueIdx = cursor.getColumnIndex(COLUMN_LEVEL_VALUE);
		int unitIdx = cursor.getColumnIndex(COLUMN_LEVEL_UNIT);

		cursor.moveToFirst();
		levelView.setText(cursor.getDouble(valueIdx) + " " + cursor.getString(unitIdx));
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader) { }

}