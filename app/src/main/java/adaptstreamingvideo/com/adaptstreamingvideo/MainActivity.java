package adaptstreamingvideo.com.adaptstreamingvideo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import net.majorkernelpanic.streaming.BitrateAdapter;
import net.majorkernelpanic.streaming.MyCallBack;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import net.majorkernelpanic.streaming.video.VideoStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements
        RtspClient.Callback,
        Session.Callback,
        SurfaceHolder.Callback,
        MyCallBack
     {
    private final static String TAG = "MainActivity";
    private Session mSession;
    private SurfaceView mSurfaceView;
    private float mDist;
    String username,password,rtspAddress,httpIP;
    TextView bitratetxt,addressRTSPtxt,uploadSpeedTxt,bufferInfoTxt,start,reso,framerate;
    int maxBitrate;


    MyCallBack callBack=this;
    private BitrateAdapter mBitrateAdapter;

    public VideoQuality videoQuality = new VideoQuality(640,480,30,500000);
    public int audioEncoder = SessionBuilder.AUDIO_AAC;
    public int videoEncoder = SessionBuilder.VIDEO_H264;

    private RtspClient mClient;




    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        copyFiletoExternalStorage(R.drawable.testimage);

        setContentView(R.layout.activity_main);

        bitratetxt=findViewById(R.id.bitrateValue);
        addressRTSPtxt=findViewById(R.id.addressRTSP);
        start=findViewById(R.id.start);
        reso=findViewById(R.id.ResolutionValue);
        framerate=findViewById(R.id.framerateValue);

        mSurfaceView = findViewById(R.id.surface);

        // Toolbar :: Transparent
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Adaptive Video Streaming");
        // Status bar :: Transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);





        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);


        // On android 3.* AAC ADTS is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
        audioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? SessionBuilder.AUDIO_AMRNB : SessionBuilder.AUDIO_AAC;
        audioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(audioEncoder)));
        videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));

        // Read video quality settings from the preferences
        videoQuality = new VideoQuality(
                settings.getInt("video_resX", videoQuality.resX),
                settings.getInt("video_resY", videoQuality.resY),
                Integer.parseInt(settings.getString("video_framerate", String.valueOf(videoQuality.framerate))),
                Integer.parseInt(settings.getString("video_bitrate", String.valueOf(videoQuality.bitrate/1000)))*1000);

        reso.setText(settings.getInt("video_resX", videoQuality.resX)+"x"+settings.getInt("video_resY", videoQuality.resY)+ "px");
        framerate.setText(settings.getString("video_framerate", String.valueOf(videoQuality.framerate))+" fps");
        bitratetxt.setText(videoQuality.bitrate/1000 + " kbps");


       addressRTSPtxt.setText(settings.getString("rtsp_address", ""));

        // Konfiguracja sesji
        mSession = SessionBuilder.getInstance()
                .setCallback(this)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setContext(getApplicationContext())
                .setAudioEncoder(audioEncoder)
                .setAudioQuality(new AudioQuality(16000, 32000))
                .setVideoEncoder(videoEncoder)
                .setVideoQuality(videoQuality)
                .build();

        // Konfiguracja klienta RTSP
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);

         mSurfaceView.getHolder().addCallback(this);
        (mSurfaceView).setAspectRatioMode(net.majorkernelpanic.streaming.gl.SurfaceView.ASPECT_RATIO_PREVIEW);
        settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleStream();
            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            updatePreferences();
            return true;
        }else if(id==R.id.action_flip){
            mSession.switchCamera();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
       mSession.startPreview();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        addressRTSPtxt.setText(settings.getString("rtsp_address", ""));
        reso.setText(settings.getInt("video_resX", videoQuality.resX)+"x"+settings.getInt("video_resY", videoQuality.resY)+ "px");
        framerate.setText(settings.getString("video_framerate", String.valueOf(videoQuality.framerate))+" fps");
        mSession.setVideoQuality(videoQuality);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.release();
        mSession.release();
        mSurfaceView.getHolder().removeCallback(this);
    }

    //region   ----------------------------------implement methods required


    @Override
    public void onBitrateUpdate(long bitrate) {

        //Log.d(TAG,"Bitrate: "+bitrate);
        bitratetxt.setText(""+bitrate/1024+" kbps");
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {

    }

    @Override
    public void onPreviewStarted() {

        if (mClient.isStreaming()) {
            mBitrateAdapter.killBitrateController();
            maxBitrate= calculateMaxBitrate();
            mBitrateAdapter = new BitrateAdapter(mSession.getVideoTrack().mMediaCodec, videoQuality.bitrate , mSession.getVideoTrack().getPacketizer().getRtpSocket(),maxBitrate,httpIP, callBack);
            Log.i(TAG, "Bitrate controller object" + "" +mSession.getBitrate());

        }
    }

    @Override
    public void onSessionConfigured() {

    }

    @Override
    public void onSessionStarted() {
        Log.i(TAG, "Session started");
//        initialize the bitrate controller
        maxBitrate= calculateMaxBitrate();
        mBitrateAdapter = new BitrateAdapter(mSession.getVideoTrack().mMediaCodec, videoQuality.bitrate , mSession.getVideoTrack().getPacketizer().getRtpSocket(), maxBitrate, httpIP, callBack);
        Log.i(TAG, "Bitrate controller object" + "" +mSession.getBitrate());

    }

    @Override
    public void onSessionStopped() {
        mBitrateAdapter.killBitrateController();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSession.startPreview();
        mSession.switchCamera();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mClient.stopStream();

    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {
        switch (message) {
            case RtspClient.ERROR_CONNECTION_FAILED:
                case RtspClient.ERROR_WRONG_CREDENTIALS:
                    alertError(exception.getMessage());
                    exception.printStackTrace();
                    break;
             }
         }
         private void alertError(final String msg) {
             final String error = (msg == null) ? "Unknown error: " : msg;
             AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
             builder.setMessage(error).setPositiveButton("Ok",
                     new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int id) {
                         }
                     });
             AlertDialog dialog = builder.create();
             dialog.show();
         }



      /* Ustawienia wideo i serwera*/
    public void updatePreferences() {
        if (!mClient.isStreaming()) {
            Intent settingsActivity = new Intent(getBaseContext(),
                    Settings.class);
            startActivity(settingsActivity);

        } else {
            Toast.makeText(this,"Aby wejść w ustawienia zatrzymaj strumieniowanie!",Toast.LENGTH_SHORT).show();
        }

    }
    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("video_resX") || key.equals("video_resY")) {
                videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
                videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
            }

            else if (key.equals("video_framerate")) {
                videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
            }

            else if (key.equals("video_bitrate")) {
                videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
            }

            else if (key.equals("audio_encoder") || key.equals("stream_audio")) {
                audioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", String.valueOf(audioEncoder)));
                SessionBuilder.getInstance().setAudioEncoder( audioEncoder );
                if (!sharedPreferences.getBoolean("stream_audio", false))
                    SessionBuilder.getInstance().setAudioEncoder(0);
            }

            else if (key.equals("stream_video") || key.equals("video_encoder")) {
                videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", String.valueOf(videoEncoder)));
                SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
                if (!sharedPreferences.getBoolean("stream_video", true))
                    SessionBuilder.getInstance().setVideoEncoder(0);


            }


        }
    };


