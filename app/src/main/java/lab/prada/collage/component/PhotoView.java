package lab.prada.collage.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.thuytrinh.multitouchlistener.MultiTouchListener;

import afzkl.development.mColorPicker.views.GeneralListener;
import lab.prada.collage.util.CollageUtils;

public class PhotoView extends ImageView implements BaseComponent {
    @Override
    public PhotoView duplicate() {

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        PhotoView newPhotoView = new PhotoView(getContext());
        newPhotoView.setImageBitmap(bitmap);
        ViewCompat.setScaleX(newPhotoView, ViewCompat.getScaleX(this));
        ViewCompat.setScaleY(newPhotoView, ViewCompat.getScaleY(this));
        ViewCompat.setRotation(newPhotoView, ViewCompat.getRotation(this));

        return newPhotoView;
    }


    public interface OnPhotoListener {
        void onModifyPhoto(PhotoView view);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        android.util.Log.d("DEBUG", "onAttachedToWindow : " + toString());
    }

    private GeneralListener listener;


    public PhotoView(Context context) {
        super(context);
        setOnTouchListener(new MultiTouchListener());
    }

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(new MultiTouchListener());
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(new MultiTouchListener());
    }

    public void setImage(Bitmap bitmap) {
        setImageBitmap(bitmap);
    }

    public void setListener(GeneralListener listener) {
        this.listener = listener;
        this.setOnTouchListener(new MultiTouchListener(
                new GestureListener()));
    }

    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (listener != null) {
                listener.onPushToBottom(PhotoView.this);
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (listener != null) {
                listener.onBringToFront(PhotoView.this);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (listener != null) {
                listener.onDuplicate(PhotoView.this);
            }
            return true;
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void setXY(int x, int y) {
        ViewCompat.setX(this, x);
        ViewCompat.setY(this, y);
    }

    public float getCenterX() {
        return ViewCompat.getX(this) + getWidth() / 2;
    }

    @Override
    public float getCenterY() {
        return ViewCompat.getY(this) + getHeight() / 2;
    }
}
