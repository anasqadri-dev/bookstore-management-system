export declare function validateSalesDateRange(startDate: string, endDate: string): string;
export declare function buildSalesReportPath(startDate: string, endDate: string): string;
export declare function filterReportRows<T extends Record<string, unknown>>(rows: T[], search: string, fields: (keyof T)[]): T[];
export declare function formatReportMoney(value: number): string;
