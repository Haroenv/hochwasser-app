package de.bitdroid.flooding.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;

import timber.log.Timber;


public class FixedMapView extends MapView {

	private static final int IGNORE_MOVE_COUNT = 2;
	private int moveCount = 0;


	public FixedMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_MOVE:

				if (moveCount > 0) {
					moveCount--;
					Timber.d("Ignored move event");
					return true;
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				moveCount = IGNORE_MOVE_COUNT;
				break;
		}
		return super.onTouchEvent(ev);
	}
}
