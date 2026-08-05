# Pipeline de dados

O pipeline consulta a API-Football v3 e publica três arquivos com contrato estável:

- `fixtures.json`: temporada, próximos jogos e resultados.
- `live.json`: partidas em andamento e eventos disponíveis.
- `standings.json`: classificação do Brasileirão Série A.

## Comandos

- `npm run fetch`: consulta e normaliza a API.
- `npm run build`: valida os JSONs existentes sem consultar a internet.
- `npm run all`: consulta e valida em sequência.

Variável obrigatória: `FOOTBALL_API_KEY`.

Variáveis opcionais:

- `CORINTHIANS_TEAM_ID` (padrão `131`).
- `BRASILEIRAO_LEAGUE_ID` (padrão `71`).
- `FOOTBALL_SEASON` (padrão: ano atual).

A gravação é atômica: um erro de consulta ou normalização não substitui um arquivo válido pela metade.
