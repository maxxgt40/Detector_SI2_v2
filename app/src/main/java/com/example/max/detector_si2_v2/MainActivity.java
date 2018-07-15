package com.example.max.detector_si2_v2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.max.detector_si2_v2.model.DatoPruebaRequest;
import com.example.max.detector_si2_v2.rest.SensoresAPIPrueba;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    //UBICACION/VELOCIDAD-----------------------------------------------------
    private FusedLocationProviderClient mFusedLocationClient;
    private double lat;
    private double lng;
    private int velocidad;

    //ACTIVIDAD----------------------------------------------------------------------
    private ActivityRecognitionClient arc;
    private BroadcastReceiver broadcastReceiver;
    private TextView textView1;
    private String Actividad = "";
    private int en_vehiculo = 0;
    private String mensajeAct = "Esperando...";
    private int contador = 0;
    //FUERZA G------------------------------------------------------------------------
    private float Acc;
    private TextView textView2;

    //SPL----------------------------------------------------------------------------
    private double SU;

    private TextView textView3;
    private AudioRecord recorder;

    private final static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final static int RECORDER_SAMPLERATE = 44100;
    private final static int BYTES_PER_ELEMENT = 2;
    private final static int BLOCK_SIZE = AudioRecord.getMinBufferSize(
            RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
            / BYTES_PER_ELEMENT;
    private final static int BLOCK_SIZE_FFT = 1764;
    private final static int NUMBER_OF_FFT_PER_SECOND = RECORDER_SAMPLERATE
            / BLOCK_SIZE_FFT;
    private final static double FREQRESOLUTION = ((double) RECORDER_SAMPLERATE)
            / BLOCK_SIZE_FFT;

    private Thread recordingThread = null;

    private boolean isRecording = false;

    private DoubleFFT_1D fft = null;

    private double filter = 0;

    private double[] weightedA = new double[BLOCK_SIZE_FFT];

    // Running Leq
    // Leq "nivel continuo equivalente"(TRADUCIDO)
    double linearFftAGlobalRunning = 0;
    private long fftCount = 0;
    private double dbFftAGlobalRunning;

    // variables finales para time display (TRADUCIDO)
    private double dbATimeDisplay;

    private void precalculateWeightedA() {
        for (int i = 0; i < BLOCK_SIZE_FFT; i++) {
            double actualFreq = FREQRESOLUTION * i;
            double actualFreqSQ = actualFreq * actualFreq;
            double actualFreqFour = actualFreqSQ * actualFreqSQ;
            double actualFreqEight = actualFreqFour * actualFreqFour;

            double t1 = 20.598997 * 20.598997 + actualFreqSQ;
            t1 = t1 * t1;
            double t2 = 107.65265 * 107.65265 + actualFreqSQ;
            double t3 = 737.86223 * 737.86223 + actualFreqSQ;
            double t4 = 12194.217 * 12194.217 + actualFreqSQ;
            t4 = t4 * t4;

            double weightFormula = (3.5041384e16 * actualFreqEight)
                    / (t1 * t2 * t3 * t4);

            weightedA[i] = weightFormula;
        }
    }

    //ACCIDENTES------------------------------------------------------------------------------

    private TextView textView4;
    private Handler handler;
    private int Accidente = 0;

    //Retrofit-------------------------------------------------------------------------------
    //Retrofit------------------------------------------------------------------
    private SensoresAPIPrueba service;


    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(broadcastReceiver==null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    String actividad1 = intent.getStringExtra("activity");
                    int confianza1 = intent.getIntExtra("confidence",0);

                    Log.i("actividades", "Actividad:"+ actividad1 +": " + confianza1 + "%");

                    if(actividad1.equalsIgnoreCase("IN_VEHICLE")&&confianza1>=50){
                        Actividad = actividad1;
                    }else if(actividad1.equalsIgnoreCase("NO_VEHICLE")&&confianza1>=90){
                        Actividad = actividad1;
                    }
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("activity_update"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.fuerzaG);
        textView3 = findViewById(R.id.decibeles);
        textView4 = findViewById(R.id.accidente);

        arc = new ActivityRecognitionClient(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Creando el sensor manager
        SensorManager SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        //Sensor acelerómetro
        Sensor miSensor = Objects.requireNonNull(SM).getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Registrar sensor listener
        //SM.registerListener(this, miSensor,SensorManager.SENSOR_DELAY_FASTEST);
        SM.registerListener(this, miSensor, 1000000, 1000000);

        handler = new Handler();

        configureNextButton();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app-sensores-pruebas.herokuapp.com/webapi/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(SensoresAPIPrueba.class);


    }

    private void configureNextButton() {
        ImageButton button = findViewById(R.id.boton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Main2Activity.class));
            }
        });
    }

    private void startRecording(final float gain, final int finalCountTimeDisplay) {

        // Mientras esta ventana esté visible para el usuario,
        // mantener la pantalla del dispositivo encendida y brillante.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BLOCK_SIZE * BYTES_PER_ELEMENT);

        if (recorder.getState() == 1){
            Log.d("nuestro log", "La grabadora esta lista");}
        else{
            Log.d("nuestro log", "La grabadora no está lista");}

        recorder.startRecording();
        isRecording = true;

        // Creo una fft da BLOCK_SIZE_FFT punti --> BLOCK_SIZE_FFT / 2 bande utili,
        // ognuna da FREQRESOLUTION Hz

        //Creo una FFT de BLOCK_SIZE_FFT puntos --> BLOCK_SIZE_FFT / 2 bandas utiles,
        //cada uno de FREQRESOLUTION Hz (TRADUCIDO)
        fft = new DoubleFFT_1D(BLOCK_SIZE_FFT);

        recordingThread = new Thread(new Runnable() {
            public void run() {

                // Array di raw data (tot : BLOCK_SIZE_FFT * 2 bytes)
                // Arreglo de datos crudos(tot : BLOCK_SIZE_FFT * 2 bytes)(TRADUCIDO)
                short rawData[] = new short[BLOCK_SIZE_FFT];

                // Array di mag non pesati (BLOCK_SIZE_FFT / 2 perchè è il numero di
                // bande utili)
                //Arreglo de mag(magnitudes) no pesadas (BLOCK_SIZE_FFT / 2 porque es el
                //número de bandas útiles (TRADUCIDO)
                //final float dbFft[] = new float[BLOCK_SIZE_FFT / 2];

                // Array di mag pesati
                // Arreglo de mag(magnitudes) pesadas (TRADUCIDO)
                final float dbFftA[] = new float[BLOCK_SIZE_FFT / 2];

                float normalizedRawData;

                // La fft lavora con double e con numeri complessi (re + im in
                // sequenza)
                // El fft funciona con double y con numeros complejos (re + im en
                // secuencia) (TRADUCIDO)
                double[] audioDataForFFT = new double[BLOCK_SIZE_FFT * 2];

                // Umbral de audibilidad (20*10^(-6))
                float amplitudeRef = 0.00002f;

                // Variabili per calcolo medie Time Display
                // Variables para el cálculo promedio Time Display (TRADUCIDO)
                int indexTimeDisplay = 1;
                double linearATimeDisplay = 0;

                int initial_delay = 0;

                while (isRecording) {

                    // Leo los datos
                    recorder.read(rawData, 0, BLOCK_SIZE_FFT);

                    // inserto un retraso inicial porque en la activación había niveles muy
                    // altos de funcionamiento leq (> 100 dB) y bajos (10 dB) debido quizás
                    // a la activación inicial del periférico
                    initial_delay++;

                    if (initial_delay > 20) {

                        for (int i = 0, j = 0; i < BLOCK_SIZE_FFT; i++, j += 2) {

                            // Range [-1,1]
                            normalizedRawData = (float) rawData[i]
                                    / (float) Short.MAX_VALUE;

                            filter = normalizedRawData;

                            // Ventana de Hanning
                            double x = (2 * Math.PI * i) / (BLOCK_SIZE_FFT - 1);
                            double winValue = (1 - Math.cos(x)) * 0.5d;

                            // Parte real
                            audioDataForFFT[j] = filter * winValue;

                            // Parte imaginaria
                            audioDataForFFT[j + 1] = 0.0;
                        }

                        // FFT
                        fft.complexForward(audioDataForFFT);

                        // Magsum (Suma de magnitudes¿?) pesada
                        double linearFftAGlobal = 0;

                        // Leo hasta BLOCK_SIZE_FFT/2 porque en tot(total ¿?)
                        // tengo BLOCK_SIZE_FFT / 2 bandas útiles
                        for (int i = 0, j = 0; i < BLOCK_SIZE_FFT / 2; i++, j += 2) {

                            double re = audioDataForFFT[j];
                            double im = audioDataForFFT[j + 1];

                            // Magnitudo
                            // Magnitud (TRADUCIDO)
                            double mag = Math.sqrt((re * re) + (im * im));

                            // Ponderata A
                            // da capire: per i = 0 viene un valore non valido (forse meno infinito), ma ha senso?
                            // questo si ritrova poi nel grafico:
                            // per i=0 la non pesata ha un valore, mentre la pesata non ce l'ha...

                            // Ponderada A
                            // para entender: para i = 0 es un valor inválido (quizás menos infinito), pero ¿tiene sentido?
                            // esto se encuentra en el gráfico:
                            // para i = 0 el no ponderado tiene un valor, mientras que el ponderado no lo tiene ...(TRADUCIDO)
                            double weightFormula = weightedA[i];

                            //dbFft[i] = (float) (10 * Math.log10(mag * mag
                            //        / amplitudeRef))
                            //        + gain;
                            dbFftA[i] = (float) (10 * Math.log10(mag * mag
                                    * weightFormula
                                    / amplitudeRef))
                                    + gain;

                            linearFftAGlobal += Math.pow(10, dbFftA[i] / 10f);

                        }


                        // Ejecutando Leq
                        fftCount++;
                        linearFftAGlobalRunning += linearFftAGlobal;
                        dbFftAGlobalRunning = 10 * Math.log10(linearFftAGlobalRunning / fftCount);

                        // LAeqTimeDisplay
                        // Cálculo de promedios para Time Display y gráficos de actualización (TRADUCIDO)
                        linearATimeDisplay += linearFftAGlobal;

                        if (indexTimeDisplay < finalCountTimeDisplay) {
                            indexTimeDisplay++;
                        } else {
                            // TimeDisplay datos
                            dbATimeDisplay = 10 * Math.log10(linearATimeDisplay / finalCountTimeDisplay);
                            SU = dbATimeDisplay;
                            indexTimeDisplay = 1;
                            linearATimeDisplay = 0;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    textView3.setText(dBformat(dbATimeDisplay));

                                }
                            });
                        }

                        // truco para no dejar dbATimeDisplay nulo en la apertura de la aplicación
                        if (dbATimeDisplay == 0){
                            dbATimeDisplay = dbFftAGlobalRunning;
                        }

                    }
                } // while
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void stopRecording() {
        // stops the recording activity
        if (recorder != null) {
            isRecording = false;
            try {
                recordingThread.join();
            } catch (Exception e) {
                Log.d("nostro log",
                        "Il Thread principale non può attendere la chiusura del thread secondario dell'audio");
            }
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    @Override
    protected void onPause() { super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() { super.onStop(); }

    @Override
    protected void onDestroy() {
        stopRecording();
        removeActivityUpdates();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopRecording();
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestActivityUpdates();

        stopRecording();

        ///////  parte para los cálculos

        // CALIBRACION + o -
        float gain = 5.5f;

        // CADA CUANTOS SEGUNDOS SE OBTIENEN LOS DATOS
        double timeDisplay = 1;


        final int finalCountTimeDisplay = (int) (timeDisplay * NUMBER_OF_FFT_PER_SECOND);


        precalculateWeightedA();


        if(!runtime_permissions()) {
            startRecording( gain, finalCountTimeDisplay);
            pedirUbicacionesActuales();
        }

        //detectarAccidente();
        mostrarDatosCadaNSegundos();
    }

    private String dBformat(double dB) {
        return "SU: " + String.format(Locale.ENGLISH, "%.1f", dB);
    }

    //Ubicacion y velocidad

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
                velocidad = (int) ((location.getSpeed() * 3600) / 1000);


                Log.i("Mapitas", "Lat: " + lat + "/Lng:" + lng +"/Vel:" + velocidad);





            }
        }
    };




    //PERMISOS

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)!=
                        PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},100);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==
                    PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED){

                onResume();
            }else{
                runtime_permissions();
            }
        }
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(MainActivity.this, ActivityRecognizeService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void requestActivityUpdates(){
        //0 es igual a tan rapido como sea posible
        //1000 es cada segundo
        arc.requestActivityUpdates(0,getActivityDetectionPendingIntent());
    }

    private void removeActivityUpdates() {
        arc.removeActivityUpdates(getActivityDetectionPendingIntent());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float fuerzaGejeX,fuerzaGejeY, fuerzaGejeZ;
        List<Float> Valores = Arrays.asList(0.0f, 0.0f, 0.0f);
        String mensajeG;

        //Fuerza G en todos los ejes
        fuerzaGejeX = Math.abs(event.values[0]/SensorManager.GRAVITY_EARTH);
        fuerzaGejeY = Math.abs(event.values[1]/SensorManager.GRAVITY_EARTH);
        fuerzaGejeZ = Math.abs(event.values[2]/SensorManager.GRAVITY_EARTH);

        //Arreglo
        Valores.set(0,fuerzaGejeX);
        Valores.set(1,fuerzaGejeY);
        Valores.set(2,fuerzaGejeZ);

        //Acc
        Acc = Collections.max(Valores);

        mensajeG = "Acc: " + String.format(Locale.ENGLISH,"%.2f", Acc);

        if(Acc>=4.0f){
            textView2.setTextColor(Color.RED);
            textView2.setText(mensajeG);
        }else{
            textView2.setTextColor(Color.BLACK);
            textView2.setText(mensajeG);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
    private void detectarAccidente(){

        handler.post(new Runnable() {
            @Override
            public void run() {

                if(((Acc/4.0f)+(SU/140.0d))>=1 && Actividad.equalsIgnoreCase("IN_VEHICLE")
                        && confianza >= 70){

                    textView4.append("\n" + "Accidente!--> " + new Date());

                }
                if(Acc>1.5f && SU>40.0d){
                    String m = "Acc:"+String.format(Locale.ENGLISH,"%.2f", Acc)
                            +":"+ dBformat(SU);
                    textView4.append("\n" + "Accidente!->" + m + new Date());
                }

                handler.postDelayed(this, 1000);
            }
        });
    }*/

    private void mostrarDatosCadaNSegundos(){


        handler.post(new Runnable() {
            @Override
            public void run() {
                //Solo para pruebas de Accidente:
                //Actividad = "IN_VEHICLE";

                if(Actividad.equalsIgnoreCase("NO_VEHICLE")){
                    contador++;
                    //60 segundos por 15 = 900 segundos = 15 minutos de espera.
                    if(contador>900){
                        mensajeAct = "Actividad:"+ Actividad;

                        en_vehiculo = 0;
                    }
                }else if(Actividad.equalsIgnoreCase("IN_VEHICLE")){
                    contador = 0;

                    mensajeAct = "Actividad:"+ Actividad;

                    en_vehiculo = 1;
                }

                textView1.append("\n" + mensajeAct);

                //Algoritmo para detectar accidente-----------------------------------------------
                if((Acc == 4.0f) &&
                        Actividad.equalsIgnoreCase("IN_VEHICLE")){

                    Accidente = 1;
                    textView4.append("\n" + "Accidente!--> " + new Date());

                }else{
                    Accidente = 0;
                }
                //--------------------------------------------------------------------------------

                DatoPruebaRequest dr = new DatoPruebaRequest();

                dr.setAccidente(Accidente);
                dr.setAcc((double)Acc);
                dr.setSu(SU);
                dr.setEn_vehiculo(en_vehiculo);
                dr.setVelo(velocidad);
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



                handler.postDelayed(this, 1000);
            }
        });
    }

}
