package ru.ilapin.exchangeapplication.backend;

import android.util.Log;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
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

	private static final String TAG = "Backend";

	private static final String BASE_URL = "http://api.fixer.io/";

	private final FixerService mFixerService;

	private final BehaviorSubject<Result<Map<String, Double>>> mRatesChangesSubject = BehaviorSubject.create();
	private final Map<Consumer<Result<Map<String, Double>>>, RatesSubscriptions> mSubscriptions = new HashMap<>();

	public Backend() {
		final Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).build();
		mFixerService = retrofit.create(FixerService.class);
	}

	public void subscribeForRatesChanges(final Currency base,
										 final Consumer<Result<Map<String, Double>>> observer) {
		final Observable<Result<Map<String, Double>>> resultObservable = Observable
				.<Result<ResponseBody>>create(subscriber -> {
					Log.d(TAG, "Requesting rates");
					subscriber.onNext(new Result<>(makeRatesRequest(base), false, false));
				})
				.onErrorReturn(throwable -> {
					Log.d(TAG, "Bad response: " + throwable.getMessage());
					return new Result<>(null, true, true);
				})
				.<Result<Map<String, Double>>>map(result -> {
					if (result.isEmpty() || result.hasError()) {
						return new Result<>(null, result.hasError(), result.isEmpty());
					} else {
						return new Result<>(parseRates(result.getData().string()), false, false);
					}
				})
				.onErrorReturn(throwable -> {
					Log.d(TAG, "Can't parse: " + throwable.getMessage());
					return new Result<>(null, true, true);
				});

		final Disposable subjectSubscription = mRatesChangesSubject.subscribe(observer);
		final Disposable intervalSubscription = Observable.interval(0, 3, TimeUnit.SECONDS)
				.flatMap(x -> resultObservable)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(mRatesChangesSubject::onNext);

		mSubscriptions.put(observer, new RatesSubscriptions(subjectSubscription, intervalSubscription));
	}

	public void unsubscribeFromRatesChanges(final Consumer<Result<Map<String, Double>>> observer) {
		final RatesSubscriptions subscriptions = Preconditions.checkNotNull(mSubscriptions.get(observer));
		subscriptions.getIntervalSubscription().dispose();
		subscriptions.getSubjectSubscription().dispose();
		mSubscriptions.remove(observer);
	}

	private ResponseBody makeRatesRequest(final Currency base) throws IOException {
		return mFixerService.latest(base.toString()).execute().body();
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
		GBP, EUR, USD
	}
}
