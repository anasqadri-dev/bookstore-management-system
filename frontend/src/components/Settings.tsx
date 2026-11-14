"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { apiRequest } from "../lib/api";
import {
    buildCreateStaffPayload,
    buildUpdateStaffPayload,
    validateStaffDraft,
    validateStaffUpdate,
} from "../lib/user-admin-model.mjs";

type ManagedUser = {
    id: number;
    username: string;
    fullName: string;
    role: "ADMIN" | "STAFF";
    active: boolean;
    createdAt: string;
};

type StaffDraft = { username: string; fullName: string; password: string };
type StaffEditDraft = { fullName: string; newPassword: string; active: boolean };

const blankDraft = (): StaffDraft => ({ username: "", fullName: "", password: "" });

export default function Settings() {
    const [users, setUsers] = useState<ManagedUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [saving, setSaving] = useState(false);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [draft, setDraft] = useState<StaffDraft>(blankDraft());
    const [createError, setCreateError] = useState("");
    const [editId, setEditId] = useState<number | null>(null);
    const [editDraft, setEditDraft] = useState<StaffEditDraft>({
        fullName: "", newPassword: "", active: true,
    });
    const [editError, setEditError] = useState("");
    const [notice, setNotice] = useState("");

    const loadUsers = useCallback(async () => {
        setLoading(true);
        try {
            const result = await apiRequest<ManagedUser[]>("/api/users", { cache: "no-store" });
            if (!Array.isArray(result)) throw new Error("The server returned an invalid user list.");
            setUsers(result);
            setLoadError("");
        } catch (cause) {
            setLoadError(cause instanceof Error ? cause.message : "Unable to load user accounts.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let active = true;
        async function loadInitialUsers() {
            try {
                const result = await apiRequest<ManagedUser[]>("/api/users", { cache: "no-store" });
                if (!Array.isArray(result)) throw new Error("The server returned an invalid user list.");
                if (active) {
                    setUsers(result);
                    setLoadError("");
                }
            } catch (cause) {
                if (active) setLoadError(cause instanceof Error ? cause.message : "Unable to load user accounts.");
            } finally {
                if (active) setLoading(false);
            }
        }
        void loadInitialUsers();
        return () => { active = false; };
    }, []);

    async function createStaff(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (saving) return;
        const validation = validateStaffDraft(draft);
        if (validation) {
            setCreateError(validation);
            return;
        }
        setSaving(true);
        setCreateError("");
        setNotice("");
        try {
            await apiRequest<ManagedUser>("/api/users", {
                method: "POST",
                body: JSON.stringify(buildCreateStaffPayload(draft)),
            });
            setDraft(blankDraft());
            setShowCreateForm(false);
            setNotice("Staff account created successfully.");
            await loadUsers();
        } catch (cause) {
            setCreateError(cause instanceof Error ? cause.message : "Unable to create the Staff account.");
        } finally {
            setSaving(false);
        }
    }

    function beginEdit(user: ManagedUser) {
        setEditId(user.id);
        setEditDraft({ fullName: user.fullName, newPassword: "", active: user.active });
        setEditError("");
        setNotice("");
    }

    async function saveStaffUpdate(userId: number) {
        if (saving) return;
        const validation = validateStaffUpdate(editDraft);
        if (validation) {
            setEditError(validation);
            return;
        }
        setSaving(true);
        setEditError("");
        setNotice("");
        try {
            await apiRequest<ManagedUser>(`/api/users/${userId}`, {
                method: "PUT",
                body: JSON.stringify(buildUpdateStaffPayload(editDraft)),
            });
            setEditId(null);
            setNotice("Staff account updated successfully.");
            await loadUsers();
        } catch (cause) {
            setEditError(cause instanceof Error ? cause.message : "Unable to update the Staff account.");
        } finally {
            setSaving(false);
        }
    }

    async function deactivateStaff(user: ManagedUser) {
        if (saving || !user.active) return;
        if (!window.confirm(`Deactivate Staff account ${user.username}? The account will be retained for historical records.`)) return;
        setSaving(true);
        setNotice("");
        setLoadError("");
        try {
            await apiRequest<ManagedUser>(`/api/users/${user.id}`, { method: "DELETE" });
            setNotice(`Staff account ${user.username} was deactivated.`);
            if (editId === user.id) setEditId(null);
            await loadUsers();
        } catch (cause) {
            setLoadError(cause instanceof Error ? cause.message : "Unable to deactivate the Staff account.");
        } finally {
            setSaving(false);
        }
    }

    const fieldClass = "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 disabled:bg-gray-100";

    return (
        <div className="space-y-6">
            <header>
                <h2 className="text-2xl font-bold text-gray-900">Settings</h2>
                <p className="mt-2 text-sm text-gray-600">
                    Manage Staff access and review the configuration supported by this local installation.
                </p>
            </header>

            <section className="rounded border border-gray-200 bg-white p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <h3 className="text-xl font-semibold text-gray-900">Staff accounts</h3>
                        <p className="mt-1 text-sm text-gray-600">
                            Create, update, reset passwords, and deactivate Staff accounts. Admin accounts are not managed here.
                        </p>
                    </div>
                    <button type="button" onClick={() => { setShowCreateForm((value) => !value); setCreateError(""); }}
                        className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
                        {showCreateForm ? "Close form" : "Add Staff"}
                    </button>
                </div>

                {notice && <p role="status" className="mt-4 rounded border border-green-200 bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
                {loadError && <div role="alert" className="mt-4 rounded bg-red-50 p-3 text-sm text-red-800">
                    {loadError} <button type="button" className="ml-2 underline" onClick={() => void loadUsers()}>Retry</button>
                </div>}

                {showCreateForm && (
                    <form onSubmit={(event) => void createStaff(event)} className="mt-5 space-y-4 rounded border border-gray-200 bg-gray-50 p-4" aria-busy={saving}>
                        <h4 className="font-semibold">Create Staff account</h4>
                        {createError && <p role="alert" className="text-sm text-red-700">{createError}</p>}
                        <div className="grid gap-4 sm:grid-cols-3">
                            <div><label htmlFor="staff-username" className="mb-1 block text-sm font-medium">Username</label>
                                <input id="staff-username" autoComplete="off" required maxLength={100} value={draft.username} disabled={saving}
                                    onChange={(event) => setDraft({ ...draft, username: event.target.value })} className={fieldClass} /></div>
                            <div><label htmlFor="staff-full-name" className="mb-1 block text-sm font-medium">Full name</label>
                                <input id="staff-full-name" required maxLength={150} value={draft.fullName} disabled={saving}
                                    onChange={(event) => setDraft({ ...draft, fullName: event.target.value })} className={fieldClass} /></div>
                            <div><label htmlFor="staff-password" className="mb-1 block text-sm font-medium">Initial password</label>
                                <input id="staff-password" type="password" autoComplete="new-password" required value={draft.password} disabled={saving}
                                    onChange={(event) => setDraft({ ...draft, password: event.target.value })} className={fieldClass} />
                                <span className="mt-1 block text-xs text-gray-500">8–72 UTF-8 bytes</span></div>
                        </div>
                        <button type="submit" disabled={saving} className="rounded bg-slate-900 px-4 py-2 text-sm text-white disabled:opacity-50">
                            {saving ? "Saving..." : "Create Staff account"}
                        </button>
                    </form>
                )}

                {loading ? <p role="status" className="py-8 text-center text-sm text-gray-500">Loading accounts...</p> : (
                    <div className="mt-5 overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-100"><tr>
                                <th className="p-3">Username</th><th className="p-3">Full name</th>
                                <th className="p-3">Role</th><th className="p-3">Status</th><th className="p-3">Actions</th>
                            </tr></thead>
                            <tbody>
                                {users.length === 0 ? <tr><td colSpan={5} className="p-8 text-center text-gray-500">No user accounts found.</td></tr> : users.map((user) => (
                                    <tr key={user.id} className="border-t align-top">
                                        <td className="p-3 font-medium">{user.username}</td>
                                        <td className="p-3">{editId === user.id ? (
                                            <div className="min-w-48 space-y-2">
                                                <label htmlFor={`staff-edit-name-${user.id}`} className="sr-only">Full name</label>
                                                <input id={`staff-edit-name-${user.id}`} value={editDraft.fullName} disabled={saving}
                                                    onChange={(event) => setEditDraft({ ...editDraft, fullName: event.target.value })} className={fieldClass} />
                                                <label htmlFor={`staff-edit-password-${user.id}`} className="sr-only">New password (optional)</label>
                                                <input id={`staff-edit-password-${user.id}`} type="password" autoComplete="new-password"
                                                    placeholder="New password (optional)" value={editDraft.newPassword} disabled={saving}
                                                    onChange={(event) => setEditDraft({ ...editDraft, newPassword: event.target.value })} className={fieldClass} />
                                                <label className="flex items-center gap-2"><input type="checkbox" checked={editDraft.active}
                                                    onChange={(event) => setEditDraft({ ...editDraft, active: event.target.checked })} disabled={saving} /> Active</label>
                                                {editError && <p role="alert" className="text-xs text-red-700">{editError}</p>}
                                                <div className="flex gap-2">
                                                    <button type="button" onClick={() => void saveStaffUpdate(user.id)} disabled={saving}
                                                        className="rounded bg-blue-600 px-3 py-1.5 text-xs text-white">Save</button>
                                                    <button type="button" onClick={() => setEditId(null)} disabled={saving}
                                                        className="rounded border px-3 py-1.5 text-xs">Cancel</button>
                                                </div>
                                            </div>
                                        ) : user.fullName}</td>
                                        <td className="p-3">{user.role}</td>
                                        <td className="p-3"><span className={user.active ? "rounded bg-green-100 px-2 py-1 text-xs text-green-800" : "rounded bg-gray-100 px-2 py-1 text-xs text-gray-700"}>{user.active ? "Active" : "Inactive"}</span></td>
                                        <td className="p-3">
                                            {user.role === "STAFF" && editId !== user.id && (
                                                <div className="flex flex-wrap gap-2">
                                                    <button type="button" onClick={() => beginEdit(user)} className="rounded border px-3 py-1.5 text-xs">Edit</button>
                                                    {user.active && <button type="button" disabled={saving} onClick={() => void deactivateStaff(user)}
                                                        className="rounded border border-red-200 px-3 py-1.5 text-xs text-red-700 disabled:opacity-50">Deactivate</button>}
                                                </div>
                                            )}
                                            {user.role === "ADMIN" && <span className="text-xs text-gray-500">Protected</span>}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-xl font-semibold text-gray-900">Store configuration</h3>
                <p className="mt-3 text-sm leading-6 text-gray-700">
                    The current backend contract does not expose a persisted store-profile or global settings resource.
                    Store identity, contact details, and global preferences cannot be edited here; this screen will not
                    claim to save unsupported settings.
                </p>
            </section>

            <section className="rounded border border-gray-200 bg-white p-6">
                <h3 className="text-lg font-semibold text-gray-900">Inventory alerts</h3>
                <p className="mt-2 text-sm text-gray-700">
                    Low-stock thresholds are configured per product in Books Inventory. A product is considered low
                    stock when its quantity is less than or equal to its minimum-stock value.
                </p>
            </section>
        </div>
    );
}
