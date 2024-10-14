package hvl.thesis.tobset.defsemtp.services;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;


@Service
public class BpmnToAlloyService {

    public void generateAlloyInit(File alloySpecFile, File outputFile, BpmnModelInstance modelInstance) throws IOException {
        StringBuilder initPredicate = new StringBuilder("pred init [] {\n");
        StringBuilder scope = new StringBuilder("run init for ");

        // Parse BPMN elements
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        Collection<Activity> activities = modelInstance.getModelElementsByType(Activity.class);
        Collection<ExclusiveGateway> exGates = modelInstance.getModelElementsByType(ExclusiveGateway.class);
        Collection<ParallelGateway> paGates = modelInstance.getModelElementsByType(ParallelGateway.class);
        Collection<IntermediateCatchEvent> catchEvents = modelInstance.getModelElementsByType(IntermediateCatchEvent.class);
        Collection<IntermediateThrowEvent> throwEvents = modelInstance.getModelElementsByType(IntermediateThrowEvent.class);
        Collection<EndEvent> terminateEvents = modelInstance.getModelElementsByType(EndEvent.class).stream()
                .filter(e -> "terminate".equals(e.getAttributeValue("eventDefinition")))  // Only TerminateEndEvent
                .collect(Collectors.toList());
        Collection<EventBasedGateway> eventGates = modelInstance.getModelElementsByType(EventBasedGateway.class);
        Collection<SequenceFlow> sequenceFlows = modelInstance.getModelElementsByType(SequenceFlow.class);

        // Define instances and connections in init predicate
        for (StartEvent start : startEvents) {
            initPredicate.append("  one sig ").append(start.getId()).append(" extends StartEvent {}\n");
        }
        for (EndEvent end : endEvents) {
            initPredicate.append("  one sig ").append(end.getId()).append(" extends EndEvent {}\n");
        }
        for (Activity task : activities) {
            initPredicate.append("  one sig ").append(task.getId()).append(" extends Activity {}\n");
        }
        for (ExclusiveGateway exGate : exGates) {
            initPredicate.append("  one sig ").append(exGate.getId()).append(" extends ExGate {}\n");
        }
        for (ParallelGateway paGate : paGates) {
            initPredicate.append("  one sig ").append(paGate.getId()).append(" extends PaGate {}\n");
        }
        for (IntermediateCatchEvent catchEvent : catchEvents) {
            initPredicate.append("  one sig ").append(catchEvent.getId()).append(" extends IntermediateCatchEvent {}\n");
        }
        for (IntermediateThrowEvent throwEvent : throwEvents) {
            initPredicate.append("  one sig ").append(throwEvent.getId()).append(" extends IntermediateThrowEvent {}\n");
        }
        for (EndEvent terminate : terminateEvents) {
            initPredicate.append("  one sig ").append(terminate.getId()).append(" extends TerminateEndEvent {}\n");
        }
        for (EventBasedGateway eventGate : eventGates) {
            initPredicate.append("  one sig ").append(eventGate.getId()).append(" extends EventBasedGateway {}\n");
        }

        initPredicate.append("\n");

        // Define sequence flow connections in init predicate
        for (SequenceFlow flow : sequenceFlows) {
            String sourceId = flow.getSource().getId();
            String targetId = flow.getTarget().getId();
            initPredicate.append("  ").append(flow.getId()).append(".source = ").append(sourceId)
                    .append(" and ").append(flow.getId()).append(".target = ").append(targetId).append("\n");
        }

        initPredicate.append("}\n\n");

        // Determine scope for each element
        scope.append(startEvents.size()).append(" StartEvent, ")
                .append(endEvents.size()).append(" EndEvent, ")
                .append(activities.size()).append(" Activity, ")
                .append(exGates.size()).append(" ExGate, ")
                .append(paGates.size()).append(" PaGate, ")
                .append(catchEvents.size()).append(" IntermediateCatchEvent, ")
                .append(throwEvents.size()).append(" IntermediateThrowEvent, ")
                .append(terminateEvents.size()).append(" TerminateEndEvent, ")
                .append(eventGates.size()).append(" EventBasedGateway, ")
                .append(sequenceFlows.size()).append(" SequenceFlow");

        scope.append("\n");

        // Append init and scope to Alloy specification file
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Copy the original Alloy specification to output
            writer.write(new String(Files.readAllBytes(alloySpecFile.toPath())));

            // Add generated init predicate and scope
            writer.write("\n");
            writer.write(initPredicate.toString());
            writer.write(scope.toString());
        }
    }
}