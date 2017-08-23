package com.huangmin66.lockpatternview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：Administrator on 2017/8/23 09:25
 * 描述：九宫格图案解锁View
 */

public class LockPatternView extends View {
    //选中点的集合
    private static final int POINT_SIZE = 5;
    //矩阵 缩放line
    private Matrix matrix = new Matrix();

    private Point[][] points = new Point[3][3];
    private boolean isInit, isSelect, isFinish, movingPoint;
    private float width, height;
    private float offsetsX, offsetsY;
    private float movingX,movingY;

    private Bitmap pointNormal, pointPressed, pointError, linePressed, lineError;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //按下的点集合
    private List<Point> pointList = new ArrayList<>();

    private OnPatterChangeListener onPatterChangeListener;

    public LockPatternView(Context context) {
        super(context);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(!isInit){
            initPoints();
        }
        //画点
        points2Canvas(canvas);
        //画线
        if (pointList.size() > 0){
            Point a = pointList.get(0);
            //绘制九宫格里坐标点
            for (int i = 0; i < pointList.size(); i++){
                Point b = pointList.get(i);
                line2Canvas(canvas, a, b);
                a = b;
            }
            //绘制鼠标坐标点
            if (movingPoint){
                line2Canvas(canvas, a, new Point(movingX, movingY));
            }
        }
    }

    /**
     * 初始化点
     */
    private void initPoints() {
        //获取布局宽高
        width = getWidth();
        height = getHeight();

        //判断横屏 还是 竖屏 的偏移量
        if (width > height){
            offsetsX = (width - height) / 2;
            width = height;
        } else {
            //竖屏
            offsetsY = (height - width) / 2;
            height = width;
        }

        //图片资源
        pointNormal = BitmapFactory.decodeResource(getResources(), R.drawable.btn_circle_normal);
        pointPressed = BitmapFactory.decodeResource(getResources(), R.drawable.btn_circle_pressed);
        pointError = BitmapFactory.decodeResource(getResources(), R.drawable.btn_circle_selected);
        linePressed = BitmapFactory.decodeResource(getResources(), R.drawable.line_pressed);
        lineError = BitmapFactory.decodeResource(getResources(), R.drawable.line_error);

        //点的坐标
        points[0][0] = new Point(offsetsX + width / 4, offsetsY + width / 4);
        points[0][1] = new Point(offsetsX + width / 2, offsetsY + width / 4);
        points[0][2] = new Point(offsetsX + width - width / 4, offsetsY + width / 4);

        points[1][0] = new Point(offsetsX + width / 4, offsetsY + width / 2);
        points[1][1] = new Point(offsetsX + width / 2, offsetsY + width / 2);
        points[1][2] = new Point(offsetsX + width - width / 4, offsetsY + width / 2);

        points[2][0] = new Point(offsetsX + width / 4, offsetsY + width - width / 4);
        points[2][1] = new Point(offsetsX + width / 2, offsetsY + width - width / 4);
        points[2][2] = new Point(offsetsX + width - width / 4, offsetsY + width - width / 4);

        //设置密码
        int index= 1;
        for (Point[] points : this.points){
            for (Point point : points){
                point.index = index++;
            }
        }

        // 初始化完成
        isInit = true;
    }

    /**
     * 将点绘制到画布上
     * @param canvas 画布
     */
    private void points2Canvas(Canvas canvas) {
        for (int i = 0; i < points.length; i++){
            for (int j = 0; j < points[i].length; j++){
                Point point = points[i][j];
                if (point.state == Point.STATE_NORMAL){
                    canvas.drawBitmap(pointNormal, point.x - pointNormal.getWidth() / 2
                            , point.y - pointNormal.getHeight() / 2, paint);
                } else if (point.state == Point.STATE_PRESSED){
                    canvas.drawBitmap(pointPressed, point.x - pointPressed.getWidth() / 2
                            , point.y - pointPressed.getHeight() / 2, paint);
                } else if (point.state == Point.STATE_ERROR){
                    canvas.drawBitmap(pointError, point.x - pointError.getWidth() / 2
                            , point.y - pointError.getHeight() / 2, paint);
                }
            }
        }
    }

