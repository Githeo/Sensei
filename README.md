# Sensei

Sensei is an Android app created to log as much sensor data as possible. The main aim is to detect on the fly car crashes, or analyse data a posteriori.

Sensei records the following sensors:
1. Gravity Sensor
2. K6DS3TR Acceleration Sensor
K6DS3TR Gyroscope Sensor
K6DS3TR Gyroscope sensor UnCalibrated
Linear Acceleration Sensor
Rotation Matrix
Samsung Rotation Vector
YAS537 Magnetic Sensor
YAS537 Uncalibrated Magnetic Sensor

The user interface is extremely simple: just open the app and click on the 'START' button when you are ready. The TEST ID  will be shown. It is actually the timestamp of the starting action and the name of the log file. When you are fine, just click on the 'STOP' button. Very easy, hein?

Sensei will store logs in the folder /sdcard/CrashTestData. Mind that sensors sampling frequency is quite high, so the log file size will increase quickly. In order to get back sensor data on your laptop I advice to connct the smartphone through a USB cable and run the command 
  $ adb pull /sdcard/CrashTestData/${Test ID}.csv ${Experiment Data Folder}

while to get the recorded audio run
  $ adb pull /sdcard/CrashTestData/${Test ID}.mp3 ${Experiment Data Folder}




The minimum SDK version is 18, while dependancies are the following:
  'com.android.support:appcompat-v7:23.2.1'
  'com.github.pwittchen:reactivesensors:0.1.1'
  'com.jakewharton:butterknife:7.0.1'
  'net.sf.opencsv:opencsv:2.3'
  'org.slf4j:slf4j-android:1.7.21'
  
