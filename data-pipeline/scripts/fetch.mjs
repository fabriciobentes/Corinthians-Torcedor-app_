import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getCompetition } from "campeonato-brasileiro-api";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = path.join(ROOT, "public");
const SOURCE = "ge-corinthians";
const SCHEDULE_URL = "https://ge.globo.com/futebol/times/corinthians/agenda-de-jogos-do-corinthians/";
const CORINTHIANS_ID = 264;
const CORINTHIANS_NAME = "Corinthians";

function githubError(message) {
  const safeMessage = String(message)
    .replaceAll("%", "%25")
    .replaceAll("\r", "%0D")
    .replaceAll("\n", "%0A");
  console.error(`::error title=Atualização dos dados::${safeMessage}`);
}

function atomicWrite(name, data) {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const target = path.join(OUT_DIR, name);
  const temporary = `${target}.tmp`;
  fs.writeFileSync(temporary, `${JSON.stringify(data, null, 2)}\n`, "utf8");
  fs.renameSync(temporary, target);
}

function extractJsonObject(text, marker, fromIndex = 0) {
  const markerIndex = text.indexOf(marker, fromIndex);
  if (markerIndex < 0) throw new Error(`Marcador de agenda ausente: ${marker}`);
  const start = text.indexOf("{", markerIndex + marker.length);
  if (start < 0) throw new Error("Início dos dados da agenda não encontrado.");

  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let index = start; index < text.length; index += 1) {
    const char = text[index];
    if (inString) {
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === "\"") inString = false;
      continue;
    }
    if (char === "\"") inString = true;
    else if (char === "{") depth += 1;
    else if (char === "}") {
      depth -= 1;
      if (depth === 0) return JSON.parse(text.slice(start, index + 1));
    }
  }
  throw new Error("Fim dos dados da agenda não encontrado.");
}

async function fetchTeamAgenda() {
  const response = await fetch(SCHEDULE_URL, {
    headers: { "user-agent": "Corinthians-Torcedor-app/2.0" }
  });
  if (!response.ok) throw new Error(`Agenda do Corinthians: HTTP ${response.status}`);
  const html = await response.text();
  const anchor = html.indexOf("window.dataSportsSchedule");
  if (anchor < 0) throw new Error("Bloco principal da agenda não encontrado.");
  const schedule = extractJsonObject(html, "scheduleTeam:", anchor);
  if (!schedule?.teamAgenda) throw new Error("Agenda do Corinthians vazia.");
  return schedule.teamAgenda;
}

function normalizedCompetition(name) {
  const aliases = {
    "Taça Conmebol Libertadores": "CONMEBOL Libertadores",
    "Campeonato Brasileiro": "Brasileirão Série A"
  };
  return aliases[name] || name || "Competição a definir";
}

function kickoffWithOffset(match) {
  if (!match.startDate) return "";
  const time = match.startHour || "00:00:00";
  return `${match.startDate}T${time}-03:00`;
}

function normalizedStatus(match) {
  const broadcast = `${match.transmission?.broadcastStatus?.id || ""} ${match.transmission?.broadcastStatus?.label || ""}`.toLowerCase();
  const moment = String(match.moment || "").toUpperCase();
  if (moment === "PAST" || broadcast.includes("encerrad")) return { short: "FT", long: "Encerrado" };
  if (broadcast.includes("adiad")) return { short: "PST", long: "Adiado" };
  if (broadcast.includes("cancel")) return { short: "CANC", long: "Cancelado" };
  if (moment === "NOW" || broadcast.includes("andamento") || broadcast.includes("intervalo")) return { short: "1H", long: "Ao vivo" };
  return { short: "NS", long: "Agendado" };
}

function winnerFlags(match, status) {
  if (status.short !== "FT") return { home: null, away: null };
  if (match.result === "FIRST_TEAM") return { home: true, away: false };
  if (match.result === "SECOND_TEAM") return { home: false, away: true };
  if (match.result === "DRAW") return { home: false, away: false };

  const home = match.scoreboard?.home;
  const away = match.scoreboard?.away;
  if (home == null || away == null || home === away) return { home: false, away: false };
  return { home: home > away, away: away > home };
}

