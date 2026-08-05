# Corinthians Live

Aplicativo Android pessoal para acompanhar a agenda, os resultados e o desempenho do Corinthians.

## O que funciona

- Agenda do Corinthians em todas as competições publicadas, incluindo Brasileirão, Copa do Brasil, Libertadores, Paulista e amistosos.
- Histórico dos resultados em todas essas competições.
- Placar e situação da partida quando o Corinthians estiver em campo.
- Classificação completa do Brasileirão, com alto contraste e destaque para o Corinthians.
- Estatísticas dos últimos 10 jogos, forma recente e desempenho por competição.
- Notificação automática nos dias de jogo, com botão de teste na tela **Ajustes**.
- Ícone oficial do Corinthians preparado para os formatos de ícone do Android.
- Atualização automática pelo GitHub a cada hora.
- Funcionamento offline com a última cópia incluída no aplicativo.
- Endereço dos dados já configurado para este repositório.
- Modo escuro e atualização manual em todas as telas.

## Dados reais sem chave

O projeto combina a agenda pública do Corinthians no ge com a biblioteca gratuita `campeonato-brasileiro-api` para a classificação do Brasileirão. Não é necessário cadastrar chave nem pagar um plano.

O secret antigo `FOOTBALL_API_KEY` pode continuar salvo no GitHub, mas não é mais utilizado e pode ser excluído em **Settings → Secrets and variables → Actions**.

A fonte gratuita informa agenda, placar e situação da partida, mas nem sempre fornece a lista completa de gols, cartões e substituições minuto a minuto. O app mostra essa limitação claramente na tela **Ao vivo**.

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

- A tabela é exclusiva do Brasileirão; as demais competições aparecem na agenda, nos resultados e nas estatísticas.
- Os avisos dependem da permissão de notificações do Android e podem sofrer pequenos atrasos por causa da economia de bateria do aparelho.
- Eventos detalhados minuto a minuto dependem do que a fonte pública disponibilizar.
- Este é um projeto pessoal e educacional, sem vínculo com o Sport Club Corinthians Paulista ou com o ge.
