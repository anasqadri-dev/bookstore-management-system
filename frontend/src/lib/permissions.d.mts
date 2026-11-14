export type UserRole = "ADMIN" | "STAFF";
export declare function canAccessSection(role: UserRole, section: string): boolean;
export declare function getVisibleSections<T extends { name: string }>(role: UserRole, sections: T[]): T[];
export declare function canManageInventory(role: UserRole): boolean;
