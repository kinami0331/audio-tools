package cc.kinami.audiotool.util;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import cc.kinami.audiotool.exception.ExceptionEnum;
import cc.kinami.audiotool.exception.KnownException;

public class BeepHttpClient {
    static String TAG = "[Beep]BeepHttpClient";

    public static void get(String url, Map<String, String> params, Callback callback) {
        new Thread(() -> {
            try {
                callback.callback(get(url, params));
            } catch (Exception e) {
                Log.e(TAG, "get: thread", e);
                callback.callback(null);
            }
        }).start();
    }

    public static JsonNode get(String url, Map<String, String> params) throws KnownException {
        String path = url;
        if (params.size() > 0) {
            path = path + "?";
            Set<String> paramNames = params.keySet();
            for (String key : paramNames) {
                if (path.charAt(path.length() - 1) != '?')
                    path += "&";
                path = path + key + "=" + params.get(key);
            }
            Log.i(TAG, "get: url = " + path);

            Log.i(TAG, "get: params = " + params.toString());
        }

        try {
            URL tarUrl = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) tarUrl.openConnection();
            connection.setConnectTimeout(1000);
            connection.setRequestMethod("GET");
            //获得结果码
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                //请求成功 获得返回的流
                InputStream inputStream = connection.getInputStream();
                String response = CommonUtil.toString(inputStream);
                Log.i(TAG, "get: response: " + response);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response);
                if (jsonNode.get("code").asInt() != 200)
                    throw new KnownException(jsonNode.get("code").asInt(), jsonNode.get("message").asText());
                return jsonNode.get("data");
            } else {
                throw new KnownException(ExceptionEnum.SERVER_CONNECT_FAILED);
            }
        } catch (IOException e) {
            throw new KnownException(ExceptionEnum.SERVER_IO_EXCEPTION);
        }
    }

    public static void downLoad(String fileUrl, String storePath, String stordName) throws Exception {
        OutputStream output = null;
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            File file = new File(storePath + "/" + stordName);
            Log.i(TAG, "downLoad: tarFilePath = " + storePath + "/" + stordName);
            if (file.exists())
                file.delete();
            file.createNewFile();
            output = new FileOutputStream(file);

            byte[] buffer = new byte[4 * 1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            Log.i(TAG, "downLoad: success");
        } catch (Exception e) {
            Log.e(TAG, "downLoad: error", e);
            throw new Exception("downLoad error");
        }
    }

    public static void uploadRecord(String uploadUrl, String filePath, int experimentId, String from, String to) {
        String fileName = from + "_to_" + to + ".wav";
        String end = "\r\n";
        String Hyphens = "--";
        String boundary = "WUm4580jbtwfJhNp7zi1djFEO3wNNm";
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            // 第一个分隔符后填写文件内容
            dataOutputStream.writeBytes(Hyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; " +
                    "filename=\"" + fileName + "\"" + end);
            dataOutputStream.writeBytes(end);

            FileInputStream fileInputStream = new FileInputStream(filePath);
            // 每次写入4 * 1024字节
            int bufferSize = 1024 * 4;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            // 将文件数据写入到缓冲区
            while ((length = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, length);
            }
            dataOutputStream.writeBytes(end);

            //第二个分隔符后是experimentId
            dataOutputStream.writeBytes(Hyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"experimentId\"" + end);
            dataOutputStream.writeBytes(end);
            dataOutputStream.writeBytes(experimentId + end);

            // 第三个分隔符后是from
            dataOutputStream.writeBytes(Hyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"from\"" + end);
            dataOutputStream.writeBytes(end);
            dataOutputStream.writeBytes(from + end);

            // 第四个分隔符后是to
            dataOutputStream.writeBytes(Hyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"to\"" + end);
            dataOutputStream.writeBytes(end);
            dataOutputStream.writeBytes(to + end);

            // 结束
            dataOutputStream.writeBytes(Hyphens + boundary + Hyphens + end);
            fileInputStream.close();
            dataOutputStream.flush();
            dataOutputStream.close();

            // 获取返回内容
            //获得结果码
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                //请求成功 获得返回的流
                InputStream inputStream = connection.getInputStream();
                String response = CommonUtil.toString(inputStream);
                Log.i(TAG, "upload: response: " + response);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response);
                if (jsonNode.get("code").asInt() != 200)
                    throw new Exception("upload请求错误");
                Log.i(TAG, "uploadRecord: " + jsonNode.get("data").asText());
            } else
                throw new Exception();

        } catch (Exception e) {
            Log.e(TAG, "uploadRecord: upload error", e);
        }
    }

    public static JsonNode post(String url, JsonNode jsonBody) throws KnownException {
        try {
            URL tarUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) tarUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            // 文件类型
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("accept", "application/json");
            // 获取json字符串
            byte[] jsonBytes = jsonBody.toString().getBytes();
            // 设置长度
            connection.setRequestProperty("Content-Length", String.valueOf(jsonBytes.length));
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonBytes);
            outputStream.flush();
            outputStream.close();

            if (connection.getResponseCode() == 200) {
                //请求成功 获得返回的流
                InputStream inputStream = connection.getInputStream();
                String response = CommonUtil.toString(inputStream);
                Log.i(TAG, "get: response: " + response);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response);
                if (jsonNode.get("code").asInt() != 200)
                    throw new KnownException(jsonNode.get("code").asInt(), jsonNode.get("message").asText());
                return jsonNode.get("data");
            } else {
                throw new KnownException(ExceptionEnum.SERVER_CONNECT_FAILED);
            }
        } catch (IOException e) {
            throw new KnownException(ExceptionEnum.SERVER_IO_EXCEPTION);
        }
    }

    @FunctionalInterface
    public interface Callback {
        void callback(JsonNode jsonNode);
    }

}
