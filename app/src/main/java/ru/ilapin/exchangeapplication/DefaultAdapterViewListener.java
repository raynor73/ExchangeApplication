package ru.ilapin.exchangeapplication;

import android.widget.AdapterView;

public abstract class DefaultAdapterViewListener implements AdapterView.OnItemSelectedListener {

	@Override
	public void onNothingSelected(final AdapterView<?> adapterView) {
		// do nothing
	}
}
