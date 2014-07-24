package de.bitdroid.flooding.alarms;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.dataselection.Extras;
import de.bitdroid.flooding.utils.StringUtils;


public final class SelectLevelFragment extends Fragment implements Extras {


	public static SelectLevelFragment newInstance(String riverName, String stationName) {
		SelectLevelFragment fragment = new SelectLevelFragment();
		Bundle extras = new Bundle();
		extras.putString(EXTRA_WATER_NAME, riverName);
		extras.putString(EXTRA_STATION_NAME, stationName);
		fragment.setArguments(extras);
		return fragment;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	@Override
	public View onCreateView(
			LayoutInflater inflater, 
			ViewGroup container, 
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.alarms_new, container, false);

		TextView stationView = (TextView) view.findViewById(R.id.station);

		final String river = getArguments().getString(EXTRA_WATER_NAME);
		final String station = getArguments().getString(EXTRA_STATION_NAME);

		final EditText levelEditText = (EditText) view.findViewById(R.id.level);
		final RadioGroup relationRadioGroup = (RadioGroup) view.findViewById(R.id.relation);

		stationView.setText(
				StringUtils.toProperCase(river) + " - " + StringUtils.toProperCase(station));

		final Button okButton = (Button) view.findViewById(R.id.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				double level = Double.valueOf(levelEditText.getText().toString());
				boolean whenAbove = relationRadioGroup.getCheckedRadioButtonId() == R.id.above;

				Alarm alarm = new LevelAlarm(river, station, level, whenAbove);
				AlarmManager manager = AlarmManager.getInstance(getActivity().getApplicationContext());
				if (manager.contains(alarm)) {
					Toast.makeText(getActivity(), getString(R.string.alarms_new_already_added), Toast.LENGTH_SHORT).show();
					return;
				}

				manager.register(alarm);

				SelectLevelFragment.this.getActivity().finish();
				Toast.makeText(
					SelectLevelFragment.this.getActivity(), 
					getString(R.string.alarms_new_created), 
					Toast.LENGTH_SHORT)
					.show();
			}
		});

		levelEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) { }
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					okButton.setEnabled(false);
				} else {
					okButton.setEnabled(true);
				}
			}
		});


		return view;
	}

}
