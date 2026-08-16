import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = path.join(ROOT, "public");
const SOURCE = "ge-corinthians";
const SCHEDULE_URL = "https://ge.globo.com/futebol/times/corinthians/agenda-de-jogos-do-corinthians/";
const CORINTHIANS_ID = 264;
const CORINTHIANS_NAME = "Corinthians";
const USER_AGENT = "Corinthians-Torcedor-app/3.0";
const FOUR_HOURS = 4 * 60 * 60 * 1000;

const COMPETITION_PAGES = {
  "Brasileirão Série A": "https://ge.globo.com/futebol/brasileirao-serie-a/",
  "Copa do Brasil": "https://ge.globo.com/futebol/copa-do-brasil/",
  "CONMEBOL Libertadores": "https://ge.globo.com/futebol/libertadores/",
  "CONMEBOL Sul-Americana": "https://ge.globo.com/futebol/sul-americana/",
  "Campeonato Paulista": "https://ge.globo.com/sp/futebol/campeonato-paulista/",
  "Supercopa do Brasil": "https://ge.globo.com/futebol/supercopa-do-brasil/"
};

const STADIUM_CITIES = {
  "neo quimica arena": "São Paulo",
  "arena corinthians": "São Paulo",
  "pacaembu": "São Paulo",
  "morumbis": "São Paulo",
  "morumbi": "São Paulo",
  "allianz parque": "São Paulo",
  "vila belmiro": "Santos",
  "maracana": "Rio de Janeiro",
  "nilton santos": "Rio de Janeiro",
  "sao januario": "Rio de Janeiro",
  "mineirao": "Belo Horizonte",
  "arena mrv": "Belo Horizonte",
  "beira-rio": "Porto Alegre",
  "arena do gremio": "Porto Alegre",
  "couto pereira": "Curitiba",
  "arena da baixada": "Curitiba",
  "ligga arena": "Curitiba",
  "castelao": "Fortaleza",
  "arena fonte nova": "Salvador",
  "ilha do retiro": "Recife",
  "arena pernambuco": "Recife",
  "serra dourada": "Goiânia",
  "arena pantanal": "Cuiabá",
  "arena conda": "Chapecó",
  "mané garrincha": "Brasília",
  "mane garrincha": "Brasília",
  "defensores del chaco": "Assunção",
  "la bombonera": "Buenos Aires",
  "monumental de nunez": "Buenos Aires",
  "ciudad de vicente lopez": "Vicente López"
};

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
  if (fs.existsSync(target)) {
    try {
      const current = JSON.parse(fs.readFileSync(target, "utf8"));
      const currentComparable = { ...current, generatedAt: "" };
      const nextComparable = { ...data, generatedAt: "" };
      if (JSON.stringify(currentComparable) === JSON.stringify(nextComparable)) return false;
    } catch {
      console.warn(`Arquivo ${name} inválido; ele será regenerado por completo.`);
    }
  }
  const temporary = `${target}.tmp`;
  fs.writeFileSync(temporary, `${JSON.stringify(data, null, 2)}\n`, "utf8");
  fs.renameSync(temporary, target);
  return true;
}

