package com.qbase.waveview;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Create by qay on 2018/12/5
 * 将占用高的绘制信息，放入此中
 */
public class RenderView extends SurfaceView implements SurfaceHolder.Callback{

    private static final String TAG = RenderView.class.getSimpleName();

    private static final Object mSurfaceLocak = new Object();

    private RenderThread mRunderThread;

    public RenderView(Context context) {
        this(context,null);
    }

    public RenderView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RenderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (mSurfaceLocak){
            mRunderThread = new RenderThread(holder);
            mRunderThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (mSurfaceLocak){
            mRunderThread.setRuning(false);
        }
    }

    /**线程绘制页面*/
    private class RenderThread extends Thread{
        private static final long SLEEP_TIME = 16;
        private SurfaceHolder mSurfaceHolder;
        private boolean mIsRuning = true;

        public RenderThread(SurfaceHolder mSurfaceHolder) {
            this.mSurfaceHolder = mSurfaceHolder;
        }

        @Override
        public void run() {
            super.run();

            long startTime = System.currentTimeMillis();

            while (true){
                synchronized (mSurfaceLocak){
                    while (true) {
                        if(!mIsRuning){
                            return;
                        }
                        Canvas canvas = mSurfaceHolder.lockCanvas();
                        if(canvas!=null){
                            //实现onDraw
                            onRenderView(canvas,System.currentTimeMillis() - startTime);
                            //解锁
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }

                        try {
                            Thread.sleep(SLEEP_TIME);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


        public void setRuning(boolean isRuning){
            this.mIsRuning = isRuning;
        }
    }


    protected void onRenderView(Canvas canvas,long time){

    }

}
