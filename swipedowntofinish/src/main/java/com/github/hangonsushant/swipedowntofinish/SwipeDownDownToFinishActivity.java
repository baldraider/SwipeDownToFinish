package com.github.hangonsushant.swipedowntofinish;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by sasuke on 17/08/17.
 */

public class SwipeDownDownToFinishActivity extends AppCompatActivity implements SwipeDownLayout.SwipeDownListener {

    private SwipeDownLayout swipeDownLayout;
    private RelativeLayout rootContainer;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(getRootContainer());
        View view = LayoutInflater.from(this).inflate(layoutResID, null);
        swipeDownLayout.addView(view);
    }

    private View getRootContainer() {
        rootContainer = new RelativeLayout(this);
        swipeDownLayout = new SwipeDownLayout(this);
        swipeDownLayout.setOnSwipeBackListener(this);
        rootContainer.setBackgroundColor(ContextCompat.getColor(this,R.color.swipe_shadow_color));
        rootContainer.addView(swipeDownLayout);
        return rootContainer;
    }

    @Override
    public void onViewPositionChanged(float fractionAnchor, float fractionScreen, int top) {
        rootContainer.setAlpha(1 - fractionScreen);
    }

    @Override
    public void onSwipeToFinish() {
        
    }
}
