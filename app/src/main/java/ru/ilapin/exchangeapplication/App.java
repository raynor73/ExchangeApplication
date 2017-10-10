package ru.ilapin.exchangeapplication;

import android.app.Application;
import android.util.Log;
import io.reactivex.plugins.RxJavaPlugins;

public class App extends Application {

	private static final String TAG = "App";

	private static ApplicationComponent sApplicationComponent;

	public static ApplicationComponent getApplicationComponent() {
		return sApplicationComponent;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		sApplicationComponent = DaggerApplicationComponent.builder().systemModule(new SystemModule(this)).build();

		RxJavaPlugins.setErrorHandler(throwable -> {
			final String msg = "Caught error: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			Log.d(TAG, msg);
		});
	}
}
