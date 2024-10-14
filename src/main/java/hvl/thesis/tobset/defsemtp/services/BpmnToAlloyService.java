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

    public String generateInitPredicate(BpmnModelInstance modelInstance) {
        StringBuilder init = new StringBuilder("pred init [] {\n");

        // Define element counts
        init.append("  #Process = 1\n");
        int startEventCount = modelInstance.getModelElementsByType(StartEvent.class).size();
        int endEventCount = modelInstance.getModelElementsByType(EndEvent.class).size();
        int taskCount = modelInstance.getModelElementsByType(Task.class).size();
        int subprocessCount = modelInstance.getModelElementsByType(SubProcess.class).size();
        int sequenceFlowCount = modelInstance.getModelElementsByType(SequenceFlow.class).size();

        // Set the exact counts for each element type
        init.append("  #StartEvent = ").append(startEventCount).append("\n");
        init.append("  #EndEvent = ").append(endEventCount).append("\n");
        init.append("  #SubProcess = ").append(subprocessCount).append("\n");
        init.append("  #Task = ").append(taskCount).append("\n");
        init.append("  #SequenceFlow = ").append(sequenceFlowCount).append("\n");
        init.append("  #Token = 1\n");
        init.append("  #ProcessSnapshot = 1\n");

        init.append("\n  some pSnapshot: ProcessSnapshot, s: StartEvent, ");
        init.append("sp: SubProcess, subStart: StartEvent, subEnd: EndEvent, mainEnd: EndEvent, act: Task {\n");

        // Process StartEvent and EndEvent connections
        for (SequenceFlow flow : modelInstance.getModelElementsByType(SequenceFlow.class)) {
            String source = flow.getSource().getId();
            String target = flow.getTarget().getId();

            if (flow.getSource() instanceof StartEvent) {
                init.append("    #s.incomingSequenceFlows = 0\n");
                init.append("    #s.outgoingSequenceFlows = 1\n");
                init.append("    s.outgoingSequenceFlows.target = ").append(target).append("\n\n");
            } else if (flow.getTarget() instanceof EndEvent) {
                init.append("    #").append(target).append(".incomingSequenceFlows = 1\n");
                init.append("    ").append(source).append(".outgoingSequenceFlows.target = ").append(target).append("\n\n");
            }
        }

        // SubProcess setup
        init.append("    sp.subStartEvent = subStart\n");
        init.append("    sp.subEndEvents = subEnd\n\n");

        // Task and SequenceFlow mappings
        for (Task task : modelInstance.getModelElementsByType(Task.class)) {
            init.append("    #").append(task.getId()).append(".incomingSequenceFlows = 1\n");
            init.append("    #").append(task.getId()).append(".outgoingSequenceFlows = 1\n");
        }

        for (SequenceFlow flow : modelInstance.getModelElementsByType(SequenceFlow.class)) {
            init.append("    ").append(flow.getId()).append(".source = ").append(flow.getSource().getId())
                    .append(" and ").append(flow.getId()).append(".target = ").append(flow.getTarget().getId()).append("\n");
        }

        // Token Initialization
        init.append("\n    one t: pSnapshot.tokens {\n");
        init.append("      t.pos = s\n");
        init.append("    }\n");

        init.append("  }\n");
        init.append("}");
        return init.toString();
    }
}