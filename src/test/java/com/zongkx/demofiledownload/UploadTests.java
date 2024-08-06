package com.zongkx.demofiledownload;
/**
 * @author zongkx
 */

import lombok.SneakyThrows;
import okhttp3.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * @author zongkx
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadTests {

    public static void uploadFileInChunks(String filePath, String uploadUrl, int chunkSize) throws IOException {
        File file = new File(filePath);
        long totalSize = file.length();
        int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int chunkNumber = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                chunkNumber++;
                boolean isLastChunk = chunkNumber == totalChunks;
                uploadChunk(uploadUrl, buffer, bytesRead, file.getName(), chunkNumber, totalChunks, isLastChunk);
                System.out.println("Uploaded chunk " + chunkNumber + " of " + totalChunks);
            }
        }
    }

    public static void uploadChunk(String uploadUrl, byte[] chunkData, int bytesRead, String fileName, int chunkNumber, int totalChunks, boolean isLastChunk) throws IOException {
        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=--boundary");

        try (OutputStream os = connection.getOutputStream()) {
            String formData = "--boundary\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + ".part" + chunkNumber + "\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n";
            os.write(formData.getBytes());
            os.write(chunkData, 0, bytesRead);
            os.write("\r\n--boundary--\r\n".getBytes());

            String metadata = "--boundary\r\n" +
                    "Content-Disposition: form-data; name=\"fileName\"\r\n\r\n" +
                    fileName + "\r\n" +
                    "--boundary\r\n" +
                    "Content-Disposition: form-data; name=\"chunkNumber\"\r\n\r\n" +
                    chunkNumber + "\r\n" +
                    "--boundary\r\n" +
                    "Content-Disposition: form-data; name=\"totalChunks\"\r\n\r\n" +
                    totalChunks + "\r\n" +
                    "--boundary--\r\n";
            os.write(metadata.getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Chunk " + chunkNumber + " uploaded successfully");
        } else {
            System.out.println("Failed to upload chunk " + chunkNumber + ", response code: " + responseCode);
        }
    }

    @BeforeAll
    public void before() {


    }

    @Test
    @SneakyThrows
    void a1() {
        String filePath = "./demo.log"; // 要上传的文件路径
        String uploadUrl = "http://localhost:8080/chunk";
        File file = new File(filePath);
        int chunkSize = 1024 * 1024; // 1MB
        int chunks = (int) Math.ceil((double) file.length() / chunkSize);

        OkHttpClient client = new OkHttpClient();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData;
                if (bytesRead < chunkSize) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer;
                }

                RequestBody fileBody = RequestBody.create(chunkData, MediaType.parse("application/octet-stream"));
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.getName(), fileBody)
                        .addFormDataPart("chunk", String.valueOf(chunkIndex))
                        .addFormDataPart("chunks", String.valueOf(chunks))
                        .addFormDataPart("fileName", file.getName())
                        .build();

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    System.out.println(response.body().string());
                }

                chunkIndex++;
            }
        }
    }
}