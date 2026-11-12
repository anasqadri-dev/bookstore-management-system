# Backend Offline-Operation Validation

## 1. SRS requirement

The SRS identifies the product as a single-store Windows desktop application and states that core functions must work without internet access (NFR-09). The SDS describes the backend as a local Spring Boot API that stores shop data in an SQLite file on the shop computer.

The backend configuration uses:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/bookstore.db
    driver-class-name: org.sqlite.JDBC
```

The datasource path is local to the backend process. Backup and restore use SQLite files; the backend does not require a cloud database for those operations.

## 2. Backend evidence

The automated test `LocalBackendOfflineWorkflowTest.localProductSaleReceiptReportsAndBackupUseTheLocalSqliteFile` exercises this local path:

1. Opens the configured datasource and checks `PRAGMA database_list` resolves the active `main` database to a local file.
2. Saves a user and product through the local database.
3. Completes a sale through `SaleService`, including stock deduction and activity logging.
4. Verifies the receipt total and saved product quantity.
5. Generates the sales and inventory reports from the saved local rows.
6. Creates a SQLite online backup and verifies the sale exists in the backup file using a separate SQLite connection.

The wider backend suite also covers school orders, quotations, suppliers, purchase orders, goods received, users, reports, backup/restore, and persistence. These modules use the same local datasource; see [BACKEND-TEST-TRACEABILITY.md](BACKEND-TEST-TRACEABILITY.md).

The backend Maven dependency list was reviewed for this validation. The application dependencies are Spring MVC, Spring Security, Spring Data JPA, validation, Hibernate's SQLite dialect, SQLite JDBC, and Lombok. No cloud database driver or third-party API integration client is configured in the backend module.

## 3. What this proves

The automated backend checks provide evidence that:

- The application data source is a file-backed SQLite database.
- Product, sale, receipt, report, and backup operations can complete through the local backend and local database.
- The sale/stock/activity operation uses the service transaction and persists to SQLite.
- Report generation reads the saved local data.
- A database backup is generated as a local SQLite file and can be read independently.

The tested workflow does not require a cloud database or external business API.

## 4. What this does not prove

The automated integration test runs in CI and does **not** physically disconnect a Windows PC from the internet. It also does not launch the Electron desktop shell or test every frontend route while the machine is offline.

The following end-to-end checks remain necessary on the target Windows machine after the application is installed:

1. Install the application and confirm all runtime assets are local before disconnecting the network.
2. Disable Wi-Fi and disconnect Ethernet.
3. Launch the desktop application and sign in with a local Admin or Staff account.
4. Search inventory, add/update a product as Admin, complete a sale, verify the receipt data, and confirm stock changed.
5. Register/use a school order, generate its quotation, create a purchase order, and record goods received as Admin.
6. Open sales and inventory reports and verify their data.
7. Create a database backup to a local folder and copy the completed backup to a USB drive.
8. Restart the desktop application while still disconnected and verify saved records remain available.
9. Restore a known-good backup while still disconnected and compare product and sale counts.
10. Record the Windows version, application build, database path, test date, screenshots/logs, and any failure.

The physical printer should be connected locally during the receipt/report printing check. Internet connectivity is not the same as printer connectivity.

## 5. Result and status

**Backend offline evidence: automated local SQLite workflow covered.**

**Full-system NFR-09 status: pending manual disconnected Windows/Electron acceptance test.**

Do not mark NFR-09 fully passed until the desktop application has been exercised with the internet physically disconnected. The backend test is a repeatable supporting check, not a replacement for the complete offline acceptance test.
