export {};

declare global {
    interface Window {
        bmsDesktop?: {
            isDesktop: boolean;
            electronVersion: string;
            platform: string;
            chooseBackupDestination: () => Promise<string | null>;
            chooseBackupFile: () => Promise<string | null>;
        };
    }
}
