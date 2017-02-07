package lab.prada.collage.component;

import android.view.View;

import afzkl.development.mColorPicker.views.GeneralListener;
import lab.prada.collage.MainActivity;

public interface BaseComponent {
	public View getView();
	public void setXY(int x, int y);
	public float getCenterX();
	public float getCenterY();
	public BaseComponent duplicate();

	void setListener(GeneralListener listener);
}
