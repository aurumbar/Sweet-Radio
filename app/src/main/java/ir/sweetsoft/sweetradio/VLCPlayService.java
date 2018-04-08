package ir.sweetsoft.sweetradio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.widget.VideoView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Will on 4/4/2018.
 */

public class VLCPlayService extends android.app.IntentService implements  IVLCVout.Callback{
//    public final static String TAG = "MainActivity";
    VideoView player;
    public final static String TAG = "VLCPlayService";
    public String mFilePath;
    public SurfaceView mSurface;
    public Activity Caller;
    public SurfaceHolder holder;
    public LibVLC libvlc;
    public org.videolan.libvlc.MediaPlayer mMediaPlayer = null;
    public int mVideoWidth;
    public int mVideoHeight;
//    public MediaPlayer mMediaPlayer = null;
public VLCPlayService() {
    super("RadioPlayService");
    Caller=null;

}
    public VLCPlayService(Activity act) {
        super("RadioPlayService");
        Caller=act;

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String URL="";
        URL=intent.getStringExtra("URL");
        createPlayer(URL);
//        play(URL,"");
    }


    @Override
    public void onDestroy() {
        try {

            mMediaPlayer.stop();
            releasePlayer();
        }
        catch (Exception ex){ex.printStackTrace();}
    }

    public void play(final String URL,final String Title)
    {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    mMediaPlayer.stop();
//                    mMediaPlayer.release();
//                    mMediaPlayer.reset();

                }
                catch (Exception ex){ex.printStackTrace();}

                try {
                    createPlayer(URL);
                    Media m = new Media(libvlc, Uri.parse(URL));
                    mMediaPlayer.setMedia(m);
                    mMediaPlayer.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    public void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

       /* int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);
*/
        holder.setFixedSize(mVideoWidth, mVideoHeight);
//        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
//        lp.width = w;
//        lp.height = h;
//        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    public void createPlayer(String media) {
        releasePlayer();
        try {
//            if (media.length() > 0) {
//                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
//                        0);
////                toast.show();
//            }

            // Creating media player
            if(mMediaPlayer==null)
            {
                // Create LibVLC
                // TODO: make this more robust, and sync with audio demo
                ArrayList<String> options = new ArrayList<String>();
                //options.add("--subsdec-encoding <encoding>");
                options.add("--aout=opensles");
                options.add("--audio-time-stretch"); // time stretching
                options.add("--rtsp-frame-buffer-size=5000");
                options.add("-vvv"); // verbosity
                Context ctx= Caller.getApplicationContext();
                libvlc = new LibVLC(ctx, options);
//                holder.setKeepScreenOn(true);

                mMediaPlayer = new org.videolan.libvlc.MediaPlayer(libvlc);
                mMediaPlayer.setEventListener(mPlayerListener);

                // Seting up video output
                final IVLCVout vout = mMediaPlayer.getVLCVout();
//                vout.setVideoView(mSurface);
                //vout.setSubtitlesView(mSurfaceSubtitles);
                vout.addCallback(this);
//                vout.attachViews();

            }

            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
//            Toast.makeText(this, "Error in creating player!", Toast
//                    .LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /**
     * Registering callbacks
     */
    public org.videolan.libvlc.MediaPlayer.EventListener mPlayerListener = new VLCPlayService.MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements org.videolan.libvlc.MediaPlayer.EventListener {
        private WeakReference<VLCPlayService> mOwner;

        public MyPlayerListener(VLCPlayService owner) {
            mOwner = new WeakReference<VLCPlayService>(owner);
        }

        @Override
        public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
            VLCPlayService player = mOwner.get();

            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
                case org.videolan.libvlc.MediaPlayer.Event.Paused:
                case org.videolan.libvlc.MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}
