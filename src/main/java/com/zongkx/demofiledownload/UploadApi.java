package com.zongkx.demofiledownload;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
public class UploadApi {


    @PostMapping("/chunk")
    public String uploadChunk(@RequestParam("file") MultipartFile file,
                              @RequestParam("chunk") int chunk,
                              @RequestParam("chunks") int chunks,
                              @RequestParam("fileName") String fileName) throws IOException {
        String uploadDir = "uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File chunkFile = new File(uploadDir + fileName + ".part" + chunk);
        try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
            fos.write(file.getBytes());
        }
        // Check if all chunks are uploaded
        if (allChunksUploaded(uploadDir, fileName, chunks)) {
            mergeChunks(uploadDir, fileName, chunks);
        }

        return "Chunk " + chunk + " uploaded";
    }

    private boolean allChunksUploaded(String uploadDir, String fileName, int chunks) {
        for (int i = 0; i < chunks; i++) {
            File chunkFile = new File(uploadDir + fileName + ".part" + i);
            if (!chunkFile.exists()) {
                return false;
            }
        }
        return true;
    }

    private void mergeChunks(String uploadDir, String fileName, int chunks) throws IOException {
        File mergedFile = new File(uploadDir + fileName);
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            for (int i = 0; i < chunks; i++) {
                File chunkFile = new File(uploadDir + fileName + ".part" + i);
                fos.write(java.nio.file.Files.readAllBytes(chunkFile.toPath()));
                chunkFile.delete();
            }
        }
    }
}
