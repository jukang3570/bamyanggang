package com.example.test3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import com.squareup.picasso.Picasso;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 100000;

    private ImageButton saveButton;
    private ImageButton youtubeButton;
    private ImageButton ig1Button;
    private ImageButton ig2Button;
    private ImageView resultImage;
    private ImageButton homeButton;

    private String currentImageUrl;
    private String currentFilename;
    private DialogFragment loadingDialog;

    private static final String TAG = "ResultActivity";
    private boolean isResizedImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        homeButton = findViewById(R.id.homebutton);
        saveButton = findViewById(R.id.saveButton);
        youtubeButton = findViewById(R.id.youtubeButton);
        ig1Button = findViewById(R.id.ig1);
        ig2Button = findViewById(R.id.ig2);
        resultImage = findViewById(R.id.resultImage);

        loadingDialog = new LoadingDialogFragment();

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // 현재 액티비티를 종료합니다.
            }
        });

        String command = getIntent().getStringExtra("command");
        sendCommandToServer(command);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentImageUrl != null) {
                    if (checkStoragePermissions()) {
                        saveImageToDisk(currentImageUrl);
                    }
                }
            }
        });

        youtubeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resizeImage(1);
            }
        });

        ig1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resizeImage(2);
            }
        });

        ig2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resizeImage(3);
            }
        });
    }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentImageUrl != null) {
                    saveImageToDisk(currentImageUrl);
                }
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendCommandToServer(String command) {
        loadingDialog.show(getSupportFragmentManager(), "loading");

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        CommandRequest commandRequest = new CommandRequest(command);

        apiService.generateImage(commandRequest).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                loadingDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    currentImageUrl = response.body().getImageUrl();
                    currentFilename = response.body().getFilename();
                    Log.d(TAG, "Image URL: " + currentImageUrl);
                    Log.d(TAG, "Filename: " + currentFilename);

                    // Picasso를 사용하여 이미지 로드
                    Picasso.get().load(currentImageUrl).into(resultImage);
                    // 원본 이미지로 설정
                    isResizedImage = false;

                    resultImage.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "Response was not successful: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                loadingDialog.dismiss();
                Log.e(TAG, "Request failed", t);
            }
        });
    }


    private void resizeImage(int platform) {
        if (currentFilename == null) {
            Log.e(TAG, "No image filename to resize");
            return;
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        ResizeRequest resizeRequest = new ResizeRequest(platform, currentFilename);
        Log.d(TAG, "Sending resize request for filename: " + currentFilename + " and platform: " + platform);

        apiService.resizeImage(resizeRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 기존 이미지 로드 코드를 사용하여 이미지를 표시
                        String resizedFilename = "resized_" + platform + "_" + currentFilename;
                        String resizedImageUrl = "http://server_ip:5000/images/" + resizedFilename;
                        Log.d(TAG, "Resized image URL: " + resizedImageUrl);

                        // Picasso를 사용하여 이미지 로드
                        Picasso.get().load(resizedImageUrl).into(resultImage);
                        resultImage.setVisibility(View.VISIBLE);

                        Log.d(TAG, "Resized image displayed successfully");
                        // 리사이징된 이미지를 저장
                        saveImageToDisk(resizedImageUrl);
                        // 리사이징된 이미지로 설정
                        isResizedImage = true;

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to display resized image", e);
                    }
                } else {
                    Log.e(TAG, "Resize response was not successful: " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Resize request failed", t);
            }
        });
    }

    private void saveImageToDisk(String imageUrl) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    // 리사이징된 이미지를 저장
                    if (isResizedImage) {
                        saveBitmap(bitmap);
                    }

                    inputStream.close();
                    connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error saving image", e);
                }
            }
        });
        thread.start();
    }

    private void saveBitmap(Bitmap bitmap) {
        File storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!storagePath.exists()) {
            storagePath.mkdirs();
        }
        String filename = "downloaded_image_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(storagePath, filename);

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            Log.d(TAG, "Image saved to " + imageFile.getAbsolutePath());

            // 갤러리에 이미지 추가
            MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image", e);
        }
    }
}