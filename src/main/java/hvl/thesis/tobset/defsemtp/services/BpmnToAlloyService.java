package hvl.thesis.tobset.defsemtp.services;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.springframework.stereotype.Service;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.TerminateEventDefinition;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.stream.Collectors;


@Service
public class BpmnToAlloyService {

    Path projectRoot = Paths.get("").toAbsolutePath();
    Path resourcePath = projectRoot.resolve("defsemTP/translations/Alloy/bpmn-spec-alloy.als");

    public String specFilePath = resourcePath.toString();

    public String generateInitPredicate(BpmnModelInstance modelInstance) {
        StringBuilder alloyModel = new StringBuilder();

        int startEventIndex = 1, taskIndex = 1, exclusiveGatewayIndex = 1;
        int parallelGatewayOpeningIndex = 1, parallelGatewayClosingIndex = 1;
        int eventBasedGatewayIndex = 1, intermediateCatchEventIndex = 1;
        int intermediateThrowEventIndex = 1, terminateEndEventIndex = 1;
        int endEventIndex = 1, subProcessIndex = 1;
        int flowIndex = 1;

        Map<String, String> nodeNames = new HashMap<>();

        for (StartEvent startEvent : modelInstance.getModelElementsByType(StartEvent.class)) {
            String nodeName = "startEvent" + startEventIndex;
            nodeNames.put(startEvent.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends StartEvent {}\n");
            startEventIndex++;
        }
        for (Task task : modelInstance.getModelElementsByType(Task.class)) {
            String nodeName = "task" + taskIndex;
            nodeNames.put(task.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends Task {}\n");
            taskIndex++;
        }
        for (ExclusiveGateway gateway : modelInstance.getModelElementsByType(ExclusiveGateway.class)) {
            String nodeName = "exclusiveGateway" + exclusiveGatewayIndex;
            nodeNames.put(gateway.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends ExGate {}\n");
            exclusiveGatewayIndex++;
        }

        int parallelGatewayTokenAdjustments = 0;

        for (ParallelGateway gateway : modelInstance.getModelElementsByType(ParallelGateway.class)) {
            String nodeName;
            if (isParallelGatewayOpening(gateway)) {
                nodeName = "parallelGatewayOpening" + parallelGatewayOpeningIndex;
                parallelGatewayOpeningIndex++;
                parallelGatewayTokenAdjustments += gateway.getOutgoing().size() - 1; // Increase by the number of outgoing flows minus one
            } else {
                nodeName = "parallelGatewayClosing" + parallelGatewayClosingIndex;
                parallelGatewayClosingIndex++;
            }
            nodeNames.put(gateway.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends PaGate {}\n");
        }

        for (EventBasedGateway gateway : modelInstance.getModelElementsByType(EventBasedGateway.class)) {
            String nodeName = "eventBasedGateway" + eventBasedGatewayIndex;
            nodeNames.put(gateway.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends EventBasedGateway {}\n");
            eventBasedGatewayIndex++;
        }

        for (IntermediateCatchEvent catchEvent : modelInstance.getModelElementsByType(IntermediateCatchEvent.class)) {
            String nodeName = "intermediateCatchEvent" + intermediateCatchEventIndex;
            nodeNames.put(catchEvent.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends IntermediateCatchEvent {}\n");
            intermediateCatchEventIndex++;
        }

        for (IntermediateThrowEvent throwEvent : modelInstance.getModelElementsByType(IntermediateThrowEvent.class)) {
            String nodeName = "intermediateThrowEvent" + intermediateThrowEventIndex;
            nodeNames.put(throwEvent.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends IntermediateThrowEvent {}\n");
            intermediateThrowEventIndex++;
        }

        for (EndEvent endEvent : modelInstance.getModelElementsByType(EndEvent.class)) {
            String nodeName;
            if (isTerminateEndEvent(endEvent)) {
                nodeName = "terminateEndEvent" + terminateEndEventIndex;
                terminateEndEventIndex++;
            } else {
                nodeName = "endEvent" + endEventIndex;
                endEventIndex++;
            }
            nodeNames.put(endEvent.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends EndEvent {}\n");
        }

        for (SubProcess subProcess : modelInstance.getModelElementsByType(SubProcess.class)) {
            String nodeName = "subProcess" + subProcessIndex;
            nodeNames.put(subProcess.getId(), nodeName);
            alloyModel.append("one sig ").append(nodeName).append(" extends SubProcess {}\n");
            subProcessIndex++;
        }

        alloyModel.append("\npred init {\n");

        Map<String, List<String>> outgoingFlows = new HashMap<>();
        Map<String, List<String>> incomingFlows = new HashMap<>();

        for (SequenceFlow flow : modelInstance.getModelElementsByType(SequenceFlow.class)) {
            String sourceName = nodeNames.get(flow.getSource().getId());
            String targetName = nodeNames.get(flow.getTarget().getId());

            outgoingFlows.computeIfAbsent(sourceName, k -> new ArrayList<>()).add(targetName);
            incomingFlows.computeIfAbsent(targetName, k -> new ArrayList<>()).add(sourceName);
            flowIndex++;
        }

        for (Map.Entry<String, List<String>> entry : outgoingFlows.entrySet()) {
            String sourceName = entry.getKey();
            List<String> targets = entry.getValue();
            alloyModel.append("    #").append(sourceName).append(".outgoingSequenceFlows = ").append(targets.size()).append("\n");
            alloyModel.append("    ").append(sourceName).append(".outgoingSequenceFlows.target = ");
            alloyModel.append(String.join(" + ", targets)).append("\n");
        }

        for (Map.Entry<String, List<String>> entry : incomingFlows.entrySet()) {
            String targetName = entry.getKey();
            List<String> sources = entry.getValue();
            alloyModel.append("    #").append(targetName).append(".incomingSequenceFlows = ").append(sources.size()).append("\n");
            alloyModel.append("    ").append(targetName).append(".incomingSequenceFlows.source = ");
            alloyModel.append(String.join(" + ", sources)).append("\n");
        }

        for (StartEvent startEvent : modelInstance.getModelElementsByType(StartEvent.class)) {
            String nodeName = nodeNames.get(startEvent.getId());
            alloyModel.append("    #").append(nodeName).append(".incomingSequenceFlows = 0\n");
        }

        for (EndEvent endEvent : modelInstance.getModelElementsByType(EndEvent.class)) {
            String nodeName = nodeNames.get(endEvent.getId());
            alloyModel.append("    #").append(nodeName).append(".outgoingSequenceFlows = 0\n");
        }

        startEventIndex = 1;
        for (StartEvent startEvent : modelInstance.getModelElementsByType(StartEvent.class)) {
            String nodeName = "startEvent" + startEventIndex;
            alloyModel.append("    one t").append(startEventIndex).append(": ProcessSnapshot.tokens | t").append(startEventIndex)
                    .append(".pos = ").append(nodeName).append("\n");
            startEventIndex++;
        }

        alloyModel.append("}\n\n");

        int initialTokens = 2;
        int tokenScope = initialTokens + parallelGatewayTokenAdjustments;

        alloyModel.append("run System for ")
                .append(startEventIndex - 1).append(" StartEvent, ")
                .append(taskIndex - 1).append(" Task, ")
                .append(exclusiveGatewayIndex - 1).append(" ExGate, ")
                .append(parallelGatewayOpeningIndex + parallelGatewayClosingIndex - 2).append(" PaGate, ")
                .append(eventBasedGatewayIndex - 1).append(" EventBasedGateway, ")
                .append(intermediateCatchEventIndex - 1).append(" IntermediateCatchEvent, ")
                .append(intermediateThrowEventIndex - 1).append(" IntermediateThrowEvent, ")
                .append(endEventIndex - 1).append(" EndEvent, ")
                .append(terminateEndEventIndex - 1).append(" TerminateEndEvent, ")
                .append(subProcessIndex - 1).append(" SubProcess, ")
                .append(flowIndex - 1).append(" SequenceFlow, ")
                .append("1 Process, ").append(tokenScope).append(" Token, 1 ProcessSnapshot, ")
                .append("1 Event, 50 steps");

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
