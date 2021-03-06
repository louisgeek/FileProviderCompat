package com.louisgeek.fileprovidercompatdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.louisgeek.FileProviderCompat.FileProviderCompat;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final int REQUEST_CODE_INSTALL_APK = 111;
    private static final int REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        installApk(this, "/storage/emulated/0/Download/mytest-6.apk");
    }

    /**
     * @param filePath not startWith  file://  or  content://
     *                 like  /storage/emulated/0/Download/mytest-6.apk
     */
    public void installApk(final FragmentActivity fragmentActivity, String filePath) {
        if (fragmentActivity == null) {
            Log.e(TAG, "installApk: fragmentActivity is null");
            return;
        }
        if (TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "installApk: error");
            return;
        }
        //
        try {
            //提升一下文件的读写权限,否则在安装的时候会出现apk解析失败的页面
            Runtime.getRuntime().exec("chmod 777 " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //兼容8.0 未知应用安装权限放到了单个应用范畴了
            // xml 必须加 android.permission.REQUEST_INSTALL_PACKAGES 权限
            Log.e(TAG, "installN: >= Build.VERSION_CODES.O " + filePath);
            boolean canRequestPackageInstalls = fragmentActivity.getPackageManager().canRequestPackageInstalls();
            if (canRequestPackageInstalls) {
                //如果已经勾选了 即可和 8以下 一样安装
                installApkCompat(fragmentActivity, filePath);
            } else {
                //设置-允许安装未知来源
                new AlertDialog.Builder(fragmentActivity)
                        .setTitle("温馨提示")
                        .setMessage("安装应用需要打开未知来源权限，请去设置中开启权限")
                        .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /* Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                                fragmentActivity.startActivityForResult(intent, REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES);*/
                                String packageName = fragmentActivity.getPackageName();
                                //直接跳转到对应APP的未知来源权限设置界面
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + packageName));
                                fragmentActivity.startActivityForResult(intent, REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            }
                        }).create()
                        .show();
            }
        } else {
            installApkCompat(fragmentActivity, filePath);
        }
    }

    private void installApkCompat(FragmentActivity fragmentActivity, String filePath) {
        File file = new File(filePath);
        if (fragmentActivity == null) {
            Log.e(TAG, "installCompat: fragmentActivity is null");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //here
        FileProviderCompat.setDataAndTypeAddFlags(fragmentActivity, intent, APK_MIME_TYPE, file);
        fragmentActivity.startActivity(intent);
//        fragmentActivity.startActivityForResult(intent, REQUEST_CODE_INSTALL_APK);
    }
}
