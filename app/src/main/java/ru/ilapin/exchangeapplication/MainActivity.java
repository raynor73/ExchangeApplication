package ru.ilapin.exchangeapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import ru.ilapin.common.android.DefaultAdapterViewListener;
import ru.ilapin.common.android.DefaultTextWatcher;
import ru.ilapin.common.android.viewmodelprovider.ViewModelProviderActivity;
import ru.ilapin.exchangeapplication.backend.Backend;

import javax.inject.Inject;
import java.util.Locale;

public class MainActivity extends ViewModelProviderActivity {

	@Inject
	Backend mBackend;

	@BindView(R.id.rate)
	TextView mRateTextView;
	@BindView(R.id.fromCurrency)
	Spinner mFromCurrencySpinner;
	@BindView(R.id.toCurrency)
	Spinner mToCurrencySpinner;
	@BindView(R.id.fromAmount)
	EditText mFromAmountEditText;
	@BindView(R.id.toAmount)
	EditText mToAmountEditText;

	private ExchangeViewModel mViewModel;

	private Disposable mRatesSubscription;
	private Disposable mFromAmountSubscription;
	private Disposable mToAmountSubscription;

	private final TextWatcher mFromAmountTextWatcher = new DefaultTextWatcher() {

		private String mOldText;

		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			mOldText = s.toString();
		}

		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			if (TextUtils.isEmpty(mOldText) || !mOldText.equals(s.toString())) {
				mViewModel.getFromAmountObserver().onNext(s.toString());
			}
		}
	};

	private final TextWatcher mToAmountTextWatcher = new DefaultTextWatcher() {

		private String mOldText;

		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			mOldText = s.toString();
		}

		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			if (TextUtils.isEmpty(mOldText) || !mOldText.equals(s.toString())) {
				mViewModel.getToAmountObserver().onNext(s.toString());
			}
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ButterKnife.bind(this);
		App.getApplicationComponent().inject(this);

		mViewModel = findVideModel(ExchangeViewModel.class);
		if (mViewModel == null) {
			mViewModel = new ExchangeViewModel(mBackend);
			putViewModel(mViewModel);
		}

		final ArrayAdapter<Backend.Currency> currencySpinnerAdapter = new ArrayAdapter<>(
				this,
				android.R.layout.simple_spinner_item,
				Backend.Currency.values()
		);
		currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mFromCurrencySpinner.setAdapter(currencySpinnerAdapter);
		mToCurrencySpinner.setAdapter(currencySpinnerAdapter);

		final Observer<Backend.Currency> fromCurrencyObserver = mViewModel.getFromCurrencyObserver();
		mFromCurrencySpinner.setOnItemSelectedListener(new DefaultAdapterViewListener() {

			@Override
			public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position,
					final long id) {
				fromCurrencyObserver.onNext(Backend.Currency.values()[position]);
			}
		});
		fromCurrencyObserver.onNext(Backend.Currency.values()[0]);

		final Observer<Backend.Currency> toCurrencyObserver = mViewModel.getToCurrencyObserver();
		mToCurrencySpinner.setOnItemSelectedListener(new DefaultAdapterViewListener() {

			@Override
			public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position,
					final long id) {
				toCurrencyObserver.onNext(Backend.Currency.values()[position]);
			}
		});
		toCurrencyObserver.onNext(Backend.Currency.values()[0]);

		mFromAmountEditText.addTextChangedListener(mFromAmountTextWatcher);
		mViewModel.getFromAmountObserver().onNext("");
		mToAmountEditText.addTextChangedListener(mToAmountTextWatcher);
		mViewModel.getToAmountObserver().onNext("");
	}

	@Override
	protected void onResume() {
		super.onResume();

		makeSubscriptions();
	}

	@Override
	protected void onPause() {
		super.onPause();

		disposeSubscriptions();
	}

	private void makeSubscriptions() {
		mRatesSubscription = mViewModel.getRateObservable().subscribe(result -> {
			if (!result.isEmpty()) {
				mRateTextView.setText(String.valueOf(result.getData()));
			} else {
				mRateTextView.setText(R.string.n_a);
			}
		});

		mFromAmountSubscription = mViewModel.getFromAmountObservable().subscribe(result -> {
			mFromAmountEditText.removeTextChangedListener(mFromAmountTextWatcher);

			if (!result.isEmpty()) {
				mFromAmountEditText.setText(String.format(Locale.US, "%.2f", result.getData()));
			} else {
				mFromAmountEditText.setText(null);
			}

			mFromAmountEditText.addTextChangedListener(mFromAmountTextWatcher);
		});

		mToAmountSubscription = mViewModel.getToAmountObservable().subscribe(result -> {
			mToAmountEditText.removeTextChangedListener(mToAmountTextWatcher);

			if (!result.isEmpty()) {
				mToAmountEditText.setText(String.format(Locale.US, "%.2f", result.getData()));
			} else {
				mToAmountEditText.setText(null);
			}

			mToAmountEditText.addTextChangedListener(mToAmountTextWatcher);
		});
	}

	private void disposeSubscriptions() {
		mRatesSubscription.dispose();
		mFromAmountSubscription.dispose();
		mToAmountSubscription.dispose();
	}
}
