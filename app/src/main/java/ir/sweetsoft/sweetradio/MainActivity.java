package ir.sweetsoft.sweetradio;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ir.sweetsoft.sweetradio.R;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback
        , NavigationView.OnNavigationItemSelectedListener {
//    VideoView player;
    public final static String TAG = "MainActivity";
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private TextView titleText;
    private PlayService ps;
    private VLCPlayService vlcps;
    private Intent LastService=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

//        player=(VideoView) findViewById(R.id.videoplayer);


        titleText=(TextView)findViewById(R.id.radiotitle);
        Typeface face= Typeface.createFromAsset(getAssets(),"fonts/IRANSansMobile.ttf");
        titleText.setTypeface(face);
        mFilePath = "http://81.236.174.34:8000/;";

        vlcps=new VLCPlayService(MainActivity.this);
        vlcps.mFilePath=mFilePath;
        ps=new PlayService();
        ps.mFilePath=mFilePath;
        Log.d(TAG, "Playing: " + mFilePath);
        mSurface = (SurfaceView) findViewById(R.id.surface);

        holder = mSurface.getHolder();
        createPlayer(mFilePath);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
    private void play(final String URL,final String Title,Boolean LoadWithVLC)
    {
        if(!LoadWithVLC)
            playByDefaultAndroidPlayer(URL,Title);
        else
            playByVLCService(URL,Title);
    }
    private void stopService()
    {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();

        Iterator<ActivityManager.RunningAppProcessInfo> iter = runningAppProcesses.iterator();

        while(iter.hasNext()){
            ActivityManager.RunningAppProcessInfo next = iter.next();

            String pricessName = getPackageName() + ".PlayService";

            if(next.processName.equals(pricessName)){

                android.os.Process.killProcess(next.pid);
                break;
            }
        }
    }
    private void playWithVLC(final String URL,final String Title)
    {
        titleText.setText("در حال بارگذاری...");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    mMediaPlayer.setAudioDelay(5000);
                    mMediaPlayer.setSpuDelay(5000);
                    mMediaPlayer.stop();
                }
                catch (Exception ex){ex.printStackTrace();}
                Media m = new Media(libvlc, Uri.parse(URL));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleText.setText(Title);
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
                        NotificationCompat.Builder mBuilder
                                = new NotificationCompat.Builder(MainActivity.this)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("Sweet Radio")
                                .setContentText("رادیو آنلاین در حال اجرا است.")
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Create the NotificationChannel, but only on API 26+ because
                            // the NotificationChannel class is new and not in the support library
                            CharSequence name = "SweetOnlineRadio";
                            String description = "SweetOnlineRadio";
                            NotificationChannel channel = new NotificationChannel("SweetOnlineRadio", name, NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setDescription(description);

//                            notificationManager.createNotificationChannel(channel);

                        }
                        // Register the channel with the system
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
// notificationId is a unique int for each notification that you must define
                        notificationManager.notify(11, mBuilder.build());

                    }
                });
            }
        });
    }
    private void playByVLCService(final String URL,final String Title)
    {
        titleText.setText("در حال بارگذاری...");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
//                Intent serviceIntent = new Intent(MainActivity.this,VLCPlayService.class);
//                serviceIntent.putExtra("URL", URL);
//                if(LastService!=null)
//                {
//                    Log.d("Stopping",LastService.getAction());
//                    stopService(LastService);
//                }
//                startService(serviceIntent);
//                LastService=serviceIntent;

                try {vlcps.mMediaPlayer.stop(); }catch (Exception ex){}
                try {ps.mMediaPlayer.stop(); }catch (Exception ex){}

                vlcps.play(URL,Title);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleText.setText(Title);
                    }
                });
            }
        });
    }
    private void playByDefaultAndroidPlayer(final String URL,final String Title)
    {
        titleText.setText("در حال بارگذاری...");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {vlcps.mMediaPlayer.stop(); }catch (Exception ex){}
                try {ps.mMediaPlayer.stop(); }catch (Exception ex){}
                ps.play(URL);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleText.setText(Title);
//                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
//                        NotificationCompat.Builder mBuilder
//                                = new NotificationCompat.Builder(MainActivity.this)
//                                .setSmallIcon(R.mipmap.ic_launcher_round)
//                                .setContentTitle("Online Radio")
//                                .setContentText("رادیو آنلاین در خال اجرا است.")
//                                .setContentIntent(pendingIntent)
//                                .setAutoCancel(true)
//                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            // Create the NotificationChannel, but only on API 26+ because
//                            // the NotificationChannel class is new and not in the support library
//                            CharSequence name = "SweetOnlineRadio";
//                            String description = "SweetOnlineRadio";
//                            NotificationChannel channel = new NotificationChannel("SweetOnlineRadio", name, NotificationManager.IMPORTANCE_DEFAULT);
//                            channel.setDescription(description);
//
////                            notificationManager.createNotificationChannel(channel);
//
//                        }
//                        // Register the channel with the system
//                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
//// notificationId is a unique int for each notification that you must define
//                        notificationManager.notify(11, mBuilder.build());

                    }
                });
            }
        });
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
//            releasePlayer();
            play("http://81.236.174.34:8000/;","رادیو استکهلم",false);
        } else if (id == R.id.nav_gallery) {
            play("http://naxos.cdnstream.com/1305_128","رادیو دنیا",false);

        } else if (id == R.id.nav_slideshow) {
            play("http://54.37.72.221:8000/1?sid=1","رادیو 6 و 8",false);

        } else if (id == R.id.nav_niaz) {
            play("http://niazplay.se:8000/stream.mp3","رادیو نیاز",true);

        } else if (id == R.id.nav_manage) {
            play("http://stream.radiojavan.com/","رادیو جوان",false);

        }else if (id == R.id.nav_youngturks) {
            play("http://tunein.streamguys1.com/secure-youngturks-commercial?key=017b52afcf2218abda70bc207f4fc69eb5e07e642a0838ba833ab4cc3701c1b1","Young Turks",true);

        }else if (id == R.id.nav_cheddar) {
            play("http://cheddar.streamguys1.com/live-aac","Cheddar",false);

        }
        else if (id == R.id.nav_kralpop) {
            play("http://kralpopsc.radyotvonline.com/","Kral Pop",true);
        }
        else if (id == R.id.nav_kralfm) {
            play("http://kralwmp.radyotvonline.com/;","Kral FM",true);
        }else if (id == R.id.nav_kralturk) {
            play("http://canliradyoyayini.com:3434/;","Kral Türk FM",false);
        }
        else if (id == R.id.nav_showradio) {
            play("http://46.20.3.229/","Show Radio",false);
        }else if (id == R.id.nav_slowturk) {
            play("http://radyo.dogannet.tv/slowturk","Slow Turk",false);
        }else if (id == R.id.nav_powerturk) {
            play("http://powerturktaptaze.listenpowerapp.com/powerturktaptaze/mpeg/icecast.audio","Power Turk TapTaze",false);
        }
        else if (id == R.id.nav_superfm) {
            play("http://18463.live.streamtheworld.com/SUPER_FM_SC","Süper FM",false);
        }else if (id == R.id.nav_dejavu) {
            play("http://radyodejavu.canliyayinda.com:8054","Radio Dejavu",false);
        }else if (id == R.id.nav_palfm) {
            play("http://46.20.13.51:1230/","Pal FM",true);
        }
        else if (id == R.id.nav_twit) {
            play("http://twit.am/listen", "TWiT Live",false);
        }else if (id == R.id.nav_wunc) {
            play("http://mediaserver.wuncfm.unc.edu:8000/wunc128", "North Carolina Public Radio",false);
        }else if (id == R.id.nav_wnyc) {
            play("http://fm939.wnyc.org/wnycfm-tunein", "WNYC-FM",false);
        }else if (id == R.id.nav_wamu) {
            play("http://wamu-1.streamguys.com/wamu-1", "WAMU:American University Radio",false);
        }else if (id == R.id.nav_classic) {
            play("http://media-ice.musicradio.com/ClassicFMMP3", "Classic FM",false);
        }
        else if (id == R.id.nav_qmusic) {
            play("http://icecast-qmusicnl-cdp.triple-it.nl/Qmusic_nl_live_96.mp3", "QMusic",false);
        }else if (id == R.id.nav_nyclive) {
            play("https://streaming.radiostreamlive.com/radionylive_devices", "Radio New York Live",false);
        }
