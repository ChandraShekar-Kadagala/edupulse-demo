import type { ReactNode } from 'react';
import { AlertCircle, Inbox, RefreshCw } from 'lucide-react';
import { Button } from '../ui/button';

export function LoadingState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div role="status" aria-live="polite" className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-gray-500 dark:text-gray-400">
      <RefreshCw className="h-8 w-8 animate-spin text-indigo-600 dark:text-indigo-400" aria-hidden="true" />
      <p className="text-sm font-semibold text-gray-800 dark:text-gray-200">{title}</p>
      {detail && <p className="text-xs">{detail}</p>}
    </div>
  );
}

export function ErrorState({ title, message, onRetry }: { title: string; message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="max-w-xl mx-auto my-10 p-6 rounded-2xl border border-red-200 dark:border-red-900/60 bg-red-50 dark:bg-red-950/40 text-red-800 dark:text-red-200 text-center">
      <AlertCircle className="w-8 h-8 mx-auto mb-2 text-red-600 dark:text-red-400" aria-hidden="true" />
      <h2 className="text-base font-bold">{title}</h2>
      <p className="text-sm mt-1">{message}</p>
      {onRetry && (
        <Button variant="destructive" size="sm" onClick={onRetry} className="mt-4">
          <RefreshCw className="h-4 w-4 mr-2" aria-hidden="true" /> Retry
        </Button>
      )}
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="p-6 rounded-xl border border-dashed border-gray-300 dark:border-gray-700 text-center text-gray-600 dark:text-gray-300">
      <Inbox className="w-7 h-7 mx-auto mb-2 text-gray-400" aria-hidden="true" />
      <p className="text-sm font-semibold">{title}</p>
      {children && <div className="text-xs mt-1 text-gray-500 dark:text-gray-400">{children}</div>}
    </div>
  );
}

/** Formats a nullable number; missing data is shown as an explicit dash, never as zero. */
export function fmt(value: number | null | undefined, suffix = '', digits = 1): string {
  return value === null || value === undefined || Number.isNaN(value) ? '—' : `${value.toFixed(digits)}${suffix}`;
}

export function riskBadgeVariant(level?: string | null): 'destructive' | 'warning' | 'success' | 'outline' {
  if (level === 'HIGH') return 'destructive';
  if (level === 'MEDIUM') return 'warning';
  if (level === 'LOW') return 'success';
  return 'outline';
}
