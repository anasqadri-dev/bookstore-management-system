# Local SQLite Backup and Restore Guide

This guide describes the backend-supported backup and restore procedures for the single-store Bookstore Management System (BMS). Backup and restore are local operations; no cloud storage or internet connection is required.

## 1. Database location

The default backend datasource is configured in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/bookstore.db
```

The path is relative to the backend process working directory. For a packaged desktop installation, use the actual local database location configured for that installation. Do not assume that a developer's current working directory is the installed application's data directory.

The API accepts an absolute path on the computer where the local backend is running. The destination directory must already exist and be writable by the account running the backend.

## 2. Before creating a backup

1. Sign in to BMS as an Admin. Staff users are not allowed to perform backup or restore.
2. Decide where the backup should be stored. A local backup directory is suitable for the first copy; an external USB drive can be used for an additional copy.
3. Create the destination directory before calling the API.
4. Choose a new filename for each backup. The backend does not overwrite an existing file.

Supported file extensions are `.db`, `.sqlite`, and `.sqlite3`.

## 3. Create a backup

Call the Admin-only endpoint:

`POST /api/backup`

Request body:

```json
{
  "destinationPath": "E:\\BMS Backups\\bookstore-backup-2026-11-12.db"
}
```

The path in this example is illustrative. Replace it with an absolute path that exists on the computer running the backend.

The operation uses SQLite's online backup capability to create a consistent copy of the active database. The backend checks the generated backup with `PRAGMA integrity_check` before returning success.

A successful response includes:

- `fileName`: the created backup filename.
- `filePath`: the absolute path of the generated backup on the backend computer.
- `sizeBytes`: the backup file size.
- `completedAt`: the completion timestamp.

Only treat the backup as complete after the API returns a successful response. If the operation fails, the backend attempts to remove the incomplete destination file.

## 4. Copy the backup to removable storage

After the backup API has returned success:

1. Open the returned `filePath` on the backend computer.
2. Copy the completed backup file to the USB drive or other removable storage.
3. Wait for the file copy to finish.
4. Safely eject the removable storage using the operating system's normal eject procedure.
5. Keep the original local backup until the copied file has been verified.

Do not copy the active `bookstore.db` file while the application is running as a substitute for the backup operation. Use the API-generated backup file.

Keep more than one dated backup when practical. Use a different filename for each backup because the API intentionally refuses to overwrite an existing file.

## 5. Validate a backup before restore

The restore endpoint validates the selected file before replacing the active database. The file must:

- Be an existing, readable, non-empty regular file.
- Use a supported SQLite extension.
- Be a valid SQLite database according to `PRAGMA integrity_check`.
- Contain the eleven tables defined by the BMS SDS:
  `users`, `products`, `sales`, `sale_items`, `schools`, `school_orders`, `school_order_items`, `suppliers`, `purchase_orders`, `purchase_order_items`, and `activity_logs`.
- Pass SQLite's foreign-key consistency check.

A database from another application, a partial file copy, or a database with a different schema is rejected before the live database is changed.

## 6. Restore a backup

Restoring replaces the active local database with the contents of the selected backup. This is a destructive operation for changes made after the selected backup was created.

Before restoring:

1. Confirm that the selected backup is the intended recovery point.
2. Make a fresh backup of the current database using Section 3.
3. Confirm that the backup file is available at the exact path supplied to the API.
4. Ensure that the Admin is signed in and that the backend process can read the file.

Call the Admin-only endpoint:

`POST /api/backup/restore`

Request body:

```json
{
  "backupPath": "E:\\BMS Backups\\bookstore-backup-2026-11-12.db"
}
```

The restore operation:

1. Validates the backup file, schema, integrity, and foreign-key consistency.
2. Creates a temporary safety copy of the current active database.
3. Restores the selected backup into the active SQLite database.
4. Verifies the restored database.
5. Removes the temporary safety copy after successful verification.

A successful response includes:

- `fileName`: the restored backup filename.
- `productCount`: the number of product records in the restored database.
- `saleCount`: the number of sales in the restored database.
- `restoredAt`: the completion timestamp.

Compare the returned counts with the expected recovery point and perform the normal application checks after restore.

## 7. Restore failure and recovery behavior

If the selected file fails pre-validation, the active database is not replaced.

If the restore operation or post-restore verification fails after the safety copy has been created, the backend attempts to restore that safety copy automatically and verify it.

- If rollback succeeds, the API reports that the restore failed and that the original database was recovered.
- If rollback cannot be verified, the API reports the path of the preserved recovery copy. Do not delete that file. Stop further restore attempts and use the preserved copy for controlled recovery.

A failed restore should never be treated as a successful restore. Check the returned error and backend logs before retrying.

## 8. Access control and common errors

| HTTP status | Meaning |
|---|---|
| 200 | Restore completed and verified. |
| 201 | Backup created and verified. |
| 400 | Missing or invalid path, unsupported extension, unreadable/empty backup, invalid SQLite file, wrong schema, or foreign-key violations. |
| 401 | The request has no authenticated session. |
| 403 | The authenticated user is not an Admin. |
| 500 | An unexpected backup/restore failure occurred. Check the response and backend logs for recovery guidance. |

The endpoints require the same authenticated HTTP session used by the local application:

- `POST /api/backup`
- `POST /api/backup/restore`

Staff users receive HTTP 403. Unauthenticated callers receive HTTP 401.

## 9. Operational checklist

### Backup

- [ ] Signed in as Admin.
- [ ] Chosen a new filename and existing writable destination directory.
- [ ] Backup API returned success.
- [ ] Backup file exists and has a non-zero size.
- [ ] Copied the completed file to removable storage, if required.
- [ ] Safely ejected removable storage.

### Restore

- [ ] Confirmed the intended recovery point.
- [ ] Created a fresh backup of the current database.
- [ ] Confirmed the restore file is readable and available on the backend computer.
- [ ] Restore API returned success.
- [ ] Reviewed the returned product and sale counts.
- [ ] Confirmed the expected application data is present after restore.

## 10. Related backend API contract

See [API-CONTRACT.md](API-CONTRACT.md) for the request/response envelope and the complete list of backend endpoints, including the Admin-only backup and restore routes.
