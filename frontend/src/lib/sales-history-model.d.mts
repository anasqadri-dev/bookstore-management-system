export interface SavedSaleItem { sku: string; title: string; quantity: number; }
export interface SavedSale {
    id: number;
    saleDate: string;
    customerName: string | null;
    cashierName: string;
    totalAmount: number;
    items: SavedSaleItem[];
}
export interface SavedSalesSummary {
    transactionCount: number;
    revenueCents: number;
    bookQuantity: number;
}
export declare function filterSavedSales<T extends SavedSale>(sales: T[], search: string, date: string): T[];
export declare function summarizeSavedSales(sales: SavedSale[]): SavedSalesSummary;
