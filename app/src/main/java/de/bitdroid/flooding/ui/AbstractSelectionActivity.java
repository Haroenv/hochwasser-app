package de.bitdroid.flooding.ui;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.network.AbstractErrorAction;
import de.bitdroid.flooding.network.NetworkUtils;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;


@ContentView(R.layout.activity_select)
abstract class AbstractSelectionActivity<T> extends AbstractActivity {

	private static final int REQUEST_MAP = 44;

	private final int searchHintStringId;
	private final int itemViewLayoutResource;

	@Inject private NetworkUtils networkUtils;
	@Inject private InputMethodManager inputMethodManager;

	@InjectView(R.id.list) RecyclerView recyclerView;

	private MenuItem searchMenuItem = null;
	private EditText searchBox = null;
	private SelectionAdapter adapter;

	protected StationSelection stationSelection;


	AbstractSelectionActivity(int searchHintStringId, int itemViewLayoutResource) {
		this.searchHintStringId = searchHintStringId;
		this.itemViewLayoutResource = itemViewLayoutResource;
	}


	protected abstract Observable<List<T>> loadItems();
	protected abstract void setDataView(T data, View view);
	protected abstract List<T> filterItems(CharSequence constraint, List<T> items);


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// read args
		stationSelection = new StationSelection(getIntent());

		// action bar back button
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// setup list
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		recyclerView.setLayoutManager(layoutManager);
		adapter = new SelectionAdapter();
		recyclerView.setAdapter(adapter);

		// load items
		loadItems()
				.compose(networkUtils.<List<T>>getDefaultTransformer())
				.subscribe(new Action1<List<T>>() {
					@Override
					public void call(List<T> items) {
						adapter.setItems(items);
					}
				}, new AbstractErrorAction(AbstractSelectionActivity.this) {
					@Override
					protected void doCall(Throwable throwable) {
						Timber.e(throwable, "failed to load data");
					}
				});
	}


	@Override
	public void onPause() {
		super.onPause();
		if (searchMenuItem != null) MenuItemCompat.collapseActionView(searchMenuItem);
	}


	@Override
	public final boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_select_water, menu);
		return true;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final ActionBar actionBar = getSupportActionBar();
		searchMenuItem = menu.findItem(R.id.search);
		MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				actionBar.setIcon(R.drawable.ic_action_search);
				showKeyboard();
				searchBox.requestFocus();
				// TODO show empty view
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setHomeButtonEnabled(true);
				actionBar.setDisplayShowHomeEnabled(true);
				actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				searchBox.setText("");
				hideKeyboard();
				return true;
			}
		});

		searchBox = (EditText) MenuItemCompat.getActionView(searchMenuItem).findViewById(R.id.search_box);
		searchBox.setHint(getString(searchHintStringId));
		searchBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				adapter.filter(s);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		return true;
	}


	@Override
	public final boolean onOptionsItemSelected(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case R.id.map:
				hideKeyboard();
				startActivityForResult(stationSelection.toIntent(this, MapSelectionActivity.class), REQUEST_MAP);
				return true;

			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}


	@Override
	public void onActivityResult(int request, int status, Intent data) {
		switch(request) {
			case REQUEST_MAP:
				if (status != RESULT_OK) return;
				setResult(RESULT_OK, data);
				finish();
				break;
		}
	}


	private void hideKeyboard() {
		inputMethodManager.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
	}


	private void showKeyboard() {
		inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		inputMethodManager.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
	}



	protected class SelectionAdapter extends RecyclerView.Adapter<ItemViewHolder> {

		private final List<T> visibleItems = new ArrayList<>();
		private final List<T> allItems = new ArrayList<>();

		@Override
		public void onBindViewHolder(ItemViewHolder holder, int position) {
			holder.setItem(visibleItems.get(position));
		}

		@Override
		public int getItemCount() {
			return visibleItems.size();
		}

		public void setItems(Collection<T> items) {
			this.visibleItems.clear();
			this.visibleItems.addAll(items);
			this.allItems.clear();
			this.allItems.addAll(items);
			notifyDataSetChanged();
		}

		@Override
		public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(itemViewLayoutResource, parent, false);
			return new ItemViewHolder(view);
		}


		public void filter(CharSequence constraint) {
			visibleItems.clear();
			visibleItems.addAll(filterItems(constraint, allItems));
			notifyDataSetChanged();
		}


		public void clearFilter() {
			visibleItems.clear();
			visibleItems.addAll(allItems);
			notifyDataSetChanged();
		}

	}


	protected class ItemViewHolder extends RecyclerView.ViewHolder {

		private final View itemView;

		public ItemViewHolder(View itemView) {
			super(itemView);
			this.itemView = itemView;
		}

		public void setItem(T item) {
			setDataView(item, itemView);
		}

	}

}

