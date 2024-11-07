import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import TokenSimulationModule from 'bpmn-js-token-simulation/lib/viewer';

const viewer = new BpmnViewer({
  container: '#bpmn-viewer',
  additionalModules: [
    TokenSimulationModule
  ]
});

document.getElementById('bpmnFileInput').addEventListener('change', async (event) => {
    const file = event.target.files[0];

    if (file && file.name.endsWith('.bpmn')) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('/upload-bpmn', {
                method: 'POST',
                body: formData
            });

            const result = await response.text();
            alert(result);

            const fileReader = new FileReader();
            fileReader.onload = async function (e) {
                const bpmnXML = e.target.result;
                try {
                    // Import
                    await viewer.importXML(bpmnXML);
                    viewer.get('canvas').zoom('fit-viewport');

                    // Token Simulation API
                    const tokenSimulation = viewer.get('tokenSimulation');
                    tokenSimulation.toggle(); // Starts simulation mode

                } catch (err) {
                    console.error('Could not display BPMN diagram', err);
                    alert('Error loading BPMN diagram. Please try again.');
                }
            };
            fileReader.readAsText(file);

        } catch (error) {
            console.error('Error uploading BPMN file:', error);
            alert('Failed to upload BPMN file.');
        }
    } else {
        alert('Please upload a valid BPMN file.');
    }
});
/**
import { initializeTranslateButton } from './translation';

document.addEventListener('DOMContentLoaded', () => {
    initializeTranslateButton();
});
**/
