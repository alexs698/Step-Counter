package com.example.fitnesstracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;


import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView stepCountTextView;
    private ProgressBar progressBar;
    private Button resetButton;

    private int stepCount = 0;
    int i = 0;

    // Define a constant for the sensor permission request
    private static final int REQUEST_SENSOR_PERMISSION = 1;
    private ProgressBar todayProgressBar, dailyGoalProgressBar, weeklyGoalProgressBar;
    private TextView todayProgressText, dailyGoalProgressText, weeklyGoalProgressText;
    private EditText editTextDailyGoal, editTextWeeklyGoal;
    private int todayProgress = 0;
    private int dailyGoalProgress = 0; // Initial progress for daily goal
    private int weeklyGoalProgress = 0; // Initial progress for weekly goal
    private int dailyGoal, weeklyGoal;
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String DAILY_GOAL_KEY = "daily_goal";
    private static final String WEEKLY_GOAL_KEY = "weekly_goal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        todayProgressBar = findViewById(R.id.progress_bar);
        dailyGoalProgressBar = findViewById(R.id.today_progress_bar);
        weeklyGoalProgressBar = findViewById(R.id.weekly_goal_progress_bar);

        todayProgressText = findViewById(R.id.progress_text);
        dailyGoalProgressText = findViewById(R.id.daily_goal_progress_text);
        weeklyGoalProgressText = findViewById(R.id.weekly_goal_progress_text);

        editTextDailyGoal = findViewById(R.id.editTextDailyGoal);
        editTextWeeklyGoal = findViewById(R.id.editTextWeeklyGoal);
        stepCountTextView = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        // Set initial progress for today
        todayProgressBar.setProgress(todayProgress);
        todayProgressText.setText(todayProgress + "%");

        // Set initial progress for daily goal
        dailyGoalProgressBar.setProgress(dailyGoalProgress);
        dailyGoalProgressText.setText(dailyGoalProgress + "%");

        // Set initial progress for weekly goal
        weeklyGoalProgressBar.setProgress(weeklyGoalProgress);
        weeklyGoalProgressText.setText(weeklyGoalProgress + "%");
        resetButton = findViewById(R.id.reset_button);

        // Set up text change listeners for user input goals
        setUpTextWatchers();

        // Check and request sensor permission if needed
        if (hasSensorPermission()) {
            initializeStepCounter();
            scheduleMidnightReset();
        } else {
            requestSensorPermission();
        }
        resetButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                resetSteps();
            }
        });
    }
    public void resetSteps(){
        stepCount = 0;
        stepCountTextView.setText(" " + stepCount);
    }
    private void setUpTextWatchers() {
        editTextDailyGoal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Update the daily goal when the user changes it
                if (!editable.toString().isEmpty()) {
                    dailyGoal = Integer.parseInt(editable.toString());
                    updateDailyGoalProgress();
                }
            }
        });

        editTextWeeklyGoal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Update the weekly goal when the user changes it
                if (!editable.toString().isEmpty()) {
                    weeklyGoal = Integer.parseInt(editable.toString());
                    updateWeeklyGoalProgress();
                }
            }
        });
    }
    private void updateDailyGoalProgress() {
        int progress = calculateProgress(todayProgress, dailyGoal);
        dailyGoalProgressBar.setProgress(progress);
        dailyGoalProgressText.setText(progress + "%");
    }

    private void updateWeeklyGoalProgress() {
        int progress = calculateProgress(weeklyGoalProgress, weeklyGoal);
        weeklyGoalProgressBar.setProgress(progress);
        weeklyGoalProgressText.setText(progress + "%");
    }
    private int calculateProgress(int currentProgress, int goal) {
        if (goal > 0) {
            return (int) ((currentProgress / (double) goal) * 100);
        } else {
            return 0;
        }
    }
    private int getDailyGoal() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(DAILY_GOAL_KEY, 5000); // Default daily goal if not set
    }

    // Method to get the weekly goal
    private int getWeeklyGoal() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(WEEKLY_GOAL_KEY, 35000); // Default weekly goal if not set
    }
    private void setGoals(int dailyGoal, int weeklyGoal) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(DAILY_GOAL_KEY, dailyGoal);
        editor.putInt(WEEKLY_GOAL_KEY, weeklyGoal);
        editor.apply();
    }
    private void initializeStepCounter() {
        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Check if the device has a step counter sensor
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) {
            // Handle the case when the device doesn't have a step counter sensor
            Log.e("StepCounter", "Step Counter Sensor not available on this device");
            stepCountTextView.setText("Step Counter Sensor not available on this device");
        } else {
            // Register the sensor listener when the activity is resumed
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d("StepCounter", "Step Counter Sensor available");
        }
    }

    private boolean hasSensorPermission() {
        // Check if the necessary sensor permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // No additional permission required for API level below Q
        }
    }

    private void requestSensorPermission() {
        // Request the necessary sensor permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_SENSOR_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Handle the result of the permission request
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SENSOR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize the step counter
                initializeStepCounter();
            } else {
                // Permission denied, handle accordingly (e.g., show a message, disable functionality)
                stepCountTextView.setText("Sensor permission denied. Step counter functionality disabled.");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Update step count when the sensor data changes
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) event.values[0];
            stepCountTextView.setText(stepCount + " ");

        }
    }
    private void updateProgressBars() {
        // Assuming you have ProgressBar instances for daily and weekly goals
        ProgressBar dailyProgressBar = findViewById(R.id.today_progress_bar);
        ProgressBar weeklyProgressBar = findViewById(R.id.weekly_goal_progress_bar);

        // Assuming you have TextView instances for displaying step counts
        TextView stepCountTextView = findViewById(R.id.progress_text);

        // Update the step count text
        stepCountTextView.setText(String.valueOf(stepCount));

        // Update the daily progress bar
        int dailyGoal = getDailyGoal(); // Replace with your method to get the daily goal
        int dailyProgress = Math.min(stepCount, dailyGoal);
        dailyProgressBar.setProgress(dailyProgress);

        // Update the weekly progress bar
        int weeklyGoal = getWeeklyGoal(); // Replace with your method to get the weekly goal
        int weeklyProgress = Math.min(stepCount, weeklyGoal);
        weeklyProgressBar.setProgress(weeklyProgress);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    private void scheduleMidnightReset(){
        Calendar currentTime = Calendar.getInstance();
        long timeUntilMidnight = getNextMidnightTime() - currentTime.getTimeInMillis();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                resetStepCount();
                scheduleMidnightReset();
            }
        }, timeUntilMidnight);
    }
    private long getNextMidnightTime(){
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        if (midnight.getTimeInMillis() <= System.currentTimeMillis()){
            midnight.add(Calendar.DAY_OF_MONTH, 1);
        }
        return midnight.getTimeInMillis();
    }
    private void resetStepCount(){
        stepCount = 0;
        stepCountTextView.setText("Step Count: " + stepCount);
    }
}