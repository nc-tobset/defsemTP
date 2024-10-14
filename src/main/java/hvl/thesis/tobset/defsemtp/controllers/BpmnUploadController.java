
package hvl.thesis.tobset.defsemtp.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class BpmnUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    // Ensure the upload directory exists
    public BpmnUploadController() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @PostMapping("/upload-bpmn")
    public ResponseEntity<String> handleBpmnUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".bpmn")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid BPMN file");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + originalFilename);

            int counter = 1;
            while (Files.exists(path)) {
                String newFilename = originalFilename.replaceFirst("(\\.bpmn)$", "_" + counter + "$1");
                path = Paths.get(UPLOAD_DIR + newFilename);
                counter++;
            }

            Files.write(path, file.getBytes());
            return ResponseEntity.ok("File uploaded successfully: " + path.getFileName());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }
}
