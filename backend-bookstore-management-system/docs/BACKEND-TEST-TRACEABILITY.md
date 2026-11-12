# Backend Test Traceability (SRS TC-01 to TC-50)

## 1. Purpose and scope

This matrix maps the Bookstore Management System SRS test cases TC-01 through TC-50 to the automated backend tests currently in the repository.

The source SRS defines TC-01 through TC-39 in Section 6 (page 10), and TC-40 through TC-50 with the remaining non-functional requirements on page 11. The full sequence is used here; the first 39 cases are not treated as the complete test plan.

This is a **backend-only** traceability view. It distinguishes:
- **Verified (backend):** an automated backend test exercises the described behavior.
- **Partial (backend):** some backend behavior is tested, but the full SRS scenario also requires UI, process-restart, printer, or broader acceptance testing.
- **UI / hardware:** the behavior is outside the backend's responsibility and needs frontend, desktop, or physical-device testing.

A mapped test is evidence for its specific backend behavior. It is not a claim that every desktop acceptance criterion has been completed.

## 2. Functional and non-functional test case map

| SRS test | SRS scenario | Backend test evidence | Backend coverage |
|---|---|---|---|
| TC-01 | Valid login | `AuthControllerTest.successfulLoginCreatesSessionAndRecordsLogin`; `BackendApiSessionSmokeTest.adminCanLoginAndReachAdminAndSharedBackendEndpoints` | Verified (backend) |
| TC-02 | Wrong or empty login details | `AuthControllerTest.invalidCredentialsAreRejected`; `GlobalExceptionHandlerTest.mapsAuthenticationAndAuthorizationFailuresToCorrectStatuses`; `GlobalExceptionHandlerTest.mapsUnreadableRequestBodyWithoutExposingParserDetails` | Partial — incorrect credentials and malformed requests are covered; the complete login-screen empty-field behavior is UI work |
| TC-03 | Staff opens an Admin feature and is blocked | `BackendApiSessionSmokeTest.staffCanLoginAndReachSharedEndpointsButIsDeniedAdminEndpoints`; module controller authorization tests | Verified (backend route enforcement) |
| TC-04 | Admin can open all screens | `BackendApiSessionSmokeTest.adminCanLoginAndReachAdminAndSharedBackendEndpoints`; dedicated school, purchase-order, report, supplier, user, and backup controller authorization tests | Partial — backend routes are covered by module tests; this is not a manual check of every desktop screen |
| TC-05 | Logout, then attempt to use a feature | `AuthControllerTest.logoutInvalidatesExistingSession`; `BackendApiSessionSmokeTest.adminCanLoginAndReachAdminAndSharedBackendEndpoints` | Verified (backend session invalidation) |
| TC-06 | Add a product with full details | `ProductIntegrationTest.createsAndRetrievesProductThroughApi` | Verified (backend API and persistence) |
| TC-07 | Missing required product data or repeated SKU | `ProductIntegrationTest.rejectsInvalidProductRequest`; `ProductIntegrationTest.rejectsDuplicateSkuWithConflictResponse`; `InvalidRequestPersistenceIntegrationTest.invalidCreateRequestsAcrossCoreModulesReturnBadRequestWithoutSavingRows` | Verified (backend validation and no persistence) |
| TC-08 | Change product details | `ProductIntegrationTest.updatesExistingProduct` | Verified (backend API) |
| TC-09 | Delete and confirm | `ProductIntegrationTest.deletesUnusedProduct`; `ProductIntegrationTest.blocksDeletionWhenProductIsUsedInSale`; `ActivityLogWorkflowIntegrationTest.persistsDeleteActivityForAuthenticatedProductDeletion` | Partial — deletion and audit behavior are covered; the confirmation dialog is a UI responsibility |
| TC-10 | Cancel a product deletion | No backend operation should be sent when a user cancels | UI-only — verify the frontend sends no DELETE request after cancellation |
| TC-11 | Search for an existing product | `ProductIntegrationTest.searchesByTitleOrSkuAndReturnsEmptyForNoMatch` | Verified (backend search API) |
| TC-12 | Search for a missing product | `ProductIntegrationTest.searchesByTitleOrSkuAndReturnsEmptyForNoMatch` | Verified (backend empty-result behavior) |
| TC-13 | Compare displayed stock with saved quantity | `ProductIntegrationTest.lowStockIncludesQuantityEqualToMinimumButExcludesAboveMinimum`; `ProductSqliteFilePersistenceTest`; `BusinessRecordSqliteFilePersistenceTest` | Partial — saved backend values are verified; matching the displayed UI value is frontend integration work |
| TC-14 | Quantity equals minimum and should trigger low-stock status | `ProductIntegrationTest.lowStockIncludesQuantityEqualToMinimumButExcludesAboveMinimum`; `DashboardServiceIntegrationTest.returnsDashboardSummaryFromPersistedData` | Verified (backend boundary rule is quantity less than or equal to minimum) |
| TC-15 | Quantity above minimum should not trigger low-stock status | `ProductIntegrationTest.lowStockIncludesQuantityEqualToMinimumButExcludesAboveMinimum`; `DashboardServiceIntegrationTest.returnsDashboardSummaryFromPersistedData` | Verified (backend boundary rule) |
| TC-16 | Complete a sale containing items | `SaleServiceIntegrationTest.createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt`; `SaleControllerTest.createsSaleAndReturnsReceipt` | Verified (backend checkout and receipt response) |
| TC-17 | Attempt a sale with no items | `SaleServiceIntegrationTest.rejectsSaleWithoutItems`; `SaleControllerTest.rejectsSaleWithNoItemsBeforeCallingService`; `SaleRequestValidationTest.rejectsSaleWithoutItems` | Verified (request and service validation) |
| TC-18 | Add sale items and quantities | `SaleServiceIntegrationTest.createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt`; `SaleCalculatorTest.calculatesLineSubtotalUsingDecimalArithmetic` | Verified (backend item aggregation and calculation) |
| TC-19 | Request a sale quantity above available stock | `SaleServiceIntegrationTest.doesNotDeductAnyStockWhenOneRequestedProductHasInsufficientQuantity`; `SaleServiceIntegrationTest.allowsSaleWhenRequestedQuantityExactlyMatchesAvailableStock` | Verified (backend stock guard and boundary) |
| TC-20 | Compare a sale total with a hand calculation | `SaleCalculatorTest.matchesSrsHandCalculationForTwoHundredAndFiftyRupees`; `SaleServiceIntegrationTest.createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt` | Verified (decimal arithmetic and saved result) |
| TC-21 | Quantity-one and maximum-available-stock calculation boundary | `SaleCalculatorTest.calculatesLineSubtotalUsingDecimalArithmetic`; `SaleServiceIntegrationTest.allowsSaleWhenRequestedQuantityExactlyMatchesAvailableStock` | Partial — quantity-one arithmetic and exact-stock boundary are covered across tests; no separate named maximum-integer stock test is claimed |
| TC-22 | Verify stock after a sale | `SaleServiceIntegrationTest.createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt`; `LocalBackendOfflineWorkflowTest.localProductSaleReceiptReportsAndBackupUseTheLocalSqliteFile` | Verified (persisted stock deduction) |
| TC-23 | Verify receipt contents | `SaleControllerTest.createsSaleAndReturnsReceipt`; `SaleControllerTest.retrievesReceiptBySaleId`; `SaleServiceIntegrationTest.createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt` | Verified (backend receipt payload; not physical printing) |
| TC-24 | Register a school | `SchoolControllerAuthorizationTest.adminCanRegisterAndListSchools`; `SchoolRegistrationApiBoundaryTest.allowsOmittedOptionalSchoolFieldsAndStoresThemAsNull` | Verified (backend registration) |
| TC-25 | Register a school without a name | `SchoolControllerAuthorizationTest.rejectsSchoolRegistrationWithoutAName` | Verified (backend validation) |
| TC-26 | Create an order for a registered school | `SchoolOrderServiceIntegrationTest.createsPendingOrderWithAggregatedItemsAndPriceSnapshots` | Verified (backend service and persistence) |
| TC-27 | Create a school order with an unavailable item | `SchoolOrderServiceIntegrationTest.rejectsSchoolOrderWhenRequestedQuantityExceedsAvailableStock`; `SchoolOrderServiceIntegrationTest.rejectsUnknownProduct` | Verified (backend stock/product checks) |
| TC-28 | Generate a school-order quotation | `SchoolQuotationServiceIntegrationTest.generatesHandCalculatedQuotationFromHistoricalUnitPrices` | Verified (backend quotation calculation) |
| TC-29 | Change and view school-order status | `SchoolOrderStatusServiceIntegrationTest.updatesOrderStatusToEachSupportedSdsValue` | Verified (backend status workflow) |
| TC-30 | Add a supplier | `SupplierControllerAuthorizationTest.adminCanCreateAndListSuppliers`; `SupplierControllerAuthorizationTest.rejectsMissingOrBlankSupplierName` | Verified (backend API) |
| TC-31 | Create a purchase order | `PurchaseOrderServiceIntegrationTest.createsOrderedPurchaseOrderWithSeparateUnitCostSnapshots`; `PurchaseOrderControllerAuthorizationTest.adminCanCreateListViewAndReceivePurchaseOrder` | Verified (backend API and persistence) |
| TC-32 | Record goods received and increase stock | `GoodsReceivedIntegrationTest.receivesGoodsUpdatesStockAndRecordsReceivedDate`; rollback and duplicate-receipt cases in the same class | Verified (backend workflow and idempotency) |
| TC-33 | Generate a sales report for selected dates | `ReportServiceIntegrationTest.reportsSalesWithinInclusiveCalendarDateRangeUsingSavedSaleTotals`; `ReportApiPerformanceTest.salesReportApiRespondsWithinNfr01ThreeSecondLimitForTwoHundredSales` | Verified (backend filtering, totals, and response-time test) |
| TC-34 | Generate an inventory report | `ReportServiceIntegrationTest.valuesInventoryFromDocumentedRetailPriceAndQuantity`; `ReportApiPerformanceTest.inventoryReportApiRespondsWithinNfr01ThreeSecondLimitForTwoHundredProducts` | Verified (backend inventory values and response-time test) |
| TC-35 | Add, change, and remove a Staff account | `UserAdministrationControllerTest.adminCanCreateAndListStaffWithoutExposingPasswordHash`; `UserAdministrationControllerTest.adminCanUpdateAndDeactivateStaffWithoutDeletingTheRecord` | Partial — removal is implemented as deactivation to preserve historical references, not physical row deletion |
| TC-36 | Create a database backup | `DatabaseBackupServiceIntegrationTest.createsReadableConsistentBackupOfActiveDatabase`; `DatabaseBackupControllerAuthorizationTest.adminCanCreateDatabaseBackup` | Verified (file creation and integrity check) |
| TC-37 | Restore from a database backup | `DatabaseRestoreServiceIntegrationTest.restoresSavedDatabaseSnapshotAndReportsRestoredCounts`; `DatabaseBackupControllerAuthorizationTest.adminCanRestoreValidatedDatabaseBackup` | Verified (restore, integrity, schema, and result payload) |
| TC-38 | Print a receipt and a report | Receipt and report payload tests exist, but backend does not control the printer | UI / hardware — physical print output and printer configuration must be tested on the target Windows PC |
| TC-39 | Time screens, searches, and buttons against the 3-second target | `ProductIntegrationTest.inventorySearchRespondsWithinThreeSecondRequirement`; `ReportApiPerformanceTest` | Partial — the named backend search and report endpoints are timed; this is not a measurement of every UI screen and button |
| TC-40 | Time sale confirmation through receipt against the 5-second target | `SalePerformanceTest.confirmToReceiptCompletesWithinFiveSecondsAfterWarmup` | Verified (backend checkout-to-receipt service path) |
| TC-41 | Complete 200 sales with zero errors | `SaleReliabilityTest.completesTwoHundredSalesWithoutErrorsOrStockMismatch` | Verified (backend reliability test) |
| TC-42 | Check stock and report figures against hand calculations | `SaleCalculatorTest.matchesSrsHandCalculationForTwoHundredAndFiftyRupees`; `ReportServiceIntegrationTest`; `SaleServiceIntegrationTest` | Verified (backend calculations and persisted figures) |
| TC-43 | Enter letters and negative numbers and verify rejection | `ProductValidationTest`; `SaleRequestValidationTest`; `PurchaseOrderRequestValidationTest`; `SchoolOrderRequestValidationTest`; `InvalidRequestPersistenceIntegrationTest` | Verified (backend validation; no partial create rows) |
| TC-44 | Close and reopen the application after a sale | `BusinessRecordSqliteFilePersistenceTest.committedSalesAndSchoolOrdersCanBeReadThroughANewSqliteConnection`; `SalePersistenceTest` | Partial — data is verified through a fresh SQLite connection; a full Electron/backend process restart is not part of this backend test |
| TC-45 | Use all features with the internet disconnected | `LocalBackendOfflineWorkflowTest.localProductSaleReceiptReportsAndBackupUseTheLocalSqliteFile`; file-backed persistence and local API/service integration tests | Partial (backend) — confirms the core local product/sale/report/backup path uses a local SQLite file and no external service in the tested flow; a fully disconnected Windows/Electron run remains manual |
| TC-46 | Check the database for plain-text passwords | `PasswordEncoderTest.hashesPasswordAndVerifiesOnlyMatchingRawPassword`; `UserAdministrationControllerTest.adminCanCreateAndListStaffWithoutExposingPasswordHash` | Verified (BCrypt storage and API response does not expose the hash) |
| TC-47 | Staff attempts to reach Admin features through another route | `BackendApiSessionSmokeTest.staffCanLoginAndReachSharedEndpointsButIsDeniedAdminEndpoints`; module-specific controller authorization tests | Verified (backend route and method security) |
| TC-48 | Check activity log after login, sale, and delete | `ActivityLogWorkflowIntegrationTest` (login, sale, and delete persistence tests) | Verified (database rows asserted) |
| TC-49 | A new user completes a sale without help | No backend test can measure ease of learning or on-screen guidance | UI / usability — perform an observed usability test with a first-time operator |
| TC-50 | Check labels and messages on every screen | `GlobalExceptionHandlerTest`; `ApiResponseTest`; controller response assertions across module tests | Partial — backend response envelope and error messages are covered; visible labels and messages on every frontend screen require UI review |

