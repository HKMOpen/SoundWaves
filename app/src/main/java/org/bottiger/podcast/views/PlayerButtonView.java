package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.listeners.DownloadObserver;
import org.bottiger.podcast.listeners.EpisodeStatus;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.ColorExtractor;

import java.lang.ref.WeakReference;

/**
 * TODO: document your custom view class.
 */
public class PlayerButtonView extends ImageButton implements PaletteListener  {

    public final static int STATE_DEFAULT = 0;
    public final static int STATE_DOWNLOAD = 1;
    public final static int STATE_DELETE = 2;
    public final static int STATE_QUEUE = 3;

    private PlayerStatusObservable.STATUS mStatus = PlayerStatusObservable.STATUS.STOPPED;
    private IEpisode mEpisode;

    private int mLastProgress = 0;
    private static final int BITMAP_OFFSET = 5;
    private static final float RECTANGLE_SCALING = 1F;

    protected Paint baseColorPaint;
    protected Paint foregroundColorPaint;
    private RectF buttonRectangle;
    private RectF buttonRectangleBitmap;

    private Context mContext;
    private WeakReference<Bitmap> s_Icon;
    private int defaultIcon;

    private int mCurrentState = 0;
    private SparseIntArray mStateIcons= new SparseIntArray();

    protected int mProgress = 0;
    protected DownloadStatus mDownloadCompletedCallback = null;

    private int mForegroundColor = getResources().getColor(R.color.colorPrimaryDark);
    private int mBackgroundColor = getResources().getColor(R.color.colorPrimaryDark);

    public PlayerButtonView(Context context) {
        super(context);
        init(context, null);
    }

    public PlayerButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayerButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        baseColorPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
        baseColorPaint.setColor(mBackgroundColor);
        baseColorPaint.setColor(-1761607680);
        baseColorPaint.setTextSize(12.0F);
        baseColorPaint.setStyle(Paint.Style.FILL_AND_STROKE); // Paint.Style.STROKE
        baseColorPaint.setStrokeWidth(10F);
        baseColorPaint.setAntiAlias(true);

        foregroundColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundColorPaint.setColor(mForegroundColor);
        foregroundColorPaint.setTextSize(12.0F);
        foregroundColorPaint.setStyle(Paint.Style.STROKE); // Paint.Style.FILL_AND_STROKE
        foregroundColorPaint.setStrokeWidth(10F);
        foregroundColorPaint.setAntiAlias(true);

        buttonRectangle = new RectF();
        buttonRectangleBitmap = new RectF();

        mContext = context;

        int image = -1;
        if (s_Icon == null) {
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.PlayerButtonViewImage, 0, 0);
            try {
                image = typedArray.getResourceId(R.styleable.PlayerButtonViewImage_image, R.drawable.generic_podcast);
            } finally {
                typedArray.recycle();
            }

            if (image > 0) {
                s_Icon = new WeakReference<Bitmap>(BitmapFactory.decodeResource(getResources(), image));
            }
        }
        defaultIcon = image;
        mStateIcons.put(PlayerButtonView.STATE_DEFAULT, defaultIcon);
    }

    public synchronized void setEpisode(IEpisode argEpisode) {
        this.mEpisode = argEpisode;
        ensureEpisode();
    }

    public synchronized void unsetEpisodeId() {
        this.mEpisode = null;
    }

    public void setStatus(PlayerStatusObservable.STATUS argStatus) {
        mStatus = argStatus;
        this.invalidate();
    }

    public void setColor(int argColor) {
        mBackgroundColor = argColor;
        this.invalidate();
    }

    public void setImage(int argImage) {
        s_Icon = new WeakReference<Bitmap>(BitmapFactory.decodeResource(getResources(), argImage));

        this.setImageBitmap(s_Icon.get());
        this.invalidate();
    }

    public void addState(int argState, int argDrawable) {
        if (mStateIcons.get(argState) != 0) {
            Log.w("PlayerButtonView", "Mapping already exists. Overwriting");
        }

        mStateIcons.put(argState, argDrawable);
    }

    public int getState() {
        return mCurrentState;
    }

    public void setState(int argState) {
        if (mStateIcons.get(argState) == 0) {
            throw new IllegalStateException("No such state exists");
        }

        mCurrentState = argState;
        setImage(mStateIcons.get(argState, defaultIcon));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long startTime = System.currentTimeMillis();
        super.onDraw(canvas);

        float halfW = getWidth()*RECTANGLE_SCALING;

        int left = (int)0;
        int width = (int)(halfW);
        int top = (int)0;

        buttonRectangle.set(left+BITMAP_OFFSET, top+BITMAP_OFFSET, left + width-BITMAP_OFFSET, top + width-BITMAP_OFFSET);

        //canvas.drawArc(buttonRectangle, -90, 360, true, baseColorPaint);

        if(mProgress!=0 && mProgress < 100) {
            if (getState() != PlayerButtonView.STATE_DEFAULT) {
                setState(PlayerButtonView.STATE_DEFAULT);
            }
            canvas.drawArc(buttonRectangle, -90, Math.round(360 * mProgress / 100F), false, foregroundColorPaint);
        }

        buttonRectangleBitmap = buttonRectangle;
        buttonRectangleBitmap.bottom -= BITMAP_OFFSET;
        buttonRectangleBitmap.top -= BITMAP_OFFSET;
        buttonRectangleBitmap.left -= BITMAP_OFFSET;
        buttonRectangleBitmap.right -= BITMAP_OFFSET;

        if (s_Icon == null) {
            Log.w("PlayerButton", "s_Icon is null");
            return;
        }

        //canvas.drawBitmap(s_Icon, null, buttonRectangle, baseColorPaint);
        mLastProgress = mProgress;
    }

    public IEpisode getEpisode() {
        ensureEpisode();

        return mEpisode;
    }

    @Override
    public void onPaletteFound(Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(argChangedPalette);

        baseColorPaint.setColor(extractor.getPrimary()); // -1761607680
        foregroundColorPaint.setColor(extractor.getSecondary());

        if (argChangedPalette != null) {
            //setBackgroundColor(ButtonColor(argChangedPalette));
        }

        invalidate();
    }

    @Override
    public String getPaletteUrl() {
        return mEpisode.getArtwork(mContext).toString();
    }

    public interface DownloadStatus {
        void FileComplete();
        void FileDeleted();
    }

    public void addDownloadCompletedCallback(DownloadStatus argCallback) {
        mDownloadCompletedCallback = argCallback;
    }

    public static int StaticButtonColor(@Nullable Context argContext, @NonNull Palette argPalette) {
        ColorExtractor extractor = new ColorExtractor(argContext, argPalette);
        return extractor.getPrimary();
    }

    public int ButtonColor(@NonNull Palette argPalette) {
        return StaticButtonColor(mContext, argPalette);
    }

    private void ensureEpisode() {
        if (mEpisode == null) {
            throw new IllegalStateException("Episode ID must be set before calling getEpisode");
        }
    }
}
