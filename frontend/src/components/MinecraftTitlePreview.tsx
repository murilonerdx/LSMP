import { renderMcText } from './MinecraftFormatter'

/**
 * Preview de como title/subtitle/actionbar/chat aparecem em jogo.
 *
 * MC vanilla NÃO tem controle de "tamanho" do title — ele é sempre fixo. Mas
 * a gente pode escolher o canal de delivery, que define visualmente:
 *   - title      → texto GIGANTE no centro
 *   - subtitle   → menor, abaixo do title
 *   - actionbar  → pequeno, acima da hotbar
 *   - chat       → texto normal no chat
 *
 * Aqui renderizo um "MC viewport" fake mostrando como vai sair.
 */

export type DeliveryMode = 'title' | 'subtitle' | 'actionbar' | 'chat'

type Props = {
  title?: string
  subtitle?: string
  mode?: DeliveryMode    // determina tamanho/posição
  className?: string
}

const MODE_LABELS: Record<DeliveryMode, string> = {
  title: '🅰 Grande',
  subtitle: '🆎 Médio',
  actionbar: '🅰 Pequeno',
  chat: '💬 Chat',
}

export function MinecraftTitlePreview({ title, subtitle, mode = 'title', className = '' }: Props) {
  return (
    <div className={`relative rounded-lg overflow-hidden border border-liberthia-500/30 ${className}`}
         style={{
           background: 'linear-gradient(180deg, #5d8ec0 0%, #8ec38e 60%, #8ec38e 100%)',
           minHeight: 220,
         }}>
      {/* Label */}
      <div className="absolute top-2 left-2 z-10">
        <span className="bg-black/70 text-white text-[10px] px-2 py-0.5 rounded font-mono">
          {MODE_LABELS[mode]} · preview
        </span>
      </div>

      {/* Conteúdo do preview baseado no mode */}
      {mode === 'title' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center px-6">
          {title && (
            <div
              className="text-white text-center"
              style={{
                fontSize: '46px',
                fontWeight: 'normal',
                fontFamily: '"Minecraftia", "Press Start 2P", monospace',
                textShadow: '4px 4px 0 rgba(0,0,0,0.6)',
                lineHeight: 1.1,
                letterSpacing: '0.5px',
              }}>
              {renderMcText(title)}
            </div>
          )}
          {subtitle && (
            <div
              className="text-white text-center mt-3"
              style={{
                fontSize: '20px',
                fontFamily: '"Minecraftia", monospace',
                textShadow: '2px 2px 0 rgba(0,0,0,0.6)',
                lineHeight: 1.1,
              }}>
              {renderMcText(subtitle)}
            </div>
          )}
        </div>
      )}

      {mode === 'subtitle' && (
        <div className="absolute inset-0 flex flex-col items-center justify-center px-6">
          <div
            className="text-white text-center"
            style={{
              fontSize: '22px',
              fontFamily: '"Minecraftia", monospace',
              textShadow: '2px 2px 0 rgba(0,0,0,0.6)',
              lineHeight: 1.2,
            }}>
            {renderMcText(title || subtitle || '')}
          </div>
        </div>
      )}

      {mode === 'actionbar' && (
        <div className="absolute bottom-12 left-0 right-0 flex justify-center">
          <div
            className="text-white text-center px-4 py-1"
            style={{
              fontSize: '14px',
              fontFamily: '"Minecraftia", monospace',
              textShadow: '1px 1px 0 rgba(0,0,0,0.7)',
            }}>
            {renderMcText(title || subtitle || '')}
          </div>
        </div>
      )}

      {mode === 'chat' && (
        <div className="absolute bottom-12 left-2 right-2">
          <div className="bg-black/40 rounded px-2 py-1">
            <div
              className="text-white"
              style={{
                fontSize: '12px',
                fontFamily: '"Minecraftia", monospace',
                textShadow: '1px 1px 0 rgba(0,0,0,0.7)',
              }}>
              {renderMcText(title || subtitle || '')}
            </div>
          </div>
        </div>
      )}

      {/* "Hotbar" fake na base pra dar contexto */}
      <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-0.5">
        {Array.from({ length: 9 }, (_, i) => (
          <div key={i} className="w-6 h-6 bg-black/30 border border-black/50 rounded-sm" />
        ))}
      </div>
    </div>
  )
}

/**
 * Selector de delivery mode (4 botões).
 */
export function DeliveryModeSelector({ value, onChange }: { value: DeliveryMode; onChange: (m: DeliveryMode) => void }) {
  const modes: DeliveryMode[] = ['title', 'subtitle', 'actionbar', 'chat']
  return (
    <div className="tab-strip">
      {modes.map((m) => (
        <div key={m}
          className={`tab-item ${value === m ? 'active' : ''}`}
          onClick={() => onChange(m)}
        >{MODE_LABELS[m]}</div>
      ))}
    </div>
  )
}
