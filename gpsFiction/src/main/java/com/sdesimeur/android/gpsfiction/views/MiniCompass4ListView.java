package com.sdesimeur.android.gpsfiction.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.sdesimeur.android.gpsfiction.activities.R;
import com.sdesimeur.android.gpsfiction.classes.GpsFictionControler;
import com.sdesimeur.android.gpsfiction.classes.PlayerBearingListener;
import com.sdesimeur.android.gpsfiction.classes.Zone;


public class MiniCompass4ListView extends View implements PlayerBearingListener {
    private Paint arrowPaint = new Paint();
    private Path arrowPathCompass = null;
    private Path arrowPathPlayerInZone = null;
    private Path centerPath = null;
    private boolean mAnimate;
    private long mNextTime;

    public MiniCompass4ListView(Context context) {
        super(context);
    }

    /*protected void onMesure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(this.getSize(), this.getSize());
    }*/
    public MiniCompass4ListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiniCompass4ListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void setArrowPaths() {
        if (this.centerPath == null) {
            int c = this.getSize() / 2;
            int h = this.getSize() * 4 / 10;
            int m = 2 * (c - h) / 3;
            this.arrowPathPlayerInZone = new Path();
            this.arrowPathPlayerInZone.addCircle(0, 0, h, Path.Direction.CW);
            this.arrowPathPlayerInZone.close();
            this.arrowPathCompass = new Path();
            this.arrowPathCompass.moveTo(0, -h - m);
            this.arrowPathCompass.lineTo(-h * 2 / 5, h - m);
            this.arrowPathCompass.lineTo(0, h * 3 / 5 - m);
            this.arrowPathCompass.lineTo(h * 2 / 5, h - m);
            this.arrowPathCompass.close();
            h = h / 7;
            this.centerPath = new Path();
            this.centerPath.addCircle(0, 0, h, Path.Direction.CW);
            this.centerPath.close();
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = this.getSize();
        this.setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int tx = this.getSize() / 2;
        canvas.translate(tx, tx);
        this.setArrowPaths();
        canvas.drawColor(this.getResources().getColor(R.color.minicompassbackground));
        if (((Zone)getTag(R.id.attachedZoneId)).isPlayerInThisZone()) {
            this.arrowPaint.setAntiAlias(true);
            this.arrowPaint.setColor(this.getResources().getColor(R.color.minicompassarrow));
            this.arrowPaint.setStyle(Paint.Style.STROKE);
            this.arrowPaint.setStrokeWidth(this.getResources().getDimension(R.dimen.compassLineWidth));
            canvas.drawPath(this.arrowPathPlayerInZone, this.arrowPaint);
            this.arrowPaint.setColor(this.getResources().getColor(R.color.minicompasscenter));
            this.arrowPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(this.centerPath, this.arrowPaint);
        } else {
            this.arrowPaint.setAntiAlias(true);
            this.arrowPaint.setColor(this.getResources().getColor(R.color.minicompassarrow));
            this.arrowPaint.setStyle(Paint.Style.FILL);
            canvas.rotate(((Zone)getTag(R.id.attachedZoneId)).getAnglePlayer2Zone());
            canvas.drawPath(this.arrowPathCompass, this.arrowPaint);
            this.arrowPaint.setColor(this.getResources().getColor(R.color.minicompasscenter));
//            this.arrowPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(this.centerPath, this.arrowPaint);
        }
    }

    @Override
    public void onBearingPlayerChanged(float angle) {
        this.invalidate();
    }

    private int getSize() {
        int size = getResources().getDimensionPixelSize(R.dimen.miniCompassSize);
        return size;
    }
    @Override
    protected void onAttachedToWindow() {
        mAnimate = true;
        int id = R.id.gpsFictionControlerId;
        GpsFictionControler gfc = (GpsFictionControler) getTag(id);
        gfc.addPlayerBearingListener(GpsFictionControler.REGISTER.VIEW, this);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        mAnimate = false;
        int id = R.id.gpsFictionControlerId;
        GpsFictionControler gfc = (GpsFictionControler) getTag(id);
        gfc.removePlayerBearingListener(GpsFictionControler.REGISTER.VIEW, this);
        super.onDetachedFromWindow();
    }

}
