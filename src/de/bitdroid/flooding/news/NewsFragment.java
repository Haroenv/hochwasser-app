package de.bitdroid.flooding.news;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import de.bitdroid.flooding.MainActivity;
import de.bitdroid.flooding.R;
import de.bitdroid.flooding.utils.ShowcaseSeries;
import de.timroes.android.listview.EnhancedListView;


public final class NewsFragment extends Fragment implements AbsListView.MultiChoiceModeListener {

	private NewsListAdapter listAdapter;
	private EnhancedListView listView;
	private ShowcaseView currentShowcaseView;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater, 
			ViewGroup container, 
			Bundle savedInstanceState) {

		// view
		View view = inflater.inflate(R.layout.news, container, false);
		listView = (EnhancedListView) view.findViewById(R.id.list);
		listAdapter = new NewsListAdapter(getActivity().getApplicationContext());
		listView.setAdapter(listAdapter);
		listView.setEmptyView(view.findViewById(R.id.empty));

		// enable editing mode
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(this);

		// enable swiping and undo
		listView.setDismissCallback(new EnhancedListView.OnDismissCallback() {
			@Override
			public EnhancedListView.Undoable onDismiss(EnhancedListView listView, int pos) {
				final NewsItem item = listAdapter.getItem(pos);
				listAdapter.removeItem(item);
				return new EnhancedListView.Undoable() {
					@Override
					public void undo() {
						listAdapter.addItem(item);
					}
				};
			}
		});
		listView.enableSwipeToDismiss();
		listView.setUndoStyle(EnhancedListView.UndoStyle.COLLAPSED_POPUP);

		// enable navigation to other fragments
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				NewsItem item = listAdapter.getItem(pos);
				if (!item.isNavigationEnabled()) return;

				Intent intent = new Intent(MainActivity.ACTION_NAVIGATE);
				intent.putExtra(MainActivity.EXTRA_POSITION, item.getNavigationPos());
				getActivity().sendBroadcast(intent);
			}
		});


		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		NewsManager.getInstance(getActivity().getApplicationContext()).markAllItemsRead();


		if (firstStart()) {
			addHelperNews();
			showHelperOverlay();
		}
	}


	@Override
	public void onStop() {
		super.onStop();
		if (currentShowcaseView != null) currentShowcaseView.hide();
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.news_menu, menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
			case R.id.news:
				addHelperNews();
				return true;

			case R.id.help:
				showHelperOverlay();
				return true;
		}
		return super.onOptionsItemSelected(menuItem);
	}



	private final List<NewsItem> selectedItems = new LinkedList<NewsItem>();

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.delete:
				for (NewsItem newsItem : selectedItems) {
					listAdapter.removeItem(newsItem);
				}
				Toast.makeText(
						getActivity(), 
						getActivity().getString(R.string.news_deleted, selectedItems.size()), 
						Toast.LENGTH_SHORT)
					.show();
				selectedItems.clear();

				mode.finish();
				return true;

			case R.id.select_all:
				selectedItems.clear();
				for (int i = 0; i < listAdapter.getCount(); i++) {
					listView.setItemChecked(i, true);
				}
				return true;

		}
		return false;
	}


	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.news_action_mode_menu, menu);
		listView.disableSwipeToDismiss();
		return true;
	}


	@Override
	public void onDestroyActionMode(ActionMode mode) {
		listView.enableSwipeToDismiss();
		selectedItems.clear();
	}


	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}


	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		NewsItem item = listAdapter.getItem(position);
		if (checked) selectedItems.add(item);
		else selectedItems.remove(item);
		mode.setTitle(getActivity().getString(R.string.news_selected, selectedItems.size()));
	}


	private static final String PREFS_NAME = "de.bitdroid.flooding.news.NewsFragment";
	private static final String KEY_FIRST_START = "KEY_FIRST_START";

	private boolean firstStart() {
		SharedPreferences prefs = getActivity().getSharedPreferences(
				PREFS_NAME, 
				Context.MODE_PRIVATE);
		boolean firstStart = prefs.getBoolean(KEY_FIRST_START, true);
		if (!firstStart) return false;

		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(KEY_FIRST_START, false);
		editor.commit();
		return true;
	}


	private void addHelperNews() {
		NewsManager manager = NewsManager.getInstance(getActivity().getApplicationContext());
		manager.addItem(new NewsItem.Builder(
				"Alarms",
				"If you want to be alarmed when water levels reach a certain level, head over to the alarms section!",
				System.currentTimeMillis())
			.setNavigationPos(1)
			.build(),
			false);

		manager.addItem(new NewsItem.Builder(
				"Data",
				"Want more details about the current water sitation? Check our the data section!",
				System.currentTimeMillis())
			.setNavigationPos(2)
			.build(),
			false);
		listAdapter.notifyDataSetInvalidated();
	}


	private void showHelperOverlay() {
		new ShowcaseSeries() {
			@Override
			public ShowcaseView getShowcase(int id) {
				Activity activity = getActivity();
				Target target;
				switch(id) {
					case 0:
						target = new ActionViewTarget(activity, ActionViewTarget.Type.TITLE);
						currentShowcaseView = new ShowcaseView.Builder(activity)
							.setTarget(target)
							.setContentTitle(R.string.help_news_welcome_title)
							.setContentText(R.string.help_news_welcome_content)
							.setStyle(R.style.CustomShowcaseTheme)
							.build();
						break;

					case 1:
						View view = listView.getChildAt(0);
						if (view == null) view = listView.getEmptyView();
						target = new ViewTarget(view);
						currentShowcaseView = new ShowcaseView.Builder(activity)
							.setTarget(target)
							.setContentTitle(R.string.help_news_news_title)
							.setContentText(R.string.help_news_news_content)
							.setStyle(R.style.CustomShowcaseTheme)
							.build();
						break;

					default:
						currentShowcaseView = null;
				}
				return currentShowcaseView;
			}
		}.start();
	}
}
