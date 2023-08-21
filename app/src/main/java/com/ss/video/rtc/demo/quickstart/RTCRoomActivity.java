package com.ss.video.rtc.demo.quickstart;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.core.callback.OperateCallback;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.FURenderManager;
import com.faceunity.core.utils.FULogger;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.authpack;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.listener.FURendererListener;
import com.faceunity.nama.ui.FaceUnityView;
import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.AudioRoute;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.RemoteStreamKey;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.data.VideoFrameInfo;
import com.ss.bytertc.engine.data.VideoPixelFormat;
import com.ss.bytertc.engine.handler.IRTCVideoEventHandler;
import com.ss.bytertc.engine.type.AudioScenarioType;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.utils.LogUtil;
import com.ss.bytertc.engine.video.VideoPreprocessorConfig;
import com.ss.rtc.demo.quickstart.R;
import com.ss.video.rtc.demo.quickstart.profile.CSVUtils;
import com.ss.video.rtc.demo.quickstart.profile.Constant;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * VolcEngineRTC 视频通话的主页面
 * 本示例不限制房间内最大用户数；同时最多渲染四个用户的视频数据（自己和三个远端用户视频数据）；
 *
 * 包含如下简单功能：
 * - 创建引擎
 * - 设置视频发布参数
 * - 渲染自己的视频数据
 * - 创建房间
 * - 加入音视频通话房间
 * - 打开/关闭麦克风
 * - 打开/关闭摄像头
 * - 渲染远端用户的视频数据
 * - 离开房间
 * - 销毁引擎
 *
 * 实现一个基本的音视频通话的流程如下：
 * 1.创建 IRTCVideo 实例。
 *   RTCVideo.createRTCVideo(Context context, String appId, IRTCVideoEventHandler handler,
 *     Object eglContext, JSONObject parameters)
 * 2.视频发布端设置期望发布的最大分辨率视频流参数，包括分辨率、帧率、码率、缩放模式、网络不佳时的回退策略等。
 *   RTCVideo.setVideoEncoderConfig(VideoEncoderConfig maxSolution)
 * 3.开启本地视频采集。 RTCVideo.startVideoCapture()
 * 4.设置本地视频渲染时，使用的视图，并设置渲染模式。
 *   RTCVideo.setLocalVideoCanvas(StreamIndex streamIndex, VideoCanvas videoCanvas)
 * 5.创建房间。RTCVideo.createRTCRoom(String roomId)
 * 6.加入音视频通话房间。
 *   RTCRoom.joinRoom(String token, UserInfo userInfo, RTCRoomConfig roomConfig)
 * 7.SDK 接收并解码远端视频流首帧后，设置用户的视频渲染视图。
 *   RTCVideo.setRemoteVideoCanvas(String userId, StreamIndex streamIndex, VideoCanvas videoCanvas)
 * 8.在用户离开房间之后移出用户的视频渲染视图。
 * 9.离开音视频通话房间。RTCRoom.leaveRoom()
 * 10.调用 RTCRoom.destroy() 销毁房间对象。
 * 11.调用 RTCVideo.destroyRTCVideo() 销毁引擎。
 *
 * 详细的API文档参见{https://www.volcengine.com/docs/6348/70080}
 */
public class RTCRoomActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "RTCRoomActivity";

    private ImageView mSpeakerIv;
    private ImageView mAudioIv;
    private ImageView mVideoIv;

    private boolean mIsSpeakerPhone = true;
    private boolean mIsMuteAudio = false;
    private boolean mIsMuteVideo = false;
    private CameraId mCameraID = CameraId.CAMERA_ID_FRONT;

    private FrameLayout mSelfContainer;
    private final FrameLayout[] mRemoteContainerArray = new FrameLayout[3];
    private final TextView[] mUserIdTvArray = new TextView[3];
    private final String[] mShowUidArray = new String[3];

    private RTCVideo mRTCVideo;
    private RTCRoom mRTCRoom;

    private String currentToken;

    private FURenderer mFURenderer = FURenderer.getInstance();
    private FaceUnityDataFactory mFaceUnityDataFactory;

    private FrameLayout mRemoteViewContainer;
    private TextView mTrackingText;
    private TextView mTvFace;

    private SensorManager mSensorManager;

    private RTCRoomEventHandlerAdapter mIRtcRoomEventHandler = new RTCRoomEventHandlerAdapter() {

        /**
         * 远端主播角色用户加入房间回调。
         */
        @Override
        public void onUserJoined(UserInfo userInfo, int elapsed) {
            super.onUserJoined(userInfo, elapsed);
            Log.d("IRTCRoomEventHandler", "onUserJoined: " + userInfo.getUid());
        }

        /**
         * 远端用户离开房间回调。
         */
        @Override
        public void onUserLeave(String uid, int reason) {
            super.onUserLeave(uid, reason);
            Log.d("IRTCRoomEventHandler", "onUserLeave: " + uid);
            runOnUiThread(() -> removeRemoteView(uid));
        }

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            Log.d("RTCRoomActivity", "onRoomStateChanged: " + roomId + " " + uid + " " + state + " " + extraInfo);
        }
    };

    private IRTCVideoEventHandler mIRtcVideoEventHandler = new IRTCVideoEventHandler() {

        /**
         * SDK收到第一帧远端视频解码数据后，用户收到此回调。
         */
        @Override
        public void onFirstRemoteVideoFrameDecoded(RemoteStreamKey remoteStreamKey, VideoFrameInfo frameInfo) {
            super.onFirstRemoteVideoFrameDecoded(remoteStreamKey, frameInfo);
            Log.d("IRTCVideoEventHandler", "onFirstRemoteVideoFrame: " + remoteStreamKey.toString());
            runOnUiThread(() -> setRemoteView(remoteStreamKey.getRoomId(), remoteStreamKey.getUserId()));
        }

        /**
         * 警告回调，详细可以看 {https://www.volcengine.com/docs/6348/70082#warncode}
         */
        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.d("IRTCVideoEventHandler", "onWarning: " + warn);
        }

        /**
         * 错误回调，详细可以看 {https://www.volcengine.com/docs/6348/70082#errorcode}
         */
        @Override
        public void onError(int err) {
            super.onError(err);
            Log.d("IRTCVideoEventHandler", "onError: " + err);
            showAlertDialog(String.format(Locale.US, "error: %d", err));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent intent = getIntent();
        String roomId = intent.getStringExtra(Constants.ROOM_ID_EXTRA);
        String userId = intent.getStringExtra(Constants.USER_ID_EXTRA);
        currentToken = intent.getStringExtra(Constants.SDK_TOKEN);

        initUI(roomId, userId);
        initEngineAndJoinRoom(roomId, userId);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void initUI(String roomId, String userId) {
        mSelfContainer = findViewById(R.id.self_video_container);
        mRemoteContainerArray[0] = findViewById(R.id.remote_video_0_container);
        mRemoteContainerArray[1] = findViewById(R.id.remote_video_1_container);
        mRemoteContainerArray[2] = findViewById(R.id.remote_video_2_container);
        mUserIdTvArray[0] = findViewById(R.id.remote_video_0_user_id_tv);
        mUserIdTvArray[1] = findViewById(R.id.remote_video_1_user_id_tv);
        mUserIdTvArray[2] = findViewById(R.id.remote_video_2_user_id_tv);
        findViewById(R.id.switch_camera).setOnClickListener((v) -> onSwitchCameraClick());
        mSpeakerIv = findViewById(R.id.switch_audio_router);
        mAudioIv = findViewById(R.id.switch_local_audio);
        mVideoIv = findViewById(R.id.switch_local_video);
        findViewById(R.id.hang_up).setOnClickListener((v) -> onBackPressed());
        mSpeakerIv.setOnClickListener((v) -> updateSpeakerStatus());
        mAudioIv.setOnClickListener((v) -> updateLocalAudioStatus());
        mVideoIv.setOnClickListener((v) -> updateLocalVideoStatus());
        TextView roomIDTV = findViewById(R.id.room_id_text);
        TextView userIDTV = findViewById(R.id.self_video_user_id_tv);
        roomIDTV.setText(String.format("RoomID:%s", roomId));
        userIDTV.setText(String.format("UserID:%s", userId));

        FaceUnityView faceUnityView = findViewById(R.id.faceUnityView);
        mTvFace = findViewById(R.id.tv_face);
        mFaceUnityDataFactory = new FaceUnityDataFactory(-1);
        faceUnityView.bindDataFactory(mFaceUnityDataFactory);
        mFURenderer.bindListener(new FURendererListener() {
            @Override
            public void onPrepare() {
                initCsvUtil(RTCRoomActivity.this);
                mFaceUnityDataFactory.bindCurrentRenderer();
            }

            @Override
            public void onTrackStatusChanged(FUAIProcessorEnum type, int status) {
                runOnUiThread(() -> {
                    mTvFace.setText(type == FUAIProcessorEnum.FACE_PROCESSOR ? R.string.toast_not_detect_face : R.string.toast_not_detect_body);
                    mTvFace.setVisibility(status > 0 ? View.INVISIBLE : View.VISIBLE);
                });
            }

            @Override
            public void onFpsChanged(double fps, double callTime) {
//                Log.d(TAG, "onFpsChanged: fps: " + fps + " ,callTime:" + callTime);
            }

            @Override
            public void onProcessTime(long processTime) {
                if (mCSVUtils != null) {
                    mCSVUtils.writeCsv(null, processTime);
                }
            }

            @Override
            public void onRelease() {

            }
        });
    }

    private void initEngineAndJoinRoom(String roomId, String userId) {
        // 创建引擎
        mRTCVideo = RTCVideo.createRTCVideo(getApplicationContext(), Constants.APPID, mIRtcVideoEventHandler, null, null);
        // 设置音频场景类型（需要调用 setAudioScenario 将音频场景切换为 AUDIO_SCENARIO_COMMUNICATION 后再调用 setAudioRoute()）
        mRTCVideo.setAudioScenario(AudioScenarioType.AUDIO_SCENARIO_COMMUNICATION);
        // 设置视频发布参数
        VideoEncoderConfig videoEncoderConfig = new VideoEncoderConfig(360, 640, 30, 800, 0);
        mRTCVideo.setVideoEncoderConfig(videoEncoderConfig);
        setLocalRenderView(userId);
        setVideoProcessor();
        // 加入房间
        mRTCRoom = mRTCVideo.createRTCRoom(roomId);
        mRTCRoom.setRTCRoomEventHandler(mIRtcRoomEventHandler);
        RTCRoomConfig roomConfig = new RTCRoomConfig(ChannelProfile.CHANNEL_PROFILE_COMMUNICATION,
                true, true, true);
        int joinRoomRes = mRTCRoom.joinRoom(currentToken,
                UserInfo.create(userId, ""), roomConfig);
        Log.d("RTCRoomActivity", "initEngineAndJoinRoom,userId:"+userId+",roomId:"+roomId+",token:"+currentToken+",joinRoomRes:" + joinRoomRes);
    }

    private void setLocalRenderView(String uid) {
        VideoCanvas videoCanvas = new VideoCanvas();
        TextureView renderView = new TextureView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mSelfContainer.removeAllViews();
        mSelfContainer.addView(renderView, params);
        videoCanvas.renderView = renderView;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置本地视频渲染视图
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRTCVideo != null) {
            // 开启本地视频采集
            mRTCVideo.startVideoCapture();
            // 开启本地音频采集
            mRTCVideo.startAudioCapture();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRTCVideo != null) {
            mRTCVideo.stopAudioCapture();
            mRTCVideo.stopVideoCapture();
        }
    }

    private SimpleVideoProcessor mCustomVideoProcessor;

    private void setVideoProcessor(){
        VideoPreprocessorConfig config = new VideoPreprocessorConfig();
        config.required_pixel_format = VideoPixelFormat.kVideoPixelFormatTexture2D;
        if(mCustomVideoProcessor == null) {
            mCustomVideoProcessor = new SimpleVideoProcessor();
            mCustomVideoProcessor.setRenderEnable(true);
        }
        mRTCVideo.registerLocalVideoProcessor(mCustomVideoProcessor, config);
    }

    private void setRemoteRenderView(String roomId, String uid, FrameLayout container) {
        TextureView renderView = new TextureView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        container.removeAllViews();
        container.addView(renderView, params);

        RemoteStreamKey streamKey = new RemoteStreamKey(roomId, uid, StreamIndex.STREAM_INDEX_MAIN);

        VideoCanvas videoCanvas = new VideoCanvas();
        videoCanvas.renderView = renderView;
        videoCanvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
        // 设置远端用户视频渲染视图
        mRTCVideo.setRemoteVideoCanvas(streamKey, videoCanvas);
    }

    private void setRemoteView(String roomId, String uid) {
        int emptyInx = -1;
        for (int i = 0; i < mShowUidArray.length; i++) {
            if (TextUtils.isEmpty(mShowUidArray[i]) && emptyInx == -1) {
                emptyInx = i;
            } else if (TextUtils.equals(uid, mShowUidArray[i])) {
                return;
            }
        }
        if (emptyInx < 0) {
            return;
        }
        mShowUidArray[emptyInx] = uid;
        mUserIdTvArray[emptyInx].setText(String.format("UserId:%s", uid));
        setRemoteRenderView(roomId, uid, mRemoteContainerArray[emptyInx]);
    }

    private void removeRemoteView(String uid) {
        for (int i = 0; i < mShowUidArray.length; i++) {
            if (TextUtils.equals(uid, mShowUidArray[i])) {
                mShowUidArray[i] = null;
                mUserIdTvArray[i].setText(null);
                mRemoteContainerArray[i].removeAllViews();
            }
        }
    }

    private void onSwitchCameraClick() {


        // 切换前置/后置摄像头（默认使用前置摄像头）
        if (mCameraID.equals(CameraId.CAMERA_ID_FRONT)) {
            mCameraID = CameraId.CAMERA_ID_BACK;
        } else {
            mCameraID = CameraId.CAMERA_ID_FRONT;
        }
        mRTCVideo.switchCamera(mCameraID);
    }

    private void updateSpeakerStatus() {
        mIsSpeakerPhone = !mIsSpeakerPhone;
        /*
        设置使用哪种方式播放音频数据
        你需要调用 setAudioScenario 将音频场景切换为 AUDIO_SCENARIO_COMMUNICATION 后再调用本接口。
        连接有线或者蓝牙音频播放设备后，音频路由将自动切换至此设备。
        移除后，音频设备会自动切换回原设备。
         */
        mRTCVideo.setDefaultAudioRoute(mIsSpeakerPhone ? AudioRoute.AUDIO_ROUTE_SPEAKERPHONE
                : AudioRoute.AUDIO_ROUTE_EARPIECE);
        mSpeakerIv.setImageResource(mIsSpeakerPhone ? R.drawable.speaker_on : R.drawable.speaker_off);
    }

    private void updateLocalAudioStatus() {
        mIsMuteAudio = !mIsMuteAudio;
        // 开启/关闭本地音频发送
        if (mIsMuteAudio) {
            mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
        } else {
            mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
        }
        mAudioIv.setImageResource(mIsMuteAudio ? R.drawable.mute_audio : R.drawable.normal_audio);
    }

    private void updateLocalVideoStatus() {
        mIsMuteVideo = !mIsMuteVideo;
        if (mIsMuteVideo) {
            // 关闭视频采集
            mRTCVideo.stopVideoCapture();
        } else {
            // 开启视频采集
            mRTCVideo.startVideoCapture();
        }
        mVideoIv.setImageResource(mIsMuteVideo ? R.drawable.mute_video : R.drawable.normal_video);
    }

    private void showAlertDialog(String message) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message);
            builder.setPositiveButton("知道了", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mCustomVideoProcessor != null) {
            mCustomVideoProcessor.mFURenderer.removeListener();
        }
        mSensorManager.unregisterListener(this);

        // 离开房间
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom.destroy();
        }
        mRTCRoom = null;

        // 销毁引擎
        RTCVideo.destroyRTCVideo();
        mIRtcVideoEventHandler = null;
        mIRtcRoomEventHandler = null;
        mRTCVideo = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.setDeviceOrientation(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.setDeviceOrientation(y > 0 ? 90 : 270);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private CSVUtils mCSVUtils;
    //性能测试部分
    private void initCsvUtil(Context context) {
        mCSVUtils = new CSVUtils(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String dateStrDir = format.format(new Date(System.currentTimeMillis()));
        dateStrDir = dateStrDir.replaceAll("-", "").replaceAll("_", "");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String dateStrFile = df.format(new Date());
        String filePath = Constant.filePath + dateStrDir + File.separator + "excel-" + dateStrFile + ".csv";
        Log.d(TAG, "initLog: CSV file path:" + filePath);
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("version：").append(FURenderer.getInstance().getVersion()).append(CSVUtils.COMMA)
                .append("机型：").append(android.os.Build.MANUFACTURER).append(android.os.Build.MODEL).append(CSVUtils.COMMA)
                .append("处理方式：双输入纹理输出").append(CSVUtils.COMMA)
                .append("编码方式：硬件编码").append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }
}