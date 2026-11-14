const { contextBridge, ipcRenderer } = require("electron");

// Expose informational, non-privileged runtime metadata only.
// No filesystem, process, shell, IPC, or arbitrary URL APIs are exposed.
contextBridge.exposeInMainWorld("bmsDesktop", Object.freeze({
    isDesktop: true,
    electronVersion: process.versions.electron ?? "unknown",
    platform: process.platform,
    chooseBackupDestination: () => ipcRenderer.invoke("bms:choose-backup-destination"),
    chooseBackupFile: () => ipcRenderer.invoke("bms:choose-backup-file"),
}));
