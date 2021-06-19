package cc.kinami.audiotool.util;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WavPlayer {
    private static final String TAG = "[Beep]WavPlayer";
    public boolean isWorking = false;
    private AudioTrack audioTrack;
    private boolean playing = false;

    public static WavParam readWavHeader(RandomAccessFile file) throws IOException {
        file.seek(22);
        byte channelCount = file.readByte();
        file.seek(24);
        // 读取比特率
        int sampleRate = file.readByte() & 0xff;
        sampleRate |= (file.readByte() & 0xff) << 8;
        sampleRate |= (file.readByte() & 0xff) << 16;
        sampleRate |= (file.readByte() & 0xff) << 24;
        // 位深
        file.seek(34);
        byte bits = file.readByte();
        return new WavParam(sampleRate, channelCount, bits);
    }

    public WavParam playWav(String filepath, Callback callback) {
        RandomAccessFile file = null;
        WavParam params = null;
        try {
            File originFile = new File(Environment.getExternalStorageDirectory(), filepath);
            Log.i(TAG, "playWav: chirp file path = " + Environment.getExternalStorageDirectory() + "/" + filepath);
            Log.d(TAG, "playWav: ExternalStorageDirectory: " + Environment.getExternalStorageDirectory().getAbsolutePath());
            if (!originFile.exists())
                throw new FileNotFoundException();
            file = new RandomAccessFile(originFile, "r");
            params = readWavHeader(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "playWav: FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "playWav: IOException", e);
        }
        if (params == null) {
            return null;
        }

        try {
            isWorking = true;
            Log.i(TAG, "playWav: fileSize: " + file.length());
            int pcmSize = (int) file.length() - 44;
            byte[] buffer = new byte[pcmSize];

            int simpleRate = params.simpleRate;
            int channelConfig = params.getOutChannelConfig();
            int audioFormat = params.getEncodingFormat();
            int minBufSize = AudioTrack.getMinBufferSize(simpleRate, channelConfig, audioFormat);
            Log.i(TAG, "playWav: simpleRate: " + simpleRate + ", channel: " + channelConfig + ", format: " + audioFormat + ", minBuf: " + minBufSize);
            audioTrack = new AudioTrack(AudioManager.STREAM_ALARM, simpleRate, channelConfig, audioFormat, pcmSize,
                    AudioTrack.MODE_STATIC);
            file.seek(44);
            int read = file.read(buffer);
            if (read != pcmSize)
                Log.e(TAG, "playWav: read error", new Exception());
            audioTrack.write(buffer, 0, read);
            audioTrack.play();
            audioTrack.setNotificationMarkerPosition(pcmSize / 2);
            audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    Log.i(TAG, "onMarkerReached: play finished");
                    track.stop();
                    track.release();
                    isWorking = false;
                    if (callback != null)
                        callback.callback();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {

                }
            });
            file.close();

        } catch (Exception e) {
            e.printStackTrace();
            isWorking = false;
        }

        return params;
    }

    @FunctionalInterface
    public interface Callback {
        void callback();
    }
}
