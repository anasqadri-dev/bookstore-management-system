# Receipt printing

## Supported workflow

1. Complete a cash sale and wait until the backend confirms the sale ID.
2. Load the saved receipt in POS. The print action is available only after a persisted receipt has been retrieved from `GET /api/sales/{saleId}/receipt`.
3. Choose the target paper layout:
   - **58 mm thermal** for narrow receipt printers.
   - **80 mm thermal** for common wide receipt printers.
   - **A4 / PDF** for a standard-page print or Save as PDF fallback.
4. Select **Print receipt**. In the Electron Windows desktop app, the preload bridge invokes the main-process print handler and opens the operating-system print dialog. In a browser development session, the standard browser print dialog is used.
5. Select the configured Windows printer, verify the preview and paper size, and confirm.

The print view uses the saved receipt response, including saved item titles, SKUs, quantities, unit-price snapshots, line subtotals, cashier, sale time, and backend-confirmed total. It does not read current product prices or create another sale.

## Failure and recovery behavior

- Cancelling the print dialog or closing the preview does not alter the saved sale or stock.
- Printing can be repeated from the saved receipt ID; it does not submit checkout again.
- If no thermal printer is available, choose **A4 / PDF** and use Save as PDF or a standard Windows printer.
- If the receipt is not loaded, retrieve it again by its saved sale ID before printing.
- A print failure is surfaced in the POS UI; it does not roll back or duplicate the saved sale.

## Electron security boundary

The renderer exposes only the `printReceipt()` operation through the isolated preload bridge. The main process accepts the request only from the trusted local renderer origin and calls Electron's `webContents.print` with the normal Windows print dialog (`silent: false`). The renderer cannot choose an arbitrary filesystem path, invoke shell commands, or silently target a printer.

## Remaining physical acceptance

The implementation and automated Windows installer build do not prove compatibility with the bookstore's specific printer. Before closing #291, perform a physical print test on the target Windows counter PC and thermal printer. Record the printer model, Windows driver, paper width, print preview, legibility, repeated-print behavior, and cancellation/failure behavior. Test both 58 mm and 80 mm only if the target printer supports both; otherwise record its actual supported width.