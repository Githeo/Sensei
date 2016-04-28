package com.axa.crashtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.pwittchen.reactivesensors.library.ReactiveSensorEvent;
import com.github.pwittchen.reactivesensors.library.ReactiveSensorFilter;
import com.github.pwittchen.reactivesensors.library.ReactiveSensors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 SAVE FILE TO STORAGE
 http://developer.android.com/training/basics/data-storage/files.html

 SENSORS
 http://developer.android.com/guide/topics/sensors/sensors_overview.html
 http://developer.android.com/reference/android/hardware/SensorEvent.html#values

 AUDIO CAPTURE
 http://www.tutorialspoint.com/android/android_audio_capture.htm

 FILE LOGGER
 https://android-arsenal.com/

 CSV_LOGGER
 'net.sf.opencsv:opencsv:2.3'

 FREQUENCY:
 Check sensor frequency in the csv file with the following:
 $ file=mProvaMax.csv; filter="BMM150 magnetometer;"; c=0; start=`cat ${file} | grep "$filter" | awk -F ";" '{print $2}' | head -1`; for i in `cat ${file}| grep "$filter" | awk -F ";" '{print $2}'`; do gap=$(($i-$start)); if [ $gap -ge 1000 ] ; then start=$i;  echo $count; count=0; fi; count=$(($count+1)); done;

 PORTRAIT
 */
public class MainActivity extends AppCompatActivity {

    PowerManager.WakeLock wakeLock;
    CSVWriter csvWriter;
    Logger mLogger;

    final static int    HZ_100 = 10000;
    final static int    HZ_50 = 20000;
    final static char   CSV_SEP = ';';
    final static String START_CRASH = "START";
    final static String STOP_CRASH = "STOP";
    final static String OUTPUT_FOLDER = "/CrashTestData/";
    private String      outputFileName;
    private String      mRotationVectorSensorName;
    private long        currentTime = System.currentTimeMillis();

    @Bind(R.id.start_button) Button startButton;
    @Bind(R.id.monitorTextView) TextView monitorTextView;

