export interface BackendHealthData { status?: string; database?: string; }
export declare function isBackendReady(data: BackendHealthData | null | undefined): boolean;