## 3. Non-functional requirement summary

| SRS requirement | Backend evidence | Status / limitation |
|---|---|---|
| NFR-01 — response within 3 seconds | Product search timing test; sales and inventory report API timing tests with 200-record fixtures | Partial — selected backend endpoints are measured, not every screen/button |
| NFR-02 — sale confirmation to receipt within 5 seconds | `SalePerformanceTest` | Verified for the backend checkout/receipt path |
| NFR-03 — 200 sales, zero errors | `SaleReliabilityTest` | Verified |
| NFR-04 — correct totals, stock, and reports | Sale calculator, sale integration, report calculation, and SQLite persistence tests | Verified for the backend scenarios covered |
| NFR-05 — delete confirmation | Backend delete and dependency checks | UI confirmation/cancel interaction remains frontend scope |
| NFR-06 — reject invalid data | DTO validation, module integration tests, and invalid-request persistence test | Verified for tested backend request classes |
| NFR-07 — automatically save data | Transactional service writes and file-backed persistence tests | Partial — process-close/reopen behavior needs a desktop-level test |
| NFR-08 — no data loss | Consistent backup, schema/integrity validation, restore round-trip, pre-restore safety copy, and rollback tests | Verified for backend backup/restore behavior |
| NFR-09 — works offline | Local SQLite datasource; no backend cloud/database service dependency; local product/sale/report/backup integration test | Partial — full network-disconnected Windows/Electron acceptance test remains |
| NFR-10 — authentication and password handling | Login/session tests, BCrypt test, and user API tests | Verified for backend authentication/password storage |
| NFR-11 — role access | Session smoke tests and module authorization tests | Verified for backend routes under test |
| NFR-12 — activity logging | Login, sale, and delete activity persistence tests | Verified |
| NFR-13 — simple screens | Not measurable from backend code | UI/usability test required |
| NFR-14 — clear labels and messages | Standard backend response envelope and exception-handler tests | Partial — screen labels and presentation remain frontend scope |

