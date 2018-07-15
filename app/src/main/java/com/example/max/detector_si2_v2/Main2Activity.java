package com.example.max.detector_si2_v2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.max.detector_si2_v2.model.DatoRequest;
import com.example.max.detector_si2_v2.rest.SensoresAPI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    //Ubicacion/Velocidad-----------------------------------------------------------------
    private FusedLocationProviderClient mFusedLocationClient;
    private TextView textView1;
    private int limiteVelocidad = 200;
    private int respLimitVel = 1;
    private double lat;
    private double lng;
    private ImageButton button30;
    private ImageButton button40;
    private ImageButton button60;
    private ImageButton button80;
    private ImageButton button100;
    //Variables para calcular velocidad manual----------------------------------------------

    //Fuerza G----------------------------------------------------------------------------
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private Handler handler1;
    private double aceleracion;
    private double frenado;
    private double cambioCarril;
    //Retrofit------------------------------------------------------------------
    private SensoresAPI service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        textView1 = findViewById(R.id.mostrarUbiVelo);
        textView2 = findViewById(R.id.aceleracion);
        textView3 = findViewById(R.id.frenado);
        textView4 = findViewById(R.id.cambioCarril);

        ImageButton backButton = findViewById(R.id.botonRegresar);
        backButton.setOnClickListener(this); // calling onClick() method
        button30 = findViewById(R.id.boton30kph);
        button30.setOnClickListener(this);
        button40 = findViewById(R.id.boton40kph);
        button40.setOnClickListener(this);
        button60 = findViewById(R.id.boton60kph);
        button60.setOnClickListener(this);
        button80 = findViewById(R.id.boton80kph);
        button80.setOnClickListener(this);
        button100 = findViewById(R.id.boton100kph);
        button100.setOnClickListener(this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Creando el sensor manager
        SensorManager SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        //Sensor acelerómetro
        Sensor miSensor = Objects.requireNonNull(SM).getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Registrar sensor listener
        //SM.registerListener(this, miSensor,SensorManager.SENSOR_DELAY_NORMAL);
        //Para obtener data cada segundo
        SM.registerListener(this, miSensor, 1000000, 1000000);

        handler1 = new Handler();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app-sensores-v2.herokuapp.com/webapi/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(SensoresAPI.class);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.botonRegresar:
                finish();
                break;

            case R.id.boton30kph:
                limiteVelocidad = 30;
                button30.setBackgroundColor(Color.RED);
                button40.setBackgroundColor(Color.TRANSPARENT);
                button60.setBackgroundColor(Color.TRANSPARENT);
                button80.setBackgroundColor(Color.TRANSPARENT);
                button100.setBackgroundColor(Color.TRANSPARENT);
                break;

            case R.id.boton40kph:
                limiteVelocidad = 40;
                button40.setBackgroundColor(Color.RED);
                button30.setBackgroundColor(Color.TRANSPARENT);
                button60.setBackgroundColor(Color.TRANSPARENT);
                button80.setBackgroundColor(Color.TRANSPARENT);
                button100.setBackgroundColor(Color.TRANSPARENT);

                break;

            case R.id.boton60kph:
                limiteVelocidad = 60;
                button60.setBackgroundColor(Color.RED);
                button40.setBackgroundColor(Color.TRANSPARENT);
                button30.setBackgroundColor(Color.TRANSPARENT);
                button80.setBackgroundColor(Color.TRANSPARENT);
                button100.setBackgroundColor(Color.TRANSPARENT);
                break;

            case R.id.boton80kph:
                limiteVelocidad = 80;
                button80.setBackgroundColor(Color.RED);
                button40.setBackgroundColor(Color.TRANSPARENT);
                button60.setBackgroundColor(Color.TRANSPARENT);
                button30.setBackgroundColor(Color.TRANSPARENT);
                button100.setBackgroundColor(Color.TRANSPARENT);
                break;

            case R.id.boton100kph:
                limiteVelocidad = 100;
                button100.setBackgroundColor(Color.RED);
                button40.setBackgroundColor(Color.TRANSPARENT);
                button60.setBackgroundColor(Color.TRANSPARENT);
                button80.setBackgroundColor(Color.TRANSPARENT);
                button30.setBackgroundColor(Color.TRANSPARENT);
                break;

            default:
                break;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();

        if(!runtime_permissions()) {
            pedirUbicacionesActuales();
        }

        mostrarDatosCadaNSegundos();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        handler1.removeCallbacksAndMessages(null);
    }

    @SuppressLint("MissingPermission")
    private void pedirUbicacionesActuales(){

        LocationRequest mLocationRequest = new LocationRequest();
        //mLocationRequest.setInterval(3000); // 3 seconds interval
        mLocationRequest.setInterval(1000);
        //mLocationRequest.setFastestInterval(3000);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());

                lat = location.getLatitude();
                lng = location.getLongitude();

                //Velocidad real
                int velocidad = (int) ((location.getSpeed() * 3600) / 1000);

                /*
                if(velocidad == 0){
                    //Solo para pruebas!!
                    velocidad = 40;
                }*/

                if(velocidad > limiteVelocidad){
                    respLimitVel = 0;
                    textView1.append("\n" + "!!!EXCESO DE VELOCIDAD!!!" + " / Velocidad: " + velocidad);

                }else{
                    respLimitVel = 1;
                    textView1.append("\n" + "Lat:" + formatLatLng(location.getLatitude()) + ", Lng:"
                            + formatLatLng(location.getLongitude())+" / Velocidad: " + velocidad);
                }




            }
        }
    };

    private String formatLatLng(double LatLng) {
        return new DecimalFormat("#.##").format(LatLng);
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED){
                pedirUbicacionesActuales();
            }else{
                runtime_permissions();
            }
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float fuerzaGejeX,fuerzaGejeY;
        String mensajeAceleracion;
        String mensajeFrenado;
        String mensajeCambioCarril;

        //Fuerza G
        fuerzaGejeX = Math.abs(event.values[0]/SensorManager.GRAVITY_EARTH);
        fuerzaGejeY = event.values[1]/SensorManager.GRAVITY_EARTH;

        mensajeCambioCarril = "CambioCarril: " + fuerzaGejeX;

        if(fuerzaGejeX>0.5f){
            textView4.setTextColor(Color.RED);
        }else{
            textView4.setTextColor(Color.BLACK);
        }

        cambioCarril = fuerzaGejeX;

        textView4.setText(mensajeCambioCarril);

        //Umbral 0.3
        //Aceleracion > 0
        //Frenado < 0
        if(fuerzaGejeY>0.3f){
            //Es una aceleracion que sobrepasa el umbral
            mensajeAceleracion = "Aceleracion: " + fuerzaGejeY;
            mensajeFrenado = "Frenado: " + 0;
            textView2.setTextColor(Color.RED);
            textView3.setTextColor(Color.BLACK);

            aceleracion = fuerzaGejeY;
            frenado = 0;
        }else if(fuerzaGejeY>0.0f && fuerzaGejeY <=0.3f){
            //Es aceleracion pero no sobrepasa el umbral
            mensajeAceleracion = "Aceleracion: " + fuerzaGejeY;
            mensajeFrenado = "Frenado: " + 0;
            textView2.setTextColor(Color.BLACK);
            textView3.setTextColor(Color.BLACK);

            aceleracion = fuerzaGejeY;
            frenado = 0;
        }else if(fuerzaGejeY == 0.0f){
            //No es aceleracion ni frenado y no se sobrepasa el umbral
            mensajeAceleracion = "Aceleracion: " + 0;
            mensajeFrenado = "Frenado: " + 0;
            textView2.setTextColor(Color.BLACK);
            textView3.setTextColor(Color.BLACK);

            aceleracion = 0;
            frenado = 0;
        }else if(fuerzaGejeY<0.0f && fuerzaGejeY>=-0.3f){
            //Es frenado pero no sobrepasa el umbral
            mensajeFrenado = "Frenado: " + fuerzaGejeY;
            mensajeAceleracion = "Aceleracion: " + 0;
            textView2.setTextColor(Color.BLACK);
            textView3.setTextColor(Color.BLACK);

            aceleracion = 0;
            frenado = fuerzaGejeY;
        }else{
            //Es frenado que sobrepasa el umbral
            mensajeFrenado = "Frenado: " + fuerzaGejeY;
            mensajeAceleracion = "Aceleracion: " + 0;
            textView2.setTextColor(Color.BLACK);
            textView3.setTextColor(Color.RED);

            aceleracion = 0;
            frenado = fuerzaGejeY;
        }

        textView2.setText(mensajeAceleracion);
        textView3.setText(mensajeFrenado);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void mostrarDatosCadaNSegundos(){


        handler1.post(new Runnable() {
            @Override
            public void run() {

                DatoRequest dr = new DatoRequest();
                dr.setAceleracion(aceleracion);
                dr.setFrenado(frenado);
                dr.setCambioCarril(cambioCarril);
                dr.setRespLimitVelo(respLimitVel);
                dr.setLat(lat);
                dr.setLng(lng);

                Call<Void> llamada = service.insetarDato(dr);

                llamada.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.i("ggwp", "si corre");
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.i("ggwp", t.getMessage());
                    }
                });



                handler1.postDelayed(this, 1000);
            }
        });
    }
}