async function fetchText(url) {
  const response = await fetch(url, { headers: { "user-agent": USER_AGENT } });
  if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`);
  return response.text();
}

function extractJsonValue(text, marker, fromIndex = 0) {
  const markerIndex = text.indexOf(marker, fromIndex);
  if (markerIndex < 0) throw new Error(`Marcador ausente: ${marker}`);
  const objectStart = text.indexOf("{", markerIndex + marker.length);
  const arrayStart = text.indexOf("[", markerIndex + marker.length);
  const start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
  if (start < 0) throw new Error(`Início dos dados ausente: ${marker}`);
  const opening = text[start];
  const closing = opening === "{" ? "}" : "]";
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
    else if (char === opening) depth += 1;
    else if (char === closing) {
      depth -= 1;
      if (depth === 0) return JSON.parse(text.slice(start, index + 1));
    }
  }
  throw new Error(`Fim dos dados ausente: ${marker}`);
}

async function fetchTeamAgenda() {
  const html = await fetchText(SCHEDULE_URL);
  const anchor = html.indexOf("window.dataSportsSchedule");
  if (anchor < 0) throw new Error("Bloco principal da agenda não encontrado.");
  const schedule = extractJsonValue(html, "scheduleTeam:", anchor);
  if (!schedule?.teamAgenda) throw new Error("Agenda do Corinthians vazia.");
  return schedule.teamAgenda;
}

function normalizedCompetition(name) {
  const aliases = {
    "Taça Conmebol Libertadores": "CONMEBOL Libertadores",
    "Copa Sul-Americana": "CONMEBOL Sul-Americana",
    "Taça Conmebol Sul-Americana": "CONMEBOL Sul-Americana",
    "Campeonato Brasileiro": "Brasileirão Série A",
    "Campeonato Paulista Série A1": "Campeonato Paulista",
    "Supercopa Rei": "Supercopa do Brasil"
  };
  return aliases[name] || name || "Competição a definir";
}

function simplify(value) {
  return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

function cityFromLocation(location) {
  const region = String(location?.region || "");
  const parts = region.split(",").map((part) => part.trim()).filter(Boolean);
  if (parts.length >= 2) return parts[1];
  const stadium = simplify(location?.popularName || location?.name);
  const known = Object.entries(STADIUM_CITIES).find(([key]) => stadium.includes(key));
  return known?.[1] || "";
}

function kickoffWithOffset(match) {
  if (!match.startDate) return "";
  return `${match.startDate}T${match.startHour || "00:00:00"}-03:00`;
}

function normalizedStatus(match) {
  const broadcast = `${match.transmission?.broadcastStatus?.id || ""} ${match.transmission?.broadcastStatus?.label || ""}`.toLowerCase();
  const moment = String(match.moment || "").toUpperCase();
  if (moment === "PAST" || broadcast.includes("encerrad")) return { short: "FT", long: "Encerrado" };
  if (broadcast.includes("adiad")) return { short: "PST", long: "Adiado" };
  if (broadcast.includes("cancel")) return { short: "CANC", long: "Cancelado" };
  if (moment === "NOW" || broadcast.includes("andamento") || broadcast.includes("intervalo")) return { short: "LIVE", long: "Ao vivo" };
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
  const broadcasters = (match.liveWatchSources || [])
    .map((source) => source.name)
    .filter((name) => name && simplify(name) !== "cartola");
  return {
    id: match.id,
    competition,
    competitionLogo: match.phase?.championshipEdition?.championship?.logoSvg || "",
    round,
    kickoff: kickoffWithOffset(match),
    stadium: match.location?.popularName || "Local a definir",
    city: cityFromLocation(match.location),
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
    detailsUrl: match.transmission?.url || event.editorialData?.url || "",
    broadcasters
  };
}

function statusFromTime(fixture, now = Date.now()) {
  if (!fixture.kickoff || fixture.kickoff.includes("T00:00:00")) return fixture;
  if (!["NS", "TBD"].includes(fixture.statusShort)) return fixture;
  const start = Date.parse(fixture.kickoff);
  if (Number.isNaN(start)) return fixture;
  if (now >= start && now < start + FOUR_HOURS) {
    return { ...fixture, statusShort: "LIVE", statusLong: "Partida em andamento" };
  }
  if (now >= start + FOUR_HOURS) {
    return { ...fixture, statusShort: "AWAITING_RESULT", statusLong: "Aguardando resultado" };
  }
  return fixture;
}

function bodyText(play) {
  return (play.body?.blocks || []).map((block) => block.text).filter(Boolean).join("\n")
    || play.title
    || play.playType?.label
    || "Lance da partida";
}

function normalizedEvent(play) {
  const description = bodyText(play);
  const playType = String(play.playType?.id || "NORMAL").toUpperCase();
  const plain = simplify(description);
  let type = playType;
  if (playType === "CARD") type = plain.includes("vermelh") ? "RED_CARD" : "YELLOW_CARD";
  else if (playType === "IMPORTANT" || playType === "NORMAL") {
    if (plain.includes("chute") || plain.includes("finaliza") || plain.includes("cabec")) type = "SHOT";
    else if (plain.includes("falta")) type = "FOUL";
    else if (plain.includes("escanteio")) type = "CORNER";
    else if (plain.includes("impedimento")) type = "OFFSIDE";
    else if (plain.includes("defesa")) type = "SAVE";
    else if (plain.includes("penalti")) type = "PENALTY";
    else type = "OTHER";
  }
  const clock = String(play.moment || "");
  return {
    id: String(play.id || `${play.period?.abbreviation || ""}-${clock}-${description}`),
    minute: Number.parseInt(clock.split(":")[0], 10) || 0,
    clock,
    period: play.period?.abbreviation || play.period?.label || "",
    team: play.details?.team?.popularName || play.details?.team?.abbreviation || play.details?.team?.name || "",
    type,
    description,
    createdAt: play.createdAt || ""
  };
}

function cleanPlayer(player) {
  return {
    name: player?.name || "",
    popularName: player?.popularName || player?.nickName || player?.name || "",
    shirtNumber: String(player?.shirtNumber || ""),
    position: {
      description: player?.position?.description || "",
      initials: player?.position?.initials || "",
      playAt: player?.position?.playAt || ""
    }
  };
}

function cleanSquad(squad) {
  if (!squad) return null;
  return {
    formation: squad.formation || "",
    coach: {
      name: squad.coach?.name || "",
      popularName: squad.coach?.popularName || squad.coach?.name || ""
    },
    lineUp: (squad.lineUp || []).map(cleanPlayer),
    bench: (squad.bench || []).map(cleanPlayer)
  };
}

const detailsCache = new Map();

async function fetchMatchDetails(fixture) {
  if (!fixture?.detailsUrl?.startsWith("http")) return { fixture, events: [], squads: null, statistics: null };
  if (detailsCache.has(fixture.detailsUrl)) return detailsCache.get(fixture.detailsUrl);
  const promise = (async () => {
    const html = await fetchText(fixture.detailsUrl);
    const anchor = html.indexOf("window.trv2");
    if (anchor < 0) return { fixture, events: [], squads: null, statistics: null };
    const match = extractJsonValue(html, '"match":', anchor);
    let plays = [];
    let statistics = null;
    try { plays = extractJsonValue(html, "plays: Array.from(", anchor); } catch {}
    try { statistics = extractJsonValue(html, "statistics:", anchor); } catch {}
    const events = plays
      .filter((play) => !["POSTGAME", "POSTGAME_HIGHLIGHT", "SUMMARY_AUTOMATIC", "STANDOUT_PLAYER"].includes(play.playType?.id))
      .map(normalizedEvent)
      .sort((a, b) => String(a.createdAt).localeCompare(String(b.createdAt)));
    const currentTime = html.slice(anchor, anchor + 1500).match(/"currentTime":"([^"]+)"/)?.[1] || "";
    const minute = Number.parseInt(currentTime.split(":")[0], 10) || fixture.minute || 0;
    const squads = match.squads ? {
      homeTeam: cleanSquad(match.squads.homeTeam),
      awayTeam: cleanSquad(match.squads.awayTeam)
    } : null;
    const hasLineup = Boolean(squads?.homeTeam?.lineUp?.length && squads?.awayTeam?.lineUp?.length);
    const enriched = {
      ...fixture,
      stadium: match.location?.popularName || fixture.stadium,
      city: cityFromLocation(match.location) || fixture.city,
      minute,
      home: {
        ...fixture.home,
        name: match.homeTeam?.popularName || fixture.home.name
      },
      away: {
        ...fixture.away,
        name: match.awayTeam?.popularName || fixture.away.name
      },
      score: {
        home: match.scoreboard?.home ?? match.detailedScoreboard?.firstParticipantScore ?? fixture.score.home,
        away: match.scoreboard?.away ?? match.detailedScoreboard?.secondParticipantScore ?? fixture.score.away,
        penalties: match.scoreboard?.penalty ?? fixture.score.penalties
      },
      squads,
      statistics,
      lineupStatus: hasLineup ? "Escalação oficial" : ""
    };
    return { fixture: enriched, events, squads, statistics };
  })().catch((error) => {
    console.warn(`Detalhes indisponíveis para ${fixture.id}: ${error.message}`);
    return { fixture, events: [], squads: null, statistics: null };
  });
  detailsCache.set(fixture.detailsUrl, promise);
  return promise;
}

function normalizedStanding(row) {
  return {
    position: row.ordem ?? row.position ?? 0,
    teamId: row.equipe_id ?? row.team?.id ?? 0,
    teamName: row.nome_popular ?? row.team?.name ?? "",
    teamLogo: row.escudo ?? row.team?.badge ?? "",
    points: row.pontos ?? row.points ?? 0,
    played: row.jogos ?? row.matches ?? 0,
    wins: row.vitorias ?? row.wins ?? 0,
    draws: row.empates ?? row.draws ?? 0,
    losses: row.derrotas ?? row.losses ?? 0,
    goalsFor: row.gols_pro ?? row.goalsFor ?? 0,
    goalsAgainst: row.gols_contra ?? row.goalsAgainst ?? 0,
    goalDifference: row.saldo_gols ?? row.goalDifference ?? 0,
    form: (row.ultimos_jogos || row.recentForm || []).join("").toUpperCase(),
    description: row.faixa_classificacao?.nome || row.legend?.name || ""
  };
}

function normalizedBracketGame(game) {
  return {
    id: game.id || 0,
    date: [game.data_realizacao, game.hora_realizacao].filter(Boolean).join(" "),
    home: game.equipes?.mandante?.nome_popular || "A definir",
    away: game.equipes?.visitante?.nome_popular || "A definir",
    scoreHome: game.placar_oficial_mandante ?? null,
    scoreAway: game.placar_oficial_visitante ?? null,
    penaltyHome: game.placar_penaltis_mandante ?? null,
    penaltyAway: game.placar_penaltis_visitante ?? null
  };
}

function titleFromSlug(slug) {
  return String(slug || "Fase atual")
    .replace(/-20\d\d$/, "")
    .split("-")
    .map((part) => part ? part[0].toUpperCase() + part.slice(1) : "")
    .join(" ");
}

function derivedBrackets(fixtures) {
  const knockout = fixtures.filter((fixture) => fixture.round && !fixture.round.includes("rodada"));
  if (!knockout.length) return [];
  const grouped = Map.groupBy(knockout, (fixture) => fixture.round);
  return [...grouped.entries()].map(([round, games]) => ({
    name: round,
    ties: games.map((game) => ({
      name: `${game.home} x ${game.away}`,
      games: [{
        id: game.id,
        date: game.kickoff,
        home: game.home.name,
        away: game.away.name,
        scoreHome: game.score.home,
        scoreAway: game.score.away,
        penaltyHome: null,
        penaltyAway: null
      }]
    }))
  }));
}

async function fetchCompetitionTable(name, url, fixtures) {
  let data = null;
  if (url) {
    try {
      const html = await fetchText(url);
      const marker = "const classificacao = ";
      const anchor = html.lastIndexOf(marker);
      if (anchor >= 0) data = extractJsonValue(html, marker, anchor);
    } catch {}
  }
  const classification = Array.isArray(data?.classificacao) ? data.classificacao : [];
  const groups = classification.length ? [{
    name: data?.fase?.slug ? titleFromSlug(data.fase.slug) : "Classificação",
    entries: classification.map(normalizedStanding)
  }] : [];
  const ties = (data?.secao || []).flatMap((section) =>
    (section.chave || []).map((tie) => ({
      name: tie.nome || "Confronto",
      games: (tie.jogos || []).map(normalizedBracketGame)
    }))
  );
  const brackets = ties.length ? [{
    name: titleFromSlug(data?.fase?.slug),
    ties
  }] : derivedBrackets(fixtures);
  return {
    name,
    phase: titleFromSlug(data?.fase?.slug || fixtures.at(-1)?.round || "Fase atual"),
    kind: groups.length ? "league" : brackets.length ? "bracket" : "fixtures",
    groups,
    brackets
  };
}

function pageFromFixture(fixture) {
  if (COMPETITION_PAGES[fixture.competition]) return COMPETITION_PAGES[fixture.competition];
  const marker = "/jogo/";
  const index = fixture.detailsUrl?.indexOf(marker) ?? -1;
  return index > 0 ? `${fixture.detailsUrl.slice(0, index)}/` : null;
}

function outcomeFor(fixture) {
  const isHome = fixture.home.id === CORINTHIANS_ID || simplify(fixture.home.name).includes("corinthians");
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
    const isHome = fixture.home.id === CORINTHIANS_ID || simplify(fixture.home.name).includes("corinthians");
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
    const isHome = fixture.home.id === CORINTHIANS_ID || simplify(fixture.home.name).includes("corinthians");
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
  const agenda = await fetchTeamAgenda();
  const now = Date.now();
  const pastFixtures = (agenda.past || []).map(normalizedFixture).filter(Boolean);
  const sourceLiveFixtures = (agenda.now || []).map(normalizedFixture).filter(Boolean);
  const futureFixtures = (agenda.future || []).map(normalizedFixture).filter(Boolean);
  const season = Number(futureFixtures[0]?.kickoff?.slice(0, 4) || new Date().getUTCFullYear());
  const seasonPast = pastFixtures.filter((fixture) => fixture.kickoff.startsWith(`${season}-`));
  const allRaw = [...(seasonPast.length ? seasonPast : pastFixtures.slice(-50)), ...sourceLiveFixtures, ...futureFixtures];
  const fixtures = allRaw.map((fixture) => statusFromTime(fixture, now))
    .sort((a, b) => a.kickoff.localeCompare(b.kickoff));
  const liveFixtures = fixtures.filter((fixture) => fixture.statusShort === "LIVE");
  const nextSoon = fixtures.find((fixture) => {
    const start = Date.parse(fixture.kickoff);
    return fixture.statusShort === "NS" && start - now <= 6 * 60 * 60 * 1000 && start - now >= -FOUR_HOURS;
  });
  const featuredBase = liveFixtures[0] || nextSoon || null;
  const featured = featuredBase ? await fetchMatchDetails(featuredBase) : null;

  const recentMatches = pastFixtures.slice(-10);
  const detailResults = await Promise.all(recentMatches.map(fetchMatchDetails));
  const detailsById = new Map(detailResults.map((detail) => [detail.fixture.id, detail]));
  const enrichedFixtures = fixtures.map((fixture) => detailsById.get(fixture.id)?.fixture || (featured?.fixture.id === fixture.id ? featured.fixture : fixture));

  const competitionNames = [...new Set(enrichedFixtures.map((fixture) => fixture.competition))].sort();
  const competitions = await Promise.all(competitionNames.map((name) => {
    const related = enrichedFixtures.filter((fixture) => fixture.competition === name);
    const url = related.map(pageFromFixture).find(Boolean) || COMPETITION_PAGES[name] || null;
    return fetchCompetitionTable(name, url, related);
  }));

  const stats = buildStats(pastFixtures);
  const matchDetails = detailResults.map((detail) => ({
    matchId: detail.fixture.id,
    statistics: detail.statistics,
    events: detail.events
  }));
  const generatedAt = new Date().toISOString();
  const eventsByFixture = Object.fromEntries(
    (featured ? [[String(featured.fixture.id), featured.events]] : [])
  );

  if (!enrichedFixtures.length || stats.summary.matches === 0) {
    throw new Error("A agenda não trouxe jogos suficientes do Corinthians.");
  }

  atomicWrite("fixtures.json", {
    schemaVersion: 2,
    generatedAt,
    source: SOURCE,
    team: { id: CORINTHIANS_ID, name: CORINTHIANS_NAME },
    season,
    competitions: competitionNames,
    fixtures: enrichedFixtures
  });
  atomicWrite("live.json", {
    schemaVersion: 2,
    generatedAt,
    source: SOURCE,
    coverage: "Placar, escalações, estatísticas e todos os lances publicados pela cobertura da partida.",
    liveMatches: liveFixtures,
    featuredMatch: featured?.fixture || featuredBase,
    eventsByFixture
  });
  atomicWrite("standings.json", {
    schemaVersion: 2,
    generatedAt,
    source: SOURCE,
    competitions
  });
  atomicWrite("stats.json", {
    schemaVersion: 2,
    generatedAt,
    source: SOURCE,
    teamId: CORINTHIANS_ID,
    window: stats.summary.matches,
    ...stats,
    matchDetails
  });

  console.log(`Dados atualizados: ${enrichedFixtures.length} jogos, ${competitionNames.length} competições, ${liveFixtures.length} ao vivo, ${competitions.length} tabelas/chaveamentos e ${matchDetails.length} partidas detalhadas.`);
}

await main().catch((error) => {
  githubError(`Falha ao atualizar dados: ${error.message}`);
  process.exit(1);
});
