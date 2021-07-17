package cc.kinami.audiotool.fragment;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import cc.kinami.audiotool.R;
import cc.kinami.audiotool.model.ControlInfo;
import cc.kinami.audiotool.model.ProcessControlEnum;
import cc.kinami.audiotool.util.BeepHttpClient;
import cc.kinami.audiotool.util.BeepWebSocketClient;
import cc.kinami.audiotool.util.BeepWebSocketHandler;
import cc.kinami.audiotool.util.WavPlayer;
import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;

public class WsClientFragment extends Fragment {

    private static final String TAG = "[Beep]BeepClientFrag";
    BeepWebSocketClient beepWebSocketClient;
    EditText serverAddrEditText;
    EditText deviceNameEditText;
    Button connectButton;
    TextView wsInfoText;
    TextView wsStatusText;
    TextView wsM2SLengthText;
    View view;
    String serverAddr;
    String deviceName;
    Double m2SLength;
    private WavPlayer wavPlayer;
    private IdealRecorder idealRecorder;
    private IdealRecorder.RecordConfig recordConfig;
    BeepWebSocketHandler webSocketHandler = new BeepWebSocketHandler() {
        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {
            Log.i(TAG, "onMessage: " + message);
            try {
                ObjectMapper mapper = new ObjectMapper();
                ControlInfo controlInfo = mapper.readValue(message, ControlInfo.class);
                switch (controlInfo.getControlInfo()) {
                    case GET_CHIRP:
                        getSignalHandler(controlInfo.getExperimentId());
                        break;
                    case START_RECORD:
                        startRecordHandler(controlInfo.getExperimentId(), controlInfo.getFrom(), controlInfo.getTo(), controlInfo.getMic(), controlInfo.getFs());
                        break;
                    case PLAY_CHIRP:
                        playSignalHandler(controlInfo.getExperimentId());
                        break;
                    case FINISH_RECORD:
                        finishRecordHandler(controlInfo.getExperimentId(), controlInfo.getFrom(), controlInfo.getTo(), controlInfo.getExperimentType());
                        break;
                    default:
                        assert false;
                }
            } catch (Exception e) {
                Log.e(TAG, "onMessage: ", e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            closeConnect();
        }

        @Override
        public void onError(Exception ex) {
            closeConnect();
        }

    };
    Handler wsInfoTextHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    wsInfoText.setText(wsInfoText.getText().toString() + msg.obj);
                    break;
                case 2:
                    wsInfoText.setText("");
                    break;
                case 3:
                    beepWebSocketClient = null;
                    connectButton.setOnClickListener(v -> connect());
                    connectButton.setText("连接服务器");
                    wsStatusText.setText("wait for connection");
                    serverAddrEditText.setEnabled(true);
                    deviceNameEditText.setEnabled(true);
                    wsM2SLengthText.setEnabled(true);
                    break;
                default:
                    break;
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_beepbeep_client, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        serverAddrEditText = view.findViewById(R.id.server_address_input);
        deviceNameEditText = view.findViewById(R.id.device_name_input);
        String deviceName;
        switch (Build.MODEL) {
            case "H60-L01":
                deviceName = "A";
                break;
            case "AUM-AL20":
                deviceName = "Z";
                break;
            case "XT1650-05":
                deviceName = "M";
                break;
            default:
                deviceName = Build.MODEL;
                break;
        }


        deviceNameEditText.setText(deviceName.toCharArray(), 0, deviceName.length());
        connectButton = view.findViewById(R.id.connect_btn);
        connectButton.setOnClickListener(v -> connect());
        connectButton.setText("连接服务器");
        wsInfoText = view.findViewById(R.id.ws_info_text);
        wsInfoText.setMovementMethod(ScrollingMovementMethod.getInstance());
        wsStatusText = view.findViewById(R.id.ws_status);
        wsM2SLengthText = view.findViewById(R.id.M_2_S_length_text);
    }

    private void connect() {
        serverAddr = serverAddrEditText.getText().toString();
        deviceName = deviceNameEditText.getText().toString();
        m2SLength = Double.valueOf(wsM2SLengthText.getText().toString());
        serverAddrEditText.setEnabled(false);
        deviceNameEditText.setEnabled(false);
        wsM2SLengthText.setEnabled(false);
        URI uri = URI.create("ws://" + serverAddr + "/ws/" + deviceName + "/" + m2SLength);
        beepWebSocketClient = new BeepWebSocketClient(uri);
        beepWebSocketClient.setWebSocketHandler(webSocketHandler);
//        wsInfoText.setText("connecting...\n");
        connectButton.setOnClickListener(v -> closeConnect());
        connectButton.setText("断开连接");
        wsInfoText.setText("");
        wsStatusText.setText("online");
        try {
            beepWebSocketClient.connectBlocking(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "connect: connect error", e);
            closeConnect();
        }
    }

    private void closeConnect() {
        try {
            if (beepWebSocketClient != null) {
                beepWebSocketClient.close();
                Log.i(TAG, "closeConnect: ");
            }
        } catch (Exception e) {
            Log.e(TAG, "closeConnect: ", e);
        } finally {
            Message message = Message.obtain();
            message.what = 3;
            wsInfoTextHandler.sendMessage(message);
        }
    }

    private void getSignalHandler(int experimentId) {
        // 清空信息
        Message message = Message.obtain();
        message.what = 2;
        wsInfoTextHandler.sendMessage(message);
        sendMsgToWsInfoTest("[Experiment begin]\n");
        sendMsgToWsInfoTest("[receive] GET_CHIRP\n    experiment id: " + experimentId + "\n");
        try {
            // 获取 chirp
            getSignal(experimentId);
            sendMsgToWsInfoTest("[send] GET_CHIRP_ACK\n");
            sendControlInfo(ControlInfo.builder()
                    .controlInfo(ProcessControlEnum.GET_CHIRP_ACK)
                    .experimentId(experimentId)
                    .build());
        } catch (Exception e) {
            sendControlInfo(ControlInfo.builder()
                    .controlInfo(ProcessControlEnum.CLIENT_ERROR)
                    .experimentId(experimentId)
                    .build());
        }

    }

    private void startRecordHandler(int experimentId, String from, String to, int mic, int fs) {
        sendMsgToWsInfoTest("[receive] START_RECORD\n"
                + "    experiment id: " + experimentId + "\n"
                + "    from: " + from + "\n"
                + "      to: " + to + "\n");

        startRecord(experimentId, mic, fs);

        sendMsgToWsInfoTest("[send] START_RECORD_ACK\n");
        sendControlInfo(ControlInfo.builder()
                .controlInfo(ProcessControlEnum.START_RECORD_ACK)
                .experimentId(experimentId)
                .build());

    }

    private void playSignalHandler(int experimentId) {
        sendMsgToWsInfoTest("[receive] PLAY_SIGNAL\n    experiment id: " + experimentId + "\n");

        playSignal(experimentId);
    }

    private void finishPlayingChirpHandler(int experimentId) {
        Log.i(TAG, "playChirp: play finished");
        assert !wavPlayer.isWorking;
        sendMsgToWsInfoTest("[send] PLAY_CHIRP_ACK\n");
        sendControlInfo(ControlInfo.builder()
                .controlInfo(ProcessControlEnum.PLAY_CHIRP_ACK)
                .experimentId(experimentId)
                .build());
    }

    private void finishRecordHandler(int experimentId, String from, String to, int experimentIType) {
        sendMsgToWsInfoTest("[receive] FINISH_RECORD\n    experiment id: " + experimentId + "\n");

        stopRecord();
        uploadRecord(experimentId, from, to, experimentIType);

        sendMsgToWsInfoTest("[send] FINISH_RECORD_ACK\n");
        sendControlInfo(ControlInfo.builder()
                .controlInfo(ProcessControlEnum.FINISH_RECORD_ACK)
                .experimentId(experimentId)
                .build());
        sendMsgToWsInfoTest("[Experiment begin]\n");
    }

    private void sendMsgToWsInfoTest(String msg) {
        Message message = Message.obtain();
        message.obj = msg;
        message.what = 1;
        wsInfoTextHandler.sendMessage(message);
    }


    private void sendControlInfo(ControlInfo controlInfo) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            beepWebSocketClient.send(mapper.writeValueAsString(controlInfo));
        } catch (Exception e) {
            throw new RuntimeException("websocket info error");
        }
    }

    private void getSignal(int experimentId) throws Exception {
        // 建立实验文件夹
//        File file = new File(Environment.getExternalStorageDirectory(), "Audio Tools/experiments/" + experimentId);
        File file = new File(Environment.getExternalStorageDirectory(), "Audio Tools/experiments/");

        if (!file.exists()) {
            if (!file.mkdirs())
                throw new IllegalStateException("创建文件夹失败");
        }
        // 构造url
        JsonNode responseData = BeepHttpClient.get("http://" + serverAddr + "/api/experiment/getChirp", new HashMap<String, String>() {{
            put("experimentId", String.valueOf(experimentId));
        }});
        String relativePath = responseData.asText().replace("./", "/");
        String chirpFileUrl = "http://" + serverAddr + relativePath;
        Log.i(TAG, "getChirp: chirpFileUrl = " + chirpFileUrl);
//        BeepHttpClient.downLoad(chirpFileUrl,
//                Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/" + experimentId,
//                "chirp.wav");
        BeepHttpClient.downLoad(chirpFileUrl,
                Environment.getExternalStorageDirectory() + "/Audio Tools/experiments",
                "signal.wav");
    }

    private void startRecord(int experimentId, int mic, int fs) {
        idealRecorder = IdealRecorder.getInstance();
        int micConfig = MediaRecorder.AudioSource.MIC, channelConfig = AudioFormat.CHANNEL_IN_MONO;


        switch (mic) {
            case 0:
                Log.i(TAG, "startRecord: mic0");
                break;
            case 1:
                Log.i(TAG, "startRecord: mic1");
                micConfig = MediaRecorder.AudioSource.CAMCORDER;
                break;
            default:
                throw new RuntimeException("something wrong");
        }


        recordConfig = new IdealRecorder.RecordConfig(micConfig,
                fs,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT);


        //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
//        idealRecorder.setRecordFilePath(Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/" + experimentId + "/" + from + "_to_" + to + ".wav");
        idealRecorder.setRecordFilePath(Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/" + "record.wav");

        //设置录音配置 最长录音时长 以及音量回调的时间间隔
        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(60000).setVolumeInterval(200);
        //设置录音时各种状态的监听
        idealRecorder.setStatusListener(new StatusListener() {
            @Override
            public void onStartRecording() {
                Log.i(TAG, "onStartRecording: begin record");
            }

            @Override
            public void onRecordData(short[] data, int length) {
            }

            @Override
            public void onVoiceVolume(int volume) {
            }

            @Override
            public void onRecordError(int code, String errorMsg) {
                Log.e(TAG, "onRecordError: record error", new Exception("record error"));
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
                Log.i(TAG, "onStopRecording: stop record");
            }
        });
        idealRecorder.start(); //开始录音
    }

    private void stopRecord() {
        //停止录音
        idealRecorder.stop();
    }

    public void playSignal(int experimentId) {
        wavPlayer = new WavPlayer();
//        wavPlayer.playWav("Audio Tools/experiments/" + experimentId + "/chirp.wav", () -> finishPlayingChirpHandler(experimentId));
        wavPlayer.playWav("Audio Tools/experiments/" + "signal.wav", () -> finishPlayingChirpHandler(experimentId));
    }


    public void uploadRecord(int experimentId, String from, String to, int experimentType) {
        switch (experimentType) {
            case 1: {
                String uploadAddr = "http://" + serverAddr + "/api/experiment/upload";
//        String filePath = Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/" + experimentId + "/" + from + "_to_" + to + ".wav";
                String filePath = Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/record.wav";
                BeepHttpClient.uploadRecord(uploadAddr, filePath, experimentId, from, to);
                break;
            }
            case 2: {
                String uploadAddr = "http://" + serverAddr + "/api/experiment/type2/upload";
                String filePath = Environment.getExternalStorageDirectory() + "/Audio Tools/experiments/record.wav";
                BeepHttpClient.uploadRecord(uploadAddr, filePath, experimentId, from, to);
                break;
            }
            default:
                assert false;
        }

    }


}
