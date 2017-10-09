package ru.ilapin.exchangeapplication;

import android.app.Application;

public class App extends Application {

	private static ApplicationComponent sApplicationComponent;

	public static ApplicationComponent getApplicationComponent() {
		return sApplicationComponent;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		sApplicationComponent = DaggerApplicationComponent.builder().systemModule(new SystemModule(this)).build();
	}
}
