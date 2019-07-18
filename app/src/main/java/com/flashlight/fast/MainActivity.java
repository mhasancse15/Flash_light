package com.flashlight.fast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private Context context;
    private LinearLayout bgElement;
    private ImageButton screenLight, flashLight, powerBtn;



    private Camera camera;
    private boolean isFlashOn;
    private boolean hasFlash, isScreen;
    private Camera.Parameters params;
    private MediaPlayer mp;
    private SurfaceHolder mHolder;
    private SurfaceView preview;
    private TextView batteryPercent;

    private int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batteryPercent = this.findViewById(R.id.batteryLevel);
        bgElement = findViewById(R.id.rootLayout);
        screenLight = findViewById(R.id.screenBtn);
        flashLight = findViewById(R.id.flashBtn);
        powerBtn = findViewById(R.id.BtnSwitch);


        context = getApplicationContext();
        //Getting Current screen brightness.

        // non fa andare lo schermo in sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Controllo che esista il flash
        checkFlash();

        isFlashOn = false;
        isScreen = false;

        getCamera();
        toggleButtonImage();
        getBatteryPercentage();

        powerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (count == 0) {
                    if (isFlashOn) {
                        // turn off flash
                        turnOffFlash();
                    } else {
                        // turn on flash
                        turnOnFlash();
                    }
                } else if (count == 1) {
                    ScreenLightOn();
                    count = 2;
                } else if (count == 2) {
                    ScreenLightOff();
                }

            }

        });


        // Get turn on screen button

        screenLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                flashLight.setVisibility(View.GONE);


                Context context = getApplicationContext();
                // Check whether has the write settings permission or not.
                boolean settingsCanWrite = hasWriteSettingsPermission(context);
                // If do not have then open the Can modify system settings panel.
                if (!settingsCanWrite) {
                    changeWriteSettingsPermission(context);
                } else {
                    count = 1;
                }
            }
        });

        flashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenLight.setVisibility(View.GONE);



                // Get app context object.
                Context context = getApplicationContext();
                // Check whether has the write settings permission or not.
                boolean settingsCanWrite = hasWriteSettingsPermission(context);

                // If do not have then open the Can modify system settings panel.
                if (!settingsCanWrite) {
                    changeWriteSettingsPermission(context);
                } else {
                    count = 0;
                    colorChange();
                    changeScreenBrightness(context, 1);

                }

            }
        });
    }


    //Screen Light


    public void ScreenLightOn() {
        screenLight.setVisibility(View.GONE);

        bgElement.setBackgroundColor(Color.WHITE);
        screenLight.setBackgroundColor(Color.WHITE);
        powerBtn.setBackgroundColor(Color.WHITE);
        flashLight.setBackgroundColor(Color.WHITE);
        powerBtn.setImageResource(R.drawable.power);
        changeScreenBrightness(context, 255);

    }

    public void ScreenLightOff() {

        screenLight.setVisibility(View.VISIBLE);
        flashLight.setVisibility(View.VISIBLE);
        powerBtn.setImageResource(R.drawable.poweroff);
        colorChange();
        changeScreenBrightness(context, 1);
        count = 0;

    }
    // Check whether this app has android write settings permission.
    private boolean hasWriteSettingsPermission(Context context) {
        boolean ret = true;
        // Get the result from below code.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = Settings.System.canWrite(context);
        }
        return ret;
    }

    // Start can modify system settings panel to let user change the write settings permission.
    private void changeWriteSettingsPermission(Context context) {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        }
        context.startActivity(intent);
    }

    // This function only take effect in real physical android device,
    // it can not take effect in android emulator.
    private void changeScreenBrightness(Context context, int screenBrightnessValue) {
        // Change the screen brightness change mode to manual.
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        // Apply the screen brightness value to the system, this will change the value in Settings ---> Display ---> Brightness level.
        // It will also change the screen brightness for the device.
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightnessValue);

        /*
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.screenBrightness = screenBrightnessValue / 255f;
        window.setAttributes(layoutParams);
        */
    }


    // Flash Light Control
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        mHolder = null;
    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // on pause turn off the flash
        turnOffFlash();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // on resume turn on the flash
        if (hasFlash)
            turnOnFlash();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // on starting the app get the camera params
        getCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }


    public void checkFlash() {
        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("Errore!");
            alert.setMessage("Il tuo telefono non ha il flash!");
            alert.setPositiveButton("OK, compro un Nexus", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
        } else {
            Log.i("SUCA", "c'è il flash");
        }

    }


    // getting camera parameters
    @SuppressLint("LongLogTag")
    private void getCamera() {
        preview = findViewById(R.id.PREVIEW);
        mHolder = preview.getHolder();
        //mHolder.addCallback(this);
        mHolder.addCallback(MainActivity.this);
        //Android < 2.3.6 ha bisogno di sto hack
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (camera == null) {
            try {


                camera = Camera.open();
                params = camera.getParameters();
                try {
                    camera.setPreviewDisplay(mHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (RuntimeException e) {
                Log.e("SUCA. Failed to Open. Error: ", e.getMessage());
            }
        }
    }

    /*
     * Turning On flash
     */
    private void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            // play sound

            isFlashOn = true;
            toggleButtonImage();

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            flashLight.setVisibility(View.GONE);
            screenLight.setVisibility(View.GONE);

           /* playButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);*/


            // changing button/switch image

            // playSound();
        }
    }

    private void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }

            // changing button/switch image
            isFlashOn = false;
            toggleButtonImage();

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();

            flashLight.setVisibility(View.VISIBLE);
            screenLight.setVisibility(View.VISIBLE);

        }
    }

    private void toggleButtonImage() {
        if (isFlashOn) {
            powerBtn.setImageResource(R.drawable.power);

        } else {
            powerBtn.setImageResource(R.drawable.poweroff);
        }
    }

    /*private void playSound(){
        if(isFlashOn){
            mp = MediaPlayer.create(ScreenLight.this, R.raw.light_switch_off);
        }else{
            mp = MediaPlayer.create(ScreenLight.this, R.raw.light_switch_on);
        }
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        });
        mp.start();
    }*/

    /**
     * Battery alert
     **/
    private void getBatteryPercentage() {
        BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int level = -1;
                if (currentLevel >= 0 && scale > 0) {
                    level = (currentLevel * 100) / scale;
                }
                batteryPercent.setText("Battery Level Remaining: " + level + "%");
            }
        };
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }

    public void colorChange() {
        bgElement.setBackgroundColor(Color.parseColor("#212121"));
        screenLight.setBackgroundColor(Color.parseColor("#212121"));
        powerBtn.setBackgroundColor(Color.parseColor("#212121"));
        flashLight.setBackgroundColor(Color.parseColor("#212121"));

    }
}