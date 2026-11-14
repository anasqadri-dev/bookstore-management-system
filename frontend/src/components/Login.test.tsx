import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import Login from "./Login";
import userEvent from "@testing-library/user-event";
import { apiRequest } from "../lib/api";

vi.mock("../lib/api", () => ({
  apiRequest: vi.fn(),
}));

describe("Login Component", () => {
  it("should display the login form", () => {
    render(<Login onLogin={vi.fn()} />);

    expect(
      screen.getByRole("heading", { name: "Welcome back" }),
    ).toBeInTheDocument();

    expect(screen.getByLabelText("Username")).toBeInTheDocument();

    expect(screen.getByLabelText("Password")).toBeInTheDocument();

    expect(screen.getByRole("button", { name: "Sign In" })).toBeInTheDocument();
  });

  it("should accept username and password input", async () => {
    const user = userEvent.setup();

    render(<Login onLogin={vi.fn()} />);

    const usernameInput = screen.getByLabelText("Username");
    const passwordInput = screen.getByLabelText("Password");

    await user.type(usernameInput, "admin");
    await user.type(passwordInput, "admin123");

    expect(usernameInput).toHaveValue("admin");
    expect(passwordInput).toHaveValue("admin123");
  });

  it("should call onLogin after successful login", async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();

    const mockUser = {
      id: 1,
      username: "admin",
      fullName: "Admin User",
      role: "ADMIN" as const,
    };

    vi.mocked(apiRequest).mockResolvedValue(mockUser);

    render(<Login onLogin={onLogin} />);

    await user.type(screen.getByLabelText("Username"), "admin");
    await user.type(screen.getByLabelText("Password"), "admin123");

    await user.click(screen.getByRole("button", { name: "Sign In" }));

    expect(apiRequest).toHaveBeenCalledWith("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: "admin",
        password: "admin123",
      }),
    });

    expect(onLogin).toHaveBeenCalledWith(mockUser);
  });

  it("should display an error when login fails", async () => {
    const user = userEvent.setup();
    const onLogin = vi.fn();

    vi.mocked(apiRequest).mockRejectedValue(
      new Error("Invalid username or password."),
    );

    render(<Login onLogin={onLogin} />);

    await user.type(screen.getByLabelText("Username"), "admin");
    await user.type(screen.getByLabelText("Password"), "wrongpassword");

    await user.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Invalid username or password.",
    );

    expect(onLogin).not.toHaveBeenCalled();
  });

  it("should disable the submit button while login is processing", async () => {
    const user = userEvent.setup();

    vi.mocked(apiRequest).mockReturnValue(new Promise(() => {}));

    render(<Login onLogin={vi.fn()} />);

    await user.type(screen.getByLabelText("Username"), "admin");
    await user.type(screen.getByLabelText("Password"), "admin123");

    await user.click(screen.getByRole("button", { name: "Sign In" }));

    expect(
      screen.getByRole("button", { name: "Signing in..." }),
    ).toBeDisabled();
  });

  it("should toggle password visibility", async () => {
    const user = userEvent.setup();

    render(<Login onLogin={vi.fn()} />);

    const passwordInput = screen.getByLabelText("Password");

    expect(passwordInput).toHaveAttribute("type", "password");

    await user.click(screen.getByRole("button", { name: "Show password" }));

    expect(passwordInput).toHaveAttribute("type", "text");

    await user.click(screen.getByRole("button", { name: "Hide password" }));

    expect(passwordInput).toHaveAttribute("type", "password");
  });

  it("should trim whitespace from username before login", async () => {
    const user = userEvent.setup();

    vi.mocked(apiRequest).mockResolvedValue({
      id: 1,
      username: "admin",
      fullName: "Admin User",
      role: "ADMIN",
    });

    render(<Login onLogin={vi.fn()} />);

    await user.type(screen.getByLabelText("Username"), "  admin  ");
    await user.type(screen.getByLabelText("Password"), "admin123");

    await user.click(screen.getByRole("button", { name: "Sign In" }));

    expect(apiRequest).toHaveBeenCalledWith("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: "admin",
        password: "admin123",
      }),
    });
  });

  it("should clear the error when the user edits an input", async () => {
    const user = userEvent.setup();

    vi.mocked(apiRequest).mockRejectedValue(
      new Error("Invalid username or password."),
    );

    render(<Login onLogin={vi.fn()} />);

    const usernameInput = screen.getByLabelText("Username");

    await user.type(usernameInput, "admin");
    await user.type(screen.getByLabelText("Password"), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();

    await user.type(usernameInput, "2");

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
