package ru.ilapin.common.android;

import android.view.View;
import android.widget.AdapterView;

public abstract class DefaultAdapterViewListener implements AdapterView.OnItemSelectedListener {

	@Override
	public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position, final long id) {
		// do nothing
	}

	@Override
	public void onNothingSelected(final AdapterView<?> adapterView) {
		// do nothing
	}
}
