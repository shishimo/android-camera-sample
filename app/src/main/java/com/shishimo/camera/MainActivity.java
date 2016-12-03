package com.shishimo.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION = 0;

    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // カメラの使用許諾
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    /**
     * カメラを起動する。
     */
    private void openCamera() {

        // カメラが撮影した画像を貼り付けるView
        TextureView textureView = (TextureView) findViewById(R.id.texture_view);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("SHISHIMO", "onSurfaceTextureAvailable");
                mCamera.open();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d("SHISHIMO", "onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d("SHISHIMO", "onSurfaceTextureDestroyed");
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //カメラ起動時は定期的に呼ばれる。
                //Log.d("SHISHIMO", "onSurfaceTextureUpdated");
            }
        });

        mCamera = new Camera(this, textureView);
    }
}
