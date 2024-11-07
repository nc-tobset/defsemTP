export async function handleTranslate() {
    const fileInput = document.getElementById('bpmnTranslationFileInput');
    const file = fileInput.files[0];

    if (!file || !file.name.endsWith('.bpmn')) {
        alert('Please upload a valid BPMN file.');
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch('/api/translate/to-alloy', {
            method: 'POST',
            body: formData
        });

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
        console.error('Error:', error);
        alert('An error occurred while processing the file.');
    }
}

export async function handleExecute() {
    try {
        const response = await fetch('/api/translate/execute', {
            method: 'POST'
        });

        if (response.ok) {
            const result = await response.text();
            alert(result);
        } else {
            alert('Failed to execute the Alloy model.');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred while executing the model.');
    }
}

export function initializeButtons() {
    document.getElementById('translateButton').addEventListener('click', handleTranslate);
    document.getElementById('executeButton').addEventListener('click', handleExecute);
}