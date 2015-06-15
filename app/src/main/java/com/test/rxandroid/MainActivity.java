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

import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.ViewObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;


public class MainActivity extends Activity {

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
        final TextView touchCountIndicator = (TextView) findViewById(R.id.touchCountIndicator);

        final PublishSubject<MotionEvent> touchPublishSubject = PublishSubject.create();

        final ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        viewGroup.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touchPublishSubject.onNext(event);

                return true;
            }
        });

        ViewObservable
                .bindView(viewGroup, touchPublishSubject)
                .filter(new Func1<MotionEvent, Boolean>() {

                    @Override
                    public Boolean call(MotionEvent motionEvent) {
                        return motionEvent.getAction() == MotionEvent.ACTION_DOWN;
                    }
                })
                .doOnNext(new Action1<MotionEvent>() {

                    @Override
                    public void call(MotionEvent motionEvent) {
                        // show the touch
                        float x = motionEvent.getX();
                        float y = motionEvent.getY();

                        final ImageView touchIndicator = new ImageView(MainActivity.this);
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
                })
                .buffer(3L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<MotionEvent>>() {

                    @Override
                    public void call(List<MotionEvent> motionEvents) {
                        // show number of touch downs
                        touchCountIndicator.setText("" + motionEvents.size());

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
                    }
                });
    }
}
