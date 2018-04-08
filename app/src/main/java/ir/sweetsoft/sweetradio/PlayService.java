package ir.sweetsoft.sweetradio;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;


import java.io.IOException;

/**
 * Created by Will on 4/4/2018.
 */

public class PlayService extends Service implements MediaPlayer.OnPreparedListener{
    public final static String TAG = "MainActivity";
    public String mFilePath;
    public MediaPlayer mMediaPlayer = null;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000);
                play(mFilePath);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        String URL="";
        URL=intent.getStringExtra("URL");
        mFilePath=URL;
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    public PlayService() {
        super();
    }

//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
////        play(URL);
//    }

    public void play(final String URL)
    {
        try
        {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer.reset();

        }
        catch (Exception ex){ex.printStackTrace();}

        try {
            mMediaPlayer=new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(PlayService.this);
            mMediaPlayer.setDataSource(URL);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public void onPrepared(MediaPlayer player) {
        player.start();
    }

//
//    @Override
//    public void onDestroy() {
//        try {
//            mMediaPlayer.pause();
//            Log.d("Stopping Service",PlayService.class.toString());
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            Log.d("Stopped Service", mFilePath);
//
//        }
//        catch (Exception ex){ex.printStackTrace();}
//    }
//
////    }
}