function normalizedFixture(event) {
  const match = event?.match;
  if (!match) return null;
  const status = normalizedStatus(match);
  const winners = winnerFlags(match, status);
  const competition = normalizedCompetition(match.phase?.championshipEdition?.championship?.name);
  const phase = match.phase?.name || "";
  const round = match.round ? `${match.round}ª rodada` : phase;
  return {
    id: match.id,
    competition,
    competitionLogo: "",
    round,
    kickoff: kickoffWithOffset(match),
    stadium: match.location?.popularName || "Local a definir",
    city: "",
    statusShort: status.short,
    statusLong: status.long,
    minute: null,
    home: {
      id: match.firstContestant?.id || 0,
      name: match.firstContestant?.popularName || match.firstContestant?.name || "A definir",
      logo: match.firstContestant?.badgeSvg || match.firstContestant?.badgePng || "",
      winner: winners.home
    },
    away: {
      id: match.secondContestant?.id || 0,
      name: match.secondContestant?.popularName || match.secondContestant?.name || "A definir",
      logo: match.secondContestant?.badgeSvg || match.secondContestant?.badgePng || "",
      winner: winners.away
    },
    score: {
      home: match.scoreboard?.home ?? null,
      away: match.scoreboard?.away ?? null,
      penalties: match.scoreboard?.penalty ?? null
    },
    detailsUrl: match.transmission?.url || "",
    broadcasters: (match.liveWatchSources || []).map((source) => source.name).filter(Boolean)
  };
}

function normalizedStanding(row) {
  return {
    position: row.position,
    teamId: row.team?.id || 0,
    teamName: row.team?.name || "",
    teamLogo: row.team?.badge || "",
    points: row.points || 0,
    played: row.matches || 0,
    wins: row.wins || 0,
    draws: row.draws || 0,
    losses: row.losses || 0,
    goalsFor: row.goalsFor || 0,
    goalsAgainst: row.goalsAgainst || 0,
    goalDifference: row.goalDifference || 0,
    form: Array.isArray(row.recentForm) ? row.recentForm.join("") : "",
    description: row.legend?.name || ""
  };
}

function outcomeFor(fixture) {
  const isHome = fixture.home.id === CORINTHIANS_ID || fixture.home.name.toLowerCase().includes("corinthians");
  const corinthians = isHome ? fixture.home : fixture.away;
  const opponent = isHome ? fixture.away : fixture.home;
  if (corinthians.winner === true) return "W";
  if (opponent.winner === true) return "L";
  return "D";
}

function buildStats(finishedFixtures) {
  const recentMatches = finishedFixtures.slice(-10);
  const outcomes = recentMatches.map(outcomeFor);
  const stats = recentMatches.reduce((summary, fixture) => {
    const isHome = fixture.home.id === CORINTHIANS_ID || fixture.home.name.toLowerCase().includes("corinthians");
    const goalsFor = (isHome ? fixture.score.home : fixture.score.away) ?? 0;
    const goalsAgainst = (isHome ? fixture.score.away : fixture.score.home) ?? 0;
    const outcome = outcomeFor(fixture);
    summary.goalsFor += goalsFor;
    summary.goalsAgainst += goalsAgainst;
    summary.cleanSheets += goalsAgainst === 0 ? 1 : 0;
    summary.scoringGames += goalsFor > 0 ? 1 : 0;
    if (outcome === "W") summary.wins += 1;
    else if (outcome === "D") summary.draws += 1;
    else summary.losses += 1;
    return summary;
  }, { wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0, cleanSheets: 0, scoringGames: 0 });

  const competitionsMap = new Map();
  recentMatches.forEach((fixture) => {
    const item = competitionsMap.get(fixture.competition) || { name: fixture.competition, matches: 0, wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0 };
    const isHome = fixture.home.id === CORINTHIANS_ID || fixture.home.name.toLowerCase().includes("corinthians");
    const outcome = outcomeFor(fixture);
    item.matches += 1;
    item.goalsFor += (isHome ? fixture.score.home : fixture.score.away) ?? 0;
    item.goalsAgainst += (isHome ? fixture.score.away : fixture.score.home) ?? 0;
    if (outcome === "W") item.wins += 1;
    else if (outcome === "D") item.draws += 1;
    else item.losses += 1;
    competitionsMap.set(fixture.competition, item);
  });

  const lastOutcome = outcomes.at(-1);
  let streakCount = 0;
  for (let index = outcomes.length - 1; index >= 0 && outcomes[index] === lastOutcome; index -= 1) streakCount += 1;
  const matches = recentMatches.length;
  return {
    summary: {
      matches,
      ...stats,
      goalDifference: stats.goalsFor - stats.goalsAgainst,
      pointsPercentage: matches ? Math.round(((stats.wins * 3 + stats.draws) / (matches * 3)) * 100) : 0,
      averageGoalsFor: matches ? Number((stats.goalsFor / matches).toFixed(1)) : 0,
      averageGoalsAgainst: matches ? Number((stats.goalsAgainst / matches).toFixed(1)) : 0,
      currentStreak: lastOutcome ? `${streakCount}${lastOutcome}` : "-"
    },
    form: outcomes,
    competitions: [...competitionsMap.values()].sort((a, b) => b.matches - a.matches || a.name.localeCompare(b.name)),
    recentMatches
  };
}

