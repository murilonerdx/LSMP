import { ReactNode } from 'react'

type Props = {
  title: string
  subtitle?: string
  icon?: string
  children?: ReactNode
}

export function PageHeader({ title, subtitle, icon, children }: Props) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 pb-2 border-b border-liberthia-600/30">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-3">
          {icon && <span className="text-3xl">{icon}</span>}
          <span className="gradient-text">{title}</span>
        </h1>
        {subtitle && <p className="text-sm text-liberthia-300/60 mt-1 ml-12">{subtitle}</p>}
      </div>
      {children && <div className="flex items-center gap-2">{children}</div>}
    </div>
  )
}
