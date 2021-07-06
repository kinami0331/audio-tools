package cc.kinami.audiotool.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import cc.kinami.audiotool.R;
import cc.kinami.audiotool.exception.KnownException;
import cc.kinami.audiotool.util.BeepHttpClient;
import cc.kinami.audiotool.util.ErrorDialog;

public class ExperimentType2ControlFragment extends Fragment {
    public static final String TAG = "[Beep]Type2CtrlFrag";
    View view;
    EditText serverAddrEditText;
    EditText signalIdEditText;
    EditText tarDeviceEditText;
    EditText masterDeviceEditText;
    EditText tarMicEditText;
    Button startExperimentButton;
    TextView experimentLogInfoText;
    Handler handler;

    ImageView figure1ImageView;
    ImageView figure2ImageView;
    ImageView figure3ImageView;
    ImageView figure4ImageView;
    ImageView figure5ImageView;
    ImageView figure6ImageView;
    ImageView figure7ImageView;
    ImageView figure8ImageView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_experiment_type2_control, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        serverAddrEditText = view.findViewById(R.id.server_address_input);
        signalIdEditText = view.findViewById(R.id.signal_id_text);
        tarDeviceEditText = view.findViewById(R.id.tar_device_name_input);
        masterDeviceEditText = view.findViewById(R.id.master_device_name_input);
        startExperimentButton = view.findViewById(R.id.start_experiment);
        tarMicEditText = view.findViewById(R.id.tar_mic_text);
        experimentLogInfoText = view.findViewById(R.id.experiment_log_info);
        figure1ImageView = view.findViewById(R.id.figure_1);
        figure2ImageView = view.findViewById(R.id.figure_2);
        figure3ImageView = view.findViewById(R.id.figure_3);
        figure4ImageView = view.findViewById(R.id.figure_4);
        figure5ImageView = view.findViewById(R.id.figure_5);
        figure6ImageView = view.findViewById(R.id.figure_6);
        figure7ImageView = view.findViewById(R.id.figure_7);
        figure8ImageView = view.findViewById(R.id.figure_8);


