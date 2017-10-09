package ru.ilapin.exchangeapplication;

import android.content.Context;
import android.content.SharedPreferences;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class SystemModule {

	private final Context mContext;

	public SystemModule(final Context context) {
		mContext = context;
	}

	@Provides
	@Singleton
	public Context provideContext() {
		return mContext;
	}

	@Provides
	@Singleton
	public SharedPreferences provideSharedPreferences(final Context context) {
		return context.getSharedPreferences("preferences", 0);
	}
}
