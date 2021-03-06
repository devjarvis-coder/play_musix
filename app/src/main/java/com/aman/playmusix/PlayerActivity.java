package com.aman.playmusix;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import static com.aman.playmusix.AlbumDetailsAdapter.musicFilesAlbums;
import static com.aman.playmusix.MainActivity.repeatBoolean;
import static com.aman.playmusix.MainActivity.shuffleBoolean;
import static com.aman.playmusix.MusicAdapter.mFiles;

public class PlayerActivity extends AppCompatActivity implements ServiceConnection, MyServiceCallback, Playable {
    TextView song_name, artist;
    TextView duration_played;
    TextView duration_total;
    ImageView cover_art;
    ImageView next, previous, back_button, shuffleBtn, repeatBtn;
    FloatingActionButton pause_play;
    SeekBar seekBar;
    int position = -1;
    Thread pausePlay, nextBtn, previousBtn;
    static ArrayList<MusicFiles> listsong = new ArrayList<>();
    private Handler mHandler = new Handler();
    Animation animBlink;
    static Uri uri1;
    MusicService musicService;
    boolean bounded = false;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();
        initViews();
        getIntentMethod();
        back_button.setOnClickListener(v -> onBackPressed());
        shuffleBtn.setOnClickListener(v -> {
            if (shuffleBoolean) {
                shuffleBoolean = false;
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
            } else {
                shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                shuffleBoolean = true;
            }
        });
        repeatBtn.setOnClickListener(v -> {
            if (repeatBoolean) {
                repeatBoolean = false;
                repeatBtn.setImageResource(R.drawable.ic_repeat_off);
            } else {
                repeatBoolean = true;
                repeatBtn.setImageResource(R.drawable.ic_repeate_on);
            }
        });
    }

    @Override
    protected void onResume() {
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekToPosition(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                    if (!musicService.isPlaying()) {
                        duration_played.setVisibility(View.VISIBLE);
                        // start the animation
                        duration_played.startAnimation(animBlink);
                    } else {
                        duration_played.setVisibility(View.VISIBLE);
                        // start the animation
                        duration_played.clearAnimation();
                    }
                }
                mHandler.postDelayed(this, 1000);
            }
        });
        animBlink = AnimationUtils.loadAnimation(this,
                R.anim.blink);
        playPauseBtnThread();
        nextBtnThread();
        previousBtnThread();
        super.onResume();
    }

    private String formattedTime(int mCurrentPosition) {

        String totalout = "";
        String totaloutNew = "";
        String seconds = String.valueOf((mCurrentPosition % 60));
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totaloutNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            return totaloutNew;
        } else {
            return totalout;
        }
    }

    private void playPauseBtnThread() {
        pausePlay = new Thread() {
            @Override
            public void run() {
                pause_play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pausePlayBtnClicked();
                    }
                });
            }
        };


        pausePlay.start();
    }

    private void nextBtnThread() {
        nextBtn = new Thread() {
            @Override
            public void run() {
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };

        nextBtn.start();
    }

    private static int getRandom(int size) {
        Random random = new Random();
        return random.nextInt((size) + 1);
    }

    private void previousBtnThread() {
        previousBtn = new Thread() {
            @Override
            public void run() {
                previous.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        previousBtnClicked();
                    }
                });
            }
        };
        previousBtn.start();
    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        if (position != -1) {
            String sender = getIntent().getStringExtra("sender");
            if (sender != null && sender.equals("albumDetails")) {
                listsong = musicFilesAlbums;
            } else {
                listsong = mFiles;
            }
            if (listsong != null) {
                pause_play.setImageResource(R.drawable.ic_pause);
                if (shuffleBoolean)
                    shuffleBtn.setImageResource(R.drawable.ic_shuffle_on);
                if (repeatBoolean)
                    repeatBtn.setImageResource(R.drawable.ic_repeate_on);
                uri1 = Uri.parse(listsong.get(position).getPath());
            }
        }
        intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);
    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist = findViewById(R.id.artist);
        cover_art = findViewById(R.id.cover_art);
        next = findViewById(R.id.id_next);
        previous = findViewById(R.id.id_prev);
        pause_play = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
        back_button = findViewById(R.id.back_Btn);
        duration_played = findViewById(R.id.duration_played);
        duration_total = findViewById(R.id.total_duration);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
    }

    private void setFullScreen() {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //   v.setImageBitmap(new_image);
                Glide.with(c).load(new_image).into(v);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        MusicService.MyBinder b = (MusicService.MyBinder) binder;
        musicService = b.getService();
        bounded = true;
        musicService.setCallbacks(PlayerActivity.this, PlayerActivity.this);
        if (musicService != null) {
            seekBar.setMax(musicService.getFileDuration() / 1000);
            song_name.setText(musicService.getObjectOfMusicFile().getTitle());
            artist.setText(musicService.getObjectOfMusicFile().getArtist());
            musicService.sendChannel2(R.drawable.ic_pause);
            metaDataMethod(musicService.getUriOfMusicFile());
            musicService.onCompleted();
            if (musicService.isPlaying())
            {
                pause_play.setImageResource(R.drawable.ic_pause);
            }
            else
            {
                pause_play.setImageResource(R.drawable.ic_play);
            }
            position = musicService.getPositionOfMusicFile();
        }
        //Toast.makeText(PlayerActivity.this, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    @Override
    public void metaDataMethod(Uri uri) {
        String totalout;
        String totaloutNew;
        // get mp3 info
        // convert duration to minute:seconds
        String duration =
                musicService.getObjectOfMusicFile().getDuration();
        long dur = Long.parseLong(duration);
        String seconds = String.valueOf((dur % 60000) / 1000);

        String minutes = String.valueOf(dur / 60000);
        totalout = minutes + ":" + seconds;
        totaloutNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1) {
            duration_total.setText(totaloutNew);
        } else {
            duration_total.setText(totalout);
        }
        byte[] art = null;
        Bitmap image = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Size size = new Size(200, 200);
            try {
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        Long.parseLong(mFiles.get(position).getId()));
                image = getApplicationContext().getContentResolver().loadThumbnail(contentUri, size, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(uri.toString());
            art = retriever.getEmbeddedPicture();
            retriever.release();
            if (art != null)
                image = BitmapFactory.decodeByteArray(art, 0, art.length);
            else
                image = BitmapFactory.decodeResource(getResources(), R.drawable.programmity);
        }


        if (art != null) {
//            Glide.with(PlayerActivity.this).asBitmap()
//                    .load(art).into(cover_art);
            //image = BitmapFactory.decodeByteArray(art, 0, art.length);
            //setting image with animation.
            ImageViewAnimatedChange(PlayerActivity.this, cover_art, image);
            Palette.from(image).generate(palette -> {
                Palette.Swatch vibrantSwach = null;
                Palette.Swatch dominantSwach = null;
                if (palette != null) {
                    vibrantSwach = palette.getVibrantSwatch();
                    dominantSwach = palette.getDominantSwatch();
                }
                //you can generate as many as you want..
                if (dominantSwach != null) {
                    ImageView imgIcon = findViewById(R.id.imageView);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    imgIcon.setBackgroundResource(R.drawable.gredient_bg);
                    song_name.setTextColor(dominantSwach.getTitleTextColor());
                    artist.setTextColor(dominantSwach.getBodyTextColor());
                    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{dominantSwach.getRgb(),
                            0x00000000});
                    imgIcon.setBackground(gd);
                    GradientDrawable gdContainer = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{dominantSwach.getRgb(), dominantSwach.getRgb()});
                    mContainer.setBackground(gdContainer);
                } else if (vibrantSwach != null) {
                    ImageView imgIcon = findViewById(R.id.imageView);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    imgIcon.setBackgroundResource(R.drawable.gredient_bg);
                    song_name.setTextColor(vibrantSwach.getTitleTextColor());
                    artist.setTextColor(vibrantSwach.getBodyTextColor());
                    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{vibrantSwach.getRgb(),
                            0x00000000});
                    imgIcon.setBackground(gd);
                    GradientDrawable gdContainer = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{vibrantSwach.getRgb(), vibrantSwach.getRgb()});
                    mContainer.setBackground(gdContainer);
                } else {
                    ImageView imgIcon = findViewById(R.id.imageView);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    imgIcon.setBackgroundResource(R.drawable.gredient_bg);
                    song_name.setTextColor(Color.WHITE);
                    artist.setTextColor(Color.DKGRAY);
                    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xff000000,
                            0x00000000});
                    imgIcon.setBackground(gd);
                    GradientDrawable gdContainer = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{0xff000000, 0xff000000});
                    mContainer.setBackground(gdContainer);
                }
            });
        } else {
            Glide.with(PlayerActivity.this)
                    .asBitmap()
                    .load(R.drawable.programmity)
                    .into(cover_art);
            image = BitmapFactory.decodeResource(getResources(), R.drawable.programmity);
            Palette.from(image).generate(palette -> {
                Palette.Swatch dominantSwach = null;
                if (palette != null) {
                    dominantSwach = palette.getDarkMutedSwatch();
                }
                if (dominantSwach != null) {
                    ImageView imgIcon = findViewById(R.id.imageView);
                    RelativeLayout mContainer = findViewById(R.id.mContainer);
                    mContainer.setBackgroundResource(R.drawable.main_bg);
                    imgIcon.setBackgroundResource(R.drawable.gredient_bg);
                    song_name.setTextColor(dominantSwach.getTitleTextColor());
                    artist.setTextColor(dominantSwach.getBodyTextColor());
                    GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{dominantSwach.getRgb(),
                            0x00000000});
                    imgIcon.setBackground(gd);

                    GradientDrawable gdContainer = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{dominantSwach.getRgb(), dominantSwach.getRgb()});
                    mContainer.setBackground(gdContainer);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
        bounded = false;
        Log.e("Paused", bounded + "");
        Log.e("Paused", "passed");
    }

    @Override
    public void pausePlayBtnClicked() {
        seekBar.setMax(musicService.getFileDuration());
        if (musicService.isPlaying()) {
            pause_play.setImageResource(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getFileDuration() / 1000);
            musicService.sendChannel2(R.drawable.ic_play);
            PlayerActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    mHandler.postDelayed(this, 200);
                }
            });
        } else {
            pause_play.setImageResource(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getFileDuration() / 1000);
            musicService.sendChannel2(R.drawable.ic_pause);
            PlayerActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (musicService != null) {
                        int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    mHandler.postDelayed(this, 200);
                }
            });
        }
    }

    @Override
    public void nextBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(listsong.size() - 1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position + 1) % listsong.size());
            }
            uri1 = Uri.parse(listsong.get(position).getPath());
            Log.e("Uri -> ", uri1.toString());
            musicService.createMediaPlayer(position);
            //MediaMetaData to set album art
            if (bounded) {
                metaDataMethod(uri1);
                song_name.setText(listsong.get(position).getTitle());
                artist.setText(listsong.get(position).getArtist());
                pause_play.setBackgroundResource(R.drawable.ic_pause);
                seekBar.setMax(musicService.getFileDuration() / 1000);
                PlayerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                });
            }
            musicService.onCompleted();
            musicService.sendChannel2(R.drawable.ic_pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(listsong.size() - 1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position + 1) % listsong.size());
            }
            uri1 = Uri.parse(listsong.get(position).getPath());
            musicService.createMediaPlayer(position);
            musicService.onCompleted();
            if (bounded) {
                song_name.setText(listsong.get(position).getTitle());
                artist.setText(listsong.get(position).getArtist());
                pause_play.setBackgroundResource(R.drawable.ic_play);
                seekBar.setMax(musicService.getFileDuration() / 1000);
                PlayerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                });
                metaDataMethod(uri1);
            }
            musicService.sendChannel2(R.drawable.ic_play);
        }
    }

    @Override
    public void previousBtnClicked() {
        if (musicService.isPlaying()) {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(listsong.size());
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1) < 0 ? (listsong.size() - 1) : (position - 1));
            }
            uri1 = Uri.parse(listsong.get(position).getPath());
            musicService.createMediaPlayer(position);
            musicService.onCompleted();
            song_name.setText(listsong.get(position).getTitle());
            artist.setText(listsong.get(position).getArtist());
            seekBar.setMax(musicService.getFileDuration() / 1000);
            if (bounded) {
                PlayerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 200);
                    }
                });
                metaDataMethod(uri1);
                pause_play.setBackgroundResource(R.drawable.ic_pause);
            }
            musicService.sendChannel2(R.drawable.ic_pause);
            musicService.start();
        } else {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean) {
                position = getRandom(listsong.size());
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1) < 0 ? (listsong.size() - 1) : (position - 1));
            }
            uri1 = Uri.parse(listsong.get(position).getPath());
            musicService.createMediaPlayer(position);
            musicService.onCompleted();
            if (bounded) {
                song_name.setText(listsong.get(position).getTitle());
                artist.setText(listsong.get(position).getArtist());
                seekBar.setMax(musicService.getFileDuration() / 1000);
                PlayerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (musicService != null) {
                            int mCurrentPosition = musicService.getFileCurrentPosition() / 1000;
                            seekBar.setProgress(mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 200);
                    }
                });
                metaDataMethod(uri1);
                pause_play.setBackgroundResource(R.drawable.ic_play);
            }
            musicService.sendChannel2(R.drawable.ic_play);
        }
    }

    private byte[] getAlbumToAdapter(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
}
