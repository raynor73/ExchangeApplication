package ru.ilapin.exchangeapplication;

import android.text.TextUtils;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import ru.ilapin.common.android.viewmodelprovider.ViewModel;
import ru.ilapin.exchangeapplication.backend.Backend;
import ru.ilapin.exchangeapplication.backend.Result;

import java.util.Map;

public class ExchangeViewModel implements ViewModel {

	private final Backend mBackend;

	private final BehaviorSubject<Result<Double>> mRateObservable = BehaviorSubject.create();
	private final BehaviorSubject<Result<Double>> mFromAmountObservable = BehaviorSubject.create();
	private final BehaviorSubject<Result<Double>> mToAmountObservable = BehaviorSubject.create();

	private final BehaviorSubject<Backend.Currency> mFromCurrencySubject = BehaviorSubject.create();
	private final BehaviorSubject<Backend.Currency> mToCurrencySubject = BehaviorSubject.create();
	private final PublishSubject<String> mFromAmountSubject = PublishSubject.create();
	private final PublishSubject<String> mToAmountSubject = PublishSubject.create();

	private final PublishSubject<Result<Map<String, Double>>> mBackendRatesSubject = PublishSubject.create();

	private boolean mIsRatesSubscriptionToBackendMade;

	private String mPrevFromAmountString;
	private String mPrevToAmountString;

	private Disposable mFromCurrencySubscription;

	public ExchangeViewModel(final Backend backend) {
		mBackend = backend;

		mRateObservable.onNext(new Result<>(.0, false, true));
		mFromAmountObservable.onNext(new Result<>(.0, false, true));
		mToAmountObservable.onNext(new Result<>(.0, false, true));

		Observable.combineLatest(
				mBackendRatesSubject,
				mFromCurrencySubject,
				mToCurrencySubject,
				(rates, fromCurrency, toCurrency) -> {
					if (!rates.isEmpty() && fromCurrency != toCurrency) {
						return new Result<>(new CurrentRateParams(toCurrency, rates.getData()), false, false);
					}

					return new Result<CurrentRateParams>(null, false, true);
				}
		).subscribe(currentRateParamsResult -> {
			if (!currentRateParamsResult.isEmpty()) {
				final String toCurrency = currentRateParamsResult.getData().getToCurrency().toString();
				final Map<String, Double> rates = currentRateParamsResult.getData().getRates();
				if (!rates.containsKey(toCurrency)) {
					mRateObservable.onNext(new Result<>(null, false, true));
				} else {
					final double rate = rates.get(toCurrency);
					mRateObservable.onNext(new Result<>(rate, false, false));
				}
			} else {
				mRateObservable.onNext(new Result<>(null, false, true));
			}
		});

		Observable.combineLatest(
				mFromCurrencySubject,
				mToCurrencySubject,
				mFromAmountSubject,
				mToAmountSubject,
				mBackendRatesSubject,
				ExchangeParams::new
		).subscribe(exchangeParams -> {
			if (exchangeParams.getFromCurrency() == exchangeParams.getToCurrency()) {
				return;
			}

			if (exchangeParams.getRates().isEmpty() || exchangeParams.getRates().hasError()) {
				return;
			}

			final ConversionWay conversionWay = determineConversionWay(
					exchangeParams.getFromAmountString(),
					exchangeParams.getToAmountString()
			);
			if (conversionWay == ConversionWay.DIRECT) {
				final Map<String, Double> rates = exchangeParams.getRates().getData();
				final Backend.Currency toCurrency = exchangeParams.getToCurrency();
				double fromAmount;
				try {
					fromAmount = Double.parseDouble(exchangeParams.getFromAmountString());
				} catch (final NumberFormatException e) {
					fromAmount = 0;
				}
				if (!rates.containsKey(toCurrency.toString())) {
					return;
				}
				final double toAmount = rates.get(toCurrency.toString()) * fromAmount;

				if (String.valueOf(toAmount).equals(exchangeParams.getToAmountString())) {
					return;
				}
				mToAmountObservable.onNext(new Result<>(toAmount, false, false));
			} else if (conversionWay == ConversionWay.REVERSE) {
				final Map<String, Double> rates = exchangeParams.getRates().getData();
				final Backend.Currency toCurrency = exchangeParams.getToCurrency();
				double toAmount;
				try {
					toAmount = Double.parseDouble(exchangeParams.getToAmountString());
				} catch (final NumberFormatException e) {
					toAmount = 0;
				}
				if (!rates.containsKey(toCurrency.toString())) {
					return;
				}
				final double fromAmount = toAmount / rates.get(toCurrency.toString());

				if (String.valueOf(fromAmount).equals(exchangeParams.getFromAmountString())) {
					return;
				}
				mFromAmountObservable.onNext(new Result<>(fromAmount, false, false));
			}

			mPrevFromAmountString = exchangeParams.getFromAmountString();
			mPrevToAmountString = exchangeParams.getToAmountString();
		});
	}

