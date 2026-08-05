import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getCompetition } from "campeonato-brasileiro-api";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = path.join(ROOT, "public");
const SOURCE = "ge-brasileirao";
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

function readPreviousFinishedFixtures() {
  const target = path.join(OUT_DIR, "fixtures.json");
  if (!fs.existsSync(target)) return [];

  try {
    const previous = JSON.parse(fs.readFileSync(target, "utf8"));
    if (previous.source !== SOURCE || !Array.isArray(previous.fixtures)) return [];
    return previous.fixtures.filter((fixture) => ["FT", "AET", "PEN", "WO"].includes(fixture.statusShort));
  } catch {
    return [];
  }
}

function isCorinthiansMatch(match) {
  return match.homeTeam?.id === CORINTHIANS_ID
    || match.awayTeam?.id === CORINTHIANS_ID
    || match.homeTeam?.name?.toLowerCase().includes("corinthians")
    || match.awayTeam?.name?.toLowerCase().includes("corinthians");
}

function kickoffWithOffset(match) {
  const raw = match.dateTime || [match.date, match.time].filter(Boolean).join("T");
  if (!raw) return "";
  if (/Z$|[+-]\d{2}:\d{2}$/.test(raw)) return raw;
  return `${raw.length === 16 ? `${raw}:00` : raw}-03:00`;
}

function normalizedStatus(match) {
  const value = `${match.status || ""} ${match.statusCode || ""}`.toLowerCase();
  if (value.includes("finished") || value.includes("encerrada")) return { short: "FT", long: "Encerrado" };
  if (value.includes("postponed") || value.includes("adiad")) return { short: "PST", long: "Adiado" };
  if (value.includes("cancel")) return { short: "CANC", long: "Cancelado" };
  if (match.started || value.includes("live") || value.includes("andamento")) return { short: "1H", long: "Ao vivo" };
  return { short: "NS", long: "Agendado" };
}

function winnerFor(side, match, status) {
  if (status.short !== "FT" || match.score?.home == null || match.score?.away == null) return null;
  if (match.score.home === match.score.away) return false;
  return side === "home" ? match.score.home > match.score.away : match.score.away > match.score.home;
}

function normalizedFixture(match, competitionName) {
  const status = normalizedStatus(match);
  return {
    id: match.id,
    competition: competitionName,
    competitionLogo: "",
    round: match.round ? `${match.round}ª rodada` : "",
    kickoff: kickoffWithOffset(match),
    stadium: match.venue || "Local a definir",
    city: "",
    statusShort: status.short,
    statusLong: status.long,
    minute: null,
    home: {
      id: match.homeTeam?.id || 0,
      name: match.homeTeam?.name || "A definir",
      logo: match.homeTeam?.badge || "",
      winner: winnerFor("home", match, status)
    },
    away: {
      id: match.awayTeam?.id || 0,
      name: match.awayTeam?.name || "A definir",
      logo: match.awayTeam?.badge || "",
      winner: winnerFor("away", match, status)
    },
    score: { home: match.score?.home ?? null, away: match.score?.away ?? null }
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

function mergeFixtures(currentFixtures) {
  const byId = new Map(readPreviousFinishedFixtures().map((fixture) => [String(fixture.id), fixture]));
  currentFixtures.forEach((fixture) => byId.set(String(fixture.id), fixture));

  const all = [...byId.values()].sort((a, b) => a.kickoff.localeCompare(b.kickoff));
  const finished = all.filter((fixture) => ["FT", "AET", "PEN", "WO"].includes(fixture.statusShort)).slice(-8);
  const active = all.filter((fixture) => !["FT", "AET", "PEN", "WO"].includes(fixture.statusShort));
  return [...finished, ...active].sort((a, b) => a.kickoff.localeCompare(b.kickoff));
}

async function main() {
  const data = await getCompetition("a", {
    headers: { "user-agent": "Corinthians-Torcedor-app/1.0" }
  });

  const competitionName = data.competition?.name || "Brasileirão Série A";
  const currentFixtures = (data.matches || [])
    .filter(isCorinthiansMatch)
    .map((match) => normalizedFixture(match, competitionName));
  const fixtures = mergeFixtures(currentFixtures);
  const liveFixtures = currentFixtures.filter((fixture) => fixture.statusShort === "1H");
  const eventsByFixture = Object.fromEntries(liveFixtures.map((fixture) => [String(fixture.id), []]));
  const tables = (data.tables || []).map((table) => (table.entries || []).map(normalizedStanding));
  const generatedAt = new Date().toISOString();
  const season = data.competition?.season || new Date().getUTCFullYear();

  if (!tables[0]?.some((row) => row.teamName.toLowerCase().includes(CORINTHIANS_NAME.toLowerCase()))) {
    throw new Error("A classificação recebida não contém o Corinthians.");
  }
  if (!currentFixtures.length) {
    throw new Error("A rodada atual recebida não contém um jogo do Corinthians.");
  }

  atomicWrite("fixtures.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    teamId: CORINTHIANS_ID,
    season,
    coverage: "Brasileirão Série A; rodada atual e histórico acumulado pelo projeto",
    fixtures
  });
  atomicWrite("live.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    coverage: "Placar e situação da partida; lances minuto a minuto indisponíveis na fonte gratuita",
    liveMatches: liveFixtures,
    eventsByFixture
  });
  atomicWrite("standings.json", {
    schemaVersion: 1,
    generatedAt,
    source: SOURCE,
    competition: { id: 2013, name: competitionName, logo: "", season },
    tables
  });

  console.log(`Dados atualizados: ${fixtures.length} jogo(s) do Corinthians, ${liveFixtures.length} ao vivo e ${tables[0]?.length || 0} posições.`);
}

await main().catch((error) => {
  githubError(`Falha ao atualizar dados: ${error.message}`);
  process.exit(1);
});
