import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import Home from "./page";
import { apiRequest } from "../lib/api";

vi.mock("../lib/api", () => ({ apiRequest: vi.fn() }));

vi.mock("../components/BackendReadiness", () => ({
  default: ({ children }: { children: ReactNode }) => children,
}));

vi.mock("../components/Login", () => ({
  default: ({ onLogin }: { onLogin: (user: unknown) => void }) => (
    <button type="button" onClick={() => onLogin({
      id: 1, username: "admin", fullName: "Admin User", role: "ADMIN",
    })}>
      Mock Login
    </button>
  ),
}));

vi.mock("../components/BooksInventory", () => ({ default: () => <div>Books Inventory Module</div> }));
vi.mock("../components/SalesPOS", () => ({ default: () => <div>Sales POS Module</div> }));
vi.mock("../components/Suppliers", () => ({ default: () => <div>Suppliers Module</div> }));
vi.mock("../components/SchoolOrders", () => ({ default: () => <div>School Orders Module</div> }));
vi.mock("../components/Reports", () => ({ default: () => <div>Reports Module</div> }));
vi.mock("../components/Settings", () => ({ default: () => <div>Settings Module</div> }));
vi.mock("../components/SalesHistory", () => ({ default: () => <div>Sales History Module</div> }));
vi.mock("../components/PurchaseOrders", () => ({ default: () => <div>Purchase Orders Module</div> }));
vi.mock("../components/DashboardOverview", () => ({ default: () => <div>Dashboard Module</div> }));

const mockUser = {
  id: 1,
  username: "admin",
  fullName: "Admin User",
  role: "ADMIN" as const,
};

describe("Home session management", () => {
  beforeEach(() => vi.clearAllMocks());

  it("restores an existing authenticated session", async () => {
    vi.mocked(apiRequest).mockResolvedValue(mockUser);
    render(<Home />);
    expect(await screen.findByText(/Admin User/)).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledWith("/api/auth/me");
  });

  it("shows login when no authenticated session exists", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("Unauthorized"));
    render(<Home />);
    expect(await screen.findByRole("button", { name: "Mock Login" })).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledWith("/api/auth/me");
    expect(apiRequest).toHaveBeenCalledWith("/api/setup/status", { cache: "no-store" });
  });

  it("logs out and returns to login", async () => {
    const user = userEvent.setup();
    vi.mocked(apiRequest).mockResolvedValueOnce(mockUser).mockResolvedValueOnce(undefined);
    render(<Home />);
    expect(await screen.findByText(/Admin User/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Logout/ }));
    expect(await screen.findByRole("button", { name: "Mock Login" })).toBeInTheDocument();
    expect(apiRequest).toHaveBeenNthCalledWith(1, "/api/auth/me");
    expect(apiRequest).toHaveBeenNthCalledWith(2, "/api/auth/logout", { method: "POST" });
  });

  it("returns to login even when backend logout fails", async () => {
    const user = userEvent.setup();
    vi.mocked(apiRequest).mockResolvedValueOnce(mockUser).mockRejectedValueOnce(new Error("Server unavailable"));
    render(<Home />);
    expect(await screen.findByText(/Admin User/)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Logout/ }));
    expect(await screen.findByRole("button", { name: "Mock Login" })).toBeInTheDocument();
  });
});
