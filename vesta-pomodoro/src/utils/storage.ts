import type { SessionModel } from '../Models/SessionModel';
import type {
  ActiveSessionModel,
  SettingsModel,
  StreakModel,
} from '../Models/TaskStateModel';
import type { TaskModel } from '../Models/TaskModel';
import type { StopwatchModel } from '../Models/StopwatchModel';

export const STORAGE_KEYS = {
  tasks: 'vesta_tasks',
  sessions: 'vesta_sessions',
  streak: 'vesta_streak',
  settings: 'vesta_settings',
  activeSession: 'vesta_active_session',
  stopwatch: 'vesta_stopwatch',
} as const;

function readStorage(key: string): unknown {
  try {
    const value = localStorage.getItem(key);
    return value ? (JSON.parse(value) as unknown) : null;
  } catch {
    return null;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value);
}

function isTask(value: unknown): value is TaskModel {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.text === 'string' &&
    typeof value.completed === 'boolean' &&
    isFiniteNumber(value.sessionsCount) &&
    typeof value.createdAt === 'string' &&
    typeof value.updatedAt === 'string'
  );
}

function isSession(value: unknown): value is SessionModel {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.date === 'string' &&
    isFiniteNumber(value.durationMinutes) &&
    (typeof value.taskId === 'string' || value.taskId === null) &&
    (value.status === 'completed' || value.status === 'abandoned') &&
    typeof value.startedAt === 'string' &&
    typeof value.endedAt === 'string'
  );
}

export function loadTasks() {
  const tasks = readStorage(STORAGE_KEYS.tasks);
  return Array.isArray(tasks) ? tasks.filter(isTask) : [];
}

export function loadSessions() {
  const sessions = readStorage(STORAGE_KEYS.sessions);
  return Array.isArray(sessions) ? sessions.filter(isSession) : [];
}

export function loadStreak(fallback: StreakModel): StreakModel {
  const streak = readStorage(STORAGE_KEYS.streak);

  if (
    !isRecord(streak) ||
    !isFiniteNumber(streak.current) ||
    !isFiniteNumber(streak.longestEver) ||
    !(
      typeof streak.lastSessionDate === 'string' ||
      streak.lastSessionDate === null
    )
  ) {
    return fallback;
  }

  return {
    current: Math.max(0, Math.floor(streak.current)),
    lastSessionDate: streak.lastSessionDate,
    longestEver: Math.max(0, Math.floor(streak.longestEver)),
  };
}

export function loadSettings(fallback: SettingsModel): SettingsModel {
  const settings = readStorage(STORAGE_KEYS.settings);
  if (!isRecord(settings)) return fallback;

  const defaultDuration = isFiniteNumber(settings.defaultDuration)
    ? Math.min(120, Math.max(5, Math.round(settings.defaultDuration)))
    : fallback.defaultDuration;

  return {
    defaultDuration,
    soundEnabled:
      typeof settings.soundEnabled === 'boolean'
        ? settings.soundEnabled
        : fallback.soundEnabled,
  };
}

export function loadActiveSession(): ActiveSessionModel | null {
  const session = readStorage(STORAGE_KEYS.activeSession);

  if (
    !isRecord(session) ||
    typeof session.id !== 'string' ||
    !isFiniteNumber(session.durationMinutes) ||
    !(typeof session.taskId === 'string' || session.taskId === null) ||
    !(session.status === 'running' || session.status === 'paused') ||
    typeof session.startedAt !== 'string' ||
    !(isFiniteNumber(session.endsAt) || session.endsAt === null)
  ) {
    return null;
  }

  const durationMinutes = Math.min(
    120,
    Math.max(5, Math.round(session.durationMinutes)),
  );
  const pausedSecondsRemaining = isFiniteNumber(
    session.pausedSecondsRemaining,
  )
    ? Math.max(1, Math.round(session.pausedSecondsRemaining))
    : session.status === 'paused'
      ? durationMinutes * 60
      : null;

  return {
    id: session.id,
    durationMinutes,
    taskId: session.taskId,
    status: session.status,
    startedAt: session.startedAt,
    endsAt: session.endsAt,
    pausedSecondsRemaining,
  };
}

export function loadStopwatch(): StopwatchModel | null {
  const stopwatch = readStorage(STORAGE_KEYS.stopwatch);

  if (
    !isRecord(stopwatch) ||
    !(stopwatch.status === 'running' || stopwatch.status === 'paused') ||
    !isFiniteNumber(stopwatch.elapsedMs) ||
    !(
      (stopwatch.status === 'running' && isFiniteNumber(stopwatch.startedAt)) ||
      (stopwatch.status === 'paused' && stopwatch.startedAt === null)
    ) ||
    !Array.isArray(stopwatch.laps)
  ) {
    return null;
  }

  const laps = stopwatch.laps.reduce<number[]>((validLaps, lap) => {
    const previousLap = validLaps.at(-1) ?? 0;
    if (isFiniteNumber(lap) && lap >= previousLap) validLaps.push(lap);
    return validLaps;
  }, []);

  return {
    status: stopwatch.status,
    elapsedMs: Math.max(0, stopwatch.elapsedMs),
    startedAt: stopwatch.startedAt,
    laps,
  };
}

export function writeStorage(key: string, value: unknown) {
  try {
    if (value === null) {
      localStorage.removeItem(key);
      return;
    }

    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // O aplicativo continua funcional mesmo quando o navegador bloqueia storage.
  }
}
