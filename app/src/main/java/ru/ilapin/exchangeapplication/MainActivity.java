package ru.ilapin.exchangeapplication;

import android.os.Bundle;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import ru.ilapin.common.android.viewmodelprovider.ViewModelProviderActivity;
import ru.ilapin.exchangeapplication.backend.Backend;
import ru.ilapin.exchangeapplication.backend.Result;

import javax.inject.Inject;
import java.util.Map;

public class MainActivity extends ViewModelProviderActivity {

	@Inject
	Backend mBackend;

	@BindView(R.id.rate)
	TextView mRateTextView;

	private final Consumer<Result<Map<String, Double>>> mRateObservable = result -> {
		if (!result.isEmpty()) {
			mRateTextView.setText(String.valueOf(result.getData().get(Backend.Currency.EUR.toString())));
		}
	};

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

		mBackend.subscribeForRatesChanges(Backend.Currency.USD, mRateObservable);
	}

	@Override
	protected void onPause() {
		super.onPause();

		mBackend.unsubscribeFromRatesChanges(mRateObservable);
	}
}
