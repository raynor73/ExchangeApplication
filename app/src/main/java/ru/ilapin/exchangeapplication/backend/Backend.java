package ru.ilapin.exchangeapplication.backend;

import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Backend {

	private static final String BASE_URL = "http://api.fixer.io/";

	private final FixerService mFixerService;

	private final BehaviorSubject<Result<Map<String, Double>>> mRatesChangesSubject = BehaviorSubject.create();
	private final Map<Observer<Result<Map<String, Double>>>, RatesSubscriptions> mSubscriptions = new HashMap<>();
	private final Map<Currency, String> mCache = new HashMap<>();

	public Backend() {
		final Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).build();
		mFixerService = retrofit.create(FixerService.class);

		mRatesChangesSubject.onNext(new Result<>(null, false, true));
	}

	public void subscribeForRatesChanges(final Currency base,
										 final Observer<Result<Map<String, Double>>> observer) {
		final Observable<Result<Map<String, Double>>> resultObservable = Observable.combineLatest(
				Observable.<Result<String>>create(subscriber -> {
					final String cachedData = mCache.get(base);
					if (cachedData != null) {
						subscriber.onNext(new Result<>(cachedData, false, false));
					} else {
						subscriber.onNext(new Result<>(null, true, true));
					}
				}),
				Observable.<Result<String>>create(subscriber -> subscriber.onNext(new Result<>(makeRatesRequest(base), false, false))),
				(cachedResult, networkResult) -> {
					if (!networkResult.isEmpty()) {
						return networkResult;
					} else {
						return cachedResult;
					}
				}
		).onErrorReturn(throwable -> new Result<>(null, true, true))
				.<Result<Map<String, Double>>>map(result -> {
					if (result.isEmpty() || result.hasError()) {
						return new Result<>(null, result.hasError(), result.isEmpty());
					} else {
						final String data = result.getData();
						mCache.put(base, data);
						return new Result<>(parseRates(data), false, false);
					}
				})
				.onErrorReturn(throwable -> new Result<>(null, true, true));

		final Disposable subjectSubscription = mRatesChangesSubject.subscribe(observer::onNext);
		final Disposable intervalSubscription = Observable.interval(0, 30, TimeUnit.SECONDS)
				.flatMap(x -> resultObservable)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(mRatesChangesSubject::onNext);

		mSubscriptions.put(observer, new RatesSubscriptions(subjectSubscription, intervalSubscription));
	}

	public void unsubscribeFromRatesChanges(final Observer<Result<Map<String, Double>>> observer) {
		final RatesSubscriptions subscriptions = Preconditions.checkNotNull(mSubscriptions.get(observer));
		subscriptions.getIntervalSubscription().dispose();
		subscriptions.getSubjectSubscription().dispose();
		mSubscriptions.remove(observer);
	}

	private String makeRatesRequest(final Currency base) throws IOException {
		final ResponseBody responseBody = mFixerService.latest(base.toString()).execute().body();
		return responseBody != null ? responseBody.string() : "";
	}

	private Map<String, Double> parseRates(final String responseString) throws JSONException {
		final Map<String, Double> rates = new HashMap<>();

		final JSONObject ratesJsonObject = new JSONObject(responseString).getJSONObject("rates");
		final Iterator<String> ratesIterator = ratesJsonObject.keys();
		while(ratesIterator.hasNext()) {
			final String rate = ratesIterator.next();
			rates.put(rate, ratesJsonObject.getDouble(rate));
		}

		return rates;
	}

	private class RatesSubscriptions {

		private final Disposable mSubjectSubscription;
		private final Disposable mIntervalSubscription;

		public RatesSubscriptions(final Disposable subjectSubscription, final Disposable intervalSubscription) {
			mSubjectSubscription = subjectSubscription;
			mIntervalSubscription = intervalSubscription;
		}

		public Disposable getSubjectSubscription() {
			return mSubjectSubscription;
		}

		public Disposable getIntervalSubscription() {
			return mIntervalSubscription;
		}
	}

	public enum Currency {
		USD, EUR, GBP
	}
}
