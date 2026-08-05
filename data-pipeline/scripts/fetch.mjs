import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = path.join(ROOT, "public");
const API_URL = "https://v3.football.api-sports.io";
const API_KEY = process.env.FOOTBALL_API_KEY;
const TEAM_ID = Number(process.env.CORINTHIANS_TEAM_ID || 131);
const LEAGUE_ID = Number(process.env.BRASILEIRAO_LEAGUE_ID || 71);
const TIMEZONE = "America/Sao_Paulo";
const now = new Date();
const season = Number(process.env.FOOTBALL_SEASON || now.getUTCFullYear());

if (!API_KEY) {
  console.error("FOOTBALL_API_KEY não definida. Cadastre-a nos Secrets do GitHub.");
  process.exit(1);
}

async function api(endpoint, params) {
  const url = new URL(`${API_URL}/${endpoint}`);
  Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, String(value)));
  const response = await fetch(url, { headers: { "x-apisports-key": API_KEY } });
  if (!response.ok) throw new Error(`${endpoint}: HTTP ${response.status}`);
  const body = await response.json();
  if (body.errors && Object.keys(body.errors).length) {
    throw new Error(`${endpoint}: ${JSON.stringify(body.errors)}`);
  }
  return body.response || [];
}

function atomicWrite(name, data) {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const target = path.join(OUT_DIR, name);
  const temporary = `${target}.tmp`;
  fs.writeFileSync(temporary, `${JSON.stringify(data, null, 2)}\n`, "utf8");
  fs.renameSync(temporary, target);
}

function normalizedFixture(item) {
  return {
    id: item.fixture.id,
    competition: item.league.name,
    competitionLogo: item.league.logo,
    round: item.league.round || "",
    kickoff: item.fixture.date,
    stadium: item.fixture.venue?.name || "Local a definir",
    city: item.fixture.venue?.city || "",
    statusShort: item.fixture.status?.short || "NS",
    statusLong: item.fixture.status?.long || "Agendado",
    minute: item.fixture.status?.elapsed,
    home: {
      id: item.teams.home.id,
      name: item.teams.home.name,
      logo: item.teams.home.logo,
      winner: item.teams.home.winner
    },
    away: {
      id: item.teams.away.id,
      name: item.teams.away.name,
      logo: item.teams.away.logo,
      winner: item.teams.away.winner
    },
    score: { home: item.goals.home, away: item.goals.away }
  };
}

function normalizedEvent(item) {
  return {
    minute: item.time?.elapsed || 0,
    extra: item.time?.extra,
    team: item.team?.name || "",
    type: item.type || "",
    detail: item.detail || "",
    player: item.player?.name || "",
    assist: item.assist?.name || ""
  };
}

async function main() {
  // Uma consulta traz a temporada inteira; o app separa próximos, encerrados e ao vivo.
  const rawFixtures = await api("fixtures", { team: TEAM_ID, season, timezone: TIMEZONE });
  const fixtures = rawFixtures.map(normalizedFixture).sort((a, b) => a.kickoff.localeCompare(b.kickoff));
  const liveStatuses = new Set(["1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT"]);
  const liveFixtures = fixtures.filter((fixture) => liveStatuses.has(fixture.statusShort));

  const eventsByFixture = {};
  for (const fixture of liveFixtures) {
    const events = await api("fixtures/events", { fixture: fixture.id });
    eventsByFixture[String(fixture.id)] = events.map(normalizedEvent);
  }

  const standingsResponse = await api("standings", { league: LEAGUE_ID, season });
  const league = standingsResponse[0]?.league;
  const tables = (league?.standings || []).map((group) => group.map((row) => ({
    position: row.rank,
    teamId: row.team.id,
    teamName: row.team.name,
    teamLogo: row.team.logo,
    points: row.points,
    played: row.all.played,
    wins: row.all.win,
    draws: row.all.draw,
    losses: row.all.lose,
    goalsFor: row.all.goals.for,
    goalsAgainst: row.all.goals.against,
    goalDifference: row.goalsDiff,
    form: row.form || "",
    description: row.description || ""
  })));

  const generatedAt = new Date().toISOString();
  atomicWrite("fixtures.json", { schemaVersion: 1, generatedAt, source: "api-football", teamId: TEAM_ID, season, fixtures });
  atomicWrite("live.json", { schemaVersion: 1, generatedAt, source: "api-football", liveMatches: liveFixtures, eventsByFixture });
  atomicWrite("standings.json", {
    schemaVersion: 1,
    generatedAt,
    source: "api-football",
    competition: { id: league?.id || LEAGUE_ID, name: league?.name || "Brasileirão Série A", logo: league?.logo || "", season },
    tables
  });
  console.log(`Dados atualizados: ${fixtures.length} jogos, ${liveFixtures.length} ao vivo e ${tables[0]?.length || 0} posições.`);
}

<<<<<<< Updated upstream
await main().catch((error) => {
=======
main().catch((error) => {
>>>>>>> Stashed changes
  console.error(`Falha ao atualizar dados: ${error.message}`);
  process.exit(1);
});
