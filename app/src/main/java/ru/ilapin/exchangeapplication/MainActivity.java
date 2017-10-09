package ru.ilapin.exchangeapplication;

import android.os.Bundle;
import ru.ilapin.common.android.viewmodelprovider.ViewModelProviderActivity;

public class MainActivity extends ViewModelProviderActivity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
}
