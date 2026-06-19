/**
 * Charts SVG leves — sem dependência externa. Pra dashboards onde recharts
 * seria overkill. Estilo neon roxo Liberthia.
 */

export function Sparkline({
  data, width = 100, height = 32, color = '#A78BFA', fill = 'rgba(167, 139, 250, 0.2)',
}: {
  data: number[]
  width?: number
  height?: number
  color?: string
  fill?: string
}) {
  if (!data || data.length === 0) return <svg width={width} height={height} />
  const max = Math.max(...data, 1)
  const min = Math.min(...data, 0)
  const range = max - min || 1
  const step = width / Math.max(1, data.length - 1)
  const points = data.map((v, i) => {
    const x = i * step
    const y = height - ((v - min) / range) * height
    return `${x},${y}`
  }).join(' ')
  const area = `0,${height} ${points} ${width},${height}`
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
      <polygon points={area} fill={fill} />
      <polyline points={points} fill="none" stroke={color} strokeWidth={1.5} strokeLinecap="round" />
      {data.length > 0 && (
        <circle
          cx={(data.length - 1) * step}
          cy={height - ((data[data.length - 1] - min) / range) * height}
          r={2.5}
          fill={color}
        />
      )}
    </svg>
  )
}

export function BarChart({
  data, width = 240, height = 80, color = '#A78BFA', labels,
}: {
  data: number[]
  width?: number
  height?: number
  color?: string
  labels?: string[]
}) {
  if (!data || data.length === 0) return <svg width={width} height={height} />
  const max = Math.max(...data, 1)
  const barW = (width / data.length) * 0.7
  const gap = (width / data.length) * 0.3
  return (
    <svg width={width} height={height + 14} viewBox={`0 0 ${width} ${height + 14}`}>
      {data.map((v, i) => {
        const h = (v / max) * height
        const x = i * (barW + gap) + gap / 2
        const y = height - h
        return (
          <g key={i}>
            <rect x={x} y={y} width={barW} height={h} fill={color} rx={1.5} />
            {labels && labels[i] && (
              <text
                x={x + barW / 2}
                y={height + 11}
                fontSize={7}
                fill="rgba(167,139,250,0.6)"
                textAnchor="middle">
                {labels[i]}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}

/** Donut chart simples — 1 valor + total. */
export function Donut({
  value, total, size = 80, color = '#A78BFA', bgColor = 'rgba(167,139,250,0.15)',
  label,
}: {
  value: number
  total: number
  size?: number
  color?: string
  bgColor?: string
  label?: string
}) {
  const safe = Math.max(0, Math.min(total, value))
  const pct = total > 0 ? safe / total : 0
  const r = size / 2 - 8
  const c = 2 * Math.PI * r
  const dash = c * pct
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke={bgColor} strokeWidth={6} fill="none" />
        <circle
          cx={size / 2} cy={size / 2} r={r}
          stroke={color} strokeWidth={6} fill="none"
          strokeDasharray={`${dash} ${c}`}
          strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
        <div className="text-base font-bold leading-none">{Math.round(pct * 100)}%</div>
        {label && <div className="text-[8px] text-liberthia-300/60 mt-0.5">{label}</div>}
      </div>
    </div>
  )
}

export function StatCard({
  label, value, icon, color = 'purple', sub, trend,
}: {
  label: string
  value: number | string
  icon?: string
  color?: 'purple' | 'amber' | 'emerald' | 'red' | 'blue' | 'pink'
  sub?: string
  trend?: number[]
}) {
  const colorMap: Record<string, { text: string; chart: string; fill: string }> = {
    purple:  { text: 'text-purple-300', chart: '#A78BFA', fill: 'rgba(167,139,250,0.2)' },
    amber:   { text: 'text-amber-300', chart: '#FCD34D', fill: 'rgba(252,211,77,0.2)' },
    emerald: { text: 'text-emerald-300', chart: '#34D399', fill: 'rgba(52,211,153,0.2)' },
    red:     { text: 'text-red-300', chart: '#F87171', fill: 'rgba(248,113,113,0.2)' },
    blue:    { text: 'text-blue-300', chart: '#60A5FA', fill: 'rgba(96,165,250,0.2)' },
    pink:    { text: 'text-pink-300', chart: '#F472B6', fill: 'rgba(244,114,182,0.2)' },
  }
  const c = colorMap[color]
  return (
    <div className="card-glow">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="text-[10px] uppercase text-liberthia-300/50 tracking-wider">{label}</div>
          <div className={`text-3xl font-bold mt-1 ${c.text}`}>
            {icon && <span className="text-xl mr-1">{icon}</span>}{value}
          </div>
          {sub && <div className="text-[10px] text-liberthia-300/60 mt-1">{sub}</div>}
        </div>
        {trend && trend.length > 0 && (
          <Sparkline data={trend} width={70} height={28} color={c.chart} fill={c.fill} />
        )}
      </div>
    </div>
  )
}