    private SensorManager           mSensorManager;
    private MediaRecorder           mAudioRecorder;
    private List<Subscription>      subscriptionList;
    private List<Integer>           sensorTypeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CrashWakelock");
        wakeLock.acquire();
        ButterKnife.bind(this);
        mLogger = LoggerFactory.getLogger(MainActivity.class);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        startButton.setText(START_CRASH);
        startButton.setBackgroundColor(Color.GREEN);
        checkCustomPermissions();
    }

    private boolean checkCustomPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLogger.info("Writing Permission is granted");
                return true;
            } else {
                mLogger.info("Writing Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            mLogger.info("Permission: " + permissions[0]+ "was "+grantResults[0]);
        }
    }

    @OnClick(R.id.start_button)
    public void stopSensors(Button button) {
        // START recording...
        if (button.getText().equals(START_CRASH)){
            mLogger.info("STARTING SENSING");
            initRecordingFileWriter();
            initSensorList();
            initSubscription();
            startCaptureAudio();
            button.setBackgroundColor(Color.GRAY);
            button.setText(STOP_CRASH);
            return;
        } else { // STOP recording...
            mLogger.info("STOPPING SENSING");
            stopCaptureSensorData();
            monitorTextView.setText("");
            stopCaptureAudio();
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            button.setBackgroundColor(Color.GREEN);
            button.setText(START_CRASH);
            return;
        }
    }

    private void stopCaptureSensorData() {
        for (Subscription subscription : subscriptionList){
            subscription.unsubscribe();
        }
    }

    private void initRecordingFileWriter() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        outputFileName = Environment.getExternalStorageDirectory() + OUTPUT_FOLDER + sdf.format(new Date());
        Util.createDirIfNotExists(OUTPUT_FOLDER);
        try {
            csvWriter = new CSVWriter(new FileWriter(outputFileName + ".csv"), CSV_SEP, CSVWriter.NO_QUOTE_CHARACTER);
        } catch (IOException e) {
            monitorTextView.append("CSVWriter not created for file " + outputFileName +".csv");
            e.printStackTrace();
        }
        monitorTextView.append("Recording sensor data in " + outputFileName +".csv ...\n\n");
    }

    private void stopCaptureAudio() {
        try {
            mAudioRecorder.stop();
        } catch (RuntimeException stopException){
            mLogger.error("Stopping the audio recorder too fast!");
        }
        mAudioRecorder.release();
        mAudioRecorder = null;
    }

    private void startCaptureAudio() {
        mAudioRecorder = new MediaRecorder();
        mAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mAudioRecorder.setOutputFile(outputFileName + ".mp3");
        mAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mAudioRecorder.prepare();
        } catch (IOException e) {
            mLogger.error("prepare() failed");
        }
        mAudioRecorder.start();
        monitorTextView.append("Recording audio in " + outputFileName + ".mp3 ...\n\n");
    }

    private void initSensorList() {
        sensorTypeList = new ArrayList<>();
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            sensorTypeList.add(Sensor.TYPE_GYROSCOPE);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED) != null) {
            sensorTypeList.add(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensorTypeList.add(Sensor.TYPE_ACCELEROMETER);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            sensorTypeList.add(Sensor.TYPE_LINEAR_ACCELERATION);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            sensorTypeList.add(Sensor.TYPE_MAGNETIC_FIELD);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) != null) {
            sensorTypeList.add(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            sensorTypeList.add(Sensor.TYPE_ROTATION_VECTOR);
            mRotationVectorSensorName = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).getName();
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR).getName() + " ADDED");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            sensorTypeList.add(Sensor.TYPE_GRAVITY);
            mLogger.info(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY).getName() + " ADDED");
        }
        mLogger.info("sensorTypeList: " + sensorTypeList.toString());
    }

    private void initSubscription() {
        subscriptionList = new ArrayList<>();

        for(int sensorType : sensorTypeList){
            final String sensorName = mSensorManager.getDefaultSensor(sensorType).getName();
            mLogger.info("Adding subscription to sensor " + sensorName);
            subscriptionList.add(new ReactiveSensors(getApplicationContext()).observeSensor(sensorType, SensorManager.SENSOR_DELAY_FASTEST)
                    .subscribeOn(Schedulers.computation())
                    .filter(ReactiveSensorFilter.filterSensorChanged())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry() // resubscribe onError. Put this always after filter
                    .subscribe(new Subscriber<ReactiveSensorEvent>() {
                                   @Override public void onCompleted() {
                                       monitorTextView.append("No more data for sensor " + sensorName + "\n");
                                   }
                                   @Override public void onError(Throwable e) {
                                       String errorMessage = "<font color='#EE0000'>"+sensorName + " KO</font>";
                                       monitorTextView.append(Html.fromHtml(errorMessage) + "\n");
                                       e.printStackTrace();
                                   }
                                   @Override public void onNext(final ReactiveSensorEvent event) {
                                       String message = sensorName + CSV_SEP + System.currentTimeMillis() +
                                               CSV_SEP + event.getSensorEvent().timestamp + CSV_SEP +
                                               Arrays.toString(event.getSensorEvent().values);
                                       //mLogger.info(message);
                                       csvWriter.writeNext(message.split(";"));

                                       if (mRotationVectorSensorName != null &&
                                               sensorName.equals(mRotationVectorSensorName)) {
                                           float[] deviceRotationMatrix = new float[9];
                                           SensorManager.getRotationMatrixFromVector(deviceRotationMatrix,
                                                   event.getSensorEvent().values);
                                           message = "Rotation Matrix" + CSV_SEP + System.currentTimeMillis() +
                                                   CSV_SEP + event.getSensorEvent().timestamp + CSV_SEP +
                                                    Arrays.toString(deviceRotationMatrix);
                                           //mLogger.info(message);
                                           csvWriter.writeNext(message.split(";"));
                                       }
                                   }
                               }
                    )
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCaptureSensorData();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        wakeLock.release();
        stopCaptureSensorData();
    }
}

