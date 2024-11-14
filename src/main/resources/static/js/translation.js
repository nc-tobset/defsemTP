export async function handleTranslate() {
    const fileInput = document.getElementById('bpmnTranslationFileInput');
    const file = fileInput.files[0];

    if (!file || !file.name.endsWith('.bpmn')) {
        alert('Please upload a valid BPMN file.');
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    const loadingSpinner = document.getElementById('loading-spinner');
    const resultContainer = document.getElementById('result-container');
    const resultMessage = document.getElementById('result-message');

    loadingSpinner.style.display = 'block';
    resultContainer.style.display = 'none';

    try {
        const response = await fetch('/api/translate/to-alloy', {
            method: 'POST',
            body: formData
        });

        loadingSpinner.style.display = 'none';

        if (response.ok) {
            const alloyModel = await response.text();
            const blob = new Blob([alloyModel], { type: 'text/plain' });
            const downloadLink = document.getElementById('downloadLink');

            downloadLink.href = URL.createObjectURL(blob);
            downloadLink.style.display = 'inline';
            downloadLink.innerText = 'Download Alloy Model';

            // Show the execute button
            document.getElementById('executeButton').style.display = 'inline';
        } else {
            alert('Failed to translate the BPMN file.');
        }
    } catch (error) {
        loadingSpinner.style.display = 'none';
        console.error('Error:', error);
        alert('An error occurred while processing the file.');
    }
}

export async function handleExecute() {
    const loadingSpinner = document.getElementById('loading-spinner');
    const resultContainer = document.getElementById('result-container');
    const resultMessage = document.getElementById('result-message');

    loadingSpinner.style.display = 'block';
    resultContainer.style.display = 'none';

    try {
        const response = await fetch('/api/translate/execute', {
            method: 'POST'
        });

        loadingSpinner.style.display = 'none';

        if (response.ok) {
            const result = await response.text();
            resultMessage.innerText = `Solution found! This means the model eventually terminates and is never unsafe. Congratulations on making a functional business process model!\n\n${result}`;
            resultContainer.style.display = 'block';
        } else {
            resultMessage.innerText = 'Failed to execute the Alloy model.';
            resultContainer.style.display = 'block';
        }
    } catch (error) {
        loadingSpinner.style.display = 'none';
        resultMessage.innerText = 'An error occurred while executing the model.';
        resultContainer.style.display = 'block';
        console.error('Error:', error);
    }
}

export function initializeButtons() {
    document.getElementById('translateButton').addEventListener('click', handleTranslate);
    document.getElementById('executeButton').addEventListener('click', handleExecute);
}