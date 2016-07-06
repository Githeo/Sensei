# Sensei

Sensei is an Android app created to log as much sensor data as possible. 
The main aim is to detect on the fly car crashes, or analyse data a posteriori.
Sensei extensively uses the ReactiveSensors library.

Sensei records the following sensors:
* Gravity Sensor
* Acceleration Sensor
* Gyroscope Sensor
* Uncalibrated Gyroscope sensor
* Linear Acceleration Sensor
* Rotation Matrix
* Rotation Vector
* Magnetic Sensor
* Uncalibrated Magnetic Sensor
* Microphone audio

The user interface is extremely simple: just open the app and click on the 'START' button when you are ready. 
The Experiment_ID  will be shown. It is actually the timestamp of the starting action and the name of the log file itself. 
When you are fine, just click on the 'STOP' button. 
Very easy, hein?

Sensei stores logs in the sdcard folder /sdcard/CrashTestData. 
Mind that sensors sampling frequency is quite high, so the log file size will increase quickly. 
In order to get back sensor data on your laptop I advice to connect the smart-phone to your laptop 
with a USB cable and run the command.
```bash
$ adb pull /sdcard/CrashTestData/${Experiment_ID}.csv ${Experiment Data Folder}
```

while to get the recorded audio run
```bash
$ adb pull /sdcard/CrashTestData/${Experiment_ID}.mp3 ${Experiment Data Folder}
```

## Data Collected
Beyond the mp3 audio file, the csv with sensor data uses a semicolon as separator among fields
and comma to separate values of the same sensor sample (e.g., acceleration on the three axes).
Since samples can have three or more values, these are grouped by square brackets.

```
Acceleration Sensor;1466433997168;9643789614727;[-0.6608198, 9.239507, 2.370332]
Linear Acceleration Sensor;1466433997169;9643783255016;[-0.13636637, -0.43212128, 0.83580565]
Rotation Vector;1466433997171;9643783255016;[0.649372, 0.008773, -0.030913, 0.759791, 0.0]
Rotation Matrix;1466433997171;9643783255016;[0.9979348, 0.05836872, -0.026816778, -0.035580963, 0.15472084, -0.98731637, -0.053479366, 0.98623157, 0.15647814]
Rotation Vector;1466433997171;9643783255016;[0.649372, 0.008773, -0.030913, 0.759791, 0.0]
```

Fields are the following:
* Sensor name,
* System timestamp in nanoseconds,
* Sensor timestamp,
* Sensor values.

Units per sensors data values are the same written in the official Android doc here:
http://developer.android.com/guide/topics/sensors/sensors_overview.html

## Requirements

The minimum SDK version is 18, while Sensei is based on the following dependancies:
*  'com.android.support:appcompat-v7:23.2.1'
*  'com.github.pwittchen:reactivesensors:0.1.1'
*  'com.jakewharton:butterknife:7.0.1'
*  'net.sf.opencsv:opencsv:2.3'
*  'org.slf4j:slf4j-android:1.7.21'
 
