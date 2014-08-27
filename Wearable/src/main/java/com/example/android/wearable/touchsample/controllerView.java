package com.example.android.wearable.touchsample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;


public class controllerView extends ImageView {

    MainActivity mActivity;
    int mXPrev = 0;
    int mYPrev = 0;
    int mWidth = 0;
    int mHeight = 0;
    int mResolution = 20;

    public controllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (MainActivity) context;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mWidth = this.getWidth();
        mHeight = this.getHeight();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = mResolution * ((int) event.getX() / mResolution);
        int y = mResolution * ((int) event.getY() / mResolution);
        if (x != mXPrev || y != mYPrev) {
            mActivity.sendTouchPosition(x, y);
        }
        mXPrev = x;
        mYPrev = y;
        return true;
    }


}
