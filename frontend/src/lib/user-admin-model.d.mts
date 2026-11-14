export interface StaffDraft { username: string; fullName: string; password: string; }
export interface StaffUpdateDraft { fullName: string; newPassword: string; active: boolean; }
export declare function validateStaffDraft(value: StaffDraft): string;
export declare function validateStaffUpdate(value: StaffUpdateDraft): string;
export declare function buildCreateStaffPayload(value: StaffDraft): StaffDraft;
export declare function buildUpdateStaffPayload(value: StaffUpdateDraft): { fullName: string; newPassword: string | null; active: boolean };
