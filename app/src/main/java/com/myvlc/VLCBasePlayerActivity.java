package com.myvlc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.vlc.audio.AudioServiceController;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.VLCInstance;
import org.videolan.vlc.util.WeakHandler;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *  Created by 林冠宏 on 2016/5/13.
 *  VLC框架封装抽象类
 *  子类Activity继承即可
 *  通过调用 addVideoView 添加视频 View
 */

public abstract class VLCBasePlayerActivity extends Activity implements IVideoPlayer,View.OnTouchListener{

    public final static String TAG = "VLC/VideoPlayerActivity";
    private final static String PLAY_FROM_VIDEOGRID = "org.videolan.vlc.gui.video.PLAY_FROM_VIDEOGRID";

    /** 多个视频的路径队列 */
    private Map<Integer,String> videoUrlList;

    private SurfaceView mSurface;
    private SurfaceView mSubtitlesSurface;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceHolder mSubtitlesSurfaceHolder;
    private FrameLayout mSurfaceFrame;
    private MediaRouter mMediaRouter;
    private MediaRouter.SimpleCallback mMediaRouterCallback;
    //private SecondaryDisplay mPresentation;
    private LibVLC mLibVLC;
    private String mLocation;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;

    private static final int SURFACE_16_9 = 2;
    private static final int SURFACE_4_3 = 3;
    private static final int SURFACE_ORIGINAL = 4;
    private int mCurrentSize = SURFACE_BEST_FIT; // 默认模式

    private SharedPreferences mSettings;
    private LinearLayout isPlayNow;
    /** Overlay */
    //private ActionBar mActionBar;
    private boolean mOverlayUseStatusBar = false;
    private View mOverlayHeader;
    private View mOverlayOption;
    private View mOverlayProgress;
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = 3600000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SURFACE_SIZE = 3;
    private static final int AUDIO_SERVICE_CONNECTION_SUCCESS = 5;
    private static final int AUDIO_SERVICE_CONNECTION_FAILED = 6;
    private static final int FADE_OUT_INFO = 4;
    private boolean mDragging;
    private boolean mShowing;
    private int mUiVisibility = -1;
    private SeekBar mSeekbar;
    private TextView mTime;
    private TextView mLength;
    private TextView mInfo;
    private ImageView mLoading;
    private ImageView playLogo;
    private TextView mLoadingText;
    private ImageButton mPlayPause,helper,full;
    private boolean mEnableJumpButtons,myCostomHide = false;
    private boolean itCanPlay=false; /** 多个视频的锁 */
    private boolean mEnableBrightnessGesture;
    private boolean mDisplayRemainingTime = false;
    private int mScreenOrientation;
    private ImageButton mLock;
    private ImageButton mSize;
    private ImageButton mMenu;
    private boolean mIsLocked = false;
    private int mLastAudioTrack = -1;
    private int mLastSpuTrack = -2;

    //private String customLocation;
    /**
     * For uninterrupted switching between audio and video mode
     */
    private boolean mSwitchingView;
    private boolean mEndReached;
    private boolean isAnyVideoPlay = false;
    private boolean mCanSeek;
    private boolean isFullShow = false;

    //private String myLocation = null;
    // Playlist
    private int savedIndexPosition = -1;

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    //Volume
    private AudioManager mAudioManager;
    private int mAudioMax;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
    private boolean mMute = false;
    private int mVolSave;
    private float mVol;

    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction;
    private int mSurfaceYDisplayRange;
    private float mTouchY, mTouchX;

    //stick event
    private static final int JOYSTICK_INPUT_DELAY = 300;
    private long mLastMove;

    // Brightness
    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;

    private final ArrayList<String> mSubtitleSelectedFiles = new ArrayList<String>();

    // Whether fallback from HW acceleration to SW decoding was done.
    private boolean mDisabledHardwareAcceleration = false;
    private int mPreviousHardwareAccelerationMode;

    // Tips
    //private View mOverlayTips;
    //private static final String PREF_TIPS_SHOWN = "video_player_tips_shown";

    // Navigation handling (DVD, Blu-Ray...)
    private boolean mHasMenu = false;
    private boolean mIsNavMenu = false;
    private LinearLayout videoFatherTemp;

