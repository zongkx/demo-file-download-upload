package com.zongkx.demofiledownload;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

@RestController
public class DownloadApi {

    @GetMapping("/download")
    public void downloadFile(@RequestParam String fileName, HttpServletRequest request, HttpServletResponse response) {
        File file = new File("/comen/data/" + fileName);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;
        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            String[] ranges = range.substring(6).split("-");
            try {
                if (ranges.length > 0) {
                    start = Long.parseLong(ranges[0]);
                }
                if (ranges.length > 1) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
        }
        long contentLength = end - start + 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Length", String.valueOf(contentLength));
        if (range != null) {
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        } else {
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            randomAccessFile.seek(start);
            while ((bytesRead = randomAccessFile.read(buffer)) != -1) {
                if (start + bytesRead > end) {
                    outputStream.write(buffer, 0, (int) (end - start + 1));
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
                start += bytesRead;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
