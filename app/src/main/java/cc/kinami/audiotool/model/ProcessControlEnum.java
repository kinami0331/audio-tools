package cc.kinami.audiotool.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ProcessControlEnum {

    // 初始化
    INIT("init"),
    // 通知一个设备获取chirp
    GET_CHIRP("get_chirp"),
    // 设备确定chirp获取完毕
    GET_CHIRP_ACK("get_chirp_ack"),
    // 通知一个设备打开麦克风
    START_RECORD("start_record"),
    // 设备确定开启麦克风
    START_RECORD_ACK("start_record_ack"),
    // 通知一个设备播放chirp
    PLAY_CHIRP("play_chirp"),
    // 设备确认播放完毕
    PLAY_CHIRP_ACK("play_chirp_ack"),
    // 通知设备关闭麦克风并上传文件
    FINISH_RECORD("finish_record"),
    // 确认麦克风关闭并上传了文件
    FINISH_RECORD_ACK("finish_record_ack"),
    CLIENT_ERROR("client_error");

    private String description;

    @JsonCreator
    public static ProcessControlEnum getEnum(String description) {
        for (ProcessControlEnum item : values())
            if (item.getDescription().equals(description))
                return item;
        return null;
    }

    @JsonValue
    public String getStringValue() {
        return description;
    }


}
