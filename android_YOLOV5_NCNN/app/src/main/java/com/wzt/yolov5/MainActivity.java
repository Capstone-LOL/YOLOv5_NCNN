package com.wzt.yolov5;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    public static int YOLOV5S = 1;
    public static int YOLOV4_TINY = 2;
    public static int MOBILENETV2_YOLOV3_NANO = 3;
    public static int SIMPLE_POSE = 4;
    public static int YOLACT = 5;
    public static int ENET = 6;
    public static int FACE_LANDMARK = 7;
    public static int DBFACE = 8;

    public static int USE_MODEL = MOBILENETV2_YOLOV3_NANO;
    public static boolean USE_GPU = false;

    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.BACK;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };
    private ImageView resultImageView;
    private SeekBar nmsSeekBar;
    private SeekBar thresholdSeekBar;
    private TextView thresholdTextview;
    private TextView tvInfo;
    private double threshold = 0.3, nms_threshold = 0.7;
    private TextureView viewFinder;

    private AtomicBoolean detecting = new AtomicBoolean(false);
    private AtomicBoolean detectPhoto = new AtomicBoolean(false);

    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;

    double total_fps = 0;
    int fps_count = 0;

    protected Bitmap mutableBitmap;

    ExecutorService detectService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA
            );
            finish();
        }
        if (USE_MODEL == YOLOV5S) {
            YOLOv5.init(getAssets(), USE_GPU);
        } else if (USE_MODEL == YOLOV4_TINY) {
            YOLOv4.init(getAssets(), true, USE_GPU);
        } else if (USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
            YOLOv4.init(getAssets(), false, USE_GPU);
        } else if (USE_MODEL == SIMPLE_POSE) {
            SimplePose.init(getAssets(), USE_GPU);
        } else if (USE_MODEL == YOLACT) {
            Yolact.init(getAssets(), USE_GPU);
        } else if (USE_MODEL == ENET) {
            ENet.init(getAssets(), USE_GPU);
        } else if (USE_MODEL == FACE_LANDMARK) {
            FaceLandmark.init(getAssets(), USE_GPU);
        } else if (USE_MODEL == DBFACE) {
            DBFace.init(getAssets(), USE_GPU);
        }
        resultImageView = findViewById(R.id.imageView);
        thresholdTextview = findViewById(R.id.valTxtView);
        tvInfo = findViewById(R.id.tv_info);
        nmsSeekBar = findViewById(R.id.nms_seek);
        thresholdSeekBar = findViewById(R.id.threshold_seek);
        if (USE_MODEL != YOLOV5S && USE_MODEL != DBFACE) {
            nmsSeekBar.setEnabled(false);
            thresholdSeekBar.setEnabled(false);
        } else if (USE_MODEL == YOLOV5S) {
            threshold = 0.3f;
            nms_threshold = 0.7f;
        } else if (USE_MODEL == DBFACE) {
            threshold = 0.4f;
            nms_threshold = 0.6f;
        }
        nmsSeekBar.setProgress((int) (nms_threshold * 100));
        thresholdSeekBar.setProgress((int) (threshold * 100));
        final String format = "Thresh: %.2f, NMS: %.2f";
        thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
        nmsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nms_threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Button inference = findViewById(R.id.button);
        inference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            }
        });

        resultImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectPhoto.set(false);
            }
        });

        viewFinder = findViewById(R.id.view_finder);
        viewFinder.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                updateTransform();
            }
        });

        viewFinder.post(new Runnable() {
            @Override
            public void run() {
                startCamera();
            }
        });
    }

    private void updateTransform() {
        Matrix matrix = new Matrix();
        // Compute the center of the view finder
        float centerX = viewFinder.getWidth() / 2f;
        float centerY = viewFinder.getHeight() / 2f;

        float[] rotations = {0, 90, 180, 270};
        // Correct preview output to account for display rotation
        float rotationDegrees = rotations[viewFinder.getDisplay().getRotation()];

        matrix.postRotate(-rotationDegrees, centerX, centerY);

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix);
    }

    private void startCamera() {
        CameraX.unbindAll();
        // 1. preview
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(CAMERA_ID)
//                .setTargetAspectRatio(Rational.NEGATIVE_INFINITY)  // 宽高比
                .setTargetResolution(new Size(480, 640))  // 分辨率
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) viewFinder.getParent();
                parent.removeView(viewFinder);
                parent.addView(viewFinder, 0);

                viewFinder.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });
        DetectAnalyzer detectAnalyzer = new DetectAnalyzer();
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, gainAnalyzer(detectAnalyzer));

    }


    private UseCase gainAnalyzer(DetectAnalyzer detectAnalyzer) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder();
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(480, 640));  // 输出预览图像尺寸
        ImageAnalysisConfig config = analysisConfigBuilder.build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setAnalyzer(detectAnalyzer);
        return analysis;
    }

    private class DetectAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy image, final int rotationDegrees) {
            if (detecting.get() || detectPhoto.get()) {
                return;
            }
            detecting.set(true);
            startTime = System.currentTimeMillis();
            final Bitmap bitmapsrc = imageToBitmap(image);  // 格式转换
            if (detectService == null) {
                detecting.set(false);
                return;
            }
            detectService.execute(new Runnable() {
                @Override
                public void run() {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    width = bitmapsrc.getWidth();
                    height = bitmapsrc.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false);

                    Box[] result = null;
                    KeyPoint[] keyPoints = null;
                    YolactMask[] yolactMasks = null;
                    FaceKeyPoint[] faceKeyPoints = null;
                    float[] enetMasks = null;
                    if (USE_MODEL == YOLOV5S) {
                        result = YOLOv5.detect(bitmap, threshold, nms_threshold);
                    } else if (USE_MODEL == YOLOV4_TINY || USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
                        result = YOLOv4.detect(bitmap, threshold, nms_threshold);
                    } else if (USE_MODEL == SIMPLE_POSE) {
                        keyPoints = SimplePose.detect(bitmap);
                    } else if (USE_MODEL == YOLACT) {
                        yolactMasks = Yolact.detect(bitmap);
                    } else if (USE_MODEL == ENET) {
                        enetMasks = ENet.detect(bitmap);
                    } else if (USE_MODEL == FACE_LANDMARK) {
                        faceKeyPoints = FaceLandmark.detect(bitmap);
                    } else if (USE_MODEL == DBFACE) {
                        keyPoints = DBFace.detect(bitmap, threshold, nms_threshold);
                    }
                    if (result == null && keyPoints == null && yolactMasks == null && enetMasks == null && faceKeyPoints == null) {
                        detecting.set(false);
                        return;
                    }
                    mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY || USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
                        mutableBitmap = drawBoxRects(mutableBitmap, result);
                    } else if (USE_MODEL == SIMPLE_POSE) {
                        mutableBitmap = drawPersonPose(mutableBitmap, keyPoints);
                    } else if (USE_MODEL == YOLACT) {
                        mutableBitmap = drawYolactMask(mutableBitmap, yolactMasks);
                    } else if (USE_MODEL == ENET) {
                        mutableBitmap = drawENetMask(mutableBitmap, enetMasks);
                    } else if (USE_MODEL == FACE_LANDMARK) {
                        mutableBitmap = drawFaceLandmark(mutableBitmap, faceKeyPoints);
                    } else if (USE_MODEL == DBFACE) {
                        mutableBitmap = drawDBFaceLandmark(mutableBitmap, keyPoints);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            detecting.set(false);
                            if (detectPhoto.get()) {
                                return;
                            }
                            resultImageView.setImageBitmap(mutableBitmap);
                            endTime = System.currentTimeMillis();
                            long dur = endTime - startTime;
                            float fps = (float) (1000.0 / dur);
                            total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                            fps_count++;
                            String modelName = getModelName();

                            tvInfo.setText(String.format(Locale.CHINESE,
                                    "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
                                    modelName, height, width, dur / 1000.0, fps, (float) total_fps / fps_count));
                        }
                    });
                }
            });
        }

        private Bitmap imageToBitmap(ImageProxy image) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ImageProxy.PlaneProxy y = planes[0];
            ImageProxy.PlaneProxy u = planes[1];
            ImageProxy.PlaneProxy v = planes[2];
            ByteBuffer yBuffer = y.getBuffer();
            ByteBuffer uBuffer = u.getBuffer();
            ByteBuffer vBuffer = v.getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();

            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }

    }

    protected Bitmap drawDBFaceLandmark(Bitmap mutableBitmap, KeyPoint[] keyPoints) {
        if (keyPoints == null || keyPoints.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint keyPointPaint = new Paint();
        keyPointPaint.setAlpha(200);
        keyPointPaint.setStyle(Paint.Style.STROKE);
        keyPointPaint.setColor(Color.BLUE);
//        Log.d("wzt", "dbface size:" + keyPoints.length);
        for (int i = 0; i < keyPoints.length; i++) {
            // 其它随机颜色
            Random random = new Random(i + 2020);
            int color = Color.argb(255, random.nextInt(256), 125, random.nextInt(256));
            keyPointPaint.setColor(color);
            keyPointPaint.setStrokeWidth(9 * mutableBitmap.getWidth() / 800.0f);
            for (int j = 0; j < 5; j++) {
                canvas.drawPoint(keyPoints[i].x[j], keyPoints[i].y[j], keyPointPaint);
            }
            keyPointPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
            canvas.drawRect(keyPoints[i].x0, keyPoints[i].y0, keyPoints[i].x1, keyPoints[i].y1, keyPointPaint);
        }
        return mutableBitmap;
    }

    protected Bitmap drawENetMask(Bitmap mutableBitmap, float[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint maskPaint = new Paint();
        maskPaint.setStyle(Paint.Style.STROKE);
        maskPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        maskPaint.setTextSize(40 * mutableBitmap.getWidth() / 800.0f);
        float mask = 0;
        for (int y = 0; y < mutableBitmap.getHeight(); y++) {
            for (int x = 0; x < mutableBitmap.getWidth(); x++) {
                mask = results[y * mutableBitmap.getWidth() + x];
                Random random = new Random((long) (mask));
                int color = Color.argb(255, random.nextInt(256), 125, random.nextInt(256));
                maskPaint.setColor(color);
                maskPaint.setAlpha(100);
                canvas.drawPoint(x, y, maskPaint);
            }
        }
        return mutableBitmap;
    }

    protected Bitmap drawYolactMask(Bitmap mutableBitmap, YolactMask[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint maskPaint = new Paint();
        maskPaint.setAlpha(200);
        maskPaint.setStyle(Paint.Style.STROKE);
        maskPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        maskPaint.setTextSize(40 * mutableBitmap.getWidth() / 800.0f);
        maskPaint.setColor(Color.BLUE);
        for (YolactMask mask : results) {
            if (mask.prob < 0.4f) {
                continue;
            }
            int index = 0;
            for (int y = 0; y < mutableBitmap.getHeight(); y++) {
                for (int x = 0; x < mutableBitmap.getWidth(); x++) {
                    if (mask.mask[index] != 0) {
                        maskPaint.setColor(mask.getColor());
                        maskPaint.setAlpha(100);
                        canvas.drawPoint(x, y, maskPaint);
                    }
                    index++;
                }
            }
            // 标签跟框放后面画，防止被 mask 挡住
            maskPaint.setColor(mask.getColor());
            maskPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(mask.getLabel() + String.format(Locale.CHINESE, " %.3f", mask.getProb()), mask.left, mask.top - 15 * mutableBitmap.getWidth() / 1000.0f, maskPaint);
            maskPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(new RectF(mask.left, mask.top, mask.right, mask.bottom), maskPaint);
        }
        return mutableBitmap;
    }

    protected Bitmap drawBoxRects(Bitmap mutableBitmap, Box[] results) {
        if (results == null || results.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * mutableBitmap.getWidth() / 800.0f);
        boxPaint.setTextSize(40 * mutableBitmap.getWidth() / 800.0f);
        for (Box box : results) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel() + String.format(Locale.CHINESE, " %.3f", box.getScore()), box.x0 + 3, box.y0 + 40 * mutableBitmap.getWidth() / 1000.0f, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }
        return mutableBitmap;
    }

    protected String getModelName() {
        String modelName = "ohhhhh";
        if (USE_MODEL == YOLOV5S) {
            modelName = "YOLOv5s";
        } else if (USE_MODEL == YOLOV4_TINY) {
            modelName = "YOLOv4-tiny";
        } else if (USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
            modelName = "MobileNetV2-YOLOv3-Nano";
        } else if (USE_MODEL == SIMPLE_POSE) {
            modelName = "Simple-Pose";
        } else if (USE_MODEL == YOLACT) {
            modelName = "Yolact";
        } else if (USE_MODEL == ENET) {
            modelName = "ENet";
        } else if (USE_MODEL == FACE_LANDMARK) {
            modelName = "YoloFace500k-landmark106";
        } else if (USE_MODEL == DBFACE) {
            modelName = "DBFace";
        }
        return modelName;
    }

    protected Bitmap drawFaceLandmark(Bitmap mutableBitmap, FaceKeyPoint[] keyPoints) {
        if (keyPoints == null || keyPoints.length <= 0) {
            return mutableBitmap;
        }
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint keyPointPaint = new Paint();
        keyPointPaint.setAlpha(200);
        keyPointPaint.setStyle(Paint.Style.STROKE);
        keyPointPaint.setStrokeWidth(8 * mutableBitmap.getWidth() / 800.0f);
        keyPointPaint.setColor(Color.BLUE);
//        Log.d("wzt", "facePoint length:" + keyPoints.length);
        for (int i = 0; i < keyPoints.length; i++) {
            // 其它随机颜色
            Random random = new Random(i / 106 + 2020);
            int color = Color.argb(255, random.nextInt(256), 125, random.nextInt(256));
            keyPointPaint.setColor(color);
            canvas.drawPoint(keyPoints[i].x, keyPoints[i].y, keyPointPaint);
        }
        return mutableBitmap;
    }

    protected Bitmap drawPersonPose(Bitmap mutableBitmap, KeyPoint[] keyPoints) {
        if (keyPoints == null || keyPoints.length <= 0) {
            return mutableBitmap;
        }
        // draw bone
        // 0 nose, 1 left_eye, 2 right_eye, 3 left_Ear, 4 right_Ear, 5 left_Shoulder, 6 rigth_Shoulder, 7 left_Elbow, 8 right_Elbow,
        // 9 left_Wrist, 10 right_Wrist, 11 left_Hip, 12 right_Hip, 13 left_Knee, 14 right_Knee, 15 left_Ankle, 16 right_Ankle
        int[][] joint_pairs = {{0, 1}, {1, 3}, {0, 2}, {2, 4}, {5, 6}, {5, 7}, {7, 9}, {6, 8}, {8, 10}, {5, 11}, {6, 12}, {11, 12}, {11, 13}, {12, 14}, {13, 15}, {14, 16}};
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint keyPointPaint = new Paint();
        keyPointPaint.setAlpha(200);
        keyPointPaint.setStyle(Paint.Style.STROKE);
        keyPointPaint.setColor(Color.BLUE);
        int color = Color.BLUE;
        // 画线、画框、画点
        for (int i = 0; i < keyPoints.length; i++) {
            // 其它随机颜色
            Random random = new Random(i + 2020);
            color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
            // 画线
            keyPointPaint.setStrokeWidth(5 * mutableBitmap.getWidth() / 800.0f);
            for (int j = 0; j < 16; j++) {  // 17个点连成16条线
                int pl0 = joint_pairs[j][0];
                int pl1 = joint_pairs[j][1];
                // 人体左侧改为红线
                if ((pl0 % 2 == 1) && (pl1 % 2 == 1) && pl0 >= 5 && pl1 >= 5) {
                    keyPointPaint.setColor(Color.RED);
                } else {
                    keyPointPaint.setColor(color);
                }
                canvas.drawLine(keyPoints[i].x[joint_pairs[j][0]], keyPoints[i].y[joint_pairs[j][0]],
                        keyPoints[i].x[joint_pairs[j][1]], keyPoints[i].y[joint_pairs[j][1]],
                        keyPointPaint);
            }
            // 画点
            keyPointPaint.setColor(Color.GREEN);
            keyPointPaint.setStrokeWidth(8 * mutableBitmap.getWidth() / 800.0f);
            for (int n = 0; n < 17; n++) {
                canvas.drawPoint(keyPoints[i].x[n], keyPoints[i].y[n], keyPointPaint);
            }
            // 画框
            keyPointPaint.setColor(color);
            keyPointPaint.setStrokeWidth(3 * mutableBitmap.getWidth() / 800.0f);
            canvas.drawRect(keyPoints[i].x0, keyPoints[i].y0, keyPoints[i].x1, keyPoints[i].y1, keyPointPaint);
        }
        return mutableBitmap;
    }


    @Override
    protected void onDestroy() {
        if (detectService != null) {
            detectService.shutdown();
            detectService = null;
        }
        CameraX.unbindAll();
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission!", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        detectPhoto.set(true);
        Bitmap image = getPicture(data.getData());
        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);

        Box[] result = null;
        KeyPoint[] keyPoints = null;
        YolactMask[] yolactMasks = null;
        FaceKeyPoint[] faceKeyPoints = null;
        float[] enetMasks = null;
        if (USE_MODEL == YOLOV5S) {
            result = YOLOv5.detect(image, threshold, nms_threshold);
        } else if (USE_MODEL == YOLOV4_TINY || USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
            result = YOLOv4.detect(image, threshold, nms_threshold);
        } else if (USE_MODEL == SIMPLE_POSE) {
            keyPoints = SimplePose.detect(image);
        } else if (USE_MODEL == YOLACT) {
            yolactMasks = Yolact.detect(image);
        } else if (USE_MODEL == ENET) {
            enetMasks = ENet.detect(image);
        } else if (USE_MODEL == FACE_LANDMARK) {
            faceKeyPoints = FaceLandmark.detect(image);
        } else if (USE_MODEL == DBFACE) {
            keyPoints = DBFace.detect(image, threshold, nms_threshold);
        }
        if (USE_MODEL == YOLOV5S || USE_MODEL == YOLOV4_TINY || USE_MODEL == MOBILENETV2_YOLOV3_NANO) {
            mutableBitmap = drawBoxRects(mutableBitmap, result);
        } else if (USE_MODEL == SIMPLE_POSE) {
            mutableBitmap = drawPersonPose(mutableBitmap, keyPoints);
        } else if (USE_MODEL == YOLACT) {
            mutableBitmap = drawYolactMask(mutableBitmap, yolactMasks);
        } else if (USE_MODEL == ENET) {
            mutableBitmap = drawENetMask(mutableBitmap, enetMasks);
        } else if (USE_MODEL == FACE_LANDMARK) {
            mutableBitmap = drawFaceLandmark(mutableBitmap, faceKeyPoints);
        } else if (USE_MODEL == DBFACE) {
            mutableBitmap = drawDBFaceLandmark(mutableBitmap, keyPoints);
        }
        resultImageView.setImageBitmap(mutableBitmap);
    }

    public Bitmap getPicture(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        int rotate = readPictureDegree(picturePath);
        return rotateBitmapByDegree(bitmap, rotate);
    }

    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }


}
