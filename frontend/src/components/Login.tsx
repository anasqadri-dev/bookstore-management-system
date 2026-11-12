
"use client";

import { useState } from "react";
import type { CSSProperties, FormEvent } from "react";

type LoginProps = {
    onLogin: () => void;
};

export default function Login({ onLogin }: LoginProps) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [focused, setFocused] = useState("");

    function handleLogin(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();

        if (
            email.trim().toLowerCase() === "admin@bookstore.com" &&
            password === "admin123"
        ) {
            setError("");
            onLogin();
        } else {
            setError("Invalid email or password. Please try again.");
        }
    }

    const inputStyle = (name: string): CSSProperties => ({
        width: "100%",
        height: "46px",
        padding: "0 15px",
        paddingRight: name === "password" ? "75px" : "15px",
        border: focused === name
            ? "1px solid #2563eb"
            : "1px solid #d1d5db",
        borderRadius: "8px",
        backgroundColor: "#ffffff",
        color: "#111827",
        fontSize: "14px",
        outline: "none",
        boxSizing: "border-box",
        boxShadow: focused === name
            ? "0 0 0 3px rgba(37,99,235,0.12)"
            : "none",
        transition: "border 0.2s, box-shadow 0.2s",
    });

    return (
        <div className="login-page">
            <div className="login-card">

                {/* Left Panel */}
                <div className="login-left">
                    <div className="login-brand">
                        <div className="login-logo">
                            <svg
                                width="26"
                                height="26"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="1.8"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                aria-hidden="true"
                            >
                                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                                <path d="M9 7h7" />
                                <path d="M9 11h5" />
                            </svg>
                        </div>

                        <div>
                            <h1>Book Store</h1>
                            <p>Management System</p>
                        </div>
                    </div>

                    <div className="login-intro">
                        <div className="login-accent" />
                        <p className="login-eyebrow">ADMIN DASHBOARD</p>

                        <h2>
                            Your bookstore,
                            <span>under control.</span>
                        </h2>

                        <p className="login-description">
                            One organized workspace to manage your
                            inventory, sales, suppliers and school orders.
                        </p>

                        <div className="login-features">
                            {[
                                "Inventory & Stock Control",
                                "Sales & Order Management",
                                "Business Reports",
                            ].map((feature) => (
                                <div className="login-feature" key={feature}>
                                    <span className="login-check">✓</span>
                                    <span>{feature}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    <p className="login-footer">
                        Book Store Management System
                    </p>
                </div>

                {/* Right Panel */}
                <div className="login-right">
                    <div className="login-form-container">
                        <p className="login-eyebrow">
                            SECURE ADMIN ACCESS
                        </p>

                        <h2 className="login-welcome">
                            Welcome back
                        </h2>

                        <p className="login-subtitle">
                            Enter your credentials to continue.
                        </p>

                        <form onSubmit={handleLogin}>
                            <div className="login-field">
                                <label htmlFor="login-email">
                                    Email Address
                                </label>

                                <input
                                    id="login-email"
                                    type="email"
                                    required
                                    autoComplete="username"
                                    placeholder="Enter your email"
                                    value={email}
                                    onChange={(e) => {
                                        setEmail(e.target.value);
                                        setError("");
                                    }}
                                    onFocus={() => setFocused("email")}
                                    onBlur={() => setFocused("")}
                                    style={inputStyle("email")}
                                />
                            </div>

                            <div className="login-field">
                                <label htmlFor="login-password">
                                    Password
                                </label>

                                <div className="login-password-wrapper">
                                    <input
                                        id="login-password"
                                        type={showPassword ? "text" : "password"}
                                        required
                                        autoComplete="current-password"
                                        placeholder="Enter your password"
                                        value={password}
                                        onChange={(e) => {
                                            setPassword(e.target.value);
                                            setError("");
                                        }}
                                        onFocus={() => setFocused("password")}
                                        onBlur={() => setFocused("")}
                                        style={inputStyle("password")}
                                    />

                                    <button
                                        type="button"
                                        className="login-show"
                                        onClick={() => setShowPassword(!showPassword)}
                                        aria-label={
                                            showPassword
                                                ? "Hide password"
                                                : "Show password"
                                        }
                                    >
                                        {showPassword ? "Hide" : "Show"}
                                    </button>
                                </div>
                            </div>

                            {error && (
                                <p className="login-error" role="alert">
                                    {error}
                                </p>
                            )}

                            <button type="submit" className="login-submit">
                                Sign In
                            </button>
                        </form>

                        <div className="login-demo">
                            <p className="login-demo-title">DEMO ACCOUNT</p>
                            <p>Email: admin@bookstore.com</p>
                            <p>Password: admin123</p>
                        </div>

                        <p className="login-form-footer">
                            Book Store Management System
                        </p>
                    </div>
                </div>
            </div>

            <style jsx>{`
                .login-page {
                    min-height: 100dvh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                    background: #f3f4f6;
                    color: #111827;
                    box-sizing: border-box;
                }

                .login-card {
                    display: grid;
                    grid-template-columns: 43% 57%;
                    width: 100%;
                    max-width: 980px;
                    min-height: 540px;
                    overflow: hidden;
                    border-radius: 16px;
                    border: 1px solid #e5e7eb;
                    background: white;
                    box-shadow: 0 16px 45px rgba(15, 23, 42, 0.09);
                }

                .login-left {
                    background: #030712;
                    color: white;
                    padding: 32px 38px;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    gap: 24px;
                }

                .login-brand {
                    display: flex;
                    align-items: center;
                    gap: 14px;
                }

                .login-logo {
                    width: 46px;
                    height: 46px;
                    flex-shrink: 0;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #1e293b;
                    border-radius: 10px;
                }

                .login-brand h1 {
                    font-size: 21px;
                    font-weight: 700;
                    line-height: 1.3;
                }

                .login-brand p {
                    font-size: 12px;
                    color: #94a3b8;
                    margin-top: 4px;
                }

                .login-accent {
                    width: 42px;
                    height: 4px;
                    background: #3b82f6;
                    border-radius: 5px;
                    margin-bottom: 20px;
                }

                .login-eyebrow {
                    color: #3b82f6;
                    font-size: 11px;
                    font-weight: 700;
                    letter-spacing: 1.8px;
                }

                .login-left .login-eyebrow {
                    color: #60a5fa;
                }

                .login-intro h2 {
                    margin-top: 16px;
                    font-size: clamp(25px, 2.5vw, 33px);
                    line-height: 1.25;
                    font-weight: 750;
                    letter-spacing: -0.8px;
                }

                .login-intro h2 span {
                    display: block;
                    color: #60a5fa;
                }

                .login-description {
                    margin-top: 18px;
                    font-size: 13px;
                    line-height: 1.8;
                    color: #94a3b8;
                }

                .login-features {
                    margin-top: 25px;
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                }

                .login-feature {
                    display: flex;
                    align-items: center;
                    gap: 11px;
                    font-size: 12px;
                    color: #cbd5e1;
                }

                .login-check {
                    width: 23px;
                    height: 23px;
                    flex-shrink: 0;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 50%;
                    background: #1e293b;
                    color: #60a5fa;
                }

                .login-footer {
                    font-size: 11px;
                    color: #64748b;
                }

                .login-right {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 36px;
                }

                .login-form-container {
                    width: 100%;
                    max-width: 355px;
                }

                .login-welcome {
                    margin-top: 12px;
                    font-size: 28px;
                    font-weight: 750;
                    letter-spacing: -0.7px;
                }

                .login-subtitle {
                    margin-top: 9px;
                    margin-bottom: 28px;
                    color: #6b7280;
                    font-size: 13px;
                    line-height: 1.6;
                }

                .login-field {
                    margin-bottom: 20px;
                }

                .login-field label {
                    display: block;
                    margin-bottom: 9px;
                    font-size: 13px;
                    font-weight: 600;
                    color: #374151;
                }

                .login-password-wrapper {
                    position: relative;
                }

                .login-show {
                    position: absolute;
                    top: 50%;
                    right: 11px;
                    transform: translateY(-50%);
                    border: 0;
                    background: transparent;
                    padding: 6px;
                    color: #2563eb;
                    font-size: 12px;
                    font-weight: 600;
                    cursor: pointer;
                }

                .login-submit {
                    width: 100%;
                    height: 46px;
                    border: none;
                    border-radius: 8px;
                    background: #2563eb;
                    color: white;
                    font-size: 14px;
                    font-weight: 650;
                    cursor: pointer;
                    transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
                }

                .login-submit:hover {
                    background: #1d4ed8;
                    transform: translateY(-1px);
                    box-shadow: 0 5px 14px rgba(37, 99, 235, 0.2);
                }

                .login-submit:active {
                    transform: translateY(0);
                }

                .login-submit:focus-visible,
                .login-show:focus-visible {
                    outline: 3px solid #93c5fd;
                    outline-offset: 3px;
                }

                .login-error {
                    margin-bottom: 14px;
                    padding: 11px;
                    border-radius: 7px;
                    background: #fef2f2;
                    color: #dc2626;
                    font-size: 12px;
                }

                .login-demo {
                    margin-top: 25px;
                    padding: 15px 18px;
                    border: 1px solid #e2e8f0;
                    border-radius: 9px;
                    background: #f8fafc;
                }

                .login-demo-title {
                    font-weight: 700;
                    color: #334155 !important;
                    margin-bottom: 8px;
                }

                .login-demo p {
                    font-size: 12px;
                    line-height: 1.9;
                    color: #64748b;
                }

                .login-form-footer {
                    margin-top: 20px;
                    text-align: center;
                    font-size: 11px;
                    color: #94a3b8;
                }

                @media (max-width: 700px) {
                    .login-page {
                        padding: 14px;
                        align-items: flex-start;
                    }

                    .login-card {
                        grid-template-columns: 1fr;
                        min-height: auto;
                    }

                    .login-left {
                        padding: 25px;
                        gap: 18px;
                    }

                    .login-intro {
                        display: none;
                    }

                    .login-footer {
                        display: none;
                    }

                    .login-right {
                        padding: 35px 24px;
                    }

                    .login-form-container {
                        max-width: 100%;
                    }
                }

                @media (max-height: 650px) and (min-width: 701px) {
                    .login-page {
                        align-items: flex-start;
                    }

                    .login-card {
                        min-height: auto;
                    }
                }

                @media (prefers-reduced-motion: reduce) {
                    .login-submit {
                        transition: none;
                    }
                }
            `}</style>
        </div>
    );
}
