# Liberthia Admin Frontend

Painel web React + Vite + Tailwind.

## Dev

```bash
npm install
npm run dev
```

Abre `http://localhost:5173`. Vite proxy direciona `/api/*` e `/ws` pro backend em `localhost:8080`.

## Funcionalidades

- 📊 **Dashboard:** stats do servidor, lista de players, console de comandos, eventos live
- 👤 **Player Detail:** abas de Inventário / Dar Item / Matéria / Ações
- 🎁 **Dar Item:** picker com 1800+ items (vanilla + mod) + encantamentos custom
- ⚛ **Matéria:** sliders pra ajustar DM/WM/YM em tempo real
- ⚙ **Ações:** teleport, effect, kick, clear inventory
- 📡 **Eventos Live:** WebSocket conectado mostra login/logout/death

## Build prod

```bash
npm run build
```

Saída em `dist/` — serve com qualquer static server.
