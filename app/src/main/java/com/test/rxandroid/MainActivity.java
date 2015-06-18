package com.test.rxandroid;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewObservable;
import rx.subjects.PublishSubject;


public class MainActivity extends Activity {

    private ViewGroup viewGroup;

    private TextView touchCountIndicator;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        touchCountIndicator = (TextView) findViewById(R.id.touchCountIndicator);

        PublishSubject<MotionEvent> touchPublishSubject = PublishSubject.create();

        viewGroup = (ViewGroup) findViewById(android.R.id.content);
        viewGroup.setOnTouchListener((v, event) -> {
            touchPublishSubject.onNext(event);

            return true;
        });

        ViewObservable
                .bindView(viewGroup, touchPublishSubject.onBackpressureDrop())
                .filter(motionEvent -> {
                    int action = motionEvent.getActionMasked();
                    return action == MotionEvent.ACTION_DOWN
                            || action == MotionEvent.ACTION_POINTER_DOWN
                            || action == MotionEvent.ACTION_MOVE;
                })
                .doOnNext(this::showTouch)
                .map(motionEvent -> MotionEvent.obtain(motionEvent))
                .buffer(3L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showTouchCount);
    }

    private void showTouch(MotionEvent motionEvent) {
        for (int i = 0; i < motionEvent.getPointerCount(); ++i) {
            float x = motionEvent.getX(i);
            float y = motionEvent.getY(i);

            ImageView touchIndicator = new ImageView(MainActivity.this);
            touchIndicator.setImageResource(R.drawable.touch);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(10, 10);
            params.leftMargin = (int) (x - 5);
            params.topMargin = (int) (y - 5);
            viewGroup.addView(touchIndicator, params);

            touchIndicator
                    .animate()
                    .alpha(0F)
                    .scaleXBy(25F)
                    .scaleYBy(25F)
                    .setDuration(1000L)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            viewGroup.removeView(touchIndicator);
                        }
                    });
        }
    }

    private void showTouchCount(List<MotionEvent> motionEvents) {
        Observable
                .from(motionEvents)
                .doOnNext(MotionEvent::recycle)
                .reduce(0, (accumulator, motionEvent) -> accumulator + motionEvent.getPointerCount())
                .subscribe(pointerCount -> {
                    touchCountIndicator.setText("" + motionEvents.size() + " " + pointerCount);

                    touchCountIndicator
                            .animate()
                            .alpha(0F)
                            .scaleXBy(15F)
                            .scaleYBy(15F)
                            .setDuration(1000L)
                            .setListener(new AnimatorListenerAdapter() {

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    touchCountIndicator.setText(null);
                                    touchCountIndicator.setAlpha(1F);
                                    touchCountIndicator.setScaleX(1F);
                                    touchCountIndicator.setScaleY(1F);
                                }
                            });
                });
    }
}
