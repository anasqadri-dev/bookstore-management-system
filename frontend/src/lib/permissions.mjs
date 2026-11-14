const ADMIN_ONLY_SECTIONS = new Set([
    "Suppliers",
    "Purchase Orders",
    "School Orders",
    "Reports",
    "Settings",
]);

export function canAccessSection(role, section) {
    return role === "ADMIN" || !ADMIN_ONLY_SECTIONS.has(section);
}

export function getVisibleSections(role, sections) {
    return sections.filter((section) => canAccessSection(role, section.name));
}

export function canManageInventory(role) {
    return role === "ADMIN";
}
