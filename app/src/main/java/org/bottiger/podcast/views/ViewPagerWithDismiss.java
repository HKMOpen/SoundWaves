package org.bottiger.podcast.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by apl on 13-09-2014.
 */
public class ViewPagerWithDismiss extends ViewPager {

    public static final String TAG = "ViewPagerWithDismiss";

    float downX = -1;

    public ViewPagerWithDismiss(Context context) {
        super(context);
    }

    public ViewPagerWithDismiss(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * This is used for dsmissing episodes in the playlist by swiping them to the right.
     * Since the viewpager can only scroll to the left we make sure canScroll() returns false
     * when the user tries to scroll to the right (swiping an episode away)
     *
     * @param v
     * @param checkV
     * @param dx
     * @param x
     * @param y
     * @return
     */
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        Log.d(TAG, "canScroll, x => " + x + ", y => " + y + ", dx => " + dx);

        View focusedChild = ((ViewPagerWithDismiss) v).getFocusedChild();

        if (focusedChild instanceof SwipeRefreshExpandableLayout) {
            return dx > 0;
        }

        return false;
    }

    @Override
    public boolean 	onInterceptTouchEvent(MotionEvent ev) {

        boolean doIntercept = super.onInterceptTouchEvent(ev);

        if (getCurrentItem() > 0) {
            return doIntercept;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = ev.getRawX();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:{
                downX = -1;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getRawX()-downX;
                if (dx > 0)
                    doIntercept = false;
                break;
            }
        }

        Log.d(TAG, "onInterceptTouchEvent, x => " + ev.getX() + ", y => " + ev.getY() + " doIntercept: " + doIntercept);
        return doIntercept;
    }
}
