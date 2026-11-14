function businessDate(value) {
    return new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Karachi",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).format(new Date(value));
}

export function filterSavedSales(sales, search, date) {
    const term = search.trim().toLowerCase();
    return sales.filter((sale) => {
        const itemText = sale.items.map((item) => `${item.sku} ${item.title}`).join(" ");
        const haystack = `${sale.id} ${sale.customerName ?? ""} ${sale.cashierName} ${itemText}`.toLowerCase();
        return (!term || haystack.includes(term))
            && (!date || businessDate(sale.saleDate) === date);
    });
}

export function summarizeSavedSales(sales) {
    return sales.reduce((summary, sale) => ({
        transactionCount: summary.transactionCount + 1,
        revenueCents: summary.revenueCents + Math.round(sale.totalAmount * 100),
        bookQuantity: summary.bookQuantity + sale.items.reduce(
            (quantity, item) => quantity + item.quantity, 0,
        ),
    }), { transactionCount: 0, revenueCents: 0, bookQuantity: 0 });
}
