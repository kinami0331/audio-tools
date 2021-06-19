package cc.kinami.audiotool.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExceptionEnum {
    SERVER_IO_EXCEPTION(49997, "网络IO出错"),
    SERVER_CONNECT_FAILED(49998, "服务器连接出错，请检查与服务器的连接状态"),
    ILLEGAL_DEVICE_NUM(49999, "非法的设备数量");

    private final Integer errCode;
    private final String errMsg;


}
