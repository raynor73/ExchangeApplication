package ru.ilapin.exchangeapplication.backend;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

interface FixerService {

	@GET("latest")
	Call<ResponseBody> latest(@Query("base") String base);
}
