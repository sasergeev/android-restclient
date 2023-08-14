package com.github.sasergeev.example;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.github.sasergeev.example.pojo.UserData;
import com.github.sasergeev.restclient.RestClientBuilder;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private ProgressBar progressBar;
    private TextView progressText;
    private AlertDialog alertDialog;
    private ProgressBar progressRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.data);
        Button requestButton = findViewById(R.id.fetch_button);
        Button downloadButton = findViewById(R.id.download_button);
        Button listButton = findViewById(R.id.list_button);
        Button loadButton = findViewById(R.id.load_image_button);
        ImageView loadedImage = findViewById(R.id.loaded_image);
        progressRequest = findViewById(R.id.progress_bar);
        requestButton.setOnClickListener(v -> fetchData("5", progressRequest));
        downloadButton.setOnClickListener(v -> downloadData());
        loadButton.setOnClickListener(v -> loadImage(loadedImage, progressRequest));
        listButton.setOnClickListener(v -> startActivity(new Intent(this, ListActivity.class)));
    }

    // Sample code for GET-request to fetch list of objects
    private void fetchData(ProgressBar progress) {
        String MOCK_API_SERVER = "https://63d7676e5c4274b136f38138.mockapi.io/";
        RestClientBuilder.build(UserData[].class)
                .url(MOCK_API_SERVER)
                .uri("simpleapi/v1/")
                .get()
                .before(() -> UI.indeterminateProgress(progress, true))
                .finish(() -> UI.indeterminateProgress(progress, false))
                .success((object, headers, status) -> textView.setText("Count objects: " + object.length))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this,
                        "Error message: " + error + " " + "Status server: " + status, Toast.LENGTH_LONG).show());
    }

    // Sample code for GET-request by some id to fetch object
    private void fetchData(String id, ProgressBar progress) {
        String MOCK_API_SERVER = "https://63d7676e5c4274b136f38138.mockapi.io/";
        RestClientBuilder.build(UserData.class)
                .url(MOCK_API_SERVER)
                .uri("simpleapi/v1/{id}")
                .get(id)
                .before(() -> UI.indeterminateProgress(progress, true))
                .finish(() -> UI.indeterminateProgress(progress, false))
                .success((object, headers, status) -> textView.setText(object.toString()))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this,
                        "Error message: " + error + " " + "Status server: " + status, Toast.LENGTH_LONG).show());
    }

    // Sample code for POST-request to create object
    private void createData(ProgressBar progress) {
        String MOCK_API_SERVER = "https://63d7676e5c4274b136f38138.mockapi.io/";
        RestClientBuilder.build(UserData.class)
                .url(MOCK_API_SERVER)
                .uri("simpleapi/v1/")
                .post(new UserData("10", true, "Created Pojo Object"))
                .before(() -> UI.indeterminateProgress(progress, true))
                .finish(() -> UI.indeterminateProgress(progress, false))
                .success((object, headers, status) -> textView.setText(String.valueOf(status.value())))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this,
                        "Error message: " + error + " " + "Status server: " + status, Toast.LENGTH_LONG).show());
    }

    // Sample code for PUT-request by some id to modify object
    private void changeData(String id, ProgressBar progress) {
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("isActive", true);
        jsonBody.put("nickName", "Changed Name");
        jsonBody.put("clientId", id);
        String MOCK_API_SERVER = "https://63d7676e5c4274b136f38138.mockapi.io/";
        RestClientBuilder.build(UserData.class)
                .url(MOCK_API_SERVER)
                .uri("simpleapi/v1/{id}")
                .put(jsonBody, id)
                .before(() -> UI.indeterminateProgress(progress, true))
                .finish(() -> UI.indeterminateProgress(progress, false))
                .success((object, headers, status) -> textView.setText(String.valueOf(status.value())))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this,
                        "Error message: " + error + " " + "Status server: " + status, Toast.LENGTH_LONG).show());
    }

    // Sample code for DELETE-request by some id to delete object
    private void deleteData(String id, ProgressBar progress) {
        String MOCK_API_SERVER = "https://63d7676e5c4274b136f38138.mockapi.io/";
        RestClientBuilder.build(UserData.class)
                .url(MOCK_API_SERVER)
                .uri("simpleapi/v1/{id}")
                .delete(id)
                .before(() -> UI.indeterminateProgress(progress, true))
                .finish(() -> UI.indeterminateProgress(progress, false))
                .success((object, headers, status) -> textView.setText(String.valueOf(status.value())))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this,
                        "Error message: " + error + " " + "Status server: " + status, Toast.LENGTH_LONG).show());
    }

    // Sample code for downloading file
    private void downloadData() {
        UI.showDownloadProgressDialog(MainActivity.this, (dialog, progress, percent) -> {
            this.alertDialog = dialog;
            this.progressBar = progress;
            this.progressText = percent;
        });
        String filePath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();
        String MOCK_API_SERVER = "";
        RestClientBuilder.build(Class.class)
                .url(MOCK_API_SERVER)
                .before(() -> alertDialog.show())
                .execute(() -> progressBar.setIndeterminate(false))
                .progress(value -> {
                    progressBar.setProgress(value);
                    progressText.setText(String.valueOf(value));
                })
                .done((String file, String type) -> UI.showChoiceDialog(MainActivity.this, (dialog, i) -> {
                    dialog.dismiss();
                    UI.openFile(MainActivity.this, file, type);
                }))
                .finish(() -> alertDialog.dismiss())
                .error((error, headers, status) -> Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show())
                .download(filePath);
    }

    // Sample code for loading image
    private void loadImage(ImageView image, ProgressBar progress) {
        String MOCK_API_SERVER = "http://192.168.0.242:8080/api/get/image/1";
        RestClientBuilder.build(Class.class)
                .url(MOCK_API_SERVER)
                .before(() -> UI.indeterminateProgress(progress, true))
                .done((ByteArrayOutputStream bytes, String type) -> {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size());
                    image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, image.getWidth(), image.getHeight(), false));
                })
                .finish(() -> UI.indeterminateProgress(progress, false))
                .error((error, headers, status) -> Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show())
                .download();
    }

}
