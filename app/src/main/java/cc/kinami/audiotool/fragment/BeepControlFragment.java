package cc.kinami.audiotool.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.kinami.audiotool.R;
import cc.kinami.audiotool.exception.KnownException;
import cc.kinami.audiotool.util.BeepHttpClient;
import cc.kinami.audiotool.util.ErrorDialog;

public class BeepControlFragment extends Fragment {
    public static final String TAG = "[Beep]CtrlFrag";

    View view;
    TextView onlineDeviceList;
    TextView experimentLogInfoText;
    Handler handler;

    Button getOnlineDeviceListButton;
    Button startExperimentWithTwoDevicesButton;
    EditText serverAddrEditText;
    String serverAddr;

    LinearLayout imageListLinear;
    List<String> currentOnlineDevices = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_beep_control, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onlineDeviceList = view.findViewById(R.id.device_list_text);
        getOnlineDeviceListButton = view.findViewById(R.id.get_online_device_list_button);
        serverAddrEditText = view.findViewById(R.id.server_address_input);
        getOnlineDeviceListButton.setOnClickListener(v -> getOnlineDevice());
        startExperimentWithTwoDevicesButton = view.findViewById(R.id.start_experiment_with_two_devices);
        startExperimentWithTwoDevicesButton.setOnClickListener(v -> startExperimentWithTwoDevices());
        experimentLogInfoText = view.findViewById(R.id.experiment_log_info);
        imageListLinear = view.findViewById(R.id.image_list_linear);
        handler = new BeepControlHandler(this);
    }

    void getOnlineDevice() {
        serverAddr = serverAddrEditText.getText().toString();
        currentOnlineDevices = new ArrayList<>();
        BeepHttpClient.get("http://" + serverAddr + "/api/manage/device-list", new HashMap<>(), this::getOnlineDeviceCallback);
    }

    void getOnlineDeviceCallback(JsonNode response) {
        if (response != null) {
            Iterator<JsonNode> elements = response.elements();
            String deviceListString = "";
            while (elements.hasNext()) {
                JsonNode node = elements.next();
                currentOnlineDevices.add(node.asText());
                deviceListString = deviceListString + node.asText() + "\n";
            }
            Message message = Message.obtain();
            message.obj = deviceListString;
            message.what = BeepControlHandler.DEVICE_LIST;
            handler.sendMessage(message);
        }
    }

    void startExperimentWithTwoDevices() {
        startExperimentWithTwoDevicesButton.setEnabled(false);
        experimentLogInfoText.setText("实验进行中...");
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("正在进行中")
                .setCancelable(false)
                .setNegativeButton("中断操作", (dialog, which) -> {
                })
                .create();
        final Thread workThread = new Thread(() -> {
            try {
                // 获取设备列表
                serverAddr = serverAddrEditText.getText().toString();
                currentOnlineDevices = new ArrayList<>();
                JsonNode response = BeepHttpClient.get("http://" + serverAddr + "/api/manage/device-list", new HashMap<>());
                if (response != null) {
                    Iterator<JsonNode> elements = response.elements();
                    StringBuilder deviceListString = new StringBuilder();
                    while (elements.hasNext()) {
                        JsonNode node = elements.next();
                        currentOnlineDevices.add(node.asText());
                        deviceListString.append(node.asText()).append("\n");
                    }
                    Message message = Message.obtain();
                    message.obj = deviceListString.toString();
                    message.what = BeepControlHandler.DEVICE_LIST;
                    handler.sendMessage(message);
                }

                // 如果设备数量不为两个，弹出警告
                if (currentOnlineDevices.size() != 2) {
                    Log.w(TAG, "startExperimentWithTwoDevices: 在线设备数量错误", null);
                    ErrorDialog.showDiaglog(getContext(), "当前在线设备数量不为2，请检查设备列表。\n"
                            + "当前在线设备数: " + currentOnlineDevices.size() + "\n"
                    );
                    return;
                }

                // 构造Json
                Map<String, Object> postBody = new HashMap<>();
                postBody.put("deviceList", currentOnlineDevices);

                Map<String, Double> argument = new HashMap<>();
                switch ((int) ((Spinner) view.findViewById(R.id.sampling_rate_choose)).getSelectedItemId()) {
                    case 0:
                        argument.put("samplingRate", 44100.0);
                        break;
                    case 1:
                        argument.put("samplingRate", 48000.0);
                        break;
                    default:
                        throw new RuntimeException("这不合理");
                }
                argument.put("lowerLimit",
                        Double.valueOf(((EditText) view.findViewById(R.id.frequncy_lower_limit_text)).getEditableText().toString()));
                argument.put("upperLimit",
                        Double.valueOf(((EditText) view.findViewById(R.id.frequncy_upper_limit_text)).getEditableText().toString()));
                argument.put("chirpTime",
                        Double.valueOf(((EditText) view.findViewById(R.id.chirp_time_text)).getEditableText().toString()));
                argument.put("prepareTime",
                        Double.valueOf(((EditText) view.findViewById(R.id.prepare_time_text)).getEditableText().toString()));
                argument.put("soundSpeed",
                        Double.valueOf(((EditText) view.findViewById(R.id.sound_speed_text)).getEditableText().toString()));
                postBody.put("chirpParameters", argument);
                ObjectMapper mapper = new ObjectMapper();
                Log.i(TAG, "startExperimentWithTwoDevices: postBody = " + mapper.writeValueAsString(postBody));
                // 创建实验
                response = BeepHttpClient.post("http://" + serverAddr + "/api/experiment/create",
                        mapper.readTree(mapper.writeValueAsString(postBody)));
                Log.i(TAG, "startExperimentWithTwoDevices: create response = " + mapper.writeValueAsString(response));

                // 开始实验
                int experimentId = response.asInt();
                // 通知实验id
                Message message = Message.obtain();
                message.what = BeepControlHandler.EXPERIMENT_INFO;
                message.obj = "当前实验编号：" + experimentId;
                handler.sendMessage(message);

                Map<String, Integer> tmpMap = new HashMap<>();
                tmpMap.put("experimentId", experimentId);
                String tmpStr = mapper.writeValueAsString(tmpMap);
                JsonNode tmpNode = mapper.readTree(tmpStr);
                response = BeepHttpClient.post("http://" + serverAddr + "/api/experiment/begin", tmpNode);
                Log.i(TAG, "startExperimentWithTwoDevices: begin response = " + mapper.writeValueAsString(response));

                // 通知实验结果
                message = Message.obtain();
                message.what = BeepControlHandler.EXPERIMENT_INFO;
                message.obj = "测量距离：" + (Math.round(response.get("distance").asDouble() * 10000) / 10000.0) + " m";
                handler.sendMessage(message);

                // 显示图片
                message = Message.obtain();
                message.what = BeepControlHandler.SHOW_IMAGE;
                message.obj = response.get("imageList");
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
                message.what = BeepControlHandler.EXPERIMENT_FINIEH;
                handler.sendMessage(message);
                alertDialog.cancel();
            }
        });
        alertDialog.setOnShowListener((dialog) -> {
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener((e) -> {
                if (workThread.getState() == Thread.State.NEW)
                    return;
                workThread.interrupt();
                startExperimentWithTwoDevicesButton.setEnabled(true);
                alertDialog.cancel();
            });
        });
        alertDialog.show();
        workThread.start();
    }

    private static class BeepControlHandler extends Handler {
        public static final int DEVICE_LIST = 0;
        public static final int EXPERIMENT_FINIEH = 1;
        public static final int EXPERIMENT_INFO = 2;
        public static final int SHOW_IMAGE = 3;
        WeakReference<BeepControlFragment> beepControlFragmentWeakReference;

        public BeepControlHandler(BeepControlFragment beepControlFragment) {
            this.beepControlFragmentWeakReference = new WeakReference<>(beepControlFragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            BeepControlFragment beepControlFragment = beepControlFragmentWeakReference.get();
            if (beepControlFragment == null) {
                throw new RuntimeException("这不合理");
            }
            switch (msg.what) {
                case DEVICE_LIST:
                    beepControlFragment.onlineDeviceList.setText(msg.obj.toString());
                    break;
                case EXPERIMENT_FINIEH:
                    Log.i(TAG, "handleMessage: experiment finish ");
                    beepControlFragment.startExperimentWithTwoDevicesButton.setEnabled(true);
                    break;
                case EXPERIMENT_INFO:
                    String preText = beepControlFragment.experimentLogInfoText.getText().toString();
                    beepControlFragment.experimentLogInfoText.setText(preText + "\n" + msg.obj.toString());
                    break;
                case SHOW_IMAGE:
                    JsonNode recordListJson = (JsonNode) msg.obj;
                    ArrayList<String> imageList = new ArrayList<>();
                    for (Iterator<JsonNode> it = recordListJson.elements(); it.hasNext(); ) {
                        JsonNode node = it.next();
                        String tmp = node.asText().replace("./", "http://" + beepControlFragment.serverAddr + "/");
                        imageList.add(tmp);
                    }
                    beepControlFragment.imageListLinear.removeAllViews();
                    for (int i = 0; i < imageList.size(); i++) {
                        ImageView imageView = new ImageView(beepControlFragment.getContext());
                        beepControlFragment.imageListLinear.addView(imageView);
                        Glide.with(beepControlFragment)
                                .load(imageList.get(i))
                                .into(imageView);
                    }

                    break;
                default:
                    throw new RuntimeException("这不合理");
            }
        }
    }

}
