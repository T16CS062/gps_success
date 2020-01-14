package com.example.newgps;

//import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import android.os.Handler;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

//import twitter4j.Tweet;

//AndroidX
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;


import android.os.AsyncTask;

import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;



public class LocationActivity extends FragmentActivity implements LocationListener,SensorEventListener {

    private LocationManager locationManager;
    public TextView textView,textGyro,textTweet,textDire,textElevation;

    protected final static double RAD2DEG = 180/Math.PI;

    int i;

    private ImageView arrowView;

    private StringBuilder strBuf = new StringBuilder();
    private StringBuilder strBuf_gyro = new StringBuilder();

    private static final int MinTime = 0;
    private static final float MinDistance = 0;

    private SensorManager sensorManager;

    private String str_tweet = new String(); // 取得したツイートが格納される（N:○○ E:○○）

    // Twitterオブジェクト
    private Twitter twitter = null;

    private ArrayList<Float> location_list = new ArrayList<Float>(); // 0:Latitude 1:longitude 2:bearing 3:Altitude
    private ArrayList<Float> gyro_list = new ArrayList<Float>(); //0:X 1:Y 2:Z
    private ArrayList<Float> balloon_list = new ArrayList<Float>(); //0:N 1:E 2:Altitude

    public float[] rotationMatrix = new float[9];
    public float[] gravity = new float[3];
    public float[] geomagnetic = new float[3];
    public float[] attitude = new float[3];

    float distance,direction; // 気球と端末の平面距離,方角

    Timer timer,timer_bearing;

    long interval = 30 /* msec */;

    long interval_bearing = 5000 /* msec */;

