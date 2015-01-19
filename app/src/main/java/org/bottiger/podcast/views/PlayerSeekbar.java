package org.bottiger.podcast.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.listeners.PlayerStatusObserver;
import org.bottiger.podcast.provider.FeedItem;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by apl on 03-09-2014.
 */
public class PlayerSeekbar extends SeekBar implements PlayerStatusObserver, PaletteListener {

    private static final int RANGE_MIN = 0;
    private static final int RANGE_MAX = 1000;

    private int mSideMargin = 0;

    private static ObjectAnimator animator;

    private boolean mPaintSeekInfo = false;

    private View mOverlay = null;
    private FeedItem mEpisode = null;

    private TextView mBackwards;
    private TextView mCurrent;
    private TextView mForwards;

    private String mBackwardsText = "";
    private String mCurrentText = "";
    private String mForwardText = "";

    private int mStartSeekPosition = -1;
    private long mDurationMs = -1;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.PAUSED;
    private boolean mIsTouching = false;
    private HashSet<OnSeekListener> mSeekListeners = new HashSet<OnSeekListener>();

    private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

        private final int cDrawThresshold = 10;
        private int mThressholdCounter = -1;

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.d("PlayerSeekbar state", "onStopTrackingTouch");
            mIsTouching = false;
            mPaintSeekInfo = false;
            Log.d("mPaintSeekInfo", "onStopTracking mPaintSeekInfo => " + mPaintSeekInfo);
            mThressholdCounter = -1;

            validateState();
            long timeMs = mEpisode.getDuration() * seekBar.getProgress()
                    / RANGE_MAX;

            if (mEpisode == PodcastBaseFragment.mPlayerServiceBinder.getCurrentItem()) {
                PodcastBaseFragment.mPlayerServiceBinder.seek(timeMs);
            }

            invalidate();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.d("PlayerSeekbar state", "onStartTrackingTouch");
            validateState();

            Log.d("mPaintSeekInfo", "onStartTracking mPaintSeekInfo => " + mPaintSeekInfo);
            mIsTouching = true;
            mThressholdCounter = cDrawThresshold;
            mStartSeekPosition = seekBar.getProgress();

            long timeMs = mEpisode.getDuration() * seekBar.getProgress()
                    / RANGE_MAX;
            mEpisode.offset = (int) timeMs;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
            Log.d("PlayerSeekbar state", "Seekbar onProgressCanged");
            if (!fromUser) {
                Log.d("PlayerSeekbar", "Seekbar change , but not by the user");
                return;
            }

            Log.d("mPaintSeekInfo", "ProgressTracking1 mPaintSeekInfo => " + mPaintSeekInfo + " mThressholdCounter => " + mThressholdCounter);
            if (mThressholdCounter > 0) {
                mThressholdCounter--;
            } else {
                mPaintSeekInfo = true;
            }
            Log.d("mPaintSeekInfo", "ProgressTracking2 mPaintSeekInfo => " + mPaintSeekInfo + " mThressholdCounter => " + mThressholdCounter);

            if (mDurationMs < 1) {
                Log.d("PlayerSeekbar", "Duration unknown. Skipping overlay");
                mPaintSeekInfo = false;

            }

            float currentSeekPosition = (float)progress / RANGE_MAX;
            float SeekDiff = (float)(progress-mStartSeekPosition) / RANGE_MAX;

            long currentPositionMs = (long)(currentSeekPosition*mDurationMs);
            long diffMs = (long)(SeekDiff*mDurationMs);

            if (SeekDiff > 0) {
                mForwardText = "+" + msToString(diffMs);
                mBackwardsText = "";
            } else {
                mForwardText = "";
                mBackwardsText = "-" + msToString(-diffMs);
            }

            mCurrentText = msToString(currentPositionMs);

