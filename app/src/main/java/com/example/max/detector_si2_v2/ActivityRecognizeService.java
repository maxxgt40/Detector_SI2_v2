package com.example.max.detector_si2_v2;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import java.util.List;

/**
 * Created by max on 10/10/2017.
 */

public class ActivityRecognizeService extends IntentService {

    private Intent i = new Intent("activity_update");

    public ActivityRecognizeService(){
        super("ActivityRecognizeService");
    }
    /*
    public ActivityRecognizeService(String name){
        super(name);
    }*/

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivity(result.getProbableActivities());

        }
    }

    private void handleDetectedActivity(List<DetectedActivity> probableActivities){
        String actividad;
        int confianza;

        for (DetectedActivity activity: probableActivities){
            switch(activity.getType()){

                case DetectedActivity.IN_VEHICLE:{
                    //mensaje = "Actividad:IN_VEHICLE: " + activity.getConfidence() + "%";
                    actividad = "IN_VEHICLE";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }

                /*
                case DetectedActivity.ON_BICYCLE:{

                    actividad = "ON_BICYCLE";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }

                case DetectedActivity.ON_FOOT:{

                    actividad = "ON_FOOT";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }

                case DetectedActivity.RUNNING:{

                    actividad = "RUNNING";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }

                case DetectedActivity.STILL:{

                    actividad = "STILL";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }

                case DetectedActivity.WALKING:{

                    actividad = "WALKING";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);

                    break;
                }
                */
                case DetectedActivity.TILTING:{

                    /*
                    actividad = "TILTING";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);
                    */
                    break;
                }

                case DetectedActivity.UNKNOWN:{

                    /*
                    actividad = "UNKNOWN";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);*/

                    break;
                }

                default:{
                    actividad = "NO_VEHICLE";
                    i.putExtra("activity",actividad);

                    confianza = activity.getConfidence();
                    i.putExtra("confidence",confianza);

                    sendBroadcast(i);
                    break;
                }
            }
        }

    }

   /*private void mostrarActividad(final String rpt){
       handler.post(new Runnable() {
           @Override
           public void run() {
               Toast.makeText(getApplicationContext(),rpt, Toast.LENGTH_SHORT).show();
           }
       });
   }*/
}
