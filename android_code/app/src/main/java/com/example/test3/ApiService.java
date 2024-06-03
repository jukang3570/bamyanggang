package com.example.test3;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("generate")
    Call<ImageResponse> generateImage(@Body CommandRequest commandRequest);

    @POST("resize")
    Call<ResponseBody> resizeImage(@Body ResizeRequest resizeRequest);
}
