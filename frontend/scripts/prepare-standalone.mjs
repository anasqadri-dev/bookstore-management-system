import { cp, mkdir } from "node:fs/promises";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const nextRoot = path.join(projectRoot, ".next");
const standaloneRoot = path.join(nextRoot, "standalone");

if (!existsSync(path.join(standaloneRoot, "server.js"))) {
    throw new Error("Next.js standalone server.js was not generated. Check next.config.ts output mode.");
}

const staticSource = path.join(nextRoot, "static");
const staticTarget = path.join(standaloneRoot, ".next", "static");
await mkdir(path.dirname(staticTarget), { recursive: true });
if (existsSync(staticSource)) {
    await cp(staticSource, staticTarget, { recursive: true, force: true });
}

const publicSource = path.join(projectRoot, "public");
const publicTarget = path.join(standaloneRoot, "public");
if (existsSync(publicSource)) {
    await cp(publicSource, publicTarget, { recursive: true, force: true });
}

process.stdout.write("Prepared Next.js standalone runtime assets.\n");
