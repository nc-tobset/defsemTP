package hvl.thesis.tobset.defsemtp.controllers;

import hvl.thesis.tobset.defsemtp.services.AlloyRunnerService;
import hvl.thesis.tobset.defsemtp.services.BpmnToAlloyService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/translate")
public class TranslationController {

    @Autowired
    private BpmnToAlloyService bpmnToAlloyService;

    @Autowired
    private AlloyRunnerService alloyRunnerService;

    private File translatedModelFile;

    @PostMapping(value = "/to-alloy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> translateToAlloy(@RequestParam("file") MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            BpmnModelInstance modelInstance = Bpmn.readModelFromStream(inputStream);

            String alloyModel = bpmnToAlloyService.generateInitPredicate(modelInstance);

            // Save the translated Alloy model to a temporary file
            translatedModelFile = saveAlloyModelToFile(alloyModel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "model.als");

            return new ResponseEntity<>(alloyModel, headers, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Failed to process the file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<String> executeModel() {
        if (translatedModelFile == null) {
            return new ResponseEntity<>("No model uploaded.", HttpStatus.BAD_REQUEST);
        }

        try {
            String executionResult = alloyRunnerService.executeAlloyModel(translatedModelFile);
            return new ResponseEntity<>(executionResult, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to execute the model: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private File saveAlloyModelToFile(String alloyModel) throws IOException {
        File tempFile = File.createTempFile("model", ".als");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(alloyModel.getBytes());
        }
        return tempFile;
    }
}