## 4. Coverage limitations and follow-up

1. TC-10, TC-38, and TC-49 are intentionally not described as backend tests: delete cancellation, physical printing, and first-time-user usability belong to the UI/desktop/hardware test workstream.
2. TC-04, TC-09, TC-13, TC-21, TC-35, TC-39, TC-44, TC-45, and TC-50 have explicit partial coverage notes above. No full-system claim is made for these cases.
3. TC-45 requires one manual acceptance run on the target Windows machine with internet physically disconnected after installation. The backend integration test proves the local SQLite workflow but does not simulate a complete air-gapped desktop environment.
4. TC-38 requires the supported receipt printer and report printer path to be tested on the target machine.
5. The backend suite is automated with Maven/Surefire. CI success is evidence that the current automated suite passes; it is not a substitute for the outstanding UI/hardware/manual checks.

## 5. Repository test classes referenced

The principal automated backend test classes include:

- `AuthControllerTest`, `BackendApiSessionSmokeTest`, `AuthorizationMethodSecurityTest`
- `ProductIntegrationTest`, `ProductValidationTest`, `ProductSqliteFilePersistenceTest`
- `SaleControllerTest`, `SaleServiceIntegrationTest`, `SaleCalculatorTest`, `SalePerformanceTest`, `SaleReliabilityTest`, `SaleRollbackIntegrationTest`
- `SchoolControllerAuthorizationTest`, `SchoolRegistrationApiBoundaryTest`, `SchoolOrderServiceIntegrationTest`, `SchoolQuotationServiceIntegrationTest`, `SchoolOrderStatusServiceIntegrationTest`
- `SupplierControllerAuthorizationTest`, `PurchaseOrderServiceIntegrationTest`, `PurchaseOrderControllerAuthorizationTest`, `GoodsReceivedIntegrationTest`
- `ReportServiceIntegrationTest`, `ReportsControllerAuthorizationTest`, `ReportApiPerformanceTest`
- `UserAdministrationControllerTest`, `PasswordEncoderTest`
- `DatabaseSchemaContractTest`, `BusinessRecordSqliteFilePersistenceTest`, `SqliteConnectionPoolLifecycleTest`
- `DatabaseBackupServiceIntegrationTest`, `DatabaseRestoreServiceIntegrationTest`, `DatabaseBackupControllerAuthorizationTest`
- `ActivityLogWorkflowIntegrationTest`, `GlobalExceptionHandlerTest`, `ApiResponseTest`
- `InvalidRequestPersistenceIntegrationTest`, `LocalBackendOfflineWorkflowTest`

This matrix should be updated whenever a test is renamed, removed, or materially changes its scope.
