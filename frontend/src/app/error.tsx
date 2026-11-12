"use client";

import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Do not log error details that could contain user or session data.
    console.error("A bookstore page failed to render.", { digest: error.digest });
  }, [error]);

  return (
    <main role="alert" className="flex min-h-screen items-center justify-center bg-gray-100 p-6">
      <section className="w-full max-w-md rounded-lg border border-gray-200 bg-white p-8 text-gray-900">
        <h1 className="text-2xl font-semibold">Something went wrong</h1>
        <p className="mt-3 text-sm text-gray-600">
          We could not display this page. Please try again.
        </p>
        <button
          type="button"
          onClick={reset}
          className="mt-6 rounded bg-blue-600 px-5 py-3 text-white hover:bg-blue-700 focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
        >
          Try again
        </button>
      </section>
    </main>
  );
}
