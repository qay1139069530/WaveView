package com.qbase.waveview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Create by qay on 2018/11/28
 */
public class WaveView extends View {

    private int waveView_BoatBitmap;//波上图片
    private boolean waveView_rise;//是否上升
    private int duration;//动画快慢
    private int originY;//水位的初始位置
    private int waveHeight;//波峰的高度
    private int waveLength;//波长
    private Paint paint;
    private Path path;

    private int dx;
    private int dy;
    private int width, height;

    private Region region;

    private Bitmap mBitmap;

    private ValueAnimator valueAnimator;

    public WaveView(Context context) {
        super(context);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveView);
        if (typedArray != null) {
            waveView_BoatBitmap = typedArray.getResourceId(R.styleable.WaveView_boatBitmap, 0);
            waveView_rise = typedArray.getBoolean(R.styleable.WaveView_rise, false);
            duration = (int) typedArray.getDimension(R.styleable.WaveView_duration, 2000);
            originY = (int) typedArray.getDimension(R.styleable.WaveView_originY, 500);
            waveHeight = (int) typedArray.getDimension(R.styleable.WaveView_waveHeight, 200);
            waveLength = (int) typedArray.getDimension(R.styleable.WaveView_waveLength, 400);
            typedArray.recycle();
        }

        if (waveView_BoatBitmap > 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;//缩放
            mBitmap = BitmapFactory.decodeResource(getResources(), waveView_BoatBitmap, options);
            mBitmap = getCircleBitmap(mBitmap);
        } else {
            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP){
                Drawable vectorDrawable = context.getDrawable(R.mipmap.ic_launcher_round);
                mBitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                        vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mBitmap);
                vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                vectorDrawable.draw(canvas);
            }else {
                mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
            }
        }

        paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);//填充式曲线

        path = new Path();

    }

    /**
     * 获取圆形图  使用Xfermode加载成圆形图片
     */
    private Bitmap getCircleBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try {
            Bitmap circleBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(circleBitmap);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
            float roundPx = 0.0f;
            if (bitmap.getWidth() > bitmap.getHeight()) {
                roundPx = bitmap.getHeight() / 2.0f;
            } else {
                roundPx = bitmap.getWidth() / 2.0f;
            }

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            final Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            canvas.drawBitmap(bitmap, src, rect, paint);

            return circleBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (originY == 0) {
            originY = height;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制水波纹
        //定义曲线
        setPathData();
        canvas.drawPath(path, paint);

        if (mBitmap != null) {

            Rect bounds = region.getBounds();
            if (bounds.top > 0 || bounds.bottom > 0) {
                //绘制头像
                //计算位置
                if (bounds.top < originY) {
                    //从波峰滑落到基准线
                    canvas.drawBitmap(mBitmap, bounds.right - mBitmap.getWidth() / 2, bounds.top - mBitmap.getHeight(), paint);
                } else {
                    canvas.drawBitmap(mBitmap, bounds.left - mBitmap.getWidth() / 2, bounds.bottom - mBitmap.getHeight(), paint);
                }
            } else {
                //画到正中间
                float x = width / 2 - mBitmap.getWidth() / 2;
                canvas.drawBitmap(mBitmap, (int) x, originY - mBitmap.getHeight(), paint);
            }
        }
    }

    /**
     * 设置曲线
     */
    private void setPathData() {
        path.reset();
        int halfWaveLength = waveLength / 2;
        int widthL = width + waveLength;
        path.moveTo(-waveLength + dx, originY);
        for (int i = -waveLength; i < widthL; i += waveLength) {
            //相对坐标
            path.rQuadTo(halfWaveLength / 2, -waveHeight, halfWaveLength, 0);
            path.rQuadTo(halfWaveLength / 2, waveHeight, halfWaveLength, 0);
        }

        //相交区域
        region = new Region();
        //切割区域
        Region clip = new Region((int) (width / 2 - 0.1), 0, width / 2, height * 2);
        region.setPath(path, clip);


        //曲线封闭
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close();
    }


    /**
     * 开始动画
     */
    public void startAnimation() {
        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(duration);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = (float) animation.getAnimatedValue();
                dx = (int) (waveLength * fraction);
                dy += 2;
                postInvalidate();
            }
        });
        valueAnimator.start();
    }
}
