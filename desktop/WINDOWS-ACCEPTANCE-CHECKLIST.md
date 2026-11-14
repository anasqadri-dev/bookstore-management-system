# Windows Release Acceptance Checklist

**Status:** Not yet executed. Complete this checklist on a clean Windows 10 and Windows 11 machine before closing #178–#181, #232, #237, #279, or #291.

Record the Windows version/build, installer artifact name and SHA-256, test date, printer model/driver, and tester.

## 1. Clean installation and first launch

- [ ] Use a Windows account with no JDK, Node.js, or separate backend/frontend process installed.
- [ ] Run the NSIS installer and install per-user to a non-default directory containing spaces.
- [ ] Confirm the installer creates Start Menu and desktop shortcuts.
- [ ] Launch the application; confirm the backend starts before the UI is shown.
- [ ] Confirm first-run Admin setup appears on a fresh database and requires the owner to choose a password.
- [ ] Confirm no default Admin password is documented, shipped, or accepted.
- [ ] Confirm both local services bind only to loopback (127.0.0.1), not 0.0.0.0 or a LAN address.
- [ ] Confirm a second application launch focuses the existing instance rather than starting another database process.

## 2. Offline operation and persistence

- [ ] Disconnect internet access after installation and restart the application.
- [ ] Confirm login, Inventory, POS, School Orders, Suppliers/Purchase Orders, Sales History, Reports, and Settings continue to operate without cloud access.
- [ ] Create a product and a sale, close the application, relaunch, and confirm the saved records remain.
- [ ] Confirm the SQLite database is stored under the current user's application-data directory, not under the installation folder.
- [ ] Confirm no second backend/frontend process is left running after normal application exit.
- [ ] Confirm the application reports a clear error if ports 3000 or 8080 are already occupied.

## 3. Backup and restore

- [ ] Create a backup from Settings and verify the chosen .db, .sqlite, or .sqlite3 file exists.
- [ ] Verify the backup with SQLite PRAGMA integrity_check.
- [ ] Add a new record after the backup, restore the earlier backup, and confirm the later record is absent after re-login.
- [ ] Confirm restore signs out all active sessions and requires sign-in again.
- [ ] Attempt to restore a corrupt or invalid SQLite file; confirm the active database remains usable and the recovery copy is retained if rollback is needed.
- [ ] Confirm Staff cannot access backup/restore endpoints or Settings controls.

## 4. Receipt printing

- [ ] Print a newly completed POS receipt using a 58 mm thermal printer.
- [ ] Print a receipt using an 80 mm thermal printer.
- [ ] Print a saved receipt from Sales History using the same paper sizes.
- [ ] Confirm the system printer dialog allows choosing an installed printer.
- [ ] Confirm item names, SKU, quantities, historical unit prices, totals, cashier, customer, and cash payment label are legible and not clipped.
- [ ] Confirm A4/PDF output remains readable.
- [ ] Confirm canceling the print dialog does not change or duplicate the sale.

## 5. Display scaling and keyboard accessibility

- [ ] Test at Windows display scaling 100%, 125%, and 150%.
- [ ] Test the dashboard at 1024×768 and 1280×720 window sizes.
- [ ] Confirm sidebar, dialogs, tables, and forms remain usable without overlapping or clipped primary actions.
- [ ] Complete login, inventory search, POS checkout, school-order status change, backup, and print using keyboard only.
- [ ] Confirm visible focus, skip link, and no keyboard traps.
- [ ] Run a screen-reader pass on Login, Admin Setup, Inventory, POS, and Settings.

## 6. Upgrade and uninstall

- [ ] Install a newer build over the existing installation.
- [ ] Confirm the existing SQLite database and user accounts remain intact.
- [ ] Confirm the application launches and can sign in after upgrade.
- [ ] Uninstall the application.
- [ ] Confirm uninstall removes application binaries but preserves the per-user database and backups.
- [ ] Reinstall and confirm the previous database is recognized.

## 7. Evidence to attach

- [ ] Installer SHA-256 and Windows version/build.
- [ ] Screenshot of first-run setup and main dashboard.
- [ ] Listener check showing only loopback bindings.
- [ ] Backup integrity-check result and restore/re-login result.
- [ ] 58 mm, 80 mm, and A4/PDF receipt samples.
- [ ] Process list before and after shutdown.
- [ ] Completed checklist signed by a second contributor.
