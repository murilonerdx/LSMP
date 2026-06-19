import { useToasts } from '../store/toast'

export function ToastContainer() {
  const list = useToasts((s) => s.list)
  const dismiss = useToasts((s) => s.dismiss)
  return (
    <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
      {list.map((t) => (
        <div
          key={t.id}
          onClick={() => dismiss(t.id)}
          className={`pointer-events-auto cursor-pointer min-w-[260px] max-w-sm px-4 py-3 rounded-xl backdrop-blur-md border shadow-2xl text-sm
            transition-all toast-anim
            ${t.kind === 'ok' ? 'bg-emerald-500/15 border-emerald-400/40 text-emerald-100' : ''}
            ${t.kind === 'err' ? 'bg-red-500/15 border-red-400/40 text-red-100' : ''}
            ${t.kind === 'info' ? 'bg-liberthia-500/15 border-liberthia-400/40 text-liberthia-100' : ''}`}
        >
          <div className="flex items-start gap-2">
            <span className="text-base">{t.kind === 'ok' ? '✓' : t.kind === 'err' ? '✗' : 'ℹ'}</span>
            <span className="flex-1">{t.text}</span>
          </div>
        </div>
      ))}
    </div>
  )
}
