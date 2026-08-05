# Corinthians Live

Aplicativo Android pessoal para acompanhar os jogos e a classificação do Corinthians.

## O que funciona

- Próximo jogo do Corinthians no Brasileirão Série A.
- Histórico dos últimos resultados capturados pelo projeto.
- Placar e situação da partida quando o Corinthians estiver em campo.
- Classificação completa do Brasileirão com destaque para o Corinthians.
- Atualização automática pelo GitHub a cada hora.
- Funcionamento offline com a última cópia incluída no aplicativo.
- Endereço dos dados já configurado para este repositório.
- Modo escuro e atualização manual em todas as telas.

## Dados reais sem chave

O projeto usa a biblioteca gratuita `campeonato-brasileiro-api`, que lê a rodada atual e a classificação publicadas pelo ge. Não é necessário cadastrar chave nem pagar um plano.

O secret antigo `FOOTBALL_API_KEY` pode continuar salvo no GitHub, mas não é mais utilizado e pode ser excluído em **Settings → Secrets and variables → Actions**.

A fonte gratuita informa placar e situação da partida, mas não fornece a lista completa de gols, cartões e substituições minuto a minuto. O app mostra essa limitação claramente na tela **Ao vivo**.

## Publicação automática

Os workflows do GitHub fazem todo o trabalho:

1. **Atualizar dados do Corinthians** busca e valida os dados a cada hora.
2. Quando os JSONs mudam, o bot cria um commit automático.
3. **Publicar dados no GitHub Pages** publica os arquivos consumidos pelo aplicativo.

Endereço configurado no app:

`https://fabriciobentes.github.io/Corinthians-Torcedor-app_`

Para uma atualização imediata, abra **Actions → Atualizar dados do Corinthians → Run workflow**.

## Executar o atualizador localmente

No PowerShell, dentro de `data-pipeline`:

```powershell
npm.cmd ci
npm.cmd run all
```

Os arquivos finais ficam em `data-pipeline/public`.

## Compilar o Android

Abra `android-app` no Android Studio ou use Java 17+:

```powershell
./gradlew assembleDebug
```

O endereço online padrão já está embutido. Para usar outro endereço:

```powershell
./gradlew assembleDebug -PCORINTHIANS_DATA_URL=https://exemplo.com/dados
```

Ele também pode ser alterado depois na tela **Ajustes** do aplicativo.

## Estrutura

- `android-app/`: aplicativo Kotlin e Jetpack Compose.
- `data-pipeline/`: busca, normalização e validação dos dados.
- `data-pipeline/public/`: JSONs consumidos pelo aplicativo.
- `.github/workflows/`: atualização automática e publicação no GitHub Pages.

## Limitações conhecidas

- A fonte gratuita expõe a rodada ativa do Brasileirão, não a temporada inteira.
- O projeto acumula até oito resultados à medida que as rodadas são atualizadas.
- Outras competições do Corinthians ainda não estão incluídas.
- Este é um projeto pessoal e educacional, sem vínculo com o Sport Club Corinthians Paulista ou com o ge.
