package hvl.thesis.tobset.defsemtp.services;

import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class AlloyRunnerService {

    public String executeAlloyModel(MultipartFile file) {
        try {
            // Save the uploaded file to a temporary location
            File tempFile = convertMultipartFileToFile(file);
            return executeAlloyModel(tempFile);
        } catch (IOException e) {
            return "Error converting MultipartFile to File: " + e.getMessage();
        }
    }

    public String executeAlloyModel(File file) {
        try {
            // Set up Alloy options
            A4Options options = new A4Options();

            // Parse the Alloy model
            CompModule world = CompUtil.parseEverything_fromFile(null, null, file.getAbsolutePath());

            // Execute the first command in the model
            Command cmd = world.getAllCommands().get(0);
            A4Solution solution = TranslateAlloyToKodkod.execute_command(null, world.getAllReachableSigs(), cmd, options);

            // Process the results
            if (solution.satisfiable()) {
                return "Solution found!";
            } else {
                return "No solution found.";
            }
        } catch (Exception e) {
            return "Error executing Alloy model: " + e.getMessage();
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
