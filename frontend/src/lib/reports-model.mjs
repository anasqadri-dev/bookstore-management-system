export function validateSalesDateRange(startDate, endDate) {
    if (!startDate || !endDate) return "Choose both a start date and an end date.";
    if (!/^\\d{4}-\\d{2}-\\d{2}$/.test(startDate)
        || !/^\\d{4}-\\d{2}-\\d{2}$/.test(endDate)) {
        return "Dates must use the YYYY-MM-DD format.";
    }
    if (startDate > endDate) return "Start date cannot be after the end date.";
    return "";
}

export function buildSalesReportPath(startDate, endDate) {
    const validationError = validateSalesDateRange(startDate, endDate);
    if (validationError) throw new Error(validationError);
    const params = new URLSearchParams({ startDate, endDate });
    return `/api/reports/sales?${params.toString()}`;
}

export function filterReportRows(rows, search, fields) {
    const query = search.trim().toLocaleLowerCase();
    if (!query) return rows;
    return rows.filter((row) => fields.some((field) =>
        String(row[field] ?? "").toLocaleLowerCase().includes(query)
    ));
}

export function formatReportMoney(value) {
    return Number(value).toLocaleString("en-PK", {
        style: "currency",
        currency: "PKR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}
