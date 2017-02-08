package lab.prada.collage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultipleImagePickerActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_READ_EXTERNAL_PERMISSION = 1;
    protected static final String EXTRA_IMAGE_PICKER_IMAGE_PATH = "image_path";
    private ImageAdapter mAdapter;
    private ArrayList<String> mSelectedUrl = new ArrayList<>();
    private static final int LOADER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiple_image_picker);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        RecyclerView listView = (RecyclerView) findViewById(R.id.photo_list);

        mAdapter = new ImageAdapter(this, new IPhotoChecker() {
            @Override
            public boolean isSelected(String path) {
                return mSelectedUrl.contains(path);
            }
        }, new OnRecyclerItemClickedListener<String>() {
            @Override
            public void onItemClicked(String data) {
                selected(data);
            }
        });
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        listView.setLayoutManager(glm);
        listView.setAdapter(mAdapter);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //TODO: add a dialog to go to settings
                Toast.makeText(getBaseContext(), R.string.user_denied_settings, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_PERMISSION);
            }
        } else {
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_PERMISSION:
                if (grantResults != null && grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, getString(R.string.user_denied), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                break;
        }
    }

    private boolean selected(String path) {
        if (mSelectedUrl.size() > Constants.SUPPORTED_FRAME_NUMBER) {
            Toast.makeText(MultipleImagePickerActivity.this, R.string.over_num_of_images, Toast.LENGTH_LONG).show();
            return false;
        }
        if (mSelectedUrl.contains(path)) {
            mSelectedUrl.remove(path);
        } else {
            mSelectedUrl.add(path);
        }
        return true;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return new CursorLoader(this, images, projection, "", null, "");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cur) {
        switch (loader.getId()) {
            case LOADER_ID:
                ArrayList<String> imagePaths = new ArrayList<>(cur.getCount());
                int rawCol = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                if (cur.moveToFirst()) {
                    do {
                        imagePaths.add(cur.getString(rawCol));
                    } while (cur.moveToNext());
                }
                mAdapter.updateImages(imagePaths);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.updateImages(null);
    }

    public interface IPhotoChecker {
        boolean isSelected(String path);
    }

    public interface OnRecyclerItemClickedListener<T> {
        void onItemClicked(T data);
    }

    static class ImageAdapter extends RecyclerView.Adapter {
        private List<String> datasource;
        private final Context mContext;
        Cursor mCursor;
        private final IPhotoChecker mChecker;
        private final OnRecyclerItemClickedListener<String> mListener;

        ImageAdapter(Context ctx, IPhotoChecker checker,
                     OnRecyclerItemClickedListener<String> listener) {
            mContext = ctx;
            mChecker = checker;
            mListener = listener;
        }

        public void updateImages(List<String> images) {
            datasource = images;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.item_grid_image, parent, false);
            return new RecyclerView.ViewHolder(v) {
            };
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            final String path = datasource.get(position);
            int visible = mChecker.isSelected(path) ? View.VISIBLE : View.INVISIBLE;
            holder.itemView.findViewById(R.id.imageView1).setVisibility(visible);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClicked(path);
                    }
                    int visible = mChecker.isSelected(path) ? View.VISIBLE : View.INVISIBLE;
                    holder.itemView.findViewById(R.id.imageView1).setVisibility(visible);
                }
            });
            Glide.with(mContext)
                    .load(new File(path))
                    .placeholder(R.drawable.ic_empty)
                    .error(R.drawable.ic_error)
                    .into((ImageView) holder.itemView.findViewById(R.id.image));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return datasource == null ? 0 : datasource.size();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.multiple_image_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_choose:
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(EXTRA_IMAGE_PICKER_IMAGE_PATH, mSelectedUrl);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
