package hvl.thesis.tobset.defsemtp.services;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.springframework.stereotype.Service;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.TerminateEventDefinition;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;


@Service
public class BpmnToAlloyService {

    public String generateInitPredicate(BpmnModelInstance modelInstance) {
        StringBuilder alloyModel = new StringBuilder();

        int startEventIndex = 1, taskIndex = 1, exclusiveGatewayIndex = 1;
        int parallelGatewayOpeningIndex = 1, parallelGatewayClosingIndex = 1;
        int eventBasedGatewayIndex = 1, intermediateCatchEventIndex = 1;
        int intermediateThrowEventIndex = 1, terminateEndEventIndex = 1;
        int endEventIndex = 1, subProcessIndex = 1;

        for (StartEvent startEvent : modelInstance.getModelElementsByType(StartEvent.class)) {
            alloyModel.append("one sig startEvent").append(startEventIndex).append(" extends StartEvent {}\n");
            startEventIndex++;
        }
        for (Task task : modelInstance.getModelElementsByType(Task.class)) {
            alloyModel.append("one sig task").append(taskIndex).append(" extends Task {}\n");
            taskIndex++;
        }
        for (ExclusiveGateway gateway : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
            alloyModel.append("one sig exclusiveGateway").append(exclusiveGatewayIndex).append(" extends ExclusiveGateway {}\n");
            exclusiveGatewayIndex++;
        }

        int parallelGatewayTokenAdjustments = 0;

        for (ParallelGateway gateway : modelInstance.getModelElementsByType(ParallelGateway.class)) {
            if (isParallelGatewayOpening(gateway)) {
                alloyModel.append("one sig parallelGatewayOpening").append(parallelGatewayOpeningIndex).append(" extends ParallelGateway {}\n");
                parallelGatewayOpeningIndex++;
                parallelGatewayTokenAdjustments++;
            } else {
                alloyModel.append("one sig parallelGatewayClosing").append(parallelGatewayClosingIndex).append(" extends ParallelGateway {}\n");
                parallelGatewayClosingIndex++;
                parallelGatewayTokenAdjustments--;
            }
        }

        for (EventBasedGateway gateway : modelInstance.getModelElementsByType(EventBasedGateway.class)) {
            alloyModel.append("one sig eventBasedGateway").append(eventBasedGatewayIndex).append(" extends EventBasedGateway {}\n");
            eventBasedGatewayIndex++;
        }

        for (IntermediateCatchEvent catchEvent : modelInstance.getModelElementsByType(IntermediateCatchEvent.class)) {
            alloyModel.append("one sig intermediateCatchEvent").append(intermediateCatchEventIndex).append(" extends IntermediateCatchEvent {}\n");
            intermediateCatchEventIndex++;
        }

        for (IntermediateThrowEvent throwEvent : modelInstance.getModelElementsByType(IntermediateThrowEvent.class)) {
            alloyModel.append("one sig intermediateThrowEvent").append(intermediateThrowEventIndex).append(" extends IntermediateThrowEvent {}\n");
            intermediateThrowEventIndex++;
        }

        for (EndEvent endEvent : modelInstance.getModelElementsByType(EndEvent.class)) {
            if (isTerminateEndEvent(endEvent)) {
                alloyModel.append("one sig terminateEndEvent").append(terminateEndEventIndex).append(" extends TerminateEndEvent {}\n");
                terminateEndEventIndex++;
            } else {
                alloyModel.append("one sig endEvent").append(endEventIndex).append(" extends EndEvent {}\n");
                endEventIndex++;
            }
        }

        for (SubProcess subProcess : modelInstance.getModelElementsByType(SubProcess.class)) {
            alloyModel.append("one sig subProcess").append(subProcessIndex).append(" extends SubProcess {}\n");
            subProcessIndex++;
        }

        alloyModel.append("\npred init {\n");

        int flowIndex = 1;
        for (SequenceFlow flow : modelInstance.getModelElementsByType(SequenceFlow.class)) {
            String sourceId = flow.getSource().getId();
            String targetId = flow.getTarget().getId();

            alloyModel.append("    sequenceFlow").append(flowIndex).append(".source = ").append(sourceId)
                    .append(" and sequenceFlow").append(flowIndex).append(".target = ").append(targetId).append("\n");
            flowIndex++;
        }

        startEventIndex = 1;
        for (StartEvent startEvent : modelInstance.getModelElementsByType(StartEvent.class)) {
            alloyModel.append("    one t").append(startEventIndex).append(": Token | t").append(startEventIndex)
                    .append(".pos = startEvent").append(startEventIndex).append("\n");
            startEventIndex++;
        }

        alloyModel.append("}\n\n");

        int initialTokens = startEventIndex - 1;
        int tokenScope = initialTokens + parallelGatewayTokenAdjustments;

        alloyModel.append("run init for ")
                .append(startEventIndex - 1).append(" StartEvent, ")
                .append(taskIndex - 1).append(" Task, ")
                .append(exclusiveGatewayIndex - 1).append(" ExclusiveGateway, ")
                .append(parallelGatewayOpeningIndex + parallelGatewayClosingIndex - 2).append(" ParallelGateway, ")
                .append(eventBasedGatewayIndex - 1).append(" EventBasedGateway, ")
                .append(intermediateCatchEventIndex - 1).append(" IntermediateCatchEvent, ")
                .append(intermediateThrowEventIndex - 1).append(" IntermediateThrowEvent, ")
                .append(endEventIndex - 1).append(" EndEvent, ")
                .append(terminateEndEventIndex - 1).append(" TerminateEndEvent, ")
                .append(subProcessIndex - 1).append(" SubProcess, ")
                .append(flowIndex - 1).append(" SequenceFlow, ")
                .append("1 Process, ").append(tokenScope).append(" Token, 1 ProcessSnapshot");

        return alloyModel.toString();
    }

    public boolean isParallelGatewayOpening(ParallelGateway gateway) {
        // if opening --> outgoing flows will be >1
        return gateway.getOutgoing().size() > 1;
    }

    public boolean isTerminateEndEvent(EndEvent endEvent) {
        Collection<TerminateEventDefinition> terminateEventDefinitions = endEvent.getChildElementsByType(TerminateEventDefinition.class);
        return !terminateEventDefinitions.isEmpty();
    }

    public void generateAlloyModelFile(String bpmnSpecFilePath, BpmnModelInstance modelInstance, String outputFilePath) {
        try {
            StringBuilder alloyModel = new StringBuilder();
            Files.lines(Paths.get(bpmnSpecFilePath)).forEach(line -> alloyModel.append(line).append("\n"));

            String initPredicate = generateInitPredicate(modelInstance);
            alloyModel.append("\n// Generated init predicate\n").append(initPredicate);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(alloyModel.toString());
            }

            System.out.println("Alloy model file generated at: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("Error generating Alloy model file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}