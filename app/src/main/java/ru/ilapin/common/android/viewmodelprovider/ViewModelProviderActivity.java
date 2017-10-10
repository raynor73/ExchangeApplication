package ru.ilapin.common.android.viewmodelprovider;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.Observable;

public class ViewModelProviderActivity extends AppCompatActivity {

	private static final String RETAINED_FRAGMENT_TAG = "RetainedFragment";

	private RetainedFragment mRetainedFragment;

	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			mRetainedFragment = new RetainedFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.add(mRetainedFragment, RETAINED_FRAGMENT_TAG)
					.commit();
		} else {
			mRetainedFragment = (RetainedFragment) getSupportFragmentManager().findFragmentByTag(RETAINED_FRAGMENT_TAG);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		Observable.fromIterable(mRetainedFragment.getViewModles()).forEach(ViewModel::onResume);
	}

	@Override
	protected void onPause() {
		super.onPause();

		Observable.fromIterable(mRetainedFragment.getViewModles()).forEach(ViewModel::onPause);

		if (isFinishing()) {
			Observable.fromIterable(mRetainedFragment.getViewModles()).forEach(ViewModel::onCleared);
		}
	}

	public void putViewModel(final ViewModel viewModel) {
		mRetainedFragment.putViewModel(viewModel);
	}

	public <T extends ViewModel> T findVideModel(final Class<T> type) {
		return mRetainedFragment.findViewModel(type);
	}
}
