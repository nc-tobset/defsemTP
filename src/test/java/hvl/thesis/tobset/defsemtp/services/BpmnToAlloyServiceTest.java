package hvl.thesis.tobset.defsemtp.services;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class BpmnToAlloyServiceTest {

    private BpmnToAlloyService service;
    private BpmnModelInstance modelInstance;

    @BeforeEach
    void setUp() {
        service = new BpmnToAlloyService();

        // Create a BPMN model instance with a parallel gateway opening
        modelInstance = Bpmn.createProcess("process")
                .startEvent("startEvent")
                .parallelGateway("parallelGatewayOpening") // Opening gateway
                .serviceTask("task1").name("Task 1") // First branch from the gateway
                .parallelGateway("parallelGatewayClosing") // Closing gateway
                .endEvent("endEvent") // End event
                .moveToNode("parallelGatewayOpening") // Move back to opening gateway
                .serviceTask("task2").name("Task 2") // Second branch from the gateway
                .connectTo("parallelGatewayClosing") // Connect second task to closing gateway
                .done();

        // Access the process model and elements to configure sequence flows
        Process process = modelInstance.getModelElementById("process");
        ParallelGateway openingGateway = modelInstance.getModelElementById("parallelGatewayOpening");

        // Ensure there are two outgoing flows
        Collection<SequenceFlow> outgoingFlows = openingGateway.getOutgoing();
        assertTrue(outgoingFlows.size() > 1, "Parallel gateway should have multiple outgoing flows for opening detection.");
    }


    @Test
    void testGenerateInitPredicate() {
        String alloyModel = service.generateInitPredicate(modelInstance);

        // Check that basic structure exists
        assertTrue(alloyModel.contains("pred init {"));
        assertTrue(alloyModel.contains("run init for"));

        // Check that StartEvent, Task, and EndEvent are generated
        assertTrue(alloyModel.contains("startEvent1"));
        assertTrue(alloyModel.contains("task1"));
        assertTrue(alloyModel.contains("endEvent1"));

        // Check for the presence of ParallelGateway with opening and closing labels
        assertTrue(alloyModel.contains("parallelGatewayOpening1"));
        assertTrue(alloyModel.contains("parallelGatewayClosing1"));

    }

    @Test
    void testIsParallelGatewayOpening() {
        ParallelGateway parallelGatewayOpening = (ParallelGateway) modelInstance.getModelElementById("parallelGatewayOpening");
        ParallelGateway parallelGatewayClosing = (ParallelGateway) modelInstance.getModelElementById("parallelGatewayClosing");

        assertTrue(service.isParallelGatewayOpening(parallelGatewayOpening), "Expected parallel gateway to be identified as opening.");
        assertFalse(service.isParallelGatewayOpening(parallelGatewayClosing), "Expected parallel gateway to be identified as closing.");
    }

    @Test
    void testIsTerminateEndEvent() {
        // Set up an EndEvent in the BPMN model
        EndEvent endEvent = modelInstance.newInstance(EndEvent.class);
        endEvent.setId("endEvent");
        TerminateEventDefinition terminateDefinition = modelInstance.newInstance(TerminateEventDefinition.class);
        endEvent.addChildElement(terminateDefinition);

        // Add the EndEvent to the model
        Process process = modelInstance.getModelElementById("process");
        process.addChildElement(endEvent);

        assertTrue(service.isTerminateEndEvent(endEvent), "Expected the end event to be recognized as a TerminateEndEvent.");
    }

}
