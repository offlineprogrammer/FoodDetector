package com.offlineprogrammer.fooddetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amplifyframework.predictions.models.Label;
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.result.IdentifyLabelsResult;
import com.amplifyframework.rx.RxAmplify;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.model.Image;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    Button capture_button;
    ImageView capturedImage;
    TextView objectTextView;
    ImageView flagImage;
    ProgressDialog progressDialog;
    private static final int CAMERA_REQUEST = 2222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capturedImage = findViewById(R.id.capturedImage);
        objectTextView = findViewById(R.id.objectTextView);
        capture_button = findViewById(R.id.capture_button);
        flagImage = findViewById(R.id.flagImage);

        capture_button.setOnClickListener(view -> {
            ImagePicker.create(MainActivity.this).returnMode(ReturnMode.ALL)
                    .folderMode(true).includeVideo(false).limit(1).theme(R.style.AppTheme_NoActionBar).single().start();
            flagImage.setVisibility(View.GONE);
            flagImage.setImageDrawable(getDrawable(R.drawable.ic_baseline_close_24));

        });

    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        EasyPermissions.onRequestPermissionsResult(i, strArr, iArr, this);
    }


    public void onPermissionsGranted(int i, @NonNull List<String> list) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new AppSettingsDialog.Builder(this).setTitle("Permissions Required").setPositiveButton("Settings").setNegativeButton("Cancel").setRequestCode(5).build().show();
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (ImagePicker.shouldHandle(i, i2, intent)) {
            Image firstImageOrNull = ImagePicker.getFirstImageOrNull(intent);
            if (firstImageOrNull != null) {
                UCrop.of(Uri.fromFile(new File(firstImageOrNull.getPath())), Uri.fromFile(new File(getCacheDir(), "cropped"))).withAspectRatio(1.0f, 1.0f).start(this);
            }
        }
        if (i == UCrop.REQUEST_CROP) {
            onCropFinish(intent);
        }
    }

    public void onCropFinish(Intent intent) {
        if (intent == null) {
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processing...");
        progressDialog.show();

        GlideApp.with(this)
                .asBitmap()
                .load(UCrop.getOutput(intent).getPath()).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).centerCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        capturedImage.setImageBitmap(resource);
                        detectLabels(resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    public void detectLabels(Bitmap image) {
        RxAmplify.Predictions.identify(LabelType.LABELS, image)
                .subscribe(
                        result -> {
                            String sLabel = "";
                            IdentifyLabelsResult identifyResult = (IdentifyLabelsResult) result;
                            //    Label label = identifyResult.getLabels().get(0);
                            List<Label> labels = identifyResult.getLabels();
                            for (Label label : labels) {
                                sLabel += String.format(" %s | ",label.getName());
                                if("food".equalsIgnoreCase(label.getName())){
                                    flagImage.setImageDrawable(getDrawable(R.drawable.ic_baseline_check_24));
                                }
                            }
                            String finalSLabel = sLabel;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    objectTextView.setText(finalSLabel);
                                    flagImage.setVisibility(View.VISIBLE);
                                    progressDialog.dismiss();

                                }
                            });
                            Log.i("MyAmplifyApp", sLabel);
                        },
                        error -> Log.e("MyAmplifyApp", "Label detection failed", error)
                );
    }
}