package com.zongkx.demofiledownload;
/**
 * @author zongkx
 */

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * @author zongkx
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadTests {

    public void downloadFileWithResume(String fileURL, String saveFilePath, int chunkSize) throws Exception {
        File file = new File(saveFilePath);
        long downloadedBytes = 0;

        if (file.exists()) {
            downloadedBytes = file.length();
        }

        HttpURLConnection httpConn = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            URL url = new URL(fileURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
            httpConn.connect();

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpConn.getInputStream();
                outputStream = new FileOutputStream(file, true);

                byte[] buffer = new byte[chunkSize];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                    System.out.println("Downloaded " + downloadedBytes + " bytes");
                }
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    @BeforeAll
    public void before() {


    }

    @Test
    @SneakyThrows
    void a1() {
        String fileName = "1.log"; // 要下载的文件名
        String downloadUrl = "http://localhost:8080/download?fileName=" + fileName;
        String saveFilePath = "downloaded_" + fileName; // 保存文件的路径
        int chunkSize = 1024 * 1024; // 每次下载的块大小 (1MB)
        try {
            downloadFileWithResume(downloadUrl, saveFilePath, chunkSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}