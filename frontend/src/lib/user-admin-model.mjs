export function validateStaffDraft({ username, fullName, password }) {
    if (!username?.trim()) return "Username is required.";
    if (!fullName?.trim()) return "Full name is required.";
    const bytes = new TextEncoder().encode(password ?? "").length;
    if (bytes < 8 || bytes > 72) return "Password must contain 8 to 72 UTF-8 bytes.";
    return "";
}

export function validateStaffUpdate({ fullName, newPassword }) {
    if (!fullName?.trim()) return "Full name is required.";
    if (newPassword) {
        const bytes = new TextEncoder().encode(newPassword).length;
        if (bytes < 8 || bytes > 72) return "New password must contain 8 to 72 UTF-8 bytes.";
    }
    return "";
}

export function buildCreateStaffPayload({ username, fullName, password }) {
    return { username: username.trim(), fullName: fullName.trim(), password };
}

export function buildUpdateStaffPayload({ fullName, newPassword, active }) {
    return {
        fullName: fullName.trim(),
        newPassword: newPassword || null,
        active,
    };
}
