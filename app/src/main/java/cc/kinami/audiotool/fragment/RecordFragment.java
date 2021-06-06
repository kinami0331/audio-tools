package cc.kinami.audiotool.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import cc.kinami.audiotool.R;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;

public class RecordFragment extends Fragment {

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
            status.setText("录音错误" + errorMsg);
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
            status.setText("点击下方按钮以开始录音");
        }
    };
    private IdealRecorder idealRecorder;
    private IdealRecorder.RecordConfig recordConfig;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }
}
