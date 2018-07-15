package com.example.max.detector_si2_v2.rest;

import com.example.max.detector_si2_v2.model.DatoRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SensoresAPI {

    @POST("servicio/insertarDato")
    Call<Void>insetarDato(@Body DatoRequest datoRequest);

}
