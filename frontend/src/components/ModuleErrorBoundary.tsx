"use client";

import { Component, type ReactNode } from "react";

type Props = {
  children: ReactNode;
  onReturnToDashboard: () => void;
};

type State = { hasError: boolean };

export default class ModuleErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch() {
    // Avoid logging raw exception messages, stack traces, or sensitive data.
    console.error("A bookstore module failed to render.");
  }

  reset = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (this.state.hasError) {
      return (
        <section role="alert" className="rounded border border-red-200 bg-white p-6">
          <h3 className="text-xl font-semibold">This section could not be displayed</h3>
          <p className="mt-2 text-sm text-gray-600">
            Other sections are still available. You can retry or return to the dashboard.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <button type="button" onClick={this.reset}
              className="rounded bg-blue-600 px-4 py-2 text-white focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-blue-600">
              Try again
            </button>
            <button type="button" onClick={this.props.onReturnToDashboard}
              className="rounded border border-gray-300 px-4 py-2 focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-blue-600">
              Back to dashboard
            </button>
          </div>
        </section>
      );
    }

    return this.props.children;
  }
}
