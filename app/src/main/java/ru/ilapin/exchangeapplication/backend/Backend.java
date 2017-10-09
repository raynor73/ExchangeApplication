package ru.ilapin.exchangeapplication.backend;

import retrofit2.Retrofit;

public class Backend {

	private static final String BASE_URL = "http://api.fixer.io/";

	private final FixerService mFixerService;

	public Backend() {
		final Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).build();
		mFixerService = retrofit.create(FixerService.class);
	}
}