            for (OnSeekListener seekListener : mSeekListeners) {
                seekListener.seekTo(progress, RANGE_MIN, RANGE_MAX);
            }
        }
    };


    public PlayerSeekbar(Context context) {
        super(context);
        init(context);
    }

    public PlayerSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayerSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PlayerSeekbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(@NonNull Context argContext) {
        if (isInEditMode()) {
            return;
        }

        setMax(RANGE_MAX);
        setOnSeekBarChangeListener(onSeekBarChangeListener);
        PlayerStatusObservable.registerListener(this);
        mStatus = PlayerStatusObservable.getStatus();

        mSideMargin = argContext.getResources().getDimensionPixelSize(R.dimen.action_bar_height);

        Drawable thumb = getResources().getDrawable(R.drawable.seekbar_handle);
        Bitmap bitmap = ((BitmapDrawable) thumb).getBitmap();
        thumb = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 60, 90, true));

        setThumb(thumb);
    }

    public void setEpisode(FeedItem argEpisode) {
        mEpisode = argEpisode;
        mDurationMs = argEpisode.getDuration();
    }

    public void setOverlay(View argLayout) {
        mOverlay = argLayout;
        mBackwards = (TextView) mOverlay.findViewById(R.id.seekbar_time_backwards);
        mCurrent =   (TextView) mOverlay.findViewById(R.id.seekbar_time_current);
        mForwards =  (TextView) mOverlay.findViewById(R.id.seekbar_time_forward);
    }

    @Override
    public FeedItem getEpisode() {
        validateState();
        return mEpisode;
    }

    private FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(0 ,0);
    //private RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(200, 400);
    private int[] loc = new int[2];

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //mOverlay.setTranslationY(800);

        Log.d("mPaintSeekInfo", "draw with mPaintSeekInfo => " + mPaintSeekInfo);

        if (isInEditMode()) {
            return;
        }

        if (mPaintSeekInfo) {
            Log.d("PlayerSeekbar", "Draw seekinfo");
            if (params.height == 0) {
                mOverlay.setVisibility(INVISIBLE);
                params.height = mOverlay.getHeight();
                params.width = mOverlay.getWidth();
                mOverlay.bringToFront();
            }

            mBackwards.setText(mBackwardsText);
            mCurrent.setText(mCurrentText);
            mForwards.setText(mForwardText);

            this.getLocationOnScreen(loc);
            int offset = loc[1] - this.getHeight()*2;
            int translationY = (int)((View)this.getParent()).getTranslationY();
            Log.d("PlayerSeekbar", "trans => " + translationY);
            Log.d("PlayerSeekbar", "loc0 => " + loc[0] + " loc0 => " +loc[1]);
            params.setMargins(mSideMargin, offset, mSideMargin, 0);

            if (mOverlay != null) {
                mOverlay.setLayoutParams(params);
                mOverlay.setVisibility(VISIBLE);
                //mOverlay.bringToFront();
            }
        } else {
            Log.d("PlayerSeekbar", "Remove seekinfo");
            if (mOverlay != null) {
                mOverlay.setVisibility(GONE);
                //mOverlay.setVisibility(VISIBLE);
                //mOverlay.bringToFront();
            }
        }
    }

    @Override
    public void setProgressMs(long progressMs) {
        if (isTouching()) {
            return;
        }

        if (progressMs < 0) {
            throw new IllegalStateException("Progress must be positive");
        }
        float progress = 0;
        float duration = mEpisode.getDuration();

        if (duration <= 0) {
            Log.d("Warning", "Seekbar state may be invalid");
            return;
        }

        try {
            progress = progressMs / duration * RANGE_MAX;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgress((int) progress);
    }

    @Override
    public void onStateChange(EpisodeStatus argStatus) {
        validateState();

        mStatus = argStatus.getStatus();

        float currentPositionMs = argStatus.getPlaybackPositionMs() < 0 ? 0 : argStatus.getPlaybackPositionMs();
        double episodeLenghtMS = (double)mEpisode.getDuration();

        if (episodeLenghtMS < 0 || currentPositionMs > episodeLenghtMS) {
            return;
            //throw new IllegalStateException("Illegal Seekbar State: current position: " + currentPositionMs + ", episode length: " + episodeLenghtMS);
        }

        if (mStatus == PlayerStatusObservable.STATUS.PLAYING) {
            float progress = currentPositionMs / mEpisode.getDuration() * RANGE_MAX;
            setProgress((int) progress);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        String tag = "Seekbar";
        Log.d(tag, event.toString());
        Log.d(tag, "------------");
        requestParentTouchRecursive(getParent(), true);

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                onSeekBarChangeListener.onStopTrackingTouch(this);
                invalidate();
                requestParentTouchRecursive(getParent(), false);
                break;
            case MotionEvent.ACTION_DOWN:
                onSeekBarChangeListener.onStartTrackingTouch(this);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                int pos = (int) (getMax() * event.getX() / getWidth());
                Log.d(tag, "pos => " + pos);
                setProgress(pos);
                onSeekBarChangeListener.onProgressChanged(this, pos, true);
                //onSizeChanged(getWidth(), getHeight(), 0, 0);
                invalidate();
                break;

            default:
            case MotionEvent.ACTION_CANCEL:
                onSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
                requestParentTouchRecursive(getParent(), false);
                invalidate();
                break;
        }
        return true;

    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        Palette.Swatch swatch = argChangedPalette.getLightVibrantSwatch();
        if (swatch==null)
            return;

        int color = swatch.getTitleTextColor();
        ColorDrawable dc = new ColorDrawable(color);
        dc.setAlpha(200);
        setProgressDrawable(dc);
        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getImageURL(getContext());
    }

    public interface OnSeekListener {
        public void seekTo(int argCurrentSeekValue, int argMinValue, int argMaxValue);
    }

    public void registerListener(OnSeekListener argListener) {
        mSeekListeners.add(argListener);
    }

    public void unregisterListener(OnSeekListener argListener) {
        if (!mSeekListeners.contains(argListener)) {
            throw new IllegalArgumentException("Listener not registered");
        }

        mSeekListeners.remove(argListener);
    }

    public void validateState() {
        if (mEpisode == null) {
            throw new IllegalStateException("Episode needs to be set");
        }

        if (mOverlay == null) {
            throw new IllegalStateException("Overlay needs to be set");
        }
    }

    private void requestParentTouchRecursive(@NonNull ViewParent argThisParent, boolean argDisallowTouch) {
        argThisParent.requestDisallowInterceptTouchEvent(argDisallowTouch);

        ViewParent nextParent = argThisParent.getParent();

        if (nextParent != null) {
            //Log.d("PlayerSeekbar", nextParent.toString() + " -> " + argDisallowTouch);
            requestParentTouchRecursive(nextParent, argDisallowTouch);
        }
    }

    /*
    Fast but not so readable. From here: http://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format
     */
    public static String msToString(final long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);

        StringBuilder b = new StringBuilder();

        if (hours > 0) {
            b.append(hours < 10 ? String.valueOf("" + hours) : String.valueOf(hours));
            b.append(":");
        }

        b.append(minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) :
                String.valueOf(minutes));
        b.append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) :
                String.valueOf(seconds));
        return b.toString();
    }

    public boolean isTouching() {
        return mIsTouching;
    }
}