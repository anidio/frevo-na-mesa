const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let javaProcess;

function createWindow() {
    const jarPath = path.join(__dirname, '..', 'backend', 'target', 'frevonamesa-0.0.1-SNAPSHOT.jar');

    // A MÁGICA ACONTECE AQUI:
    // Iniciamos o backend passando o argumento para ativar o perfil "desktop"
    javaProcess = spawn('java', [
        '-jar',
        jarPath,
        '--spring.profiles.active=desktop'
    ]);

    javaProcess.stdout.on('data', (data) => {
        console.log(`Backend: ${data}`);
    });
    javaProcess.stderr.on('data', (data) => {
        console.error(`Backend ERROR: ${data}`);
    });

    const mainWindow = new BrowserWindow({ width: 1200, height: 800 });
    mainWindow.loadFile(path.join(__dirname, '..', 'frontend', 'dist', 'index.html'));
}

app.whenReady().then(createWindow);

app.on('will-quit', () => {
    if (javaProcess) {
        javaProcess.kill();
    }
});