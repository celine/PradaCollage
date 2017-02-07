package lab.prada.collage.component;

import com.thuytrinh.multitouchlistener.MultiTouchListener;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;

import afzkl.development.mColorPicker.views.GeneralListener;

public class StickerView extends ImageView implements BaseComponent{
	StickerView(Context context) {
		super(context);
		setOnTouchListener(new MultiTouchListener());
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

	@Override
	public BaseComponent duplicate() {
		return null;
	}

	@Override
	public void setListener(GeneralListener listener) {

	}
}