    /**
     * 画线
     * @param canvas
     * @param a 第一个点
     * @param b 第二个点
     */
    private void line2Canvas(Canvas canvas, Point a, Point b){
        //线的长度
        float lineLength = (float) Point.distance(a, b);
        float degrees = getDegrees(a, b);
        canvas.rotate(degrees, a.x, a.y);
        if (a.state == Point.STATE_PRESSED) {
            matrix.setScale(lineLength / linePressed.getWidth(), 1);
            matrix.postTranslate(a.x - linePressed.getWidth() / 2, a.y - linePressed.getHeight() / 2);
            canvas.drawBitmap(linePressed, matrix, paint);
        } else {
            matrix.setScale(lineLength / lineError.getWidth(), 1);
            matrix.postTranslate(a.x - lineError.getWidth() / 2, a.y - lineError.getHeight() / 2);
            canvas.drawBitmap(lineError, matrix, paint);
        }

        canvas.rotate(-degrees, a.x, a.y);
    }

    private float getDegrees(Point a, Point b) {
        return (float) Math.toDegrees(Math.atan2(b.y - a.y, b.x
                - a.x));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        movingX = event.getX();
        movingY = event.getY();

        isFinish = false;
        movingPoint = false;

        Point point = null;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                resetPoint();
                //判断按下的位置是不是九宫格的点
                point = checkSelectPoint();
                if (point != null){
                    isSelect = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isSelect){
                    point = checkSelectPoint();
                    if (point == null){
                        movingPoint = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                isFinish = true;
                isSelect = false;
                break;
        }

        //选中重复的检查
        if (!isFinish && isSelect && point != null){
            if (crossPoint(point)){
                movingPoint = true;

            } else {
                point.state = Point.STATE_PRESSED;
                pointList.add(point);
            }
        }

        if (isFinish){
            if (pointList.size() == 1){
                //绘制不成立
                resetPoint();
            } else if (pointList.size() < POINT_SIZE && pointList.size() > 0){
                //绘制错误
                errorPoint();
                if (onPatterChangeListener != null) {
                    onPatterChangeListener.onPatterChange(null);
                }
            } else{
                if (onPatterChangeListener != null) {
                    String passwordStr = "";
                    for (Point p : pointList) {
                        passwordStr += p.index;
                    }

                    if (!TextUtils.isEmpty(passwordStr)) {
                        onPatterChangeListener.onPatterChange(passwordStr);
                    }
                }
            }
        }

        //刷新view
        postInvalidate();
        return true;
    }

    /**
     * 交叉点
     * @param point
     * @return 是否交叉
     */
    private boolean crossPoint(Point point){
        if(pointList.contains(point)){
            return true;
        } else {
            return false;
        }
    }

    /**
     * 绘制不成立
     */
    public void resetPoint(){
        for (Point point: pointList){
            point.state = Point.STATE_NORMAL;
        }
        pointList.clear();
    }

    /**
     * 绘制错误
     */
    public void errorPoint(){
        for (Point point: pointList){
            point.state = Point.STATE_ERROR;
        }
    }

    private Point checkSelectPoint(){
        for (int i = 0; i < points.length; i++){
            for (int j = 0; j < points[i].length; j++){
                Point point = points[i][j];
                if (Point.with(point.x, point.y, pointNormal.getWidth() / 2, movingX, movingY)){
                    return point;
                }
            }
        }
        return null;
    }

    /**
     * 自定义九宫格 点
     */
    public static class Point{
        //正常
        public static int STATE_NORMAL = 0;
        //选中
        public static int STATE_PRESSED = 1;
        //错误
        public static int STATE_ERROR = 2;

        public float x,y;
        public int index = 0,state = 0;
        public Point() {}

        public Point(float x, float y){
            this.x = x;
            this.y = y;
        }

        public static double distance(Point a, Point b){
            // x轴差的平方 加上 y轴差的平方， 对和开方
            return Math.sqrt(Math.abs(a.x - b.x) * Math.abs(a.x - b.x) + Math.abs(a.y - b.y) * Math.abs(a.y - b.y));
        }

        /**
         * 是否重合
         * @param pointX 参考的点x
         * @param pointY 参考的点y
         * @param r      圆的半径
         * @param movingX 移动点x
         * @param movingY 移动点y
         * @return
         */
        public static boolean with(float pointX, float pointY, float r, float movingX, float movingY){
            // 开方
            return Math.sqrt((pointX - movingX) * (pointX - movingX) + (pointY - movingY) * (pointY - movingY)) < r;
        }
    }

    /**
     * 图案监听器
     */
    public static interface OnPatterChangeListener{
        /**
         * 图案改变
         * @param passwordStr 密码
         */
        void onPatterChange(String passwordStr);
    }

    public void setPatterChangeListener(OnPatterChangeListener onPatterChangeListener){
        if (onPatterChangeListener != null)
            this.onPatterChangeListener = onPatterChangeListener;
    }
}
