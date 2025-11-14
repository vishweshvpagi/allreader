package com.example.allreader.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.recyclerview.widget.RecyclerView;

public class HackyRecyclerView extends RecyclerView {

    public HackyRecyclerView(Context context) {
        super(context);
    }

    public HackyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HackyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
