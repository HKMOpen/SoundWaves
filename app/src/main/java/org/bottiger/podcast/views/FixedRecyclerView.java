package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Debug;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.bottiger.podcast.adapters.decoration.OnDragStateChangedListener;

/**
 * Created by apl on 22-08-2014.
 *
 * http://stackoverflow.com/questions/25178329/recyclerview-and-swiperefreshlayout
 */
public class FixedRecyclerView extends RecyclerView implements OnDragStateChangedListener{

    private boolean mCanScrollRecyclerView = false;

    private ScrollerCompat mScroller;
    private int mLastY = 0;


    public static final int DOWN = -1;
    private static final int UP = 1;

    public FixedRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FixedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private double lastTime = 1;
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    protected void init(Context context) {
        mScroller = ScrollerCompat.create(context);
        if (this.isInEditMode())
            return;
    }

    public static boolean mSeekbarSeeking = false;

    // http://stackoverflow.com/questions/25178329/recyclerview-and-swiperefreshlayout
    @Override
    public boolean canScrollVertically(int direction) {
        return true;
        /*
        if (mSeekbarSeeking)
            return false;

        // check if scrolling up
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            mCanScrollRecyclerView = !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
            Log.d("FixedRecyclerView", "(mCanScrollRecyclerView) canscroll: " + mCanScrollRecyclerView);
            return mCanScrollRecyclerView;
        }
        boolean canScroll = super.canScrollVertically(direction);
        Log.d("FixedRecyclerView", "(super) canscroll: " + canScroll);
        return canScroll;
        */
    }


    private MotionEvent mouseDown;
    private float transStart = -1;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d("FixedRecyclerView", "(MotionEvent1)  " + ev.toString());

        if (!isDragging) {

            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                mouseDown = ev;
                transStart = ev.getRawY();
                //transStart = getTranslationY();
                mScroller.abortAnimation();
            }

            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                transStart = -1;
            }

            if (transStart != -1) {
                float diffY = getTranslationY() - transStart;
                Log.d("FixedRecyclerView", "(diffy)  " + diffY);

                float x = ev.getX();
                float y = ev.getY();

                ev.setLocation(x, y + diffY);
            } else {
                transStart = getTranslationY(); //ev.getRawY();
            }

            Log.d("FixedRecyclerView", "(MotionEvent2)  " + ev.toString());
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean argDisallow) {
        super.requestDisallowInterceptTouchEvent(argDisallow);
    }


    public boolean isDragging = false;
    @Override
    public void onDragStart(int position) {
        isDragging = true;
    }

    @Override
    public void onDragStop(int position) {
        isDragging = false;
    }
}