public int calculateMaxBitrate(){
    int maxBitrate=0;
    switch (videoQuality.resX){
        case 1280:
            maxBitrate=3200*1024;
            break;
        case 720:
            maxBitrate=1470*1024;
            break;
        case 640:
            maxBitrate=1280*1024;
            break;
        case 320:
            maxBitrate=640*1024;
            break;

    }
    return maxBitrate;

};

    // Połączenie/rozłoączenie z serwerem RTSP, startowanie stopowanie strumienia wideo
    public void toggleStream() {

        if (!mClient.isStreaming()) {


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            username = prefs.getString("username", "");
            password= prefs.getString("password", "");
            rtspAddress = prefs.getString("rtsp_address", "");

            String ip, port, path;
            start.setText("STOP");
            // We parse the URI written in the Editext
            Pattern uri = Pattern.compile("rtsp://(.+):(\\d+)/(.+)");
            Matcher m = uri.matcher(rtspAddress);
            m.find();
            ip = m.group(1);
            port = m.group(2);
            path = m.group(3);
            httpIP=ip;
            mClient.setCredentials(username,
                    password);
            mClient.setServerAddress(ip, Integer.parseInt(port));
            mClient.setStreamPath("/" + path);
            mClient.startStream();

        } else {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
            start.setText("START");
        }
    }

    @Override
    public void UpdateBufferPercent(final double uploadSpeed, final int step, final int newBitrate) {

        uploadSpeedTxt=findViewById(R.id.buffereValue);
        bufferInfoTxt=findViewById(R.id.infoAboutBitrate);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(step>0){
                    bufferInfoTxt.setText("Bitrate został zwiększony o " + step/1024 + " kbps i wynosi: " + newBitrate/1024 + " kbps");
                    uploadSpeedTxt.setText(Integer.toString((int) (uploadSpeed/1024))+" kbps");
                    uploadSpeedTxt.setTextColor(getResources().getColor(R.color.green));
                }else if(step<0){
                    bufferInfoTxt.setText("Bitrate został zmniejszony o " + step/1024*(-1) + " kbps i wynosi: " + newBitrate/1024 + " kbps");
                    uploadSpeedTxt.setText(Integer.toString((int) (uploadSpeed/1024))+" kbps");
                    uploadSpeedTxt.setTextColor(getResources().getColor(R.color.red));
                }else{
                    bufferInfoTxt.setText("Bitrate wynosi: " +newBitrate/1024+" kbps");
                    uploadSpeedTxt.setText(Integer.toString((int) (uploadSpeed/1024))+" kbps");
                    uploadSpeedTxt.setTextColor(getResources().getColor(R.color.white));
                }
            }
        });

    }

    /**
     Zarządzanie zoomem i autofocusem
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID

        Camera.Parameters params = VideoStream.mCamera.getParameters();
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                VideoStream.mCamera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
            }
        }
        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        VideoStream.mCamera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            VideoStream.mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                }
            });
        }
    }


    private float getFingerSpacing(MotionEvent event) {

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

         private void copyFiletoExternalStorage(int resourceId){
             Bitmap bm = BitmapFactory.decodeResource(getResources(), resourceId);
             File f = new File(Environment.getExternalStorageDirectory(),"testimage.jpg");
             if(!f.exists()){
                 try {
                     FileOutputStream outStream = new FileOutputStream(f);
                     bm.compress(Bitmap.CompressFormat.JPEG, 40, outStream);
                     outStream.flush();
                     outStream.close();
                 } catch (FileNotFoundException e1) {
                     e1.printStackTrace();
                 } catch (IOException e1) {
                     e1.printStackTrace();
                 }
             }

         }
}



