package cc.kinami.audiotool;


import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.kinami.audiotool.widget.WaveView;
import jaygoo.widget.wlv.WaveLineView;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;

public class MainActivity extends AppCompatActivity {


    private final Rationale<java.util.List<java.lang.String>> rationale = (context, data, executor) -> {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("提示")
                .setMessage("录制声音需要录音和麦克风权限")
                .setPositiveButton("允许", (dialog, which) -> executor.execute())
                .setNegativeButton("拒绝", (dialog, which) -> executor.execute())
                .create();
        alertDialog.show();
    };
    private WaveView waveView;
    private WaveLineView waveLineView;
    private TextView tips;
    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
            waveLineView.startAnim();
            tips.setText("正在录音");
        }

        @Override
        public void onRecordData(short[] data, int length) {

            for (int i = 0; i < length; i += 60) {
                waveView.addData(data[i]);
            }
            Log.d("MainActivity", "current buffer size is " + length);
        }

        @Override
        public void onVoiceVolume(int volume) {
            double myVolume = (volume - 40) * 4;
            waveLineView.setVolume((int) myVolume);
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            tips.setText("录音错误" + errorMsg);
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(MainActivity.this, "文件保存失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(MainActivity.this, "文件保存成功,路径是" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            tips.setText("点击下方按钮以开始录音");
            waveLineView.stopAnim();
        }
    };
    private IdealRecorder idealRecorder;
    private IdealRecorder.RecordConfig recordConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
         * 准备录音 录音之前 先判断是否有相关权限
         */
        getPermission();

        Button recordBtn = findViewById(R.id.register_record_btn);
        waveView = findViewById(R.id.wave_view);
        waveLineView = findViewById(R.id.waveLineView);
        tips = findViewById(R.id.tips);
        idealRecorder = IdealRecorder.getInstance();
        recordBtn.setText("开始录音");
        recordBtn.setOnClickListener(v -> {
            // 激活状态说明正在录音
            if (recordBtn.isActivated()) {
                stopRecord();
                recordBtn.setText("开始录音");
                recordBtn.setActivated(false);
            } else {
                record();
                recordBtn.setText("停止录音");
                recordBtn.setActivated(true);
            }
        });


        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC,
                IdealRecorder.RecordConfig.SAMPLE_RATE_44K_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

    }

    /**
     * 开始录音
     */
    private void record() {
        //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
        idealRecorder.setRecordFilePath(getSaveFilePath());
//        idealRecorder.setWavFormat(false);
        //设置录音配置 最长录音时长 以及音量回调的时间间隔
        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(60000).setVolumeInterval(200);
        //设置录音时各种状态的监听
        idealRecorder.setStatusListener(statusListener);
        idealRecorder.start(); //开始录音

    }

    /**
     * 获取文件保存路径
     *
     * @return 文件的保存路径
     */
    private String getSaveFilePath() {
        File file = new File(Environment.getExternalStorageDirectory(), "Audio");
        if (!file.exists()) {
            if (!file.mkdirs())
                throw new IllegalStateException("创建文件夹失败");
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss", Locale.CHINA);
        String timeStr = dateFormat.format(new Date());
        File wavFile = new File(file, timeStr + ".wav");
        return wavFile.getAbsolutePath();
    }


    /**
     * 停止录音
     */
    private void stopRecord() {
        //停止录音
        idealRecorder.stop();
    }

    private void getPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.MICROPHONE, Permission.Group.STORAGE)
                .rationale(rationale)
                .onDenied(permissions -> {
                    if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions))
                        System.out.println("哈哈哈");
                })
                .start();
    }

    public void tabView(View view) {
        startActivity(new Intent(this, TabTestActivity.class));
    }
}