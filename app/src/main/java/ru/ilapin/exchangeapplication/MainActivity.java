package ru.ilapin.exchangeapplication;

import android.os.Bundle;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import ru.ilapin.common.android.viewmodelprovider.ViewModelProviderActivity;
import ru.ilapin.exchangeapplication.backend.Backend;

public class MainActivity extends ViewModelProviderActivity {

	@Inject
	Backend mBackend;

	@BindView(R.id.rate)
	TextView mRateTextView;

	private Disposable mRateSubscription;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ButterKnife.bind(this);
		App.getApplicationComponent().inject(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		mRateSubscription = mBackend.getExchangeRateObservable(Backend.Currency.USD).subscribe(result -> {
			if (!result.isEmpty()) {
				mRateTextView.setText(String.valueOf(result.getData().get("EUR")));
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();

		mRateSubscription.dispose();
	}
}
