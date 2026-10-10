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
          'border-transparent bg-green-500/20 text-green-400 hover:bg-green-500/30': variant === 'success',
          'border-transparent bg-amber-500/20 text-amber-400 hover:bg-amber-500/30': variant === 'warning',
          'border-transparent bg-red-500/20 text-red-400 hover:bg-red-500/30': variant === 'destructive',
          'text-gray-100 border-gray-700': variant === 'outline',
        },
        className
      )}
      {...props}
    />
  )
}

export { Badge }