async function main() {
  const [agenda, standingsData] = await Promise.all([
    fetchTeamAgenda(),
    getCompetition("a", { headers: { "user-agent": "Corinthians-Torcedor-app/2.0" } })
  ]);

  const pastFixtures = (agenda.past || []).map(normalizedFixture).filter(Boolean);
  const liveFixtures = (agenda.now || []).map(normalizedFixture).filter(Boolean);
  const futureFixtures = (agenda.future || []).map(normalizedFixture).filter(Boolean);
  const fixtures = [...pastFixtures.slice(-20), ...liveFixtures, ...futureFixtures.slice(0, 30)]
    .sort((a, b) => a.kickoff.localeCompare(b.kickoff));
  const tables = (standingsData.tables || []).map((table) => (table.entries || []).map(normalizedStanding));
  const eventsByFixture = Object.fromEntries(liveFixtures.map((fixture) => [String(fixture.id), []]));
  const generatedAt = new Date().toISOString();
  const season = Number(agenda.future?.[0]?.match?.startDate?.slice(0, 4) || new Date().getUTCFullYear());
  const stats = buildStats(pastFixtures);
  const competitions = [...new Set(fixtures.map((fixture) => fixture.competition))].sort();

  if (!tables[0]?.some((row) => row.teamName.toLowerCase().includes(CORINTHIANS_NAME.toLowerCase()))) {
    throw new Error("A classificação recebida não contém o Corinthians.");
  }
  if (!fixtures.length || stats.summary.matches < 10) {
    throw new Error("A agenda não trouxe jogos suficientes do Corinthians.");
  }

  atomicWrite("fixtures.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    teamId: CORINTHIANS_ID,
    season,
    coverage: "Todas as competições disponíveis na agenda do Corinthians",
    competitions,
    fixtures
  });
  atomicWrite("live.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    coverage: "Placar e situação da partida; lances minuto a minuto dependem da cobertura da fonte",
    liveMatches: liveFixtures,
    eventsByFixture
  });
  atomicWrite("standings.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    competition: { id: 2013, name: standingsData.competition?.name || "Brasileirão Série A", logo: "", season },
    tables
  });
  atomicWrite("stats.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    teamId: CORINTHIANS_ID,
    window: stats.summary.matches,
    ...stats
  });

  console.log(`Dados atualizados: ${fixtures.length} jogos em ${competitions.length} competição(ões), ${liveFixtures.length} ao vivo, ${tables[0]?.length || 0} posições e estatísticas de ${stats.summary.matches} jogos.`);
}

await main().catch((error) => {
  githubError(`Falha ao atualizar dados: ${error.message}`);
  process.exit(1);
});
