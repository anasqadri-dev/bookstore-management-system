export function rememberVisitedPage(previous, page) {
    if (previous.has(page)) return previous;
    const next = new Set(previous);
    next.add(page);
    return next;
}
