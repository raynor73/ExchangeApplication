package ru.ilapin.exchangeapplication;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import butterknife.*;
import io.reactivex.disposables.Disposable;
import ru.ilapin.common.android.viewmodelprovider.ViewModelProviderActivity;
import ru.ilapin.exchangeapplication.backend.Backend;

import javax.inject.Inject;

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

	private ArrayAdapter<Backend.Currency> mCurrencySpinnerAdapter;

	private Disposable mRatesSubscription;
	private Disposable mFromAmountSubscription;
	private Disposable mToAmountSubscription;

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

		mCurrencySpinnerAdapter = new ArrayAdapter<>(
				this,
				android.R.layout.simple_spinner_item,
				Backend.Currency.values()
		);
		mCurrencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mFromCurrencySpinner.setAdapter(mCurrencySpinnerAdapter);
		mToCurrencySpinner.setAdapter(mCurrencySpinnerAdapter);

		mFromCurrencySpinner.setOnItemSelectedListener(new DefaultAdapterViewListener() {

			@Override
			public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position,
					final long id) {
				mViewModel.getFromCurrencyObserver().onNext(Backend.Currency.values()[position]);
			}
		});

		mToCurrencySpinner.setOnItemSelectedListener(new DefaultAdapterViewListener() {

			@Override
			public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position,
					final long id) {
				mViewModel.getToCurrencyObserver().onNext(Backend.Currency.values()[position]);
			}
		});

		mFromAmountEditText.addTextChangedListener(new TextWatcher() {

			private String mOldText;

			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				mOldText = s.toString();
			}

			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				if (!TextUtils.isEmpty(mOldText) && !mOldText.equals(s.toString())) {
					mViewModel.getFromAmountObserver().onNext(s.toString());
				}
			}

			@Override
			public void afterTextChanged(final Editable s) {
				// do nothing
			}
		});

		mToAmountEditText.addTextChangedListener(new TextWatcher() {

			private String mOldText;

			@Override
			public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
				mOldText = s.toString();
			}

			@Override
			public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
				if (!TextUtils.isEmpty(mOldText) && !mOldText.equals(s.toString())) {
					mViewModel.getToAmountObserver().onNext(s.toString());
				}
			}

			@Override
			public void afterTextChanged(final Editable s) {
				// do nothing
			}
		});
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

	@OnClick(R.id.button)
	public void onButtonClicked() {
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
			if (!result.isEmpty()) {
				mFromAmountEditText.setText(String.valueOf(result.getData()));
			} else {
				mFromAmountEditText.setText(null);
			}
		});

		mToAmountSubscription = mViewModel.getToAmountObservable().subscribe(result -> {
			if (!result.isEmpty()) {
				mToAmountEditText.setText(String.valueOf(result.getData()));
			} else {
				mToAmountEditText.setText(null);
			}
		});
	}

	private void disposeSubscriptions() {
		mRatesSubscription.dispose();
		mFromAmountSubscription.dispose();
		mToAmountSubscription.dispose();
	}
}
