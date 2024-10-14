package hvl.thesis.tobset.defsemtp.services;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BpmnToAlloyServiceTest {

    private BpmnToAlloyService bpmnToAlloyService;

    @BeforeEach
    void setUp() {
        bpmnToAlloyService = new BpmnToAlloyService();
    }

    @Test
    void testGenerateInitPredicate() {
        // Step 1: Create a simple BPMN model instance with StartEvent, Task, and EndEvent
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        // Create Definitions element and add it to the model
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        modelInstance.setDefinitions(definitions);

        // Create a Process element and add it to Definitions
        Process process = modelInstance.newInstance(Process.class);
        process.setId("process1");
        definitions.addChildElement(process);

        // Create StartEvent
        StartEvent startEvent = modelInstance.newInstance(StartEvent.class);
        startEvent.setId("startEvent1");
        process.addChildElement(startEvent);

        // Create Task
        Task task = modelInstance.newInstance(Task.class);
        task.setId("task1");
        process.addChildElement(task);

        // Create EndEvent
        EndEvent endEvent = modelInstance.newInstance(EndEvent.class);
        endEvent.setId("endEvent1");
        process.addChildElement(endEvent);

        // Create SequenceFlows
        SequenceFlow flow1 = modelInstance.newInstance(SequenceFlow.class);
        flow1.setId("flow1");
        flow1.setSource(startEvent);
        flow1.setTarget(task);
        process.addChildElement(flow1);

        SequenceFlow flow2 = modelInstance.newInstance(SequenceFlow.class);
        flow2.setId("flow2");
        flow2.setSource(task);
        flow2.setTarget(endEvent);
        process.addChildElement(flow2);

        // Step 2: Generate Alloy init predicate from the BPMN model
        String actualInitPredicate = bpmnToAlloyService.generateInitPredicate(modelInstance);

        // Step 3: Define expected Alloy init predicate
        String expectedInitPredicate =
                "pred init [] {\n" +
                        "    #Process = 1\n" +
                        "    #StartEvent = 1\n" +
                        "    #Task = 1\n" +
                        "    #EndEvent = 1\n" +
                        "    #SequenceFlow = 2\n" +
                        "    #Token = 1\n" +
                        "    #ProcessSnapshot = 1\n\n" +
                        "    some pSnapshot: ProcessSnapshot, s: StartEvent, task: Task, e: EndEvent {\n" +
                        "        #s.incomingSequenceFlows = 0\n" +
                        "        #s.outgoingSequenceFlows = 1\n" +
                        "        s.outgoingSequenceFlows.target = task\n\n" +
                        "        #task.incomingSequenceFlows = 1\n" +
                        "        #task.outgoingSequenceFlows = 1\n" +
                        "        task.outgoingSequenceFlows.target = e\n\n" +
                        "        #e.incomingSequenceFlows = 1\n" +
                        "        #e.outgoingSequenceFlows = 0\n\n" +
                        "        // Initial token position at StartEvent\n" +
                        "        one t: pSnapshot.tokens { t.pos = s }\n" +
                        "    }\n" +
                        "}";

        // Step 4: Assert that generated predicate matches expected predicate
        assertEquals(expectedInitPredicate, actualInitPredicate);
    }
}
