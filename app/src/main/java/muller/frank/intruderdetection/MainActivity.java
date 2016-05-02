package muller.frank.intruderdetection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


public class MainActivity extends AppCompatActivity {

    TextView textView;
    SoundMeter soundMeter;
    private SensorManager mSensorManager;
    private Sensor mLight;
    private Sensor mAccel;
    private float currentLux;
    private float acc_x;
    private float acc_y;
    private float acc_z;
    private WebSocketClient mWs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if( event.sensor.getType() == Sensor.TYPE_LIGHT)
                {
                    currentLux = event.values[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor mySensor = event.sensor;

                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    acc_x = event.values[0];
                    acc_y = event.values[1];
                    acc_z = event.values[2];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, mAccel, SensorManager.SENSOR_DELAY_NORMAL);


        soundMeter = new SoundMeter();
        soundMeter.start();
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();


        try {
            mWs = new WebSocketClient( new URI( "wss://api.ferrai.io" ), new Draft_10() )
            {
                @Override
                public void onMessage( String message ) {

                    Log.d("", message);

                }

                @Override
                public void onOpen( ServerHandshake handshake ) {
                    Log.d("WebSocket", "Opened connection");
                }

                @Override
                public void onClose( int code, String reason, boolean remote ) {
                    Log.d("WebSocket", "Closed connection");
                }

                @Override
                public void onError( Exception ex ) {
                    ex.printStackTrace();
                }

            };
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            mWs.setSocket( factory.createSocket() );
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mWs.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //mWs.connect();

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        // do something here
                        String output = "{\"sensors\": {\""+ Secure.getString(getApplicationContext().getContentResolver(),Secure.ANDROID_ID) +"\": {\"accelerometer\": {\"x\": "+ acc_x +", \"y\": "+ acc_y +", \"z\": "+ acc_z +"}, \"light\": "+ currentLux +", \"microphone\": "+ Double.toString(soundMeter.getAmplitude()) +", \"timestamp\": "+ System.currentTimeMillis() +"}}, \"key\": \"DfgWpVZFF8a6uLW8ODpJ_0zsOu1xKZp7\"}";
                        Log.d("Data", output);
                        if(mWs.isOpen()){
                            mWs.send(output);
                            Log.d("Data", "Send to server");
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e("TEST", "local Thread error", e);
                    }
                }
            }
        };
        thread.start();
    }
}
