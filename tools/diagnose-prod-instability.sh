#!/bin/bash
# Diagnóstico da instabilidade em produção.
# Rodar no VPS (srv614263) via:
#   curl -fsSL https://raw.githubusercontent.com/.../diagnose-prod-instability.sh | bash
# ou copiar via scp e: bash diagnose-prod-instability.sh

set -e
echo "═══════════════════════════════════════════════════════════════"
echo " Diagnóstico de instabilidade lsmp.astaroneremita.com"
echo " Hora: $(date)"
echo "═══════════════════════════════════════════════════════════════"

echo
echo "▶ 1. Versões das imagens em uso (deveria ser backend:v112)"
docker service ls --format 'table {{.Name}}\t{{.Image}}\t{{.Replicas}}' | grep lsmp

echo
echo "▶ 2. Replicas — alguma instância morrendo?"
docker service ps lsmp_backend --no-trunc | head -10

echo
echo "▶ 3. Token configurado no docker-stack vs token do mod"
echo "   docker-stack MOD_TOKEN:"
docker service inspect lsmp_backend --format '{{range .Spec.TaskTemplate.ContainerSpec.Env}}{{if eq (index (split . "=") 0) "MOD_TOKEN"}}     {{.}}{{end}}{{end}}'

echo
echo "▶ 4. Teste DIRETO do token contra o mod"
TOKEN=$(docker service inspect lsmp_backend --format '{{range .Spec.TaskTemplate.ContainerSpec.Env}}{{if eq (index (split . "=") 0) "MOD_TOKEN"}}{{index (split . "=") 1}}{{end}}{{end}}')
echo "   GET /api/players com token atual:"
curl -sS -m 10 -H "Authorization: Bearer $TOKEN" http://lsmp.ddns.net:25580/api/players \
  | head -c 200
echo
echo "   Se retornar {\"error\":\"invalid token\"} → TOKEN ESTÁ ERRADO no stack."
echo "   Se retornar JSON com players → token OK, problema é OUTRO."

echo
echo "▶ 5. Mod /health responde?"
curl -sS -m 5 http://lsmp.ddns.net:25580/health || echo "   (mod offline)"

echo
echo "▶ 6. Últimas 20 linhas do log do backend"
docker service logs --tail 20 lsmp_backend 2>&1 | tail -20

echo
echo "═══════════════════════════════════════════════════════════════"
echo " Próximos passos baseados no resultado de #4:"
echo
echo " Se invalid token → o mod regenerou o token. Você precisa:"
echo "   1. SSH no server MC, lê o token novo de config/liberthia*.toml"
echo "   2. Atualiza MOD_TOKEN no docker-stack.yml (no VPS)"
echo "   3. docker stack deploy -c /root/docker-stack.yml lsmp"
echo
echo " Se token OK mas players retorna erro diferente → bug no mod,"
echo " precisa logar o mod do server MC pra ver o que tá rejeitando."
echo "═══════════════════════════════════════════════════════════════"
