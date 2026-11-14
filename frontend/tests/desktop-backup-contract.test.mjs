import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const read = (path) => readFile(new URL(path, import.meta.url), "utf8");

test("Settings uses the documented backup and restore endpoints", async () => {
    const settings = await read("../src/components/Settings.tsx");
    assert.ok(settings.includes('apiRequest<void>("/api/backup"'));
    assert.ok(settings.includes('apiRequest<void>("/api/backup/restore"'));
    assert.ok(settings.includes("chooseBackupDestination()"));
    assert.ok(settings.includes("chooseBackupFile()"));
    assert.ok(settings.includes("window.confirm("));
    assert.ok(settings.includes('window.dispatchEvent(new Event("bms:session-expired"))'));
});

test("preload exposes only the two native database file-picker operations", async () => {
    const preload = await read("../../desktop/preload.cjs");
    assert.ok(preload.includes('chooseBackupDestination: () => ipcRenderer.invoke("bms:choose-backup-destination")'));
    assert.ok(preload.includes('chooseBackupFile: () => ipcRenderer.invoke("bms:choose-backup-file")'));
    assert.equal(preload.includes("readFileSync"), false);
    assert.equal(preload.includes("writeFileSync"), false);
    assert.equal(preload.includes("shell.openExternal"), false);
});

test("main process validates the renderer origin before opening native file dialogs", async () => {
    const main = await read("../../desktop/main.cjs");
    assert.ok(main.includes('ipcMain.handle("bms:choose-backup-destination"'));
    assert.ok(main.includes('ipcMain.handle("bms:choose-backup-file"'));
    assert.ok(main.includes('isTrustedNavigation(event.senderFrame?.url ?? "", LOCAL_ORIGIN)'));
    assert.ok(main.includes('extensions: ["db", "sqlite", "sqlite3"]'));
});

test("POS and saved Sales History receipts expose native print-dialog layouts", async () => {
    const pos = await read("../src/components/SalesPOS.tsx");
    const history = await read("../src/components/SalesHistory.tsx");
    const styles = await read("../src/app/globals.css");
    for (const source of [pos, history]) {
        assert.ok(source.includes("window.print()"));
        assert.ok(source.includes('data-paper-width={paperWidth}'));
        assert.ok(source.includes('value="58"') || source.includes('value="80"'));
        assert.ok(source.includes('value="standard"'));
        assert.ok(source.includes('className="receipt-print'));
    }
    assert.ok(styles.includes('.receipt-print[data-paper-width="58"]'));
    assert.ok(styles.includes('.receipt-print[data-paper-width="80"]'));
    assert.ok(styles.includes('.receipt-print[data-paper-width="standard"]'));
});

test("receipt print actions use the native Windows printer dialog in Electron", async () => {
    const preload = await read("../../desktop/preload.cjs");
    const main = await read("../../desktop/main.cjs");
    const pos = await read("../src/components/SalesPOS.tsx");
    const history = await read("../src/components/SalesHistory.tsx");
    assert.ok(preload.includes('printReceipt: () => ipcRenderer.invoke("bms:print-receipt")'));
    assert.ok(main.includes('ipcMain.handle("bms:print-receipt"'));
    assert.ok(main.includes("{ silent: false, printBackground: true }"));
    assert.ok(main.includes('isTrustedNavigation(event.senderFrame?.url ?? "", LOCAL_ORIGIN)'));
    assert.ok(pos.includes("window.bmsDesktop.printReceipt()"));
    assert.ok(history.includes("window.bmsDesktop.printReceipt()"));
});
