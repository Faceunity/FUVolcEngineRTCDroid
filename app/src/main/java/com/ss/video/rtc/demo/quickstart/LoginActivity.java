package com.ss.video.rtc.demo.quickstart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ss.bytertc.engine.RTCEngine;
import com.ss.bytertc.engine.utils.LogUtil;
import com.ss.rtc.demo.quickstart.R;

import java.util.List;
import java.util.regex.Pattern;

/**
 * VolcEngineRTC 音视频通话入口页面
 *
 * 包含如下简单功能：
 * - 该页面用来跳转至音视频通话主页面
 * - 申请相关权限
 * - 校验房间名和用户名
 * - 展示当前 SDK 使用的版本号 {@link RTCEngine#getSdkVersion()}
 *
 * 有以下常见的注意事项：
 * 1.SDK必要的权限有：外部内存读写、摄像头权限、麦克风权限，其余完整的权限参见{@link src/main/AndroidManifest.xml}。
 * 没有这些权限不会导致崩溃，但是会影响SDK的正常使用。
 * 2.SDK 对房间名、用户名的限制是：非空且最大长度不超过128位的数字、大小写字母、@ . _ \ -
 */
public class LoginActivity extends AppCompatActivity {

    private String currentToken = Constants.TOKEN_2;
    private String currentUserId = Constants.USER_ID_2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText roomInput = findViewById(R.id.room_id_input);
        EditText userInput = findViewById(R.id.user_id_input);

        roomInput.setText(Constants.ROOM_ID);
        userInput.setText(currentUserId);
        findViewById(R.id.tvToken1).setOnClickListener(v -> {
            Toast.makeText(this, "token1", Toast.LENGTH_SHORT).show();
            currentToken = Constants.TOKEN_1;
            currentUserId = Constants.USER_ID_1;
            userInput.setText(currentUserId);
        });
        findViewById(R.id.tvToken2).setOnClickListener(v -> {
            Toast.makeText(this, "token2", Toast.LENGTH_SHORT).show();
            currentToken = Constants.TOKEN_2;
            currentUserId = Constants.USER_ID_2;
            userInput.setText(currentUserId);
        });

        findViewById(R.id.join_room_btn).setOnClickListener((v) -> {
            String roomId = roomInput.getText().toString();
            String userId = userInput.getText().toString();
            joinChannel(roomId, userId);
        });
        // 获取当前SDK的版本号
        String SDKVersion = RTCEngine.getSdkVersion();
        TextView versionTv = findViewById(R.id.version_tv);
        versionTv.setText(String.format("VolcEngineRTC v%s", SDKVersion));
        LogUtil.setDebug(true);
        LogUtil.setLogLevel(LogUtil.LogLevel.LOG_LEVEL_TRACE);
        FURenderer.getInstance().setup(getApplicationContext());
        FURenderer.getInstance().setMarkFPSEnable(true);
//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
//                1000);

        XXPermissions.with(this)
                // 申请多个权限
                .permission(Permission.Group.STORAGE)
                .permission(Permission.READ_EXTERNAL_STORAGE,
                        Permission.WRITE_EXTERNAL_STORAGE, Permission.CAMERA, Permission.RECORD_AUDIO)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                    }
                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                    }
                });
        FUConfig.DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevelGPU();
    }

    private void joinChannel(String roomId, String userId) {
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "房间号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Pattern.matches(Constants.INPUT_REGEX, roomId)) {
            Toast.makeText(this, "房间号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Pattern.matches(Constants.INPUT_REGEX, userId)) {
            Toast.makeText(this, "用户名格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RTCRoomActivity.class);
        intent.putExtra(Constants.ROOM_ID_EXTRA, roomId);
        intent.putExtra(Constants.USER_ID_EXTRA, userId);
        intent.putExtra(Constants.SDK_TOKEN, currentToken);
        startActivity(intent);
    }
}