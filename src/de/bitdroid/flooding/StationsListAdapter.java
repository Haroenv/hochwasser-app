package de.bitdroid.flooding;

import static de.bitdroid.flooding.pegelonline.PegelOnlineSource.COLUMN_STATION_NAME;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import de.bitdroid.flooding.utils.Log;


final class StationsListAdapter extends BaseAdapter {
		

	private final List<String> items = new LinkedList<String>();
	private final Context context;
	private final StationsLoaderCallbacks loaderCallback;

	public StationsListAdapter(Context context) {
		if (context == null) throw new NullPointerException("context cannot be null");
		this.context = context;

		this.loaderCallback = new StationsLoaderCallbacks(context) {
			@Override
			protected void onLoadFinishedHelper(Loader<Cursor> loader, Cursor cursor) {
				Log.debug("onLoadFinished called");
				items.clear();
				if (cursor == null) return;
				cursor.moveToFirst();
				while (cursor.moveToNext()) {
					int idx = cursor.getColumnIndex(COLUMN_STATION_NAME);
					items.add(cursor.getString(idx));
				}
				notifyDataSetChanged();
			}

			@Override
			protected void onLoaderResetHelper(Loader<Cursor> loader) { }

			@Override
			protected String[] getColumnNames() {
				return new String[] { COLUMN_STATION_NAME };
			}
		};
	}


	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater 
			= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View itemView = inflater.inflate(R.layout.item, parent, false);
		TextView textView = (TextView) itemView.findViewById(R.id.item_text);
		textView.setText(items.get(position));

		return itemView;

	}


	@Override
	public Object getItem(int position) { 
		return items.get(position);
	}
	

	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public boolean hasStableIds() {
		return true;
	}


	@Override
	public int getCount() {
		return items.size();
	}


	public StationsLoaderCallbacks getLoaderCallback() {
		return loaderCallback;
	}
}
