# Contrato simples do backend

## Endpoint
`POST /api/v1/infection/snapshot`

## Exemplo de payload
```json
{
  "playerUuid": "00000000-0000-0000-0000-000000000000",
  "playerName": "Murilo",
  "level": "minecraft:overworld",
  "infection": 42,
  "stage": 2,
  "permanentHealthPenalty": 4,
  "x": 120,
  "y": 71,
  "z": -38,
  "timestamp": 1711920000000
}
```

## Uso sugerido
- painel web
- ranking de infectados
- protocolos narrativos do Anfitrião
- eventos remotos do servidor
