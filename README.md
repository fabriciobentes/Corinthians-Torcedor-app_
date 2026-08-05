# Corinthians Live

Aplicativo Android pessoal para acompanhar jogos, resultados, placar ao vivo, eventos e a classificação do Corinthians.

## O que já funciona

- Próximos jogos e últimos resultados em uma única agenda.
- Placar e eventos quando houver uma partida ao vivo.
- Classificação do Brasileirão Série A com destaque para o Corinthians.
- Atualização manual em todas as telas.
- Modo offline usando a última cópia de dados incluída no aplicativo.
- Configuração do endereço dos dados dentro do próprio app.
- Pipeline sem custo executado pelo GitHub Actions.
- Chave da API protegida: ela nunca é incluída no APK ou nos JSONs públicos.

## Ativar os dados reais

1. Crie uma conta na API-Football/API-Sports e copie sua chave.
2. Envie este projeto para um repositório no GitHub.
3. No repositório, abra **Settings → Secrets and variables → Actions**.
4. Crie o secret `FOOTBALL_API_KEY` com a chave da API.
5. Abra **Actions → Atualizar dados do Corinthians → Run workflow**.
6. Em **Settings → Pages**, selecione **GitHub Actions** como fonte de publicação.
7. Execute o workflow **Publicar dados no GitHub Pages** caso ele ainda não tenha iniciado.
8. No aplicativo, abra **Ajustes** e cole o endereço publicado, normalmente:

   `https://SEU-USUARIO.github.io/NOME-DO-REPOSITORIO`

Os dados são atualizados automaticamente uma vez por hora. Essa frequência preserva a cota gratuita e acompanha a recomendação da fonte para tabelas e partidas.

## Executar o pipeline localmente

No PowerShell, dentro de `data-pipeline`:

```powershell
$env:FOOTBALL_API_KEY="sua-chave"
npm.cmd run all
```

Os arquivos finais ficam em `data-pipeline/public`. Nunca coloque a chave em arquivos versionados.

## Compilar o Android

Abra `android-app` no Android Studio ou use Java 17+ e execute:

```powershell
./gradlew assembleDebug
```

Para compilar com um endereço online já embutido:

```powershell
./gradlew assembleDebug -PCORINTHIANS_DATA_URL=https://SEU-USUARIO.github.io/REPOSITORIO
```

O endereço também pode ser alterado depois, na tela **Ajustes**.

## Estrutura

- `android-app/`: aplicativo Kotlin e Jetpack Compose.
- `data-pipeline/`: consulta e normalização dos dados da API-Football.
- `data-pipeline/public/`: JSONs consumidos pelo aplicativo.
- `.github/workflows/`: atualização automática e publicação no GitHub Pages.

## Observações

- O ID padrão do Corinthians é `131` e o do Brasileirão Série A é `71` na API-Football.
- Eles podem ser sobrescritos no pipeline pelas variáveis `CORINTHIANS_TEAM_ID` e `BRASILEIRAO_LEAGUE_ID`.
- Algumas competições não fornecem todos os eventos. Nesses casos, o placar continua visível e o app informa que os lances estão indisponíveis.