    /** 正在显示播放的视频View */
    private View showingVideo;
    /** 由子类返回全屏播放容器 */
    //private RelativeLayout videoFullSizeFather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preperWork();
        Log.d("zzzzz", "onCreate");
        videoUrlList = setVideoPaths();
    }

    public abstract RelativeLayout setFullViewContainer();

    /** 子类不推荐 继承 onTouch 接口，应该直接 setOnTouchListener(new onTouch...) */
    //public abstract boolean onTouchE(View v, MotionEvent event);
    public abstract Map<Integer,String> setVideoPaths();
    public abstract boolean onKeyDownE(int keyCode, KeyEvent event);

    public void preperWork(){
        preperWorkBeforeSetContentLayout();
        if (LibVlcUtil.isICSOrLater()) {
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        /** onTouch被navigation bar截获，使用 OnSystemUiVisibilityChangeListener */
                        public void onSystemUiVisibilityChange(int visibility) {
                            if (visibility == mUiVisibility)
                                return;
                            setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
                            /** 下面显示出工具栏 */
                            if (visibility == View.SYSTEM_UI_FLAG_VISIBLE && !mShowing && !isFinishing()) {
                                showOverlay();
                            }
                            mUiVisibility = visibility;
                        }
                    }
            );
        }
    }

    private void preperWorkBeforeSetContentLayout(){
        if (LibVlcUtil.isJellyBeanMR1OrLater()) {
            // Get the media router service (Miracast)
            mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
            if(Build.VERSION.SDK_INT>=16){
                mMediaRouterCallback = new MediaRouter.SimpleCallback() {
                    @Override
                    public void onRoutePresentationDisplayChanged(
                            MediaRouter router, MediaRouter.RouteInfo info) {
                        Log.d(TAG, "onRoutePresentationDisplayChanged: info=" + info);
                        removePresentation();
                    }
                };
            }
            Log.d(TAG, "MediaRouter information : " + mMediaRouter  .toString());
            myCostomHide = true;
        } else {
            myCostomHide = false;
        }
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /** 一个播放视频的界面所有控件初始化 */
    private void createVideoViewInit(View videoLayoutView){
        mOverlayHeader = videoLayoutView.findViewById(R.id.player_overlay_header);
        Log.d("zzzzz","bigin ");
        mOverlayOption = videoLayoutView.findViewById(R.id.option_overlay); // 视频功能选择按钮容器布局
        mOverlayProgress = videoLayoutView.findViewById(R.id.progress_overlay); // 视频控件栏容器布局

        // Position and remaining time
        mTime = (TextView) videoLayoutView.findViewById(R.id.player_overlay_time); // 目前播放了多少时间
        mTime.setOnClickListener(mRemainingTimeListener);
        mLength = (TextView) videoLayoutView.findViewById(R.id.player_overlay_length); // 视频总长时间
        mLength.setOnClickListener(mRemainingTimeListener);

        mInfo = (TextView) videoLayoutView.findViewById(R.id.player_overlay_info); //正在加载的文字提示
        mEnableBrightnessGesture = mSettings.getBoolean("enable_brightness_gesture", true);
        mScreenOrientation = Integer.valueOf(mSettings.getString("screen_orientation_value", "1" /*SCREEN_ORIENTATION_SENSOR*/));

        mEnableJumpButtons = mSettings.getBoolean("enable_jump_buttons", false);
        mPlayPause = (ImageButton) videoLayoutView.findViewById(R.id.player_overlay_play); // 播放、暂停按钮
        helper = (ImageButton) videoLayoutView.findViewById(R.id.helper); // 用来消除视频界面点击界面变形的辅助控件
        mPlayPause.setOnClickListener(mPlayPauseListener);
        full = (ImageButton) videoLayoutView.findViewById(R.id.full); // 全屏按钮
        full.setOnClickListener(new fullClick(videoLayoutView));

        mLock = (ImageButton) videoLayoutView.findViewById(R.id.lock_overlay_button); // 锁屏按钮
        mLock.setOnClickListener(mLockListener);
        mSize = (ImageButton) videoLayoutView.findViewById(R.id.player_overlay_size); // 调整界面尺寸按钮
        mSize.setOnClickListener(mSizeListener);
        //mMenu = (ImageButton) videoLayoutView.findViewById(R.id.player_overlay_adv_function); // 标题栏功能按钮

        try {
            Log.d("zzzzz","init vlc ");
            mLibVLC = VLCInstance.getLibVlcInstance();// the most importance（jni，视频播放类的初始化，一切靠它传递）
        } catch (LibVlcException e) {
            Log.d("zzzzz", "LibVLC initialisation failed");
            return;
        }
        Log.d("zzzzz","after init vlc ");
        mSurface = (SurfaceView) videoLayoutView.findViewById(R.id.player_surface); // 播放的 surface
        mSurface.setKeepScreenOn(true);
        mSurfaceHolder = mSurface.getHolder();
        mSurfaceFrame = (FrameLayout) videoLayoutView.findViewById(R.id.player_surface_frame); //  surface 父容器
        String chroma = mSettings.getString("chroma_format", "");
        if(LibVlcUtil.isGingerbreadOrLater() && chroma.equals("YV12")) { // 播放像素格式
            mSurfaceHolder.setFormat(ImageFormat.YV12);
        } else if (chroma.equals("RV16")) {
            mSurfaceHolder.setFormat(PixelFormat.RGB_565);
        } else {
            mSurfaceHolder.setFormat(PixelFormat.RGBX_8888);
        }
        Log.d("zzzzz","middle ");
        mSubtitlesSurface = (SurfaceView) videoLayoutView.findViewById(R.id.subtitles_surface); // 辅助 surface
        mSubtitlesSurfaceHolder = mSubtitlesSurface.getHolder();
        mSubtitlesSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
        mSubtitlesSurface.setZOrderMediaOverlay(true);
        mSurfaceHolder.addCallback(mSurfaceCallback);
        mSubtitlesSurfaceHolder.addCallback(mSubtitlesSurfaceCallback);

        mSeekbar = (SeekBar) videoLayoutView.findViewById(R.id.player_overlay_seekbar); // 进度条
        mSeekbar.setOnSeekBarChangeListener(mSeekListener);

        /* Loading view */
        mLoading = (ImageView) videoLayoutView.findViewById(R.id.player_overlay_loading); //加载图片
        mLoadingText = (TextView) videoLayoutView.findViewById(R.id.player_overlay_loading_text);// 加载文字
        mLoading.setVisibility(View.GONE);
        mLoadingText.setVisibility(View.GONE);

        mSwitchingView = false;
        mEndReached = false;

        IntentFilter filter = new IntentFilter();
        if (!mOverlayUseStatusBar)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(UILApplication.SLEEP_INTENT);
        //registerReceiver(mReceiver, filter);
        Log.d(TAG,"Hardware acceleration mode: "+ Integer.toString(mLibVLC.getHardwareAcceleration()));
        if (mLibVLC.getHardwareAcceleration() == LibVLC.HW_ACCELERATION_FULL)
            mSubtitlesSurface.setVisibility(View.VISIBLE);
        mLibVLC.eventVideoPlayerActivityCreated(true);

        EventHandler em = EventHandler.getInstance();
        em.addHandler(eventHandler);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setRequestedOrientation(getScreenOrientation());
        Log.d("zzzzz","finish ");
    }

    @Override
    public void finish() {
        super.finish();
        Log.d("zzzzz", "finish ");
        setFullViewContainer().removeAllViews();
        videoOnDestory();
        relase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("zzzzz", "onPause ");
        doPlayPause();
        //videoOnPause();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("zzzzz", "onDestroy ");
        videoOnStop();
        // Dismiss the presentation when the activity is not visible.

    }

    public void videoOnPause(){
        if(isAnyVideoPlay){
            if (mMediaRouter != null) {
                // Stop listening for changes to media routes.
                mediaRouterAddCallback(false);
            }

            if(mSwitchingView) {
                Log.d(TAG, "mLocation = \"" + mLocation + "\"");
                AudioServiceController.getInstance().showWithoutParse(savedIndexPosition);
                AudioServiceController.getInstance().unbindAudioService(this);
                return;
            }

            long time = mLibVLC.getTime();
            long length = mLibVLC.getLength();
            //remove saved position if in the last 5 seconds
            if (length - time < 5000)
                time = 0;
            else
                time -= 5000; // go back 5 seconds, to compensate loading time

        /*
         * Pausing here generates errors because the vout is constantly
         * trying to refresh itself every 80ms while the surface is not
         * accessible anymore.
         * To workaround that, we keep the last known position in the playlist
         * in savedIndexPosition to be able to restore it during onResume().
         */
            mLibVLC.stop();

            mSurface.setKeepScreenOn(false);

            //SharedPreferences.Editor editor = mSettings.edit();
            // Save position
            /*if (time >= 0 && mCanSeek) {
                if(MediaDatabase.getInstance().mediaItemExists(mLocation)) {
                    MediaDatabase.getInstance().updateMedia(
                            mLocation,
                            MediaDatabase.mediaColumn.MEDIA_TIME,
                            time);
                } else {
                    // Video file not in media library, store time just for onResume()
                    editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, time);
                }
            }*/
            // Save selected subtitles
            String subtitleList_serialized = null;
            if(mSubtitleSelectedFiles.size() > 0) {
                Log.d(TAG, "Saving selected subtitle files");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(mSubtitleSelectedFiles);
                    subtitleList_serialized = bos.toString();
                } catch(IOException e) {}
            }
            //editor.putString(PreferencesActivity.VIDEO_SUBTITLE_FILES, subtitleList_serialized);

            //editor.commit();
            AudioServiceController.getInstance().unbindAudioService(this);
        }
    }

    private void videoOnStop(){
        if(isAnyVideoPlay){
            /*if (mPresentation != null) {
                Log.i(TAG, "Dismissing presentation because the activity is no longer visible.");
                mPresentation.dismiss();
                mPresentation = null;
            }*/
            restoreBrightness();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("zzzzz", "onDestroy ");
        //videoOnDestory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchingView = false;
        //itCanPlay = false;
        //onConnection();
    }

    private void videoOnDestory(){
        if(isAnyVideoPlay){
            //unregisterReceiver(mReceiver);
            EventHandler em = EventHandler.getInstance();
            em.removeHandler(eventHandler);
            // MediaCodec opaque direct rendering should not be used anymore since there is no surface to attach.
            mLibVLC.eventVideoPlayerActivityCreated(false);
            // HW acceleration was temporarily disabled because of an error, restore the previous value.
            if (mDisabledHardwareAcceleration)
                mLibVLC.setHardwareAcceleration(mPreviousHardwareAccelerationMode);
            mAudioManager = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void restoreBrightness() {
        if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness*255f);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
    }

    /**
     * Add or remove MediaRouter callbacks. This is provided for version targeting.
     *
     * @param add true to add, false to remove
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void mediaRouterAddCallback(boolean add) {
        if(!LibVlcUtil.isJellyBeanMR1OrLater() || mMediaRouter == null) return;

        if(add)
            mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        else
            mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    private void startPlayback() {
        Log.i("zzzzz", "startPlayback() ");
        loadMedia();

        /*
         * if the activity has been paused by pressing the power button,
         * pressing it again will show the lock screen.
         * But onResume will also be called, even if vlc-android is still in the background.
         * To workaround that, pause playback if the lockscreen is displayed
         */
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mLibVLC != null && mLibVLC.isPlaying()) {
                    KeyguardManager km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
                    if (km.inKeyguardRestrictedInputMode())
                        mLibVLC.pause();
                }
            }}, 500);

        // Add any selected subtitle file from the file picker
        if(mSubtitleSelectedFiles.size() > 0) {
            for(String file : mSubtitleSelectedFiles) {
                Log.i(TAG, "Adding user-selected subtitle " + file);
                mLibVLC.addSubtitleTrack(file);
            }
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        showOverlay();
        return true;
    }

    @TargetApi(12) //only active for Android 3.1+
    public boolean dispatchGenericMotionEvent(MotionEvent event){

        InputDevice mInputDevice = event.getDevice();

        float x = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X);
        float y = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y);
        float z = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Z);
        float rz = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_RZ);

        if (System.currentTimeMillis() - mLastMove > JOYSTICK_INPUT_DELAY){
            if (Math.abs(x) > 0.3){
                seek(x > 0.0f ? 10000 : -10000);
                mLastMove = System.currentTimeMillis();
            } else if (Math.abs(y) > 0.3){
                if (mIsFirstBrightnessGesture)
                    initBrightnessTouch();
                changeBrightness(-y/10f);
                mLastMove = System.currentTimeMillis();
            } else if (Math.abs(rz) > 0.3){
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int delta = -(int) ((rz / 7) * mAudioMax);
                int vol = (int) Math.min(Math.max(mVol + delta, 0), mAudioMax);
                setAudioVolume(vol);
                mLastMove = System.currentTimeMillis();
            }
        }
        return true;
    }


    private boolean navigateDvdMenu(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                mLibVLC.playerNavigate(LibVLC.INPUT_NAV_UP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mLibVLC.playerNavigate(LibVLC.INPUT_NAV_DOWN);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mLibVLC.playerNavigate(LibVLC.INPUT_NAV_LEFT);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mLibVLC.playerNavigate(LibVLC.INPUT_NAV_RIGHT);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                mLibVLC.playerNavigate(LibVLC.INPUT_NAV_ACTIVATE);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        showOverlay(OVERLAY_TIMEOUT);
        switch (keyCode) {
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_BUTTON_R1:
                seek(10000);
                return true;
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_BUTTON_L1:
                seek(-10000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_BUTTON_A:
                doPlayPause();
                return true;
            case KeyEvent.KEYCODE_V:
            case KeyEvent.KEYCODE_BUTTON_Y:
                //selectSubtitles();
                return true;
            case KeyEvent.KEYCODE_B:
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
            case KeyEvent.KEYCODE_BUTTON_B:
                //selectAudioTrack();
                return true;
            case KeyEvent.KEYCODE_M:
            case KeyEvent.KEYCODE_MENU:
                showNavMenu();
                return true;
            case KeyEvent.KEYCODE_O:
                //showAdvancedOptions(mMenu);
                return true;
            case KeyEvent.KEYCODE_A:
                resizeVideo();
                return true;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_BUTTON_X:
                updateMute();
                return true;
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (mIsNavMenu)
                    return navigateDvdMenu(keyCode);
                else
                    return onKeyDownE(keyCode, event);
            default:
                return onKeyDownE(keyCode, event);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("zzzzz","onConfigurationChanged");
        setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void setSurfaceSize(int width, int height, int visible_width, int visible_height, int sar_num, int sar_den) {
        if (width * height == 0)
            return;

        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visible_height;
        mVideoVisibleWidth  = visible_width;
        mSarNum = sar_num;
        mSarDen = sar_den;
        Message msg = mHandler.obtainMessage(SURFACE_SIZE);
        mHandler.sendMessage(msg);
    }

    /**
     * Lock screen rotation
     */
    private void lockScreen() {
        Log.d("zzzzz","Lock screen rotation");
        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d("zzzzz","change 870");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }else {
                Log.d("zzzzz","change 872");
                setRequestedOrientation(getScreenOrientation());
            }
        }
        showInfo("已锁屏", 1000);
        mLock.setBackgroundResource(R.mipmap.ic_locked);
        mTime.setEnabled(false);
        mSeekbar.setEnabled(false);
        mLength.setEnabled(false);
        hideOverlay(true);
    }

    /**
     * Remove screen lock
     */
    private void unlockScreen() {
        Log.d("zzzzz","Remove screen lock");
        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            Log.d("zzzzz","change 891");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        //showInfo(R.string.unlocked, 1000);
        mLock.setBackgroundResource(R.mipmap.ic_lock);
        mTime.setEnabled(true);
        mSeekbar.setEnabled(true);
        mLength.setEnabled(true);
        mShowing = false;
        showOverlay();
    }

    /**
     * Show text in the info view for "duration" milliseconds
     * @param text
     * @param duration
     */
    private void showInfo(String text, int duration) {
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(text);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }

    private void showInfo(int textid, int duration) {
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(textid);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }

    /**
     * Show text in the info view
     * @param text
     */
    private void showInfo(String text) {
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(text);
        mHandler.removeMessages(FADE_OUT_INFO);
    }

    /**
     * hide the info view with "delay" milliseconds delay
     * @param delay
     */
    private void hideInfo(int delay) {
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay);
    }

    /**
     * hide the info view
     */
    private void hideInfo() {
        hideInfo(0);
    }

    private void fadeOutInfo() {
        if (mInfo.getVisibility() == View.VISIBLE)
            mInfo.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
        mInfo.setVisibility(View.INVISIBLE);
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private int changeAudioFocus(boolean acquire) {
        if(!LibVlcUtil.isFroyoOrLater()) // NOP if not supported
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (mAudioFocusListener == null) {
            mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    /*
                     * Pause playback during alerts and notifications
                     */
                    switch (focusChange)
                    {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (mLibVLC.isPlaying())
                                mLibVLC.pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            if (!mLibVLC.isPlaying())
                                mLibVLC.play();
                            break;
                    }
                }
            };
        }

        int result;
        if(acquire) {
            result = mAudioManager.requestAudioFocus(mAudioFocusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            mAudioManager.setParameters("bgm_state=true");
        }
        else {
            if (mAudioManager != null) {
                result = mAudioManager.abandonAudioFocus(mAudioFocusListener);
                mAudioManager.setParameters("bgm_state=false");
            }
            else
                result = AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }

        return result;
    }

    /**
     *  Handle libvlc asynchronous events
     */
    private final Handler eventHandler = new VideoPlayerEventHandler(this);

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //Log.d("zzzzz", "onTouch");
        if(itCanPlay) {

            if (mIsLocked) { //如果屏幕已锁，阻止一切触屏事件
                // locked, only handle show/hide & ignore all actions
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (!mShowing) {
                        showOverlay();
                    } else {
                        Log.d("zzzzz", "lock hide");
                        hideOverlay(true);
                    }
                }
                return true;
            }
            //Log.d("zzzzz", "lock touch");

            DisplayMetrics screen = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(screen);

            if (mSurfaceYDisplayRange == 0)
                mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);

            float y_changed = event.getRawY() - mTouchY;
            float x_changed = event.getRawX() - mTouchX;

            // coef is the gradient's move to determine a neutral zone
            float coef = Math.abs(y_changed / x_changed);
            float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);

        /* Offset for Mouse Events */
            int[] offset = new int[2];
            mSurface.getLocationOnScreen(offset);
            int xTouch = Math.round((event.getRawX() - offset[0]) * mVideoWidth / mSurface.getWidth());
            int yTouch = Math.round((event.getRawY() - offset[1]) * mVideoHeight / mSurface.getHeight());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Audio
                    mTouchY = event.getRawY();
                    mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mTouchAction = TOUCH_NONE;
                    // Seek
                    mTouchX = event.getRawX();
                    // Mouse events for the core
                    LibVLC.sendMouseEvent(MotionEvent.ACTION_DOWN, 0, xTouch, yTouch);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Mouse events for the core
                    LibVLC.sendMouseEvent(MotionEvent.ACTION_MOVE, 0, xTouch, yTouch);

                    // No volume/brightness action if coef < 2 or a secondary display is connected
                    //TODO : Volume action when a secondary display is connected
                    if (coef > 2) {
                        mTouchY = event.getRawY();
                        mTouchX = event.getRawX();
                        // Volume (Up or Down - Right side)
                        if (!mEnableBrightnessGesture || (int) mTouchX > (screen.widthPixels / 2)) {
                            doVolumeTouch(y_changed);
                        }
                        // Brightness (Up or Down - Left side)
                        if (mEnableBrightnessGesture && (int) mTouchX < (screen.widthPixels / 2)) {
                            doBrightnessTouch(y_changed);
                        }
                        // Extend the overlay for a little while, so that it doesn't
                        // disappear on the user if more adjustment is needed. This
                        // is because on devices with soft navigation (e.g. Galaxy
                        // Nexus), gestures can't be made without activating the UI.
                        if (AndroidDevices.hasNavBar())
                            showOverlay();
                    } else {
                        // Seek (Right or Left move)
                        doSeekTouch(coef, xgesturesize, false);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    // Mouse events for the core
                    LibVLC.sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);

                    // Audio or Brightness
                    if (mTouchAction == TOUCH_NONE) {
                        if (!mShowing) {
                            showOverlay();
                        } else {
                            hideOverlay(true);
                        }
                    }
                    // Seek
                    doSeekTouch(coef, xgesturesize, true);
                    break;
            }
            return mTouchAction != TOUCH_NONE;
        }else{
            Log.d("zzzzz", "itCanPlay is false");
            return false;
        }
    }

    private static class VideoPlayerEventHandler extends WeakHandler<VLCBasePlayerActivity> {
        public VideoPlayerEventHandler(VLCBasePlayerActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            VLCBasePlayerActivity activity = getOwner();
            if(activity == null) return;
            // Do not handle events if we are leaving the VideoPlayerActivity
            if (activity.mSwitchingView) return;

            switch (msg.getData().getInt("event")) {
                case EventHandler.MediaParsedChanged:
                    Log.i(TAG, "MediaParsedChanged");
                    //activity.updateNavStatus();
                    if (!activity.mHasMenu && activity.mLibVLC.getVideoTracksCount() < 1) {
                        Log.i(TAG, "No video track, open in audio mode");
                        activity.switchToAudioMode();
                    }
                    break;
                case EventHandler.MediaPlayerPlaying:
                    Log.i(TAG, "MediaPlayerPlaying");
                    activity.stopLoadingAnimation();
                    activity.showOverlay();
                    /** FIXME: update the track list when it changes during the
                     *  playback. (#7540) */
                    //activity.setESTrackLists(true);
                    activity.setESTracks();
                    activity.changeAudioFocus(true);
                    //activity.updateNavStatus();
                    break;
                case EventHandler.MediaPlayerPaused:
                    Log.i(TAG, "MediaPlayerPaused");
                    break;
                case EventHandler.MediaPlayerStopped:
                    Log.i(TAG, "MediaPlayerStopped");
                    activity.changeAudioFocus(false);
                    break;
                case EventHandler.MediaPlayerEndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
                    activity.changeAudioFocus(false);
                    activity.endReached();
                    break;
                case EventHandler.MediaPlayerVout:
                    //activity.updateNavStatus();
                    if (!activity.mHasMenu)
                        activity.handleVout(msg);
                    break;
                case EventHandler.MediaPlayerPositionChanged:
                    if (!activity.mCanSeek)
                        activity.mCanSeek = true;
                    //don't spam the logs
                    break;
                case EventHandler.MediaPlayerEncounteredError:
                    Log.i(TAG, "MediaPlayerEncounteredError");
                    activity.encounteredError();
                    break;
                case EventHandler.HardwareAccelerationError:
                    Log.i(TAG, "HardwareAccelerationError");
                    activity.handleHardwareAccelerationError();
                    break;
                case EventHandler.MediaPlayerTimeChanged:
                    // avoid useless error logs
                    break;
                default:
                    Log.e(TAG, String.format("Event not handled (0x%x)", msg.getData().getInt("event")));
                    break;
            }
            activity.updateOverlayPausePlay();
        }
    };

    /**
     * Handle resize of the surface and the overlay
     */
    private final Handler mHandler = new VideoPlayerHandler(VLCBasePlayerActivity.this);

    private static class VideoPlayerHandler extends WeakHandler<VLCBasePlayerActivity> {
        public VideoPlayerHandler(VLCBasePlayerActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            VLCBasePlayerActivity activity = getOwner();
            if(activity == null) // WeakReference could be GC'ed early
                return;

            switch (msg.what) {
                case FADE_OUT:
                    activity.hideOverlay(false);
                    break;
                case SHOW_PROGRESS:
                    int pos = activity.setOverlayProgress();
                    if (activity.canShowProgress()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case SURFACE_SIZE:
                    Log.d("zzzzz","SURFACE_SIZE");
                    activity.changeSurfaceSize();
                    break;
                case FADE_OUT_INFO:
                    activity.fadeOutInfo();
                    break;
                case AUDIO_SERVICE_CONNECTION_SUCCESS:
                    activity.startPlayback();
                    break;
                case AUDIO_SERVICE_CONNECTION_FAILED:
                    activity.finish();
                    break;
            }
        }
    };

    private boolean canShowProgress() {
        if(mLibVLC!=null){
            return !mDragging && mShowing && mLibVLC.isPlaying();
        }
        return false;
    }

    private void endReached() {
        if(mLibVLC.getMediaList().expandMedia(savedIndexPosition) == 0) {
            Log.d(TAG, "Found a video playlist, expanding it");
            eventHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadMedia();
                }
            }, 1000);
        } else {
            /* Exit player when reaching the end */
            mEndReached = true;
            //finish();// 播放结束自动 finish----------------
        }
    }

    private void encounteredError() {
        /* Encountered Error, exit player with a message */
        AlertDialog dialog = new AlertDialog.Builder(VLCBasePlayerActivity.this)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setTitle("出错啦>_<")
                .setMessage("请重试")
                .create();
        dialog.show();
    }

    /*public void eventHardwareAccelerationError() {
        EventHandler em = EventHandler.getInstance();
        em.callback(EventHandler.HardwareAccelerationError, new Bundle());
    }*/

    private void handleHardwareAccelerationError() {
        mLibVLC.stop();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mDisabledHardwareAcceleration = true;
                        mPreviousHardwareAccelerationMode = mLibVLC.getHardwareAcceleration();
                        mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED);
                        mSubtitlesSurface.setVisibility(View.INVISIBLE);
                        loadMedia();
                    }
                })
                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setTitle("意外出错")
                .setMessage("请重试")
                .create();
        if(!isFinishing())
            dialog.show();
    }

    private void handleVout(Message msg) {
        if (msg.getData().getInt("data") == 0 && !mEndReached) {
            /* Video track lost, open in audio mode */
            Log.i(TAG, "Video track lost, switching to audio");
            mSwitchingView = true;
            finish();
        }
    }

    private void switchToAudioMode() {
        mSwitchingView = true;
        // Show the MainActivity if it is not in background.
        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        }
        finish();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void changeSurfaceSize() {
        int sw;
        int sh;
        // get screen size
        sw = getWindow().getDecorView().getWidth();
        sh = getWindow().getDecorView().getHeight();

        double dw = sw, dh = sh;
        boolean isPortrait;

        //if (mPresentation == null) {
        // getWindow().getDecorView() doesn't always take orientation into account, we have to correct the values
        isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        /*} else {
            isPortrait = false;
        }*/

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // sanity check
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mSarNum / mSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = dw / ar;
                break;
            /*case SURFACE_FIT_VERTICAL:
                dw = dh * ar;
                break;
            case SURFACE_FILL:
                break;*/
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        SurfaceView surface;
        SurfaceView subtitlesSurface;
        SurfaceHolder surfaceHolder;
        SurfaceHolder subtitlesSurfaceHolder;
        FrameLayout surfaceFrame;

        //if (mPresentation == null) {
        surface = mSurface;
        subtitlesSurface = mSubtitlesSurface;
        surfaceHolder = mSurfaceHolder;
        subtitlesSurfaceHolder = mSubtitlesSurfaceHolder;
        surfaceFrame = mSurfaceFrame;
        /*}else {
            surface = mPresentation.mSurface;
            subtitlesSurface = mPresentation.mSubtitlesSurface;
            surfaceHolder = mPresentation.mSurfaceHolder;
            subtitlesSurfaceHolder = mPresentation.mSubtitlesSurfaceHolder;
            surfaceFrame = mPresentation.mSurfaceFrame;
        }*/

        // force surface buffer size
        surfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        subtitlesSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = surface.getLayoutParams();
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        surface.setLayoutParams(lp);
        subtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = surfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        surfaceFrame.setLayoutParams(lp);
        surface.invalidate();
        subtitlesSurface.invalidate();
    }

    /**
     * show/hide the overlay
     */

    private void doSeekTouch(float coef, float gesturesize, boolean seek) {
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (coef > 0.5 || Math.abs(gesturesize) < 1 || !mCanSeek)
            return;

        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK)
            return;
        mTouchAction = TOUCH_SEEK;

        // Always show seekbar when searching
        if (!mShowing) showOverlay();

        long length = mLibVLC.getLength();
        long time = mLibVLC.getTime();

        // Size of the jump, 10 minutes max (600000), with a bi-cubic progression, for a 8cm gesture
        int jump = (int) (Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000));

        // Adjust the jump
        if ((jump > 0) && ((time + jump) > length))
            jump = (int) (length - time);
        if ((jump < 0) && ((time + jump) < 0))
            jump = (int) -time;

        //Jump !
        if (seek && length > 0)
            mLibVLC.setTime(time + jump);

        if (length > 0)
            //Show the jump's size
            showInfo(String.format("%s%s (%s)",
                    jump >= 0 ? "+" : "",
                    Strings.millisToString(jump),
                    Strings.millisToString(time + jump)), 1000);
        else
            showInfo("播放时不支持定位的流", 1000);
    }

    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        float delta = - ((y_changed * 2f / mSurfaceYDisplayRange) * mAudioMax);
        mVol += delta;
        int vol = (int) Math.min(Math.max(mVol, 0), mAudioMax);
        if (delta != 0f) {
            setAudioVolume(vol);
        }
    }

    private void setAudioVolume(int vol) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
        mTouchAction = TOUCH_VOLUME;
        showInfo("音量" + '\u00A0' + Integer.toString(vol),1000);
    }

    private void updateMute () {
        if (!mMute) {
            mVolSave = Float.floatToIntBits(mVol);
            mMute = true;
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            showInfo("声音关闭",1000);
        } else {
            mVol = mVolSave;
            mMute = false;
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Float.floatToIntBits(mVol), 0);
            showInfo("声音开启",1000);
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void initBrightnessTouch() {
        float brightnesstemp = 0.6f;
        // Initialize the layoutParams screen brightness
        try {
            if (LibVlcUtil.isFroyoOrLater() &&
                    Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                mRestoreAutoBrightness = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else {
                brightnesstemp = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightnesstemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture) initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;

        // Set delta : 2f is arbitrary for now, it possibly will change in the future
        float delta = - y_changed / mSurfaceYDisplayRange * 2f;

        changeBrightness(delta);
    }

    private void changeBrightness(float delta) {
        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness =  Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1);
        // Set Brightness
        getWindow().setAttributes(lp);
        showInfo("亮度" + '\u00A0' + Math.round(lp.screenBrightness * 15), 1000);
    }

    /**
     * handle changes of the seekbar (slicer)
     */
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            showOverlay(OVERLAY_INFINITE);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            showOverlay();

            hideInfo();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mCanSeek) {
                mLibVLC.setTime(progress);
                setOverlayProgress();
                Log.d("zzzzz","onProgressChanged");
                mTime.setText(Strings.millisToString(progress));
                showInfo(Strings.millisToString(progress));
            }

        }
    };

    private void showNavMenu() {
        /* Try to return to the menu. */
        /* FIXME: not working correctly in all cases */
        mLibVLC.setTitle(0);
    }

    /**
     * 暂停播放点击事件
     */
    private final View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doPlayPause();
        }
    };

    /** 添加视频入口 */
    public View addVideoView(int id){
        View viewBefore = LayoutInflater.from(this).inflate(R.layout.video_father, null, false);
        viewBefore.setId(id);
        ImageView playLogo = (ImageView) viewBefore.findViewById(R.id.player_logo); // 刚开始进入的触发按钮
        playLogo.setTag(id);
        playLogo.setOnClickListener(new playLogoClick(playLogo));
        return viewBefore;
    }

    /** 开始播放点击入口 */
    public final class playLogoClick implements View.OnClickListener{

        private View view;

        public playLogoClick(View view){
            this.view = view;
        }

        @Override
        public void onClick(View v) {
            //playLogo.setVisibility(View.GONE);
            if(!itCanPlay) {
                isAnyVideoPlay = true; // 用户触发了视频播放
                playEnter(view);
            }else{
                //Log.d("zzzzz",""+mLibVLC.getTime()+"----"+mLibVLC.getLength()+"----"+mLibVLC.getPosition());
                if((mLibVLC.getPosition()*100)>92){ // 判断是否播放到尾
                    relase();
                    playEnter(view);
                }else {
                    showDialog(view);
                }
            }
        }
    };

    private void playEnter(View v){
        if (v != null) {
            /** 下面这句设置隐藏掉 logo image */
            v.setVisibility(View.GONE);
            int index = (int) v.getTag();
            LinearLayout viewTrmp = (LinearLayout) findViewById(index);
            if (viewTrmp != null) {
                videoFatherTemp = viewTrmp;
                isPlayNow = viewTrmp; // 总是保存上一个正在播放的 VideoView father
                playLogo = (ImageView) v; //总是保存上一个的播放标志按钮
                itCanPlay = true;
                //customLocation = "http://www.xiangjiaoyun.com:8888/bananacloudapp/fileUploads/video/video.mp4";
                showingVideo = LayoutInflater.from(VLCBasePlayerActivity.this).inflate(R.layout.player, viewTrmp, false);
                showingVideo.setOnTouchListener(this);
                createVideoViewInit(showingVideo);
                viewTrmp.addView(showingVideo);
                mLibVLC.playMRL(videoUrlList.get(index));
                mLoading.setVisibility(View.VISIBLE);
                mLoadingText.setVisibility(View.VISIBLE);
                startLoadingAnimation();
            }
        }
    }

    private void showDialog(View v){
        final CharSequence[] items = {"是", "否"};
        final View temp = v;
        new AlertDialog.Builder(this).setTitle("是否停止另外一个视频播放").setItems(items,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            Log.d("zzzzz","play again");
                            relase();
                            playEnter(temp);
                        } else {
                            dialog.cancel();
                        }
                    }
                }).create().show();
    }

    private void relase(){
        mLibVLC.pause();
        mLibVLC.stop();
        mOverlayHeader = null;
        mOverlayOption = null;
        mOverlayProgress = null;
        mTime = null;
        mLength = null;
        mInfo = null;
        mPlayPause = null;
        helper = null;
        full = null;
        mLock = null;
        mSize = null;
        mMenu = null;
        mSurface = null;
        mSurfaceFrame = null;
        mSeekbar = null;
        mLoading = null;
        mLoadingText = null;
        if(isPlayNow!=null){
            isPlayNow.removeAllViews();
            playLogo.setVisibility(View.VISIBLE);
            playLogo.setOnClickListener(new playLogoClick(playLogo));
            isPlayNow.addView(playLogo);
        }
        itCanPlay = false;
    }

    /** 全屏点击入口 */
    private final class fullClick implements View.OnClickListener{

        private View view;
        public fullClick(View view){
            this.view = view;
        }

        @Override
        public void onClick(View v) {
            //RelativeLayout video_father = (RelativeLayout) view.findViewById(R.id.video_father);
            FrameLayout video_frame_father = (FrameLayout) view.findViewById(R.id.video_frame_father);
            if(!isFullShow) {
                Log.d("zzzzz","full view");
                isFullShow = true;
                dimStatusBar(true);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                ViewGroup.LayoutParams my = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//                ViewGroup.LayoutParams myFrame = new RelativeLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT);
//
//                video_frame_father.setLayoutParams(myFrame);
                video_frame_father.setLayoutParams(my);
                videoFatherTemp.removeView(showingVideo);
                setFullViewContainer().addView(showingVideo);
                full.setBackgroundResource(R.drawable.biz_video_shrink);
            }else{
                Log.d("zzzzz","full view 2");
                isFullShow = false;
                dimStatusBar(false);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                ViewGroup.LayoutParams my = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,250*3);
//                video_frame_father.setLayoutParams(my);
                //video_father.setLayoutParams(my);
                //video_frame_father.setLayoutParams(my);
                video_frame_father.setLayoutParams(my);
                setFullViewContainer().removeView(showingVideo);
                videoFatherTemp.addView(showingVideo);
                full.setBackgroundResource(R.drawable.biz_video_expand);
            }
        }
    }

    /** 暂停播放 */
    private void doPlayPause() {
        if(mEndReached){ // 播放是否结束过
            // 重播
            loadMedia();
            mEndReached = false;
        }else {
            if (mLibVLC.isPlaying()) {
                Log.d("zzzzz", "pause");
                pause();
            } else {
                Log.d("zzzzz", "play");
                play();
            }
            showOverlay();
        }
    }

    /**
     *
     */
    /*private final View.OnClickListener mBackwardListener = new View.OnClickListener() { // 后进
        @Override
        public void onClick(View v) {
            seek(-10000);
        }
    };*/

    /**
     *
     */
    /*private final View.OnClickListener mForwardListener = new View.OnClickListener() { // 快进
        @Override
        public void onClick(View v) {
            seek(10000);
        }
    };*/

    public void seek(int delta) {
        // unseekable stream
        if(mLibVLC.getLength() <= 0 || !mCanSeek) return;
        if(mLibVLC.isSeekable()){
            long position = mLibVLC.getTime() + delta;
            if (position < 0) position = 0;
            mLibVLC.setTime(position);
        }else{
            Toast.makeText(this,"不能拖动",Toast.LENGTH_SHORT).show();
        }
        showOverlay();
    }

    /**
     *
     */
    private final View.OnClickListener mLockListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mIsLocked) {
                mIsLocked = false;
                unlockScreen();
            } else {
                mIsLocked = true;
                lockScreen();
            }
        }
    };

    /**
     *
     */
    private final View.OnClickListener mSizeListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            resizeVideo();
        }
    };

    private void resizeVideo() {
        if (mCurrentSize < SURFACE_ORIGINAL) {
            mCurrentSize++;
        } else {
            mCurrentSize = 0;
        }
        changeSurfaceSize();
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                showInfo("最为合适", 1000);
                break;
            case SURFACE_FIT_HORIZONTAL:
                showInfo("水平匹配", 1000);
                break;
            /*case SURFACE_FIT_VERTICAL:
                showInfo("垂直匹配", 1000);
                break;
            case SURFACE_FILL:
                showInfo("填充", 1000);
                break;*/
            case SURFACE_16_9:
                showInfo("16:9", 1000);
                break;
            case SURFACE_4_3:
                showInfo("4:3", 1000);
                break;
            case SURFACE_ORIGINAL:
                showInfo("居中", 1000);
                break;
        }
        showOverlay();
    }

    private final View.OnClickListener mRemainingTimeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mDisplayRemainingTime = !mDisplayRemainingTime;
            showOverlay();
        }
    };

    /**
     * attach and disattach surface to the lib
     */
    private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(format == PixelFormat.RGBX_8888)
                Log.d("zzzzz", "Pixel format is RGBX_8888");
            else if(format == PixelFormat.RGB_565)
                Log.d("zzzzz", "Pixel format is RGB_565");
            else if(format == ImageFormat.YV12)
                Log.d("zzzzz", "Pixel format is YV12");
            else
                Log.d("zzzzz", "Pixel format is other/unknown");
            if(mLibVLC != null) {
                Log.d("zzzzz", "mLibVLC != null) {");
                mLibVLC.attachSurface(holder.getSurface(), VLCBasePlayerActivity.this);//------------
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(mLibVLC != null)
                mLibVLC.detachSurface();
        }
    };

    private final SurfaceHolder.Callback mSubtitlesSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(mLibVLC != null)
                mLibVLC.attachSubtitlesSurface(holder.getSurface());
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(mLibVLC != null)
                mLibVLC.detachSubtitlesSurface();
        }
    };

    /**
     * show overlay the the default timeout
     */
    private void showOverlay() {
        showOverlay(OVERLAY_TIMEOUT);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setActionBarVisibility(boolean show) {
        /*if (show)
            mActionBar.show();
        else
            mActionBar.hide();*/
    }

    /**
     * show overlay
     */
    private void showOverlay(int timeout) {
        if (mIsNavMenu)
            return;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (!mShowing) {
            mShowing = true;
            if (!mIsLocked) {
                if (mOverlayUseStatusBar)
                    setActionBarVisibility(true);
                else if (mOverlayHeader != null)
                    mOverlayHeader.setVisibility(View.VISIBLE);
                if(mOverlayOption!=null && mPlayPause!=null && helper!=null){
                    mOverlayOption.setVisibility(View.VISIBLE);
                    mPlayPause.setVisibility(View.VISIBLE);
                    helper.setVisibility(View.VISIBLE);
                    dimStatusBar(false);
                }
            }
            if(mOverlayProgress!=null){
                mOverlayProgress.setVisibility(View.VISIBLE);
            }else{
                return;
            }

            //if (mPresentation != null) mOverlayBackground.setVisibility(View.VISIBLE);
        }
        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
        updateOverlayPausePlay();
    }


    /**
     * hider overlay
     */
    private void hideOverlay(boolean fromUser) {
        if (mShowing) {
            mHandler.removeMessages(SHOW_PROGRESS);
            Log.i(TAG, "remove View!");
            //if (mOverlayTips != null) mOverlayTips.setVisibility(View.INVISIBLE);
            if (!fromUser && !mIsLocked) {
                if (mOverlayHeader != null) {
                    mOverlayHeader.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                }
                if(mOverlayOption!=null){
                    mOverlayOption.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                }else{
                    return;
                }
                mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mPlayPause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                helper.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                //mMenu.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            }
            if (mOverlayUseStatusBar) {
                Log.i(TAG, "if (mOverlayUseStatusBar) ");
                setActionBarVisibility(false);
            }else if (mOverlayHeader != null) {
                Log.i(TAG, "else if (mOverlayHeader != null)");
                mOverlayHeader.setVisibility(View.INVISIBLE);
            }
            mOverlayOption.setVisibility(View.INVISIBLE);
            mOverlayProgress.setVisibility(View.INVISIBLE);
            mPlayPause.setVisibility(View.INVISIBLE);
            //helper.setVisibility(View.INVISIBLE);
            //mMenu.setVisibility(View.INVISIBLE);
            mShowing = false;
            if(isFullShow) {
                dimStatusBar(true); // 全屏状态下才隐藏
            }
        } else if (!fromUser) { // 延时自动隐藏
            /*
             * Try to hide the Nav Bar again.
             * It seems that you can't hide the Nav Bar if you previously
             * showed it in the last 1-2 seconds.
             */
            if(isFullShow) {
                dimStatusBar(true); // 全屏状态下才隐藏
            }
        }
    }

    /**
     * Dim the status bar and/or navigation icons when needed on Android 3.x.
     * Hide it on Android 4.0 and later
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void dimStatusBar(boolean dim) {
        if (!LibVlcUtil.isHoneycombOrLater() || !AndroidDevices.hasNavBar() || mIsNavMenu)
            return;
        int layout = 0;
        if (!AndroidDevices.hasCombBar() && LibVlcUtil.isJellyBeanOrLater()) {
            if(isFullShow) { // 只设置在全屏的状态下才隐藏 系统标题栏
                layout = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            }
        }
        if (mOverlayUseStatusBar) {
            layout |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }else if(myCostomHide){
            if(isFullShow) { // 只设置在全屏的状态下才隐藏 系统标题栏
                layout |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            }
        }

        int visibility = layout;
        if (dim) {
            visibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            if (!AndroidDevices.hasCombBar()) {
                visibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (LibVlcUtil.isKitKatOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                if (mOverlayUseStatusBar) {
                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                }else if(myCostomHide){
                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                }
            }
        } else {
            visibility |= View.SYSTEM_UI_FLAG_VISIBLE;
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    private void updateOverlayPausePlay() {
        if (mLibVLC == null)
            return;
        mPlayPause.setBackgroundResource(mLibVLC.isPlaying() ? R.drawable.ic_pause_circle: R.drawable.ic_play_circle);
    }

    /**
     * update the overlay
     */
    private int setOverlayProgress() {
        if (mLibVLC == null) {
            return 0;
        }
        int time = (int) mLibVLC.getTime();
        int length = (int) mLibVLC.getLength();
        mSeekbar.setMax(length);
        mSeekbar.setProgress(time);
        if (time >= 0) mTime.setText(Strings.millisToString(time));
        if (length >= 0) mLength.setText(mDisplayRemainingTime && length > 0
                ? "- " + Strings.millisToString(length - time)
                : Strings.millisToString(length));

        return time;
    }

    private void setESTracks() {
        if (mLastAudioTrack >= 0) {
            mLibVLC.setAudioTrack(mLastAudioTrack);
            mLastAudioTrack = -1;
        }
        if (mLastSpuTrack >= -1) {
            mLibVLC.setSpuTrack(mLastSpuTrack);
            mLastSpuTrack = -2;
        }
    }

    private void play() {
        mLibVLC.play();
        mSurface.setKeepScreenOn(true);
    }

    /**
     *
     */
    private void pause() {
        mLibVLC.pause();
        mSurface.setKeepScreenOn(false);
    }

    /**
     * External extras:
     * - position (long) - position of the video to start with (in ms)
     */
    @SuppressWarnings({ "unchecked" })
    private void loadMedia() {
//        mLocation = null;
//        boolean dontParse = false;
//        int itemPosition = -1;
//
//        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
//            Log.e(TAG, "getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)");
//            /* Started from external application 'content' */
//            if (getIntent().getData() != null
//                    && getIntent().getData().getScheme() != null
//                    && getIntent().getData().getScheme().equals("content"))
//            {
//                Log.e(TAG, "if (getIntent().getData() != null");
//                // Media or MMS URI
//                if(getIntent().getData().getHost().equals("media") || getIntent().getData().getHost().equals("mms")) {
//                    try {
//                        Cursor cursor = getContentResolver().query(getIntent().getData(),new String[]{ MediaStore.Video.Media.DATA }, null, null, null);
//                        if (cursor != null) {
//                            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
//                            if (cursor.moveToFirst())
//                                mLocation = LibVLC.PathToURI(cursor.getString(column_index));
//                            cursor.close();
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Couldn't read the file from media or MMS");
//                        encounteredError();
//                    }  // Mail-based apps - download the stream to a temporary file and play it
//                }else if(getIntent().getData().getHost().equals("com.fsck.k9.attachmentprovider")
//                        || getIntent().getData().getHost().equals("gmail-ls")) {
//                    try {
//                        Cursor cursor = getContentResolver().query(getIntent().getData(),new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
//                        if (cursor != null) {
//                            cursor.moveToFirst();
//                            String filename = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
//                            cursor.close();
//                            Log.i(TAG, "Getting file " + filename + " from content:// URI");
//                            InputStream is = getContentResolver().openInputStream(getIntent().getData());
//                            OutputStream os = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);
//                            byte[] buffer = new byte[1024];
//                            int bytesRead = 0;
//                            while((bytesRead = is.read(buffer)) >= 0) {
//                                os.write(buffer, 0, bytesRead);
//                            }
//                            os.close();
//                            is.close();
//                            mLocation = LibVLC.PathToURI(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, "Couldn't download file from mail URI");
//                        encounteredError();
//                    }
//                }
//                // other content-based URI (probably file pickers)
//                else {
//                    mLocation = getIntent().getData().getPath();
//                }
//            } /* External application */
//            else if (getIntent().getDataString() != null) {
//                // Plain URI
//                mLocation = getIntent().getDataString();
//                // Remove VLC prefix if needed
//                if (mLocation.startsWith("vlc://")) {
//                    mLocation = mLocation.substring(6);
//                }
//                // Decode URI
//                if (!mLocation.contains("/")){
//                    try {
//                        mLocation = URLDecoder.decode(mLocation, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        Log.w(TAG, "UnsupportedEncodingException while decoding MRL " + mLocation);
//                    }
//                }
//            } else {
//                Log.e(TAG, "Couldn't understand the intent");
//                encounteredError();
//            }
//        }else if(getIntent().getAction() != null
//                && getIntent().getAction().equals(PLAY_FROM_VIDEOGRID)
//                && getIntent().getExtras() != null) {
//            Log.e(TAG, "else if(getIntent().getAction() != null");
//            mLocation = getIntent().getExtras().getString("itemLocation");
//            itemPosition = getIntent().getExtras().getInt("itemPosition", -1);
//        }
//
//        Log.e(TAG, "mSurface.setKeepScreenOn(true);");
//        mSurface.setKeepScreenOn(true);
//
//        if(mLibVLC == null) {
//            Log.e(TAG, "if(mLibVLC == null) {");
//            return;
//        }
//
//        if (mLocation != null && LibVlcUtil.isKitKatOrLater()) {
//            String locationLC = mLocation.toLowerCase(Locale.ENGLISH);
//            if (locationLC.endsWith(".ts")
//                    || locationLC.endsWith(".tts")
//                    || locationLC.endsWith(".m2t")
//                    || locationLC.endsWith(".mts")
//                    || locationLC.endsWith(".m2ts")) {
//                mDisabledHardwareAcceleration = true;
//                mPreviousHardwareAccelerationMode = mLibVLC.getHardwareAcceleration();
//                mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED);
//            }
//        }
//        Log.d(TAG, "if(dontParse && itemPosition >= 0) { ");
//        /* Start / resume playback */
//        if(dontParse && itemPosition >= 0) {
//            // Provided externally from AudioService
//            Log.d(TAG, "Continuing playback from AudioService at index " + itemPosition);
//            savedIndexPosition = itemPosition;
//            if(!mLibVLC.isPlaying()) {
//                Log.d(TAG, "if(!mLibVLC.isPlaying()) {");
//                // AudioService-transitioned playback for item after sleep and resume
//                //mLibVLC.playIndex(savedIndexPosition);
//                mLibVLC.playMRL("http://www.xiangjiaoyun.com:8888/bananacloudapp/fileUploads/video/video.mp4");
//                dontParse = false;
//            }else {
//                stopLoadingAnimation();
//                showOverlay();
//            }
//            //updateNavStatus();
//        } else if (savedIndexPosition > -1) {
//            AudioServiceController.getInstance().stop(); // Stop the previous playback.
//            Log.d(TAG, "savedIndexPosition > -1 ");
//            mLibVLC.setMediaList();
//            //mLibVLC.playIndex(savedIndexPosition);
//            mLibVLC.playMRL("http://www.xiangjiaoyun.com:8888/bananacloudapp/fileUploads/video/video.mp4");
//        } else if (mLocation != null && mLocation.length() > 0 && !dontParse) {
//            AudioServiceController.getInstance().stop();// Stop the previous playback.
//            Log.d(TAG, "mLocation != null && mLocation.length() > 0 ");
//            mLibVLC.setMediaList();
//            //Log.d("zzzzz","path is "+Environment.getExternalStorageDirectory().getPath()+"/sdcard/bcImageCache/video/xx.mp4");
//            mLibVLC.playMRL("file://" + Environment.getExternalStorageDirectory().getPath() + "/bcImageCache/video/xx.mp4");
//            mLibVLC.getMediaList().add(new Media(mLibVLC, mLocation));
//            savedIndexPosition = mLibVLC.getMediaList().size() - 1;
//            //mLibVLC.playIndex(savedIndexPosition);
//        }else{
//            Log.d("zzzzz","start ");
//            mLibVLC.playMRL("http://www.xiangjiaoyun.com:8888/ueditor/php/upload/video/20150926/1443238389263711.mp4");
//            //mLibVLC.getMediaList().add(new Media(mLibVLC, mLocation));
//            //savedIndexPosition = mLibVLC.getMediaList().size() - 1;
//        }
//        mCanSeek = false;
    }

    @SuppressWarnings("deprecation")
    private int getScreenRotation(){
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO /* Android 2.2 has getRotation */) {
            try {
                Method m = display.getClass().getDeclaredMethod("getRotation");
                return (Integer) m.invoke(display);
            } catch (Exception e) {
                return Surface.ROTATION_0;
            }
        } else {
            return display.getOrientation();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int getScreenOrientation(){
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rot = getScreenRotation();
        /*
         * Since getRotation() returns the screen's "natural" orientation,
         * which is not guaranteed to be SCREEN_ORIENTATION_PORTRAIT,
         * we have to invert the SCREEN_ORIENTATION value if it is "naturally"
         * landscape.
         */
        @SuppressWarnings("deprecation")
        boolean defaultWide = display.getWidth() > display.getHeight();
        if(rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270)
            defaultWide = !defaultWide;
        if(defaultWide) {
            switch (rot) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_180:
                    // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                default:
                    return 0;
            }
        } else {
            switch (rot) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_180:
                    // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                default:
                    return 0;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void removePresentation() {
        if (mMediaRouter == null)
            return;

        // Dismiss the current presentation if the display has changed.
        Log.i(TAG, "Dismissing presentation because the current route no longer "
                + "has a presentation display.");
        mLibVLC.pause(); // Stop sending frames to avoid a crash.
        finish(); //TODO restore the video on the new display instead of closing
        //if (mPresentation != null) mPresentation.dismiss();
        //mPresentation = null;
    }

    /**
     * Listens for when presentations are dismissed.
     */
    /**
     * Start the video loading animation.
     */
    private void startLoadingAnimation() {
        AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);
        mLoading.startAnimation(anim);
        mLoadingText.setVisibility(View.VISIBLE);
    }

    /**
     * Stop the video loading animation.
     */
    private void stopLoadingAnimation() {
        mLoading.setVisibility(View.INVISIBLE);
        mLoading.clearAnimation();
        mLoadingText.setVisibility(View.GONE);
    }

}
