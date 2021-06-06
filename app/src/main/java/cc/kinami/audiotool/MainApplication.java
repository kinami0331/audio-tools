package cc.kinami.audiotool;

import android.app.Application;

import tech.oom.idealrecorder.IdealRecorder;


public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        IdealRecorder.getInstance().init(this);
    }
}