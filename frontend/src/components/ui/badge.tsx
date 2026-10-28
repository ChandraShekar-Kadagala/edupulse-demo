import * as React from "react"
import { cn } from "../../utils/utils"

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'success' | 'warning' | 'destructive' | 'outline';
}

function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <div
      className={cn(
        "inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2",
        {
          'border-transparent bg-indigo-600 text-white hover:bg-indigo-700': variant === 'default',
          'border-green-300 dark:border-green-500/30 bg-green-100 dark:bg-green-500/20 text-green-800 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-500/30': variant === 'success',
          'border-amber-300 dark:border-amber-500/30 bg-amber-100 dark:bg-amber-500/20 text-amber-900 dark:text-amber-300 hover:bg-amber-200 dark:hover:bg-amber-500/30': variant === 'warning',
          'border-red-300 dark:border-red-500/30 bg-red-100 dark:bg-red-500/20 text-red-800 dark:text-red-300 hover:bg-red-200 dark:hover:bg-red-500/30': variant === 'destructive',
          'text-gray-700 dark:text-gray-200 border-gray-300 dark:border-gray-700 bg-white/50 dark:bg-gray-800/50': variant === 'outline',
        },
        className
      )}
      {...props}
    />
  )
}

export { Badge }
