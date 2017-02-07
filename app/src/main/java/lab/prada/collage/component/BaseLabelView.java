package lab.prada.collage.component;


import com.thuytrinh.multitouchlistener.MultiTouchListener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import afzkl.development.mColorPicker.views.GeneralListener;

public abstract class BaseLabelView extends View implements BaseComponent {

    protected GeneralListener listener;

    public final static int DEFAULT_FONT_SIZE = 100;
    protected String mText;
    protected int mAscent;
    protected boolean hasStroke = false;

    public BaseLabelView(Context context) {
        super(context);
    }

    public interface OnLabelListener {
         void onModifyLabel(BaseLabelView view, String text, int color,
                                  boolean hasStroke);

    }

    public abstract void setText(String text, int color, boolean hasStroke);

    public abstract int getTextColor();

    public void setListener(GeneralListener listener) {
        this.listener = listener;
        this.setOnTouchListener(new MultiTouchListener(new GestureListener()));
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
                listener.onPushToBottom(BaseLabelView.this);
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (listener != null) {
                listener.onBringToFront(BaseLabelView.this);
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (listener != null)
                listener.onDuplicate(BaseLabelView.this);
            return true;
        }
    }
}
