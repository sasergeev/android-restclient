package com.github.sasergeev.example;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import java.io.File;

public class UI {

    public static void showDownloadProgressDialog(final Context context, final Consumer<AlertDialog, ProgressBar, TextView> consumer) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogStyle);
        View mView = LayoutInflater.from(context).inflate(R.layout.download_progress_dialog, null);
        ProgressBar downloadProgress = mView.findViewById(R.id.download_progress);
        TextView downloadPercent = mView.findViewById(R.id.download_percent);
        builder
                .setCancelable(false)
                .setMessage("Downloading file...")
                .setView(downloadPercent)
                .setView(downloadProgress)
                .setView(mView);
        AlertDialog dialog = builder.create();
        consumer.accept(dialog, downloadProgress, downloadPercent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    public static void openFile(final Context context, final String path, String type) {
        File file = new File(path);
        Intent intentFile = new Intent(Intent.ACTION_VIEW);
        intentFile.setDataAndType(FileProvider.getUriForFile(context.getApplicationContext(), "com.github.saserg.example.provider", file), type);
        intentFile.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intentFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intentFile);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void showChoiceDialog(final Context context, final DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogStyle);
        builder
                .setCancelable(true)
                .setTitle("Choice Action:")
                .setMessage("Open downloaded file?")
                .setPositiveButton("Yes", onClickListener)
                .setNegativeButton("No", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public interface Consumer<S,T,V> {
        void accept(S obj1, T obj2, V obj3);
    }

    public static void indeterminateProgress(ProgressBar progressBar, boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    @BindingAdapter({"app:url"})
    public static void loadPicture(ImageView imageView, String url) {
        Picasso.with(imageView.getContext()).load(url).into(imageView);
    }

}
