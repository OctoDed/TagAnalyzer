package com.example.cameraxtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    TextView textView;
    PreviewView mCameraView;
    SurfaceHolder holder;
    SurfaceView surfaceView;
    Canvas canvas;
    Paint paint;
    int cameraHeight, cameraWidth, xOffset, yOffset, boxWidth, boxHeight;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String[] permissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
    };

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }


    /**
     * Starting Camera
     */
    void startCamera(){
        mCameraView = findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    MainActivity.this.bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     *
     * Binding to camera
     */
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mCameraView.createSurfaceProvider());


        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();

        setContentView(R.layout.activity_main);



        //Start Camera
        startCamera();

        //Create the bounding box
        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(this);

    }

    /**
     *
     * For drawing the rectangular box
     */
    private void DrawFocusRect(int color) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = mCameraView.getHeight();
        int width = mCameraView.getWidth();

        //cameraHeight = height;
        //cameraWidth = width;

        int left, right, top, bottom, diameter;

        diameter = width;
        if (height < width) {
            diameter = height;
        }

        int offset = (int) (0.05 * diameter);
        diameter -= offset;

        canvas = holder.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(5);

        left = width / 2 - diameter / 2;
        top = height / 2 - diameter / 2;
        right = width / 2 + diameter / 2;
        bottom = height / 2 + diameter / 2;

        xOffset = left;
        yOffset = top;
        boxHeight = bottom - top;
        boxWidth = right - left;
        //Changing the value of x in diameter/x will change the size of the box ; inversely proportionate to x
        canvas.drawRect(left, top, right, bottom, paint);
        holder.unlockCanvasAndPost(canvas);
    }

    /**
     * Callback functions for the surface Holder
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Drawing rectangle
        DrawFocusRect(Color.parseColor("#b3dabb"));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void capturing(View view) {

        Bitmap bmp=mCameraView.getBitmap();
        //Getting the values for cropping
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = bmp.getHeight();
        int width = bmp.getWidth();

        int left, right, top, bottom, diameter;

        diameter = width;
        if (height < width) {
            diameter = height;
        }

        int offset = (int) (0.05 * diameter);
        diameter -= offset;


        left = width / 2 - diameter / 2;
        top = height / 2 - diameter / 2;
        right = width / 2 + diameter / 2;
        bottom = height / 2 + diameter / 2;

        xOffset = left;
        yOffset = top;

        //Creating new cropped bitmap
        Bitmap bitmap = Bitmap.createBitmap(bmp, left, top, boxWidth, boxHeight);

        saveImageToExternalStorage(bitmap);

        ViewFlipper vf = (ViewFlipper) findViewById( R.id.viewFlipper );

        ImageView imageView = (ImageView) findViewById(R.id.image1);

        imageView.setImageBitmap(bitmap);

        vf.showNext();
    }

    private void uploadFile(File file) {
        // create upload service client
        FileUploadService service =
                ServiceGenerator.createService(FileUploadService.class);

        try {
            Uri fileUri = Uri.fromFile(file);
            // create RequestBody instance from file
            RequestBody requestFile =
                    RequestBody.create(
                            MediaType.parse("image/png"),
                            file
                    );

            // MultipartBody.Part is used to send also the actual file name
            MultipartBody.Part body =
                    MultipartBody.Part.createFormData("picture", file.getName(), requestFile);

            // add another part within the multipart request
            String descriptionString = "hello, this is description speaking";
            RequestBody description =
                    RequestBody.create(
                            okhttp3.MultipartBody.FORM, descriptionString);

            // finally, execute the request
            Call<JsonObject> call = service.upload(description, body);

            TextView tw = (TextView) findViewById( R.id.resulttext );

            // Set up progress before call
            ProgressDialog nDialog;
            nDialog = new ProgressDialog(MainActivity.this);
            nDialog.setMessage("Loading..");
            nDialog.setTitle("Uploading to server");
            nDialog.setIndeterminate(false);
            nDialog.setCancelable(true);
            nDialog.show();

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call,
                                       Response<JsonObject> response) {
                    Log.v("Upload", "success");
                    nDialog.dismiss();
                    //tw.setText(response.body().toString());
                    JsonObject result = response.body().getAsJsonObject();
                    String description = result.get("description").getAsString();
                    String price11 = result.get("price11").getAsString();
                    String price12 = result.get("price12").getAsString();
                    String price21 = result.get("price21").getAsString();
                    String price22 = result.get("price22").getAsString();
                    String barcode = result.get("barcode_data").getAsString();
                    tw.setText("Описание: " + description + '\n' + "Рубли без карты: " + price11 + '\n' + "Копейки без карты: " + price21 + '\n' + "Рубли по карте: " + price22 + '\n' + "Копейки по карте: " + price12 + '\n' + "Штрих-код: " + barcode);
                    /*
                    if (response.isSuccessful())

                    {
                        tw.setText(response.body().toString());
                    }
                    else
                    {
                        tw.setText("Upload error");
                    }
                    */
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("Upload error:", t.toString());
                    nDialog.dismiss();
                    tw.setText("Upload error");
                }
            });

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
            Toast.makeText(this, "ERROR?", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void backing(View view) {
        ViewFlipper vf = (ViewFlipper) findViewById( R.id.viewFlipper );
        vf.showNext();
    }

    private void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try
        {
            uploadFile(new File(myDir, fname));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}