	public Observable<Result<Double>> getRateObservable() {
		return mRateObservable;
	}

	public Observable<Result<Double>> getFromAmountObservable() {
		return mFromAmountObservable;
	}

	public Observable<Result<Double>> getToAmountObservable() {
		return mToAmountObservable;
	}

	public Observer<Backend.Currency> getFromCurrencyObserver() {
		return mFromCurrencySubject;
	}

	public Observer<Backend.Currency> getToCurrencyObserver() {
		return mToCurrencySubject;
	}

	public Observer<String> getFromAmountObserver() {
		return mFromAmountSubject;
	}

	public Observer<String> getToAmountObserver() {
		return mToAmountSubject;
	}

	@Override
	public void onResume() {
		mFromCurrencySubscription = mFromCurrencySubject.subscribe(currency -> {
			if (mIsRatesSubscriptionToBackendMade) {
				mBackend.unsubscribeFromRatesChanges(mBackendRatesSubject);
			}

			mBackend.subscribeForRatesChanges(currency, mBackendRatesSubject);
			mIsRatesSubscriptionToBackendMade = true;
		});
	}

	@Override
	public void onPause() {
		mFromCurrencySubscription.dispose();

		if (mIsRatesSubscriptionToBackendMade) {
			mBackend.unsubscribeFromRatesChanges(mBackendRatesSubject);
			mIsRatesSubscriptionToBackendMade = false;
		}
	}

	@Override
	public void onCleared() {
		mFromCurrencySubscription.dispose();

		if (mIsRatesSubscriptionToBackendMade) {
			mBackend.unsubscribeFromRatesChanges(mBackendRatesSubject);
		}
	}

	@SuppressWarnings("RedundantIfStatement")
	private ConversionWay determineConversionWay(final String fromAmount, final String toAmount) {
		if (!TextUtils.isEmpty(fromAmount) && !fromAmount.equals(mPrevFromAmountString)) {
			return ConversionWay.DIRECT;
		}

		if (!TextUtils.isEmpty(toAmount) && !toAmount.equals(mPrevToAmountString)) {
			return ConversionWay.REVERSE;
		}

		return ConversionWay.NO_CONVERSION;
	}

	private enum ConversionWay {
		NO_CONVERSION, DIRECT, REVERSE
	}

	private class CurrentRateParams {

		private final Backend.Currency mToCurrency;
		private final Map<String, Double> mRates;

		public CurrentRateParams(final Backend.Currency toCurrency,
				final Map<String, Double> rates) {
			mToCurrency = toCurrency;
			mRates = rates;
		}

		public Backend.Currency getToCurrency() {
			return mToCurrency;
		}

		public Map<String, Double> getRates() {
			return mRates;
		}
	}

	private class ExchangeParams {

		private final Backend.Currency mFromCurrency;
		private final Backend.Currency mToCurrency;
		private final String mFromAmountString;
		private final String mToAmountString;
		private final Result<Map<String, Double>> mRates;

		public ExchangeParams(final Backend.Currency fromCurrency, final Backend.Currency toCurrency,
				final String fromAmountString, final String toAmountString, final Result<Map<String, Double>> rates) {
			mFromCurrency = fromCurrency;
			mToCurrency = toCurrency;
			mFromAmountString = fromAmountString;
			mToAmountString = toAmountString;
			mRates = rates;
		}

		public Backend.Currency getFromCurrency() {
			return mFromCurrency;
		}

		public Backend.Currency getToCurrency() {
			return mToCurrency;
		}

		public String getFromAmountString() {
			return mFromAmountString;
		}

		public String getToAmountString() {
			return mToAmountString;
		}

		public Result<Map<String, Double>> getRates() {
			return mRates;
		}
	}
}
