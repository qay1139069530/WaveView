package com.qbase.waveview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

/**
 * Create by qay on 2018/12/4
 * 渐变式的波纹
 * https://www.desmos.com/calculator
 * 性能更好
 */
public class WaveDecayView extends RenderView {


    private final Paint mPaint = new Paint();
    private final Path mFirstPath = new Path();
    private final Path mSecondPath = new Path();

    /**
     * 两条正玄波之间的波，振幅比较低的一条
     */
    private final Path mCenterPath = new Path();
    private final int SAMPLINT_SIZE = 128;
    /**
     * 采样点
     */
    private float[] mSamplingX;
    /**
     * 映射点
     */
    private float[] mMapX;
    private int mWidth;
    private int mHeight;
    private int mCenterHeight;
    private int mAmplitude;//振幅

    private final float[][] mCrestAndCrossPints = new float[9][];

    private final RectF rectF = new RectF();
    private final Xfermode mXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    private final int mBackGroundColor = Color.rgb(24, 33, 41);
    private final int mCenterPathColor = Color.argb(64, 255, 255, 255);

    private long startTime = System.currentTimeMillis();

    public WaveDecayView(Context context) {
        this(context, null);
    }

    public WaveDecayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        init(context, attrs);
    }

    public WaveDecayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WaveDecayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {

        mPaint.setDither(true);//防抖动
        mPaint.setAntiAlias(true);//抗锯齿
        for (int i = 0; i < 9; i++) {
            mCrestAndCrossPints[i] = new float[2];
        }
    }


    @Override
    protected void onRenderView(Canvas canvas, long time) {
        super.onRenderView(canvas, time);
        //super.onDraw(canvas);
        //采样
        if (mSamplingX == null) {
            mWidth = canvas.getWidth();
            mHeight = canvas.getHeight();
            mCenterHeight = mHeight == 0 ? 50 : mHeight >> 1;// 1/2高
            mAmplitude = mWidth == 0 ? 30 : mWidth >> 3;//振幅为宽度的1/8
            mSamplingX = new float[SAMPLINT_SIZE + 1];//包含终点
            mMapX = new float[SAMPLINT_SIZE + 1];//包含终点
            float gap = mWidth / (float) SAMPLINT_SIZE;//采样点之间的间距
            float x;
            for (int i = 0; i <= SAMPLINT_SIZE; i++) {
                x = i * gap;
                mSamplingX[i] = x;
                //将 x  映射 [-2 ~ 2] 的区间
                mMapX[i] = (x / (float) mWidth) * 4 - 2;
            }
        }

        //绘制背景
        canvas.drawColor(mBackGroundColor);
        //移到中心点  重画
        mFirstPath.rewind();
        mSecondPath.rewind();
        mCenterPath.rewind();

        //移到中心点
        mFirstPath.moveTo(0, mCenterHeight);
        mSecondPath.moveTo(0, mCenterHeight);
        mCenterPath.moveTo(0, mCenterHeight);


        //当前时间的偏移量，通过该偏移量是的每次绘制都向右偏移，让画面动起来
        float offset = time / 500F;

        float x;
        float[] xy;

        //波形函数的值，包括上一点，当前点和下一点
        float currV = 0;
        float lastV = 0;

        //计算第一个采样点的y值
        float nextV = (float) (mAmplitude * calculate(mMapX[0], offset));

        //波形函数的绝对值，用于赛选波峰和交错点
        float absLastV, absCurV, absNextV;
        //上一个赛选出的点  是波峰还是交错点
        boolean lastIsCrest = false;
        //赛选出的波峰和交叉点的数量，包括起点和终点
        int crestAndCrossCount = 0;

        for (int i = 0; i <= SAMPLINT_SIZE; i++) {
            x = mSamplingX[i];
            lastV = currV;
            currV = nextV;
            //计算下一个采样点的y值
            nextV = i < SAMPLINT_SIZE ? (float) (mAmplitude * calculate(mMapX[i + 1], offset)) : 0;

            //连接路劲
            mFirstPath.lineTo(x, mCenterHeight + currV);
            mSecondPath.lineTo(x, mCenterHeight - currV);
            mCenterPath.lineTo(x, mCenterHeight + currV / 5f);


            //记录极值点
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(currV);
            absNextV = Math.abs(nextV);

            if (i == 0 || i == SAMPLINT_SIZE || (lastIsCrest && absCurV < absNextV && absCurV < absLastV)) {
                xy = mCrestAndCrossPints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = 0;
                lastIsCrest = false;
            } else if (!lastIsCrest && absCurV > absLastV && absCurV > absNextV) {
                xy = mCrestAndCrossPints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = currV;
                lastIsCrest = true;
            }
        }


        mFirstPath.lineTo(mWidth, mCenterHeight);
        mSecondPath.lineTo(mWidth, mCenterHeight);
        mCenterPath.lineTo(mWidth, mCenterHeight);

        //记录layer
        int saveCount = canvas.saveLayer(0, 0, mWidth, mHeight, null, Canvas.ALL_SAVE_FLAG);

        //填充上下两条正弦函数
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(1);
        canvas.drawPath(mFirstPath, mPaint);
        canvas.drawPath(mSecondPath, mPaint);
//        canvas.drawPath(mCenterPath, mPaint);

        //绘制渐变色
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setXfermode(mXfermode);

        float startX, crestY, endX;
        for (int i = 2; i < crestAndCrossCount; i += 2) {
            //每个两个点可绘制一个矩形  ，矩形参数如下
            startX = mCrestAndCrossPints[i - 2][0];
            crestY = mCrestAndCrossPints[i - 1][1];
            endX = mCrestAndCrossPints[i][0];

            if (crestY > 0) {
                mPaint.setShader(new LinearGradient(0, mCenterHeight - crestY, 0, mCenterHeight + crestY, Color.GREEN, Color.BLUE, Shader.TileMode.CLAMP));
                rectF.set(startX, mCenterHeight - crestY, endX, mCenterHeight + crestY);
            } else {
                mPaint.setShader(new LinearGradient(0, mCenterHeight + crestY, 0, mCenterHeight - crestY, Color.BLUE, Color.GREEN, Shader.TileMode.CLAMP));
                rectF.set(startX, mCenterHeight + crestY, endX, mCenterHeight - crestY);
            }
            canvas.drawRect(rectF, mPaint);
        }

        //清理画笔
        mPaint.setShader(null);
        mPaint.setXfermode(null);

        //叠加layer,因为使用了SRC_IN的模式，所以只会保留波形渐变重合的地方
        canvas.restoreToCount(saveCount);
        //图层

        //描边
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);

        //绘制上玄线
        mPaint.setColor(Color.BLUE);
        canvas.drawPath(mFirstPath, mPaint);


        //绘制下玄线
        mPaint.setColor(Color.GREEN);
        canvas.drawPath(mSecondPath, mPaint);

        //绘制中间线
        mPaint.setColor(mCenterPathColor);
        canvas.drawPath(mCenterPath, mPaint);
    }


    /**
     * Math.pow(mapX,4) 表示 mapX的 4次方
     * (4/(4+x^4))^2.5
     */
    private double calculate(float mapX, float offset) {
        offset %= 2;
        double sinFunx = Math.sin(0.75 * Math.PI * mapX - offset * Math.PI);
        double recessionFun = Math.pow((4 / (4 + Math.pow(mapX, 4))), 2.5);
        return sinFunx * recessionFun;
    }
}
