package net.majorkernelpanic.streaming;

/**
 * Created by Krystiano on 2018-03-20.
 */
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import net.majorkernelpanic.streaming.rtp.RtpSocket;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class BitrateAdapter {
    public static int MIN_BITRATE = 100*1024;
    public static int MAX_BITRATE = 5000000;//per bitrate ladder
    private int currentBitrate;
    final MediaCodec videoCodec;
    boolean enabled;
    RtpSocket rtpSocket;
    String httpIP;
    private static final int adjustBitrateMaxTime = 2000;//adjust the bitrate no more often than this time;ms

    private Handler handler = new Handler(Looper.getMainLooper());
    MyCallBack callBack=null;
    private static final String TAG = "BitrateAdapter";
    public BitrateAdapter(MediaCodec videoCodec, int currentBitrate, RtpSocket socket, int maxBitrate, String httpIP, MyCallBack callBack) {
        this.videoCodec = videoCodec;
        this.currentBitrate = currentBitrate;
        this.rtpSocket = socket;
        this.MAX_BITRATE=maxBitrate;
        this.httpIP=httpIP;
        this.callBack=callBack;
        enabled = Build.VERSION.SDK_INT >= 19 && videoCodec!=null;

        socket.setBitrateController(this);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                thread.start();
            }
        },7000);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setBitrate(double uploadSpeed) {
        int newBitrate= (int) (uploadSpeed/1.5);
        if(!enabled) return;
        int adjustedBitrate = clamp(newBitrate,MIN_BITRATE,MAX_BITRATE);
        if (adjustedBitrate!=newBitrate){
            newBitrate = adjustedBitrate;


        }
        if (currentBitrate==newBitrate){
            changeBufferTextView(uploadSpeed,newBitrate-currentBitrate,newBitrate);
            return;}
        Bundle params = new Bundle();
        params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrate);

        try{
            videoCodec.setParameters(params);
            changeBufferTextView(uploadSpeed,newBitrate-currentBitrate,newBitrate);
            Log.e("Tag",Integer.toString(newBitrate));
        }
        catch (IllegalStateException e){
            e.printStackTrace();
        }
        currentBitrate = newBitrate;

        lastTimeBitrateAdjusted = System.currentTimeMillis();


    }

    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (enabled) {

                   double uploadSpeed=measureUpload();
                    onNewSocketInfo(uploadSpeed);
                    Thread.sleep(3000 );//add extra time if the buffer is being filled from scratch
                }
            }
            catch (Exception e){
                Log.e(TAG,"error in bitrate controller "+ e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    });

    long lastTimeBitrateAdjusted;//ms
    private void onNewSocketInfo(double uploadSpeed){
        if (System.currentTimeMillis()<(lastTimeBitrateAdjusted+adjustBitrateMaxTime))return;

            setBitrate(uploadSpeed);
    }



    public void release(){
        enabled = false;
    }

    boolean queueDumped = false;
    public void onQueueDumped(){
        queueDumped=true;
        //requestSyncFrame();

    }



    public int clamp (int a, int b, int c){
        return (Math.max(b,Math.min(a,c)));
    }
    public void changeBufferTextView(double uploadSpeed,int step,int newBitrate){

        this.callBack.UpdateBufferPercent(uploadSpeed,step,newBitrate);

    }

    public void killBitrateController(){
        enabled=false;
    }

    public double measureUpload(){

        String Upload_Image_URL=httpIP+"/uploadSpeedMeasure/uploadSpeedMeasure.php";
        String fileName = "testimage";
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 2 * 1262069;
        double speed=0;

        File sourceFile = new File(String.valueOf(Environment.getExternalStorageDirectory())+"/testimage.jpg");

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist :"+fileName);

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(Upload_Image_URL);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                long start = System.currentTimeMillis();
                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);
                long end = System.currentTimeMillis();
                double time = ((end-start) / 1000.0);
                if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                speed =  sourceFile.length() / time;

                    Log.e("BitrateAdapter:", "Upload speed is: "
                            + (speed/(1024))*8+" kb/s");

                } else {
                    // something went wrong
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return speed*8;
    }
}
