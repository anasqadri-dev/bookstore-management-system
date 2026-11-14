
"use client";

import { useEffect, useRef, useState } from "react";
import Login from "../components/Login";
import AdminSetup from "../components/AdminSetup";
import ModuleErrorBoundary from "../components/ModuleErrorBoundary";
import BooksInventory from "../components/BooksInventory";
import SalesPOS from "../components/SalesPOS";
import Suppliers from "../components/Suppliers";
import SchoolOrders from "../components/SchoolOrders";
import Reports from "../components/Reports";
import Settings from "../components/Settings";
import SalesHistory from "../components/SalesHistory";
import PurchaseOrders from "../components/PurchaseOrders";
import DashboardOverview from "../components/DashboardOverview";
import { apiRequest, type AuthenticatedUser } from "../lib/api";
import BackendReadiness from "../components/BackendReadiness";
import { canAccessSection, canManageInventory, getVisibleSections } from "../lib/permissions.mjs";

function AppContent() {
    const [currentUser, setCurrentUser] = useState<AuthenticatedUser | null>(null);
    const [isSessionLoading, setIsSessionLoading] = useState(true);
    const [setupRequired, setSetupRequired] = useState(false);
    const [activePage, setActivePage] = useState("Dashboard");
    const [visitedPages, setVisitedPages] = useState<Set<string>>(() => new Set(["Dashboard"]));
    const pageHeadingRef = useRef<HTMLHeadingElement>(null);
    const previousPageRef = useRef(activePage);

    useEffect(() => {
        if (previousPageRef.current !== activePage) {
            previousPageRef.current = activePage;
            pageHeadingRef.current?.focus();
        }
    }, [activePage]);

    useEffect(() => {
        let isMounted = true;

        async function restoreSessionOrCheckFirstRun() {
            try {
                const user = await apiRequest<AuthenticatedUser>("/api/auth/me");
                if (isMounted) {
                    setCurrentUser(user);
                    setSetupRequired(false);
                }
            } catch {
                if (!isMounted) return;
                setCurrentUser(null);
                try {
                    const status = await apiRequest<{ setupRequired: boolean }>(
                        "/api/setup/status",
                        { cache: "no-store" },
                    );
                    if (isMounted) setSetupRequired(status.setupRequired);
                } catch {
                    if (isMounted) setSetupRequired(false);
                }
            } finally {
                if (isMounted) setIsSessionLoading(false);
            }
        }

        void restoreSessionOrCheckFirstRun();

        return () => {
            isMounted = false;
        };
    }, []);

    const menu = [
        { name: "Dashboard", icon: "▦" },
        { name: "Books Inventory", icon: "▤" },
        { name: "Sales / POS", icon: "🛒" },
        { name: "Suppliers", icon: "▱" },
        { name: "Sales History", icon: "▤" },
        { name: "Purchase Orders", icon: "▦" },
        { name: "School Orders", icon: "♧" },
        { name: "Reports", icon: "▥" },
        { name: "Settings", icon: "⚙" },
    ];

    const visibleMenu = getVisibleSections(currentUser?.role ?? "STAFF", menu);

    function navigateToPage(page: string) {
        if (!currentUser || !canAccessSection(currentUser.role, page)) return;
        setVisitedPages((previous) => {
            if (previous.has(page)) return previous;
            const next = new Set(previous);
            next.add(page);
            return next;
        });
        setActivePage(page);
    }

    function handleLogin(user: AuthenticatedUser) {
        setSetupRequired(false);
        setCurrentUser(user);
        setVisitedPages(new Set(["Dashboard"]));
        setActivePage("Dashboard");
    }

    async function handleLogout() {
        try {
            await apiRequest<void>("/api/auth/logout", { method: "POST" });
        } catch {
            // Clear the local view even when the backend is unavailable.
        } finally {
            setCurrentUser(null);
            setSetupRequired(false);
            setVisitedPages(new Set(["Dashboard"]));
            setActivePage("Dashboard");
        }
    }

    if (isSessionLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-[#f3f4f6] text-sm text-gray-600">
                Checking your session...
            </div>
        );
    }

    // The backend session, not a client-side flag, determines authentication.
    if (!currentUser) {
        return setupRequired
            ? <AdminSetup onComplete={() => setSetupRequired(false)} />
            : <Login onLogin={handleLogin} />;
    }

    return (
        <div className="min-h-screen bg-[#f3f4f6] text-[#111827] md:flex md:h-screen md:overflow-hidden">

            {/* Sidebar */}
            <aside className="print:hidden bg-[#030712] p-6 text-white md:h-screen md:w-[220px] md:shrink-0 md:overflow-y-auto lg:w-[260px] xl:w-[300px]">

                <h1 className="text-2xl font-bold">
                    Book Store
                </h1>

                <p className="mt-2 text-sm text-gray-400">
                    Management System
                </p>

                <nav className="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-3 md:mt-10 md:grid-cols-1">
                    {visibleMenu.map((item) => (
                        <button
                            key={item.name}
                            type="button"
                            onClick={() => navigateToPage(item.name)}
                            aria-current={activePage === item.name ? "page" : undefined}
                            className={`flex min-w-0 w-full items-center gap-2 rounded-md px-3 py-3 text-left text-xs transition sm:text-sm lg:gap-4 lg:px-4 ${
                                activePage === item.name
                                    ? "bg-slate-700 text-white"
                                    : "text-gray-300 hover:bg-slate-800"
                            }`}
                        >
                            <span className="w-5 shrink-0 text-xl">
                                {item.icon}
                            </span>
                            {item.name}
                        </button>
                    ))}
                </nav>

                {/* Logout Button */}
                <div className="mt-10 border-t border-gray-700 pt-5">
                    <button
                        type="button"
                        onClick={() => void handleLogout()}
                        className="flex w-full items-center gap-4 rounded-md px-5 py-4 text-left text-sm text-red-300 transition hover:bg-slate-800"
                    >
                        <span className="w-5 text-xl">⇥</span>
                        Logout
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main className="min-w-0 flex-1 overflow-x-hidden p-4 sm:p-6 md:h-screen md:overflow-y-auto lg:p-10 print:h-auto print:overflow-visible print:p-0">

                {/* Header */}
                <header className="print:hidden mb-8 flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <h2 ref={pageHeadingRef} tabIndex={-1} className="text-2xl font-bold sm:text-3xl">
                            {activePage}
                        </h2>

                        <p className="mt-2 text-gray-500">
                            Bookstore Management System
                        </p>
                    </div>

                    <div className="min-w-0 break-words text-sm">
                        ♙ &nbsp; {currentUser.fullName} ({currentUser.role})
                    </div>
                </header>

                {visitedPages.has("Dashboard") && (
                    <div hidden={activePage !== "Dashboard"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <DashboardOverview />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {visitedPages.has("Books Inventory") && (
                    <div hidden={activePage !== "Books Inventory"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <BooksInventory canManage={canManageInventory(currentUser.role)} />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {currentUser.role === "ADMIN" && visitedPages.has("Suppliers") && (
                    <div hidden={activePage !== "Suppliers"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <Suppliers />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {currentUser.role === "ADMIN" && visitedPages.has("School Orders") && (
                    <div hidden={activePage !== "School Orders"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <SchoolOrders />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {visitedPages.has("Sales / POS") && (
                    <div hidden={activePage !== "Sales / POS"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <SalesPOS />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {visitedPages.has("Sales History") && (
                    <div hidden={activePage !== "Sales History"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <SalesHistory />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {currentUser.role === "ADMIN" && visitedPages.has("Purchase Orders") && (
                    <div hidden={activePage !== "Purchase Orders"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <PurchaseOrders />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {currentUser.role === "ADMIN" && visitedPages.has("Reports") && (
                    <div hidden={activePage !== "Reports"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <Reports />
                        </ModuleErrorBoundary>
                    </div>
                )}
                {currentUser.role === "ADMIN" && visitedPages.has("Settings") && (
                    <div hidden={activePage !== "Settings"}>
                        <ModuleErrorBoundary onReturnToDashboard={() => navigateToPage("Dashboard")}>
                            <Settings />
                        </ModuleErrorBoundary>
                    </div>
                )}
            </main>
        </div>
    );
}


export default function Home() {
    return <BackendReadiness><AppContent /></BackendReadiness>;
}
