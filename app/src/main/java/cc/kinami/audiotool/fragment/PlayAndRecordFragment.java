package cc.kinami.audiotool.fragment;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cc.kinami.audiotool.R;
import cc.kinami.audiotool.util.WavPlayer;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;

public class PlayAndRecordFragment extends Fragment {

    private static final String TAG = "[Beep]Record&PlayFrag";

    private View view;

    private TextView status;
    private final StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
            status.setText("正在录音");
        }

        @Override
        public void onRecordData(short[] data, int length) {
            Log.d("MainActivity", "current buffer size is " + length);
        }

        @Override
        public void onVoiceVolume(int volume) {
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            status.setText("录音错误");
            Toast.makeText(getContext(), "error: " + errorMsg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(getContext(), "文件保存失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(getContext(), "文件保存成功,路径是" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            status.setText("等待录音");
        }
    };

    private Spinner recordSourceSpinner;
    private IdealRecorder idealRecorder;
    private IdealRecorder.RecordConfig recordConfig;
    private WavPlayer wavPlayer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_play_and_record, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        status = view.findViewById(R.id.status);
        idealRecorder = IdealRecorder.getInstance();
        recordSourceSpinner = view.findViewById(R.id.record_source_choose);
        Button recordBtn = view.findViewById(R.id.record_btn);
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

        Button playBtn = view.findViewById(R.id.play_btn);
        playBtn.setText("播放chirp");
        playBtn.setOnClickListener(v -> playWav());

        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.CAMCORDER,
                IdealRecorder.RecordConfig.SAMPLE_RATE_44K_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        wavPlayer = new WavPlayer();
    }


    /**
     * 开始录音
     */
    private void record() {
        int recordSource = recordSourceSpinner.getSelectedItemId() == 0
                ? MediaRecorder.AudioSource.MIC
                : MediaRecorder.AudioSource.CAMCORDER;
        android.util.Log.i(TAG, "record: source: " + (recordSource == MediaRecorder.AudioSource.MIC ? "mic" : "camcorder"));
        recordConfig = new IdealRecorder.RecordConfig(recordSource,
                IdealRecorder.RecordConfig.SAMPLE_RATE_44K_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

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
        File file = new File(Environment.getExternalStorageDirectory(), "Audio Tools");
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

    public void playWav() {
        if (wavPlayer.isWorking)
            return;
        wavPlayer.playWav("Audio Tools/chirp_for_experiment.wav", null);
    }


}