//        else if (id == R.id.nav_twit) {
//            play("http://twit.am/listen","TWiT Live");
//
//        }
// else if (id == R.id.nav_twit) {
//            play("http://twit.am/listen","TWiT Live");
//
//        }
// else if (id == R.id.nav_twit) {
//            play("http://twit.am/listen","TWiT Live");
//
//        }
        else if (id == R.id.nav_exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            getActionBar().setDisplayShowTitleEnabled(false);
        }catch (Exception ex){}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }


    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        int w = getWindow().getDecorView().getWidth();
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

        holder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(String media) {
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
                libvlc = new LibVLC(this, options);
                holder.setKeepScreenOn(true);

                mMediaPlayer = new MediaPlayer(libvlc);
                mMediaPlayer.setEventListener(mPlayerListener);

                // Seting up video output
                final IVLCVout vout = mMediaPlayer.getVLCVout();
                vout.setVideoView(mSurface);
                //vout.setSubtitlesView(mSurfaceSubtitles);
                vout.addCallback(this);
                vout.attachViews();

            }

//            Media m = new Media(libvlc, Uri.parse(media));
//            mMediaPlayer.setMedia(m);
//            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void releasePlayer() {
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
    private MediaPlayer.EventListener mPlayerListener = new MainActivity.MyPlayerListener(this);

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

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}
