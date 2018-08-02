package com.gsr.sample2.ui.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.gsr.sample2.R;
import com.gsr.sample2.clients.AlbumClient;
import com.gsr.sample2.clients.ImageClient;
import com.gsr.sample2.models.Album;
import com.gsr.sample2.models.Photo;
import com.gsr.sample2.ui.OnPhotoClickListener;
import com.gsr.sample2.ui.adapters.AlbumAdapter;
import com.takusemba.cropme.CropView;
import com.takusemba.cropme.OnCropListener;

import java.util.ArrayList;
import java.util.List;

public class CropActivity extends AppCompatActivity {

    private AlbumClient albumClient;
    private ImageClient imageClient;
    private AlbumAdapter adapter;

    private ImageView backButton;
    private ImageView cropButton;
    private RecyclerView recyclerView;
    private RelativeLayout parent;
    private CropView cropView;
    private ProgressBar progressBar;

    private static final int REQUEST_CODE_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        findViewsByIds();

        albumClient = new AlbumClient(this);
        imageClient = new ImageClient(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        OnPhotoClickListener listener = new OnPhotoClickListener() {
            @Override
            public void onPhotoClicked(Photo photo) {
                cropView.setUri(photo.uri);
            }
        };
        adapter = new AlbumAdapter(CropActivity.this, new ArrayList<Album>(), listener);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropView.crop(new OnCropListener() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        saveBitmapAndStartActivity(bitmap);
                    }

                    @Override
                    public void onFailure() {

                    }
                });
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(CropActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(parent, R.string.error_permission_is_off, Snackbar.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
            }
        } else {
            loadAlbums();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAlbums();
            } else {
                Snackbar.make(parent, R.string.error_permission_denied, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void findViewsByIds() {
        backButton = findViewById(R.id.cross);
        cropButton = findViewById(R.id.crop);
        recyclerView = findViewById(R.id.recycler_view);
        cropView = findViewById(R.id.crop_view);
        parent = findViewById(R.id.container);
        progressBar = findViewById(R.id.progress);
    }

    private void saveBitmapAndStartActivity(final Bitmap bitmap) {
        progressBar.setVisibility(View.VISIBLE);
        cropView.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                imageClient.saveBitmap(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        cropView.setEnabled(true);
                        startActivity(new Intent(CropActivity.this, ResultActivity.class));
                    }
                });
            }
        }).start();
    }

    private void loadAlbums() {
        adapter.clear();
        final List<Album> result = albumClient.getAlbums();
        for (final Album album : result) {
            new Thread(new Runnable() {
                public void run() {
                    albumClient.getResizedBitmap(CropActivity.this, album);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!album.photos.isEmpty()) {
                                if (adapter.getItemCount() == 0) {
                                    Photo photo = album.photos.get(0);
                                    cropView.setUri(photo.uri);
                                }
                                adapter.addItem(album);
                            }
                        }
                    });
                }
            }).start();
        }
    }

}