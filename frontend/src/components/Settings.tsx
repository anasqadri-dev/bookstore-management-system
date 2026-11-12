"use client";

/**
 * The current SDS and API contract do not define a persisted store-settings
 * resource. This screen deliberately exposes no editable fake configuration.
 */
export default function Settings() {
    return (
        <div className="space-y-6">
            <div>
                <h2 className="text-2xl font-bold text-gray-900">Settings</h2>
                <p className="mt-2 text-sm text-gray-600">
                    System configuration information and currently supported options.
                </p>
            </div>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-xl font-semibold text-gray-900">Store Information</h3>
                <p className="mt-3 text-sm text-gray-700">
                    Editing the store name, phone, email and address is not available yet.
                    The current backend API does not provide a store-profile settings endpoint.
                </p>
                <p className="mt-3 rounded border border-gray-200 bg-gray-50 p-3 text-sm text-gray-700">
                    No store-profile changes can be saved from this screen.
                </p>
            </section>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-xl font-semibold text-gray-900">Admin Account</h3>
                <p className="mt-3 text-sm text-gray-700">
                    Admin profile changes are not supported by this Settings screen.
                    Account administration must use the documented authenticated user-management
                    workflow when its interface becomes available.
                </p>
                <p className="mt-3 text-sm text-gray-600">
                    This page does not modify your account or login credentials.
                </p>
            </section>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-xl font-semibold text-gray-900">Low-stock Alerts</h3>
                <p className="mt-3 text-sm text-gray-700">
                    Low-stock thresholds are configured <strong>per book</strong>, not as a
                    global store setting. Each product has its own <code>minStock</code> value,
                    and a book is low on stock when its quantity is at or below that value.
                </p>
                <p className="mt-3 text-sm text-gray-600">
                    Use Books Inventory to manage supported product stock fields once
                    inventory editing is connected to the backend.
                </p>
            </section>

            <p role="status" className="text-sm text-gray-600">
                Settings that require a new backend contract will remain unavailable until
                they are designed, implemented and verified. No changes are stored locally
                or presented as saved.
            </p>
        </div>
    );
}
