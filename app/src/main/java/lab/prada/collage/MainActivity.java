package lab.prada.collage;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import afzkl.development.mColorPicker.views.GeneralListener;
import bolts.Continuation;
import bolts.Task;
import lab.prada.collage.component.BaseComponent;
import lab.prada.collage.component.BaseLabelView;
import lab.prada.collage.component.ComponentFactory;
import lab.prada.collage.component.LabelViewImpl;
import lab.prada.collage.component.PhotoView;
import lab.prada.collage.util.CameraImageHelper;
import lab.prada.collage.util.CollageUtils;
import lab.prada.collage.util.GlassesDetector;
import lab.prada.collage.util.StoreImageHelper;
import lab.prada.collage.util.StoreImageHelper.onSaveListener;

public class MainActivity extends BaseActivity implements GeneralListener,
        View.OnClickListener {

    private static final int SELECT_PHOTO = 0;
    private static final int ADD_NEW_TEXT = 1;
    private static final int MODIFY_TEXT = 2;
    private static final int MODIFY_PHOTO = 3;

    private ProgressDialog progressDialog;
    private ViewGroup allViews;
    private ViewGroup textPhotoPanel;
    FrameLayout.LayoutParams defaultParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        findViewById(R.id.btnAddPic).setOnClickListener(this);
        findViewById(R.id.btnAddText).setOnClickListener(this);
        allViews = (ViewGroup) findViewById(R.id.frame);
        textPhotoPanel = (ViewGroup) findViewById(R.id.frame_images_text);
    }

    private void showProgressDialog(boolean enable) {
        if (enable) {
            progressDialog = ProgressDialog.show(this, getResources()
                    .getString(R.string.progress_title), getResources()
                    .getString(R.string.progress_message), true);
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        switch (requestCode) {
            case SELECT_PHOTO:
                ArrayList<String> paths = intent.getStringArrayListExtra(MultipleImagePickerActivity.EXTRA_IMAGE_PICKER_IMAGE_PATH);
                if (paths.isEmpty()) {
                    return;
                }
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                List<CollageUtils.ScrapTransform> trans = CollageUtils.generateScrapsTransform(width, height, paths.size());
                clearImages();
                int i = 0;
                final int baseWidth = textPhotoPanel.getWidth() / 2;
                for (CollageUtils.ScrapTransform t : trans) {
                    final String path = paths.get(i++);
                    final CollageUtils.ScrapTransform transform = t;
                    Task.callInBackground(new Callable<Bitmap>() {
                        @Override
                        public Bitmap call() throws Exception {
                            return CameraImageHelper.checkAndRotatePhoto(path);
                        }
                    }).onSuccess(new Continuation<Bitmap, PhotoView>() {
                        @Override
                        public PhotoView then(Task<Bitmap> task) throws Exception {
                            PhotoView iv = ComponentFactory.create(ComponentFactory.COMPONENT_IMAGE,
                                    MainActivity.this, textPhotoPanel);
                            iv.setListener(MainActivity.this);
                            Bitmap bitmap = task.getResult();
                            // angle
                            ViewCompat.setRotation(iv, transform.rotation);
                            float scale = 1f * baseWidth / bitmap.getWidth();
                            ViewCompat.setScaleX(iv, scale);
                            ViewCompat.setScaleY(iv, scale);
                            iv.setImageBitmap(bitmap);
                            iv.setXY(transform.centerX - iv.getWidth() / 2, transform.centerY - iv.getHeight() / 2);
                            textPhotoPanel.addView(iv);
                            return iv;
                        }
                    }, Task.UI_THREAD_EXECUTOR);
                }
                textPhotoPanel.invalidate();
                break;
            case MODIFY_PHOTO:
                if (currentSelectedImage == null) {
                    return;
                }
                try {
                    Bitmap bitmap = CameraImageHelper.checkAndRotatePhoto(getRealPathFromURI(intent.getData()));
                    if (bitmap != null) {
                        currentSelectedImage.setImage(bitmap);
                    }
                } catch (IOException e) {
                }
                currentSelectedImage = null;
                break;
            case ADD_NEW_TEXT:
                String text = intent.getStringExtra(TextEditorActivity.EXTRA_EDITOR_TEXT);
                if (text == null || text.trim() == null)
                    return;
                int color = intent.getIntExtra(TextEditorActivity.EXTRA_EDITOR_COLOR, Color.BLACK);
                boolean hasStroke = intent.getBooleanExtra(TextEditorActivity.EXTRA_EDITOR_BORDER, false);
                addTextView(text, color, hasStroke);
                break;
            case MODIFY_TEXT:
                if (currentSelectedText == null) {
                    return;
                }
                String txt = intent.getStringExtra(TextEditorActivity.EXTRA_EDITOR_TEXT);
                if (txt == null) {
                    return;
                }
                currentSelectedText.setText(txt,
                        intent.getIntExtra(TextEditorActivity.EXTRA_EDITOR_COLOR, Color.BLACK),
                        intent.getBooleanExtra(TextEditorActivity.EXTRA_EDITOR_BORDER, false));
                currentSelectedText = null;
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    private void clearImages() {
        textPhotoPanel.removeAllViews();
    }

    @SuppressLint("NewApi")
    private void addTextView(String text, int color, boolean hasStroke) {
        BaseLabelView tv = ComponentFactory.create(ComponentFactory.COMPONENT_LABEL, this, textPhotoPanel);
        tv.setListener(this);
        tv.setXY(textPhotoPanel.getWidth() / 2, textPhotoPanel.getHeight() / 2);
        tv.setText(text, color, hasStroke);
        textPhotoPanel.addView(tv.getView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                showProgressDialog(true);
                StoreImageHelper.save(getContentResolver(), allViews,
                        new onSaveListener() {
                            @Override
                            public void onSaveSuccess() {
                                showProgressDialog(false);
                            }

                            @Override
                            public void onSaveFail() {
                                showProgressDialog(false);
                            }
                        });
                return true;
            case R.id.action_magic:
                /*
                 * showProgressDialog(true); new Thread(new Runnable() {
				 *
				 * @Override public void run() {
				 */
                ((ViewGroup) findViewById(R.id.frame_sticks)).removeAllViews();
                GlassesDetector detector = new GlassesDetector(MainActivity.this,
                        (ViewGroup) findViewById(R.id.frame_sticks));
                detector.detectFaces((ViewGroup) textPhotoPanel);
                /*
                 * showProgressDialog(false); } }).start();
				 */
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BaseLabelView currentSelectedText = null;
    private PhotoView currentSelectedImage = null;

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_exit_title)
                .setMessage(R.string.dialog_exit_message)
                .setPositiveButton(R.string.dialog_exit_ok,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int arg1) {
                                dialog.dismiss();
                                finish();
                            }

                        })
                .setNegativeButton(R.string.dialog_exit_cancel,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }


    @Override
    public void onPushToBottom(final View view) {
        int childCount = textPhotoPanel.getChildCount();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            List<Animator> animatorList = new ArrayList<>();
            for (int i = 0; i < childCount; i++) {
                View otherView = textPhotoPanel.getChildAt(i);
                if (otherView != view) {
                    Log.d("WTest", "otherView " + otherView);
                    addTranslateAnim(animatorList, otherView, view);
                }
            }

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    pushToBottom(view);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.playTogether(animatorList);
            animatorSet.setDuration(500);
            animatorSet.setInterpolator(new AnticipateOvershootInterpolator());
            animatorSet.start();

        }
    }

    //TODO:add compatibility check
    private void addTranslateAnim(List<Animator> animators, View otherView, View current) {
        float extra = 5;
        float minOffset = 30;
        float xDis = (current.getWidth() * ViewCompat.getScaleX(current) + otherView.getWidth() * otherView.getScaleX()) / 2 + extra;
        float yDis = (current.getHeight() * ViewCompat.getScaleY(current) + otherView.getHeight() * otherView.getScaleY()) / 2 + extra;

        float currentX = ((BaseComponent) current).getCenterX();
        float currentY = ((BaseComponent) current).getCenterY();
        float otherX = ((BaseComponent) otherView).getCenterX();
        float otherY = ((BaseComponent) otherView).getCenterY();

        if (Math.abs(currentX - otherX) >= xDis || Math.abs(currentY - otherY) >= yDis) {
            return;
        }
        int xDir = otherX > currentX ? 1 : -1;
        int yDir = otherY > currentY ? 1 : -1;
        float newX = currentX + xDir * xDis;
        float newY = currentY + yDir * yDis;
        if (Math.abs(newX - currentX) > Math.abs(newY - currentY)) {
            newX = otherX + xDir * minOffset;
        } else {
            newY = otherY + yDir * minOffset;
        }
        newX = newX - otherView.getWidth() / 2;
        newY = newY - otherView.getHeight() / 2;
        animators.add(ObjectAnimator.ofFloat(otherView, "translationX", newX, ViewCompat.getX(otherView)));
        animators.add(ObjectAnimator.ofFloat(otherView, "translationY", newY, ViewCompat.getY(otherView)));

    }

    private void pushToBottom(View view) {
        int childCount = textPhotoPanel.getChildCount();

        if (childCount > 0) {
            if (textPhotoPanel.getChildAt(0) != view) {
                textPhotoPanel.removeView(view);
                textPhotoPanel.addView(view, 0);
            }
        }
    }

    @Override
    public void onBringToFront(final View view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            AnimatorSet animationSet = new AnimatorSet();
            float currentScaleX = view.getScaleX();
            float currentScaleY = view.getScaleY();
            float toScale = currentScaleX * 1.2f;
            animationSet.playTogether(ObjectAnimator.ofFloat(view, "scaleX", toScale, currentScaleX), ObjectAnimator.ofFloat(view, "scaleY", toScale, currentScaleY));
            animationSet.setDuration(500);
            animationSet.setInterpolator(new OvershootInterpolator());
            animationSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    bringToFront(view);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animationSet.start();
        }

    }

    @Override
    public void onDuplicate(View view) {
        if (!(view instanceof BaseComponent)) {
            return;
        }
        BaseComponent baseComponent = ((BaseComponent) view).duplicate();
        int x = CollageUtils.getNewPos(textPhotoPanel.getWidth() - view.getWidth());
        int y = CollageUtils.getNewPos(textPhotoPanel.getHeight() - view.getHeight());
        baseComponent.setXY(x + view.getWidth() / 2, y + view.getHeight() / 2);
        baseComponent.setListener(this);
        textPhotoPanel.addView((View) baseComponent, defaultParams);
    }

    private void bringToFront(View view) {
        int childCount = textPhotoPanel.getChildCount();
        if (childCount > 0) {
            if (textPhotoPanel.getChildAt(childCount - 1) != view) {
                textPhotoPanel.removeView(view);
                textPhotoPanel.addView(view);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAddPic:
                startActivityForResult(new Intent(this, MultipleImagePickerActivity.class), SELECT_PHOTO);
                break;
            case R.id.btnAddText:
                startActivityForResult(new Intent(this, TextEditorActivity.class), ADD_NEW_TEXT);
                break;
        }
    }
}
