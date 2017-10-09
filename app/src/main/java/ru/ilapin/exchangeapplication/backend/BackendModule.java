package ru.ilapin.exchangeapplication.backend;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class BackendModule {

	@Provides
	@Singleton
	public Backend provideBackend() {
		return new Backend();
	}
}
