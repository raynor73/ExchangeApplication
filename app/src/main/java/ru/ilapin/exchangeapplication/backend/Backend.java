package ru.ilapin.exchangeapplication.backend;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

import static android.content.ContentValues.TAG;

public class Backend {

	private static final String BASE_URL = "http://api.fixer.io/";

	private final FixerService mFixerService;

	public Backend() {
		final Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).build();
		mFixerService = retrofit.create(FixerService.class);
	}

	public Observable<Result<Map<String, Double>>> getExchangeRateObservable(final RateBase base) {
		final Observable<Result<Map<String, Double>>> resultObservable = Observable
				.<Result<ResponseBody>>create(subscriber -> subscriber.onNext(new Result<>(makeRatesRequest(base), false, false)))
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

		return Observable
				.interval(0, 30, TimeUnit.SECONDS)
				.flatMap(x -> resultObservable)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}

	private ResponseBody makeRatesRequest(final RateBase base) throws IOException {
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

	public enum RateBase {
		GBP, EUR, USD
	}
}
