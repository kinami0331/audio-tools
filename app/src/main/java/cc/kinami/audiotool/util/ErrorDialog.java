package cc.kinami.audiotool.util;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;

import cc.kinami.audiotool.exception.ExceptionEnum;

public class ErrorDialog {
    public static void showDiaglog(Context context, ExceptionEnum exceptionEnum) {
        new AlertDialog.Builder(context)
                .setTitle("出错了！")
                .setMessage(exceptionEnum.getErrMsg())
                .setNegativeButton("关闭", (dialog, which) -> {
                })
                .setCancelable(false)
                .show();
    }

    public static void showDiaglog(Context context, String msg) {
        Looper.prepare();
        new AlertDialog.Builder(context)
                .setTitle("出错了！")
                .setMessage(msg)
                .setNegativeButton("关闭", (dialog, which) -> {
                })
                .setCancelable(false)
                .show();
        Looper.loop();
    }
}