        Log.i(TAG, "onActivityCreated: this model:" + Build.MODEL);
        if (Build.MODEL.equals("H60-L01")) {
            tarDeviceEditText.setText("Z".toCharArray(), 0, "Z".length());
            masterDeviceEditText.setText("A".toCharArray(), 0, "A".length());
        } else {
            masterDeviceEditText.setText("Z".toCharArray(), 0, "Z".length());
            tarDeviceEditText.setText("A".toCharArray(), 0, "A".length());
        }
        startExperimentButton.setOnClickListener(v -> startExperiment());
        handler = new WorkHandler(this);
    }

    void startExperiment() {
        startExperimentButton.setEnabled(false);
        experimentLogInfoText.setText("实验开始...");
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("正在进行中")
                .setCancelable(false)
                .setNegativeButton("中断操作", (dialog, which) -> {
                })
                .create();
        final Thread workThread = new Thread(() -> {
            try {
                String serverAddr = serverAddrEditText.getEditableText().toString();

                // 构造Json
                Map<String, Object> postBody = new HashMap<>();
                postBody.put("deviceMic", Integer.valueOf(tarMicEditText.getEditableText().toString()));
                postBody.put("deviceName", tarDeviceEditText.getEditableText().toString());
                postBody.put("masterDeviceName", masterDeviceEditText.getEditableText().toString());
                postBody.put("signalId", Integer.valueOf(signalIdEditText.getEditableText().toString()));

                ObjectMapper mapper = new ObjectMapper();
                Log.i(TAG, "startExperimentWithTwoDevices: postBody = " + mapper.writeValueAsString(postBody));
                // 创建实验
                JsonNode response = BeepHttpClient.post("http://" + serverAddr + "/api/experiment/type2/create",
                        mapper.readTree(mapper.writeValueAsString(postBody)));
                Log.i(TAG, "startExperimentWithTwoDevices: create response = " + mapper.writeValueAsString(response));

                // 获取实验id
                int experimentId = response.get("experimentId").asInt();
                // 通知实验id
                Message message = Message.obtain();
                message.what = WorkHandler.EXPERIMENT_INFO;
                message.obj = "当前实验编号：" + experimentId;
                message.obj = "当前实验编号：" + experimentId;
                handler.sendMessage(message);

                // 获取实验文件夹地址
                String experimentPath = "http://" + serverAddr +
                        response.get("experimentPath").asText().replace("./", "/")
                        + experimentId + "/";

                // 获取signal
                String signalFileUrl = "http://" + serverAddr + response.get("signalPath").asText().replace("./", "/");
                Log.i(TAG, "getSignal: signalFileUrl = " + signalFileUrl);
                BeepHttpClient.downLoad(signalFileUrl,
                        Environment.getExternalStorageDirectory() + "/Audio Tools/experiments",
                        "signal.wav");

                // 开始实验
                Map<String, Integer> tmpMap = new HashMap<>();
                tmpMap.put("experimentId", experimentId);
                String tmpStr = mapper.writeValueAsString(tmpMap);
                JsonNode tmpNode = mapper.readTree(tmpStr);
                response = BeepHttpClient.post("http://" + serverAddr + "/api/experiment/type2/begin", tmpNode);
                Log.i(TAG, "startExperiment: begin response = " + mapper.writeValueAsString(response));
//
                // 通知获取实验结果
                message = Message.obtain();
                message.what = WorkHandler.SHOW_IMAGE;
                message.obj = experimentPath;
                handler.sendMessage(message);

            } catch (KnownException e) {
                ErrorDialog.showDiaglog(getContext(), e.getErrMsg());
            } catch (JsonProcessingException e) {
                Log.e(TAG, "startExperimentWithTwoDevices: Json error", e);
                throw new RuntimeException("Json错了，这不合理");
            } catch (Exception e) {
                Log.e(TAG, "startExperimentWithTwoDevices: unknown error", e);
            } finally {
                Message message = Message.obtain();
                message.what = WorkHandler.EXPERIMENT_FINIEH;
                handler.sendMessage(message);
                alertDialog.cancel();
            }
        });
        alertDialog.setOnShowListener((dialog) -> {
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener((e) -> {
                if (workThread.getState() == Thread.State.NEW)
                    return;
                workThread.interrupt();
                startExperimentButton.setEnabled(true);
                alertDialog.cancel();
            });
        });
        alertDialog.show();
        workThread.start();
    }

    private static class WorkHandler extends Handler {
        public static final int EXPERIMENT_FINIEH = 0;
        public static final int EXPERIMENT_INFO = 1;
        public static final int SHOW_IMAGE = 2;
        WeakReference<ExperimentType2ControlFragment> experimentType2ControlFragmentWeakReference;

        public WorkHandler(ExperimentType2ControlFragment experimentType2ControlFragment) {
            this.experimentType2ControlFragmentWeakReference = new WeakReference<>(experimentType2ControlFragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            ExperimentType2ControlFragment experimentType2ControlFragment = experimentType2ControlFragmentWeakReference.get();
            if (experimentType2ControlFragment == null) {
                throw new RuntimeException("这不合理");
            }
            switch (msg.what) {
                case EXPERIMENT_FINIEH:
                    Log.i(TAG, "handleMessage: experiment finish ");
                    experimentType2ControlFragment.startExperimentButton.setEnabled(true);
                    break;
                case EXPERIMENT_INFO:
                    String preText = experimentType2ControlFragment.experimentLogInfoText.getText().toString();
                    experimentType2ControlFragment.experimentLogInfoText.setText(preText + "\n" + msg.obj.toString());
                    break;
                case SHOW_IMAGE:
                    String[] imageList = {"original_signal.png", "received_signal.png",
                            "original_signal_fft.png", "received_signal_fft.png",
                            "original_signal_STFT.png", "received_signal_STFT.png",
                            "original_signal_power_frequncy.png", "received_signal_power_frequncy.png"};
                    String path = msg.obj.toString();
                    Log.i(TAG, "handleMessage: " + path + "original_signal.png");
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "original_signal.png")
                            .into(experimentType2ControlFragment.figure1ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "received_signal.png")
                            .into(experimentType2ControlFragment.figure2ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "original_signal_fft.png")
                            .into(experimentType2ControlFragment.figure3ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "received_signal_fft.png")
                            .into(experimentType2ControlFragment.figure4ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "original_signal_STFT.png")
                            .into(experimentType2ControlFragment.figure5ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "received_signal_STFT.png")
                            .into(experimentType2ControlFragment.figure6ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "original_signal_power_frequncy.png")
                            .into(experimentType2ControlFragment.figure7ImageView);
                    Glide.with(experimentType2ControlFragment)
                            .load(path + "received_signal_power_frequncy.png")
                            .into(experimentType2ControlFragment.figure8ImageView);

                    break;
                default:
                    throw new RuntimeException("这不合理");
            }
        }
    }
}
