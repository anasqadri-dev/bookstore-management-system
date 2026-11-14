# Receipt printing

## Supported browser/local-dashboard workflow

1. Complete a cash sale and wait until the backend confirms the sale ID.
2. Load the saved receipt in POS. Receipt printing is only shown when a persisted receipt has been retrieved from `GET /api/sales/{saleId}/receipt`.
3. Choose the target paper layout:
   - **58 mm thermal** for narrow receipt printers.
   - **80 mm thermal** for common wide receipt printers.
   - **A4 / PDF** for a standard-page print or Save as PDF fallback.
4. Select **Print receipt** and choose the configured Windows printer in the operating-system print dialog. Check the preview and paper size before confirming.

The print view uses the saved receipt response, including saved item titles, SKUs, quantities, unit-price snapshots, line subtotals, cashier, sale time, and backend-confirmed total. It does not read current product prices or create another sale.

## Failure and recovery behavior

- Cancelling the print dialog or closing the preview does not alter the saved sale or stock.
- Printing can be repeated from the saved receipt ID; it does not submit checkout again.
- If no thermal printer is available, choose **A4 / PDF** and use Save as PDF or a standard Windows printer.
- If the receipt is not loaded, retrieve it again by its saved sale ID before printing.

## Desktop runtime limitation

The current UI uses the standard browser print dialog. A browser cannot safely choose an arbitrary Windows printer or perform silent printing. Direct device selection, silent/managed printing, and packaging-specific printer tests must be wired through the Electron desktop runtime tracked by #279. Before closing #291, perform a physical print test on the target bookstore thermal printer at both supported widths and record the printer model, Windows driver, paper width, and observed legibility.
