package cc.kinami.audiotool.exception;

import lombok.Getter;

@Getter
public class KnownException extends Exception {
    private final Integer errCode;
    private final String errMsg;

    public KnownException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getErrMsg());
        errCode = exceptionEnum.getErrCode();
        errMsg = exceptionEnum.getErrMsg();
    }

    public KnownException(Integer errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = "服务器报错：\n" + errMsg;
    }
}
