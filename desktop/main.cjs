const { app, BrowserWindow, dialog } = require("electron");
const { spawn } = require("node:child_process");
const http = require("node:http");
const path = require("node:path");
const {
    isBackendHealthReady,
    isTrustedNavigation,
    buildBackendEnvironment,
    waitForService,
    stopChildProcess,
} = require("./runtime-core.cjs");

const LOCAL_HOST = "127.0.0.1";
const BACKEND_PORT = 8080;
const FRONTEND_PORT = 3000;
const LOCAL_ORIGIN = `http://${LOCAL_HOST}:${FRONTEND_PORT}`;
const STARTUP_TIMEOUT_MS = 60000;

let backendProcess = null;
let frontendProcess = null;
let mainWindow = null;
let shutdownPromise = null;
let allowQuit = false;

const singleInstanceLock = app.requestSingleInstanceLock();
if (!singleInstanceLock) {
    app.quit();
} else {
    app.on("second-instance", () => {
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.focus();
        }
    });

    app.whenReady().then(startApplication).catch(async (error) => {
        await showStartupFailure(error);
        app.quit();
    });
}

function runtimeResource(...segments) {
    return app.isPackaged
        ? path.join(process.resourcesPath, ...segments)
        : path.resolve(__dirname, "..", ...segments);
}

function requestJson(url, timeoutMs = 2500) {
    return new Promise((resolve, reject) => {
        const request = http.get(url, { timeout: timeoutMs, headers: { Accept: "application/json" } }, (response) => {
            const chunks = [];
            response.on("data", (chunk) => chunks.push(chunk));
            response.on("end", () => {
                let body = null;
                try {
                    body = JSON.parse(Buffer.concat(chunks).toString("utf8"));
                } catch {
                    // A malformed health payload is not a ready service.
                }
                resolve({ statusCode: response.statusCode ?? 0, body });
            });
        });
        request.on("timeout", () => request.destroy(new Error("Local service probe timed out.")));
        request.on("error", reject);
    });
}

function javaExecutable() {
    if (app.isPackaged) {
        return path.join(process.resourcesPath, "jre", "bin",
            process.platform === "win32" ? "java.exe" : "java");
    }
    const javaHome = process.env.JAVA_HOME;
    return javaHome
        ? path.join(javaHome, "bin", process.platform === "win32" ? "java.exe" : "java")
        : (process.platform === "win32" ? "java.exe" : "java");
}

function startBackend() {
    const jarPath = app.isPackaged
        ? path.join(process.resourcesPath, "backend", "backend-bookstore-management-system-0.0.1-SNAPSHOT.jar")
        : path.resolve(__dirname, "..", "backend-bookstore-management-system", "target",
            "backend-bookstore-management-system-0.0.1-SNAPSHOT.jar");
    const databasePath = path.join(app.getPath("userData"), "bookstore.db");
    const env = buildBackendEnvironment(process.env, databasePath, LOCAL_ORIGIN);

    backendProcess = spawn(javaExecutable(), [
        "-jar",
        jarPath,
        "--server.address=127.0.0.1",
        `--server.port=${BACKEND_PORT}`,
    ], {
        env,
        windowsHide: true,
        stdio: "ignore",
    });

    let alive = true;
    backendProcess.once("exit", () => { alive = false; });
    backendProcess.once("error", () => { alive = false; });

    return waitForService({
        timeoutMs: STARTUP_TIMEOUT_MS,
        intervalMs: 400,
        isAlive: () => alive,
        probe: async () => {
            const result = await requestJson(`http://${LOCAL_HOST}:${BACKEND_PORT}/api/health`);
            return isBackendHealthReady(result.statusCode, result.body);
        },
    });
}

function startFrontend() {
    const frontendRoot = runtimeResource("frontend");
    const serverPath = path.join(frontendRoot, "server.js");
    const env = {
        ...process.env,
        ELECTRON_RUN_AS_NODE: "1",
        NODE_ENV: "production",
        NEXT_TELEMETRY_DISABLED: "1",
        HOSTNAME: LOCAL_HOST,
        PORT: String(FRONTEND_PORT),
        NEXT_PUBLIC_API_BASE_URL: `http://${LOCAL_HOST}:${BACKEND_PORT}`,
    };

    frontendProcess = spawn(process.execPath, [serverPath], {
        cwd: frontendRoot,
        env,
        windowsHide: true,
        stdio: "ignore",
    });

    let alive = true;
    frontendProcess.once("exit", () => { alive = false; });
    frontendProcess.once("error", () => { alive = false; });

    return waitForService({
        timeoutMs: STARTUP_TIMEOUT_MS,
        intervalMs: 250,
        isAlive: () => alive,
        probe: async () => {
            const result = await requestJson(LOCAL_ORIGIN);
            return result.statusCode === 200;
        },
    });
}

async function startApplication() {
    app.setAppUserModelId("com.qadribookstore.managementsystem");
    await startBackend();
    await startFrontend();
    createMainWindow();
}

function createMainWindow() {
    mainWindow = new BrowserWindow({
        width: 1440,
        height: 900,
        minWidth: 960,
        minHeight: 640,
        show: false,
        backgroundColor: "#f3f4f6",
        webPreferences: {
            preload: path.join(__dirname, "preload.cjs"),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true,
            webSecurity: true,
            allowRunningInsecureContent: false,
        },
    });

    mainWindow.webContents.setWindowOpenHandler(() => ({ action: "deny" }));
    mainWindow.webContents.on("will-navigate", (event, targetUrl) => {
        if (!isTrustedNavigation(targetUrl, LOCAL_ORIGIN)) event.preventDefault();
    });
    mainWindow.webContents.on("will-redirect", (event, targetUrl) => {
        if (!isTrustedNavigation(targetUrl, LOCAL_ORIGIN)) event.preventDefault();
    });

    mainWindow.once("ready-to-show", () => mainWindow?.show());
    mainWindow.on("closed", () => { mainWindow = null; });
    mainWindow.loadURL(LOCAL_ORIGIN);
}

async function showStartupFailure(error) {
    const message = error instanceof Error ? error.message : "Unknown startup error.";
    await dialog.showMessageBox({
        type: "error",
        title: "Bookstore Management System",
        message: "The local bookstore services could not be started.",
        detail: `${message}\n\nCheck that the required application files are installed and that local ports 3000 and 8080 are available. Your database is stored separately in the Windows application-data directory.`,
        buttons: ["Exit"],
        defaultId: 0,
        noLink: true,
    });
    await shutdownServices();
}

async function shutdownServices() {
    if (shutdownPromise) return shutdownPromise;
    shutdownPromise = (async () => {
        await Promise.allSettled([
            stopChildProcess(frontendProcess, 5000),
            stopChildProcess(backendProcess, 8000),
        ]);
        frontendProcess = null;
        backendProcess = null;
    })();
    return shutdownPromise;
}

app.on("before-quit", (event) => {
    if (allowQuit) return;
    event.preventDefault();
    void shutdownServices().finally(() => {
        allowQuit = true;
        app.quit();
    });
});

app.on("window-all-closed", () => {
    if (process.platform !== "darwin") app.quit();
});

app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0 && backendProcess && frontendProcess) {
        createMainWindow();
    }
});
