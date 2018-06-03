package com.github.hangonsushant.swipedowntofinish;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

public class SwipeDownLayout extends ViewGroup {

    private static final String TAG = "SwipeDownLayout";

    private enum DragEdge {
        TOP,
        BOTTOM
    }

    private DragEdge dragEdge = DragEdge.TOP;

    private static final double AUTO_FINISHED_SPEED_LIMIT = 3000.0;

    private final ViewDragHelper viewDragHelper;

    private View target;

    private View scrollChild;

    private int verticalDragRange = 0;

    private int horizontalDragRange = 0;

    private int draggingState = 0;

    private int draggingOffset;

    private boolean isChildScrollable = false;

    private float lastY = 0;

    private float newY = 0;

    private float offsetY = 0;

    private float lastX = 0;

    private float newX = 0;

    private float offsetX = 0;

    private boolean enablePullToBack = true;

    private static final float BACK_FACTOR = 0.5f;

    private float finishAnchor = 0;

    private SwipeDownListener swipeDownListener;

    public void setOnSwipeBackListener(SwipeDownListener listener) {
        swipeDownListener = listener;
    }

    public SwipeDownLayout(Context context) {
        this(context, null);
    }

    public SwipeDownLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        viewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallBack());
        checkDragable();
    }

    private void checkDragable() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    lastY = motionEvent.getRawY();
                    lastX = motionEvent.getRawX();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    newY = motionEvent.getRawY();
                    lastX = motionEvent.getRawX();

                    offsetY = Math.abs(newY - lastY);
                    lastY = newY;

                    offsetX = Math.abs(newX - lastX);
                    lastX = newX;

                    switch (dragEdge) {
                        case TOP:
                        case BOTTOM:
                            setEnablePullToBack(offsetY > offsetX);
                        default:
                            setEnablePullToBack(offsetY < offsetX);
                            break;
                    }
                }

                return false;
            }
        });
    }

    public void setEnablePullToBack(boolean enablePullToBack) {
        this.enablePullToBack = enablePullToBack;
    }

    private void ensureTarget() {
        if (target == null) {
            if (getChildCount() > 1) {
                throw new IllegalStateException("SwipeDownLayout must contains only one direct child");
            }
            target = getChildAt(0);

            if (scrollChild == null && target != null) {
                if (target instanceof ViewGroup) {
                    findScrollView((ViewGroup) target);
                } else {
                    scrollChild = target;
                }

            }
        }
    }

    private void findScrollView(ViewGroup viewGroup) {
        scrollChild = viewGroup;
        if (viewGroup.getChildCount() > 0) {
            int count = viewGroup.getChildCount();
            View child;
            for (int i = 0; i < count; i++) {
                child = viewGroup.getChildAt(i);
                if (child instanceof AbsListView || child instanceof ScrollView
                        || child instanceof WebView || child instanceof RecyclerView
                        || child instanceof NestedScrollView) {
                    scrollChild = child;
                    return;
                }
            }
            int start= 0;
            while (viewGroup.getChildAt(start) instanceof ViewGroup){
                findScrollView((ViewGroup) viewGroup.getChildAt(start));
                start++;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (getChildCount() == 0) return;

        View child = getChildAt(0);

        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        int childLeft = getPaddingLeft();
        int childTop = getPaddingTop();
        int childRight = childLeft + childWidth;
        int childBottom = childTop + childHeight;
        child.layout(childLeft, childTop, childRight, childBottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() > 1) {
            throw new IllegalStateException("SwipeDownLayout must contains only one direct child.");
        }

        if (getChildCount() > 0) {
            int measureWidth = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
            int measureHeight = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
            getChildAt(0).measure(measureWidth, measureHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        verticalDragRange = h;
        horizontalDragRange = w;

        switch (dragEdge) {
            case TOP:
            case BOTTOM:
                finishAnchor = finishAnchor > 0 ? finishAnchor : verticalDragRange * BACK_FACTOR;
                break;
        }
    }

    private int getDragRange() {
        switch (dragEdge) {
            case TOP:
            case BOTTOM:
                return verticalDragRange;
            default:
                return verticalDragRange;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean handled = false;
        ensureTarget();
        if (isEnabled()) {
            handled = viewDragHelper.shouldInterceptTouchEvent(ev);
        } else {
            viewDragHelper.cancel();
        }
        return handled || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        viewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public boolean canChildScrollUp() {
        return scrollChild.canScrollVertically(-1);
    }

    public void setChildScrollable(boolean isScrollable){
        this.isChildScrollable = isScrollable;
    }

    public boolean isChildScrollable(){
        return this.isChildScrollable;
    }

    public boolean canChildScrollDown() {
        return scrollChild.canScrollVertically(1);
    }

    private void finish() {
//        swipeDownListener.onSwipeToFinish();
        Activity act = (Activity) getContext();
        act.finish();
        act.overridePendingTransition(0, android.R.anim.fade_out);

    }

    private class ViewDragHelperCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == target && enablePullToBack;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return verticalDragRange;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return horizontalDragRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {

            int result = 0;

            if (dragEdge == DragEdge.TOP && !canChildScrollUp() && !isChildScrollable() && top > 0) {
                final int topBound = getPaddingTop();
                final int bottomBound = verticalDragRange;
                result = Math.min(Math.max(top, topBound), bottomBound);
            } else if (dragEdge == DragEdge.BOTTOM && !canChildScrollDown() && top < 0) {
                final int topBound = -verticalDragRange;
                final int bottomBound = getPaddingTop();
                result = Math.min(Math.max(top, topBound), bottomBound);
            }

            return result;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (state == draggingState) return;

            if ((draggingState == ViewDragHelper.STATE_DRAGGING || draggingState == ViewDragHelper.STATE_SETTLING) &&
                    state == ViewDragHelper.STATE_IDLE) {
                if (draggingOffset == getDragRange()) {
                    finish();
                }
            }

            draggingState = state;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            switch (dragEdge) {
                case TOP:
                case BOTTOM:
                    draggingOffset = Math.abs(top);
                    break;
                default:
                    break;
            }

            float fractionAnchor = (float) draggingOffset / finishAnchor;
            if (fractionAnchor >= 1) fractionAnchor = 1;

            float fractionScreen = (float) draggingOffset / (float) getDragRange();
            if (fractionScreen >= 1) fractionScreen = 1;

            if (swipeDownListener != null) {
                swipeDownListener.onViewPositionChanged(fractionAnchor, fractionScreen,top);
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (draggingOffset == 0) return;

            if (draggingOffset == getDragRange()) return;

            boolean isBack = false;

            if (backBySpeed(xvel, yvel)) {
                isBack = !canChildScrollUp();
            } else if (draggingOffset >= finishAnchor) {
                isBack = true;
            } else if (draggingOffset < finishAnchor) {
                isBack = false;
            }

            int finalTop;
            switch (dragEdge) {
                case TOP:
                    finalTop = isBack ? verticalDragRange : 0;
                    smoothScrollToY(finalTop);
                    break;
                case BOTTOM:
                    finalTop = isBack ? -verticalDragRange : 0;
                    smoothScrollToY(finalTop);
                    break;
            }

        }
    }

    private boolean backBySpeed(float xvel, float yvel) {
        switch (dragEdge) {
            case TOP:
            case BOTTOM:
                if (Math.abs(yvel) > Math.abs(xvel) && Math.abs(yvel) > AUTO_FINISHED_SPEED_LIMIT) {
                    return dragEdge == DragEdge.TOP ? !canChildScrollUp() : !canChildScrollDown();
                }
                break;
        }
        return false;
    }

    private void smoothScrollToY(int finalTop) {
        if (viewDragHelper.settleCapturedViewAt(0, finalTop)) {
            ViewCompat.postInvalidateOnAnimation(SwipeDownLayout.this);
        }
    }

    public interface SwipeDownListener {
        void onViewPositionChanged(float fractionAnchor, float fractionScreen, int top);
        void onSwipeToFinish();
    }

}
