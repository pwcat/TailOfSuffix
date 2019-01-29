package com.pwcat.root.tailofsuffix;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    EditText  tail_et;
    TextView path_tv, err_tv;
    Button pickPath_btn, commit_btn;
    static final String PREFS_NAME = "record";
    static final String TAG = "debug==========>";
    String lastPath;
    String tail;
    List<String> errs;
    int doneCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 如果未获得外部存储读写权限，则申请
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11);
        }

        fecthViews();
        initViews();
        errs = new ArrayList<>();
    }

    private void removeTails() {
        doneCount = 0;
        errs.clear();
        err_tv.setText("");
        if (lastPath == null || lastPath.equals("")){
            Toast.makeText(this, "Please Pick a Path", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        tail = sharedPreferences.getString(lastPath, "");
        if (tail.equals("")){
            Toast.makeText(this, "Record broken!", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing");
        progressDialog.setCancelable(false);
        progressDialog.show();

        File file = new File(lastPath);
        deTailFile(file);
        if (!errs.isEmpty()){
            err_tv.setText("The renaming of files below has failed:");
            for (String s : errs)
                err_tv.setText(err_tv.getText() + s);
        }
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.remove("lastPath");
        editor.remove(lastPath);
        editor.apply();
        Toast.makeText(this, doneCount + " files done", Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
    }

    private void tailFiles() {
        doneCount = 0;
        Log.i(TAG, "tailFiles: wtf");
        errs.clear();
        err_tv.setText("");
        if (lastPath == null || lastPath.equals("")){
            Toast.makeText(this, "Please Pick a Path", Toast.LENGTH_SHORT).show();
            return;
        }
        tail = tail_et.getText().toString();
        if (tail.equals("")){
            Toast.makeText(this, "Please Input a Tail String", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing");
        progressDialog.setCancelable(false);
        progressDialog.show();
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putString(lastPath, tail);
        editor.apply();

        File file = new File(lastPath);
        Log.i(TAG, file.getAbsolutePath());
        tailFile(file);
        if (!errs.isEmpty()){
            err_tv.setText("The renaming of files below has failed:");
            for (String s : errs)
                err_tv.setText(err_tv.getText() + s);
        }
        Toast.makeText(this, doneCount + " files done", Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
    }

    private void initViews() {
        SharedPreferences record = getSharedPreferences(PREFS_NAME, 0);
        lastPath = record.getString("lastPath", "");
        if (!lastPath.equals(""))
            tail = record.getString(lastPath, "");
        if (!lastPath.equals("")){
            path_tv.setText(lastPath);
            tail_et.setVisibility(View.GONE);
            commit_btn.setText("Remove Tails");
        }else{
            path_tv.setText("");
            commit_btn.setText("Start Tailing");
        }
    }

    private void fecthViews() {
        path_tv = findViewById(R.id.path_et);
        tail_et = findViewById(R.id.tail_et);
        err_tv = findViewById(R.id.err_tv);
        pickPath_btn = findViewById(R.id.pickpath_btn);
        commit_btn = findViewById(R.id.commit_btn);
        pickPath_btn.setOnClickListener(this);
        commit_btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.commit_btn:
                AlertDialog.Builder dlg = new AlertDialog.Builder(this);
                dlg.setMessage("Sure to Start ?");
                dlg.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (commit_btn.getText().toString().equalsIgnoreCase("Start Tailing")){
                            tailFiles();
                        }else{
                            removeTails();
                        }
                    }
                });
                dlg.setNegativeButton("No", null);
                dlg.create().show();
                break;
            case R.id.pickpath_btn:
                pickPath();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case 100:
                    Uri uri = data.getData();
                    if (uri != null){
                        lastPath = getAbsPathFromUri(uri);
                        SharedPreferences record = getSharedPreferences(PREFS_NAME, 0);
                        if (record.getString(lastPath, "").equals("")){
                            path_tv.setText(lastPath);
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
                            editor.putString("lastPath", lastPath);
                            editor.apply();
                            path_tv.setText(lastPath);
                            tail_et.setVisibility(View.VISIBLE);
                            commit_btn.setText("Start Tailing");
                        }else{
                            path_tv.setText(lastPath);
                            tail_et.setVisibility(View.GONE);
                            commit_btn.setText("Remove Tails");
                        }
                    }
                    break;
            }
        }
    }

    private void deTailFile(File file){
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; ++i)
                    deTailFile(files[i]);
            }
        } else{
            if (file.getAbsolutePath().substring(file.getAbsolutePath().length() - tail.length()).equals(tail)){
                File newFile = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - tail.length()));
                if (!file.renameTo(newFile)){
                    errs.add("\n" + file.getPath());
                }else{
                    ++doneCount;
                }
            }
        }
    }

    private void tailFile(File file){
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; ++i)
                    tailFile(files[i]);
            }
        } else{
            if (!file.getAbsolutePath().substring(file.getAbsolutePath().length() - tail.length()).equals(tail)){
                File newFile = new File(file.getAbsolutePath() + tail);
                if (!file.renameTo(newFile)){
                    errs.add("\n" + file.getPath());
                }else{
                    ++doneCount;
                }
            }
        }
    }

    private void pickPath() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, 100);
    }

    private String getAbsPathFromUri(Uri uri){
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + uri.getPath().substring(uri.getPath().indexOf(":")+1);
    }

}
