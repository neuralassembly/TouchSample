package com.example.android.wearable.touchsample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;


public class controllerView extends ImageView {

    MainActivity mActivity;
    int mWidth = 0;
    int mHeight = 0;

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
        int x = (int) event.getX();
        int y = (int) event.getY();
        mActivity.sendTouchPosition(x, y);
        return true;
    }


}
