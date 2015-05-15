package de.bitdroid.flooding.map;

import android.content.Context;
import android.graphics.Point;

import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;

import de.bitdroid.flooding.utils.Assert;


public final class StationsOverlay extends ItemizedOverlay<OverlayItem> {
	
	public static final int LOADER_ID = 43;

	private final StationClickListener clickListener;
	private final List<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	private final List<Station> stations = new ArrayList<Station>();

	public StationsOverlay(
			Context context,
			List<Station> stations,
			StationClickListener clickListener) {

		super(
				context.getResources().getDrawable(android.R.drawable.presence_online),
				new ResourceProxyImpl(context));

		Assert.assertNotNull(context, stations, clickListener);

		this.stations.addAll(stations);
		this.clickListener = clickListener;

		for (Station station : stations) {
			String name = station.getName();
			GeoPoint point = new GeoPoint(station.getLat(), station.getLon());
			overlayItems.add(new OverlayItem(name, name, point));
		}

		populate();
	}


	@Override
	protected OverlayItem createItem(int idx) {
		return overlayItems.get(idx);
	}


	@Override
	public int size() {
		return overlayItems.size();
	}


	@Override
	public boolean onSnapToItem(int x, int y, Point snapPoint, IMapView mapView) {
		return false;
	}


	@Override
	protected boolean onTap(int index) {
		Station station = stations.get(index);
		clickListener.onStationClicked(station);
		return true;
	}

}