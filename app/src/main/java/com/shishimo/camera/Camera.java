package com.shishimo.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Collections;

/**
 * Created by shishimo on 2016/11/29.
 */

public class Camera {

    // 本クラスを扱うアクティビティ
    Activity mActivity;

    // 本カメラクラスで撮影した画像を貼り付けるView
    TextureView mTextureView;

    // カメラオブジェクト
    CameraDevice mCamera;

    // カメラサイズ：640x480
    Size mCameraSize;

    // 撮影した画像データをビルドするビルダー
    private CaptureRequest.Builder mCaptureBuilder;

    // 撮影時のセッション
    private CameraCaptureSession mCaptureSession;

    public Camera(@NonNull Activity activity, @NonNull TextureView textureView) {

        mActivity = activity;
        mTextureView = textureView;

        // For Test
        Log.d("SHISHIMO", "Image Format JPEG:" + ImageFormat.JPEG);
    }

    /**
     * カメラを起動する。
     */
    public void open() {
        try {

            /**
             * カメラマネージャーを取得
             */
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);

            /**
             * 端末のカメラのデバイスIDを取得
             */
            for (String cameraId : manager.getCameraIdList()) {
                Log.d("SHISHIMO", "Camera ID:" + cameraId);

                // カメラの情報を取得
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // カメラの方向を確認する。
                // 画面に対して裏側のカメラか判定する。
                // 画面側のカメラは今回はスルー。
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {

                    /**
                     * カメラのデータストリームの設定を取得
                     */
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    for (int outputFormat : map.getOutputFormats()) {
                        Log.d("SHISHIMO", "Camera Supported Output Format:" + outputFormat);
                    }

                    // SurfaceTextureで使えるカメラのサイズを取得
                    for (Size size : map.getOutputSizes(SurfaceTexture.class)) {
                        Log.d("SHISHIMO", "Camera Supported Output Size for SurfaceTexture:" + size.toString());
                    }

                    // カメラサイズを取得する。降順で取れるぽいので、１番大きいサイズ640x480をここでは使う。
                    mCameraSize = map.getOutputSizes(SurfaceTexture.class)[0];
                    Log.d("SHISHIMO", "Camera Supported Output Size:" + mCameraSize.toString());

                    // スレッド作成
                    HandlerThread thread = new HandlerThread("OpenCamera");

                    // スレッド開始
                    thread.start();

                    // 別スレッドで動作するハンドラー作成
                    Handler backgroundHandler = new Handler(thread.getLooper());

                    // カメラを起動する
                    manager.openCamera(cameraId, mCameraDeviceCallback, backgroundHandler);

                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * カメラを起動後、撮影時の動作などを定義するため、CameraCaputureSessionを定義する。
     */
    private void createCaptureSession() {

        // テキスチャービューが利用できるか確認。
        // Memo: 利用できないケースは何か
        if (!mTextureView.isAvailable()) {
            Log.d("SHISHIMO", "Texture View is not available");
            return;
        }

        // MEMO: SurfaceTexture, Surfaceが何か調べる。
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mCameraSize.getWidth(), mCameraSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            // 撮影した画像のデータをビルドするビルダーを取得する。
            // 表示形式のテンプレートは、プレビューを指定する。
            mCaptureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // ビルダーが出力する面を追加する。
        mCaptureBuilder.addTarget(surface);
        try {
            // Capture Sessionを作成
            // メインスレッドで動作する。
            mCamera.createCaptureSession(Collections.singletonList(surface),
                                         mCameraCaptureSessionCallback,
                                         null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Log.d("SHISHIMO", "Created Camera Capture Session");
    }

    /**
     * ビューを更新する。
     */
    private void updatePreview() {
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * カメラのステータス変化時の動作を定義するコールバック
     */
    private CameraDevice.StateCallback mCameraDeviceCallback = new CameraDevice.StateCallback() {

        // カメラがオープンしたら
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            Log.d("SHISHIMO", "Camera Opened");

            // カメラを起動したら、撮影時のセッション（動作）をまず定義しないといけない。
            createCaptureSession();
        }

        // カメラがクローズしたら（camera.close()を呼んだら）
        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            Log.d("SHISHIMO", "Camera Closed");
        }

        // カメラが接続が外れたら
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCamera = null;
            Log.d("SHISHIMO", "Camera Disconnected");
        }

        // エラー発生時
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCamera = null;
        }
    };

    /**
     * カメラの撮影時のステータス変化時の動作を定義するコールバック
     */
    CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {

        // カメラの撮影時設定が完了した場合
        // この後撮影処理（画像のキャプチャ）が始まる
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {

            // 撮影セッションを取得
            mCaptureSession = session;

            // 撮影した画像を表示
            updatePreview();

            Log.d("SHISHIMO", "Camera has finished configuring");
        }

        // カメラの撮影時設定が失敗した場合
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d("SHISHIMO", "Camera has failed configuring");
        }
    };
}