    public LocationActivity() {
        // OAuth認証用設定（1）
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        configurationBuilder.setOAuthConsumerKey("tVt7q8NvBZJlGFtJC19Mim4rv");
        configurationBuilder.setOAuthConsumerSecret("vTw3OsWpyGaCmyUTZAFPo7pkqNGpKLwUqbMrcFwJJyKJMSCEts");
        configurationBuilder.setOAuthAccessToken("1115672515966160896-jy5SY4ZPJC6ItqoC1KFP5A5UUijUkh");
        configurationBuilder.setOAuthAccessTokenSecret("wnrKoLyHj2fXceLlAeNrRmfElOMbeez5KKfWqO4IxKnJE");

        // Twitterオブジェクトの初期化（2）
        twitter = new TwitterFactory(configurationBuilder.build()).getInstance();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // LocationManager インスタンス生成
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        textView = findViewById(R.id.text_view);
        textGyro = findViewById(R.id.text_gyro);
        textTweet = findViewById(R.id.Tweet1);
        textDire = findViewById(R.id.text_direction);
        arrowView = findViewById(R.id.arrow);
        textElevation = findViewById(R.id.text_elevation);

        // GPS測位開始
        Button buttonStart = findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGPS();
            }
        });

        // GPS測位終了
        Button buttonStop = findViewById(R.id.button_stop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGPS();
            }
        });

        init_state(); // 矢印の向きの初期化


        // GPS測位(定期実行)開始
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
            // UIスレッド
                startTweet();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(r);


    }

    @Override
    protected void onResume() {
        // このアプリケーションが表示されたら
        super.onResume();
        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            // このメソッドが定期的に実行される
            @Override
            public void run() {
                // アニメーションの状態更新（ボールの位置とか）
                update_state();

/*
                String strDir = String.format(Locale.US,
                        " Direction : %f \n" + " Distance : %f\n" + " Elevation : %f\n ",
                        arrow_direction(), balloon_user_distance(), balloon_user_elevation());

                textDire.setText(strDir);
*/

                // ビューの更新を依頼する
                // view.invalidate(); ではダメ
                arrowView.postInvalidate();
            }
        }, interval, interval);


    }

    // 矢印の状態の初期化
    void init_state() {
        i = 0;
        arrowView.setRotation(i);
    }



    // 矢印の状態の更新
    public void update_state() {
       // i += 25;

        arrowView.setRotation((float)arrow_direction());

    }

    public double arrow_direction() // 方角を返す
    {
        if(location_list != null && location_list.size() != 0 && balloon_list != null && balloon_list.size() != 0) {

            double Deltax = (double)balloon_list.get(1) - (double)location_list.get(1);
            double Fai = 90 - Math.toDegrees(Math.atan2(

                    Math.cos(Math.toRadians((double)location_list.get(0))) * Math.tan(Math.toRadians((double)balloon_list.get(0)))
                    -
                    Math.sin(Math.toRadians((double)location_list.get(0))) * Math.cos(Math.toRadians(Deltax)), Math.sin(Math.toRadians(Deltax))
            ));
            return Fai - (double)location_list.get(2);
        }

        return 0;
    }

    public double balloon_user_distance(){ // 距離を返す

        if(location_list != null && location_list.size() != 0 && balloon_list != null && balloon_list.size() != 0) {
            double R = 6378.137f; // 赤道半径

            double Deltax = balloon_list.get(1) - location_list.get(1);

            double Distance = R * Math.acos(
                    (Math.sin(Math.toRadians(location_list.get(0))) * Math.sin(Math.toRadians(balloon_list.get(0)))) +
                            (Math.cos(Math.toRadians(location_list.get(0))) * Math.cos(Math.toRadians(balloon_list.get(0)))
                                    * Math.cos(Math.toRadians(Deltax))));

            return Distance;
        }

        return 0;
    }

    public double balloon_user_elevation(){ // 仰角を返す

        if(location_list != null && location_list.size() != 0 && balloon_list != null && balloon_list.size() != 0) {
            double theta = Math.atan(
                                ((double)balloon_list.get(2) - (double)location_list.get(3)) / balloon_user_distance()
                    );




            theta = Math.toDegrees(theta) + (attitude[1] * RAD2DEG);

            //System.out.println("theta:" + theta);
/*
            System.out.println(balloon_list.get(2));
            System.out.println(location_list.get(3));
            System.out.println(balloon_user_distance());
*/
            return theta;
        }

        return 0;
    }

    protected void startGPS() {
        //strBuf.append("startGPS\n");
        Log.d("gps_status", "start");
        //textView.setText(strBuf);
        textGyro.setText(strBuf_gyro);

        // Listenerの登録
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if(gyro != null){
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            String ns = "No Support";
            //textGyro.setText(ns);
        }

        Log.d("LocationActivity", "gpsEnabled");
        final boolean gpsEnabled
                = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            // GPSを設定するように促す
            enableLocationSettings();
        }

        if (locationManager != null) {
            Log.d("LocationActivity", "locationManager.requestLocationUpdates");

            try {
                // minTime = 1000msec, minDistance = 50m
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){

                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MinTime, MinDistance, this);
            } catch (Exception e) {
                e.printStackTrace();

                Toast toast = Toast.makeText(this,
                        "例外が発生、位置情報のPermissionを許可していますか？",
                        Toast.LENGTH_SHORT);
                toast.show();

                //MainActivityに戻す
                finish();
            }
        }


        super.onResume();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);

        if (locationManager != null) {
            Log.d("LocationActivity", "locationManager.removeUpdates");
            // update を止める
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED){

                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.removeUpdates(this);
        }

        // Listenerを解除
        sensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {


        String strTmp = String.format(Locale.US, "LocationManager\n " +
                " Latitude: %f " +
                " Longitude: %f\n " +
                " Bearing: %f " +
                " Altitude: %f",
                location.getLatitude(), location.getLongitude(), location.getBearing(), location.getAltitude());

        textView.setText(strTmp);

        if(location_list == null || location_list.size() == 0){
            location_list.add(0,(float)location.getLatitude());
            location_list.add(1,(float)location.getLongitude());
            location_list.add(2,location.getBearing());
            location_list.add(3,(float)location.getAltitude());
        }else{
            location_list.set(0,(float)location.getLatitude());
            location_list.set(1,(float)location.getLongitude());
            location_list.set(2,location.getBearing());
            location_list.set(3,(float)location.getAltitude());
        }

    }


    /*sensorEvent*/
    /*------------------------------------------------*/
    @Override
    public void onSensorChanged(SensorEvent event) {

        Log.d("debug","onSensorChanged");

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
          //  strBuf_gyro = null;
            float sensorX = event.values[0];
            float sensorY = event.values[1];
            float sensorZ = event.values[2];

            String strTmp = String.format(Locale.US, "Gyroscope\n " +
                    "X: %f " +
                    "Y: %f " +
                    "Z: %f",
                    sensorX, sensorY, sensorZ);
            textGyro.setText(strTmp);

            if(gyro_list == null || gyro_list.size() == 0){
                gyro_list.add(0,sensorX);
                gyro_list.add(1,sensorY);
                gyro_list.add(2,sensorZ);
            }else {
                gyro_list.set(0, sensorX);
                gyro_list.set(1, sensorY);
                gyro_list.set(2, sensorZ);
            }

        }else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            geomagnetic = event.values.clone();

        }else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            gravity = event.values.clone();
        }

        if(geomagnetic != null && gravity != null){

            SensorManager.getRotationMatrix(
                    rotationMatrix,null,
                    gravity, geomagnetic);

            SensorManager.getOrientation(
                    rotationMatrix,
                    attitude);

            String strEle = String.format(Locale.US, "Elevation\n " +
                            "azimuth: %d " +
                            "pitch: %d " +
                            "roll: %d",
            (int)(attitude[0] * RAD2DEG), (int)(attitude[1] * RAD2DEG), (int)(attitude[2] * RAD2DEG));
            textGyro.setText(strEle);

            /*
            azimuthText.setText(Integer.toString(
                    (int)(attitude[0] * RAD2DEG)));

            pitchText.setText(Integer.toString(
                    (int)(attitude[1] * RAD2DEG)));

            rollText.setText(Integer.toString(
                    (int)(attitude[2] * RAD2DEG)));
*/
        }

        String strDir = String.format(Locale.US,
                " Direction : %f \n" + " Distance : %f km\n" + " Elevation : %f\n ",
                arrow_direction(), balloon_user_distance(), balloon_user_elevation());

        textDire.setText(strDir);

    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    /*------------------------------------------------*/

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                /*
                strBuf.append("LocationProvider.AVAILABLE\n");
                textView.setText(strBuf);
                */
                Log.d("onStatusChanged", "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                /*
                strBuf.append("LocationProvider.OUT_OF_SERVICE\n");
                textView.setText(strBuf);
                */

                Log.d("onStatusChanged", "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
               /*
                strBuf.append("LocationProvider.TEMPORARILY_UNAVAILABLE\n");
                textView.setText(strBuf);
                */
                Log.d("onStatusChanged", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }
    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    private void stopGPS(){
        if (locationManager != null) {
            Log.d("LocationActivity", "onStop()");
            /*
            strBuf.append("stopGPS\n");
            textView.setText(strBuf);
*/
            textView.setText("stopGPS\n");
            // update を止める
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopGPS();
    }

     private void startTweet() {
         Log.d("LocationActivity", "startTweet()");
         // スレッド起動
         new GetUserTweet(this).execute();

         textTweet.setText("Balloon_GPS\n " + this.str_tweet);


         // 取得される文字列の例 "N35.6858216667 E139.756656667 A:26138.34235324"
         // 空白の位置indexを見つける
         // 現在は北緯、東経であると仮定している
         if(this.str_tweet.contains("N")) { //tweetを所得できている状態

             int index = this.str_tweet.indexOf(" ");
             int index2 = this.str_tweet.indexOf(" ",index + 1);

             if (balloon_list == null || balloon_list.size() == 0) {
                 balloon_list.add(0, Float.valueOf(this.str_tweet.substring(1, index)));
                 balloon_list.add(1, Float.valueOf(this.str_tweet.substring(index + 2, index2)));
                 balloon_list.add(2, Float.valueOf(this.str_tweet.substring(index2 + 2)));



             } else {
                balloon_list.set(0, Float.valueOf(this.str_tweet.substring(1, index)));
                balloon_list.set(1, Float.valueOf(this.str_tweet.substring(index + 2, index2)));
                balloon_list.set(2, Float.valueOf(this.str_tweet.substring(index2 + 2)));


             }

         }

    }

    public void setStr(String str_tweet) {
        this.str_tweet = str_tweet;
    }



    class GetUserTweet extends AsyncTask<Void, Void, String>{

        private LocationActivity activity;

        public GetUserTweet(LocationActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            // ここに前処理を記述します
            // 例） プログレスダイアログ表示
        }

        @Override
        protected String doInBackground(Void... arg0) {
            try {

                User user = twitter.showUser("@balloon_chase");
                long id = user.getId();
                List tweetList = twitter.getUserTimeline(id);
                twitter4j.Status tweet = (twitter4j.Status) tweetList.get(0);
                System.out.println(tweet.getText());
                return tweet.getText();

            } catch (TwitterException e) {
                Log.d("twitter", e.getMessage());
            }

            return null;

        }

        @Override
        protected void onPostExecute(String result) {
            // バックグランド処理終了後の処理をここに記述します
            // 例） プログレスダイアログ終了
            //    UIコンポーネントへの処理

            activity.setStr(result);
        }
    }


}