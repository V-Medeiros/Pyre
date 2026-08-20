import { STORAGE_KEYS } from '../utils/storage';

const API_URL = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '')
  ?? 'http://localhost:8080';
export const CLOUD_ENABLED = import.meta.env.VITE_CLOUD_ENABLED !== 'false';
const CLOUD_USER_KEY = 'vesta_cloud_user_id';
const OUTBOX_KEY = 'vesta_cloud_outbox';

let accessToken: string | null = null;
let activeFlush: Promise<boolean> | null = null;
let activeRefresh: Promise<CloudUser | null> | null = null;

export type CloudUser = {
  id: string;
  email: string;
  displayName: string | null;
  timezone: string;
  locale: string;
  emailVerified: boolean;
  version: number;
};

type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: CloudUser;
};

type SyncTask = {
  id: string;
  title: string;
  completed: boolean;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
};

type SyncSession = {
  id: string;
  taskId: string | null;
  taskTitle: string | null;
  status: 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'ABANDONED';
  plannedDurationSeconds: number;
  accumulatedFocusSeconds: number;
  actualFocusSeconds: number | null;
  startedAt: string;
  deadlineAt: string | null;
  endedAt: string | null;
  timezone: string;
  localDate: string | null;
};

type SyncResponse = {
  cursor: string;
  tasks: SyncTask[];
  sessions: SyncSession[];
  preferences: {
    defaultDurationMinutes: number;
    soundEnabled: boolean;
    theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  } | null;
};

type StreakResponse = {
  current: number;
  longestEver: number;
  lastCompletedDate: string | null;
};

type OutboxItem = {
  id: string;
  method: 'POST' | 'PATCH' | 'DELETE';
  path: string;
  body?: unknown;
  attempts: number;
};

export class CloudApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(
    status: number,
    code: string,
    message: string,
  ) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function csrfCookie() {
  const prefix = 'vesta_csrf=';
  return document.cookie.split(';').map((value) => value.trim())
    .find((value) => value.startsWith(prefix))?.slice(prefix.length) ?? null;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    credentials: 'include',
    headers,
  });
  if (response.status === 401 && retry && accessToken && path !== '/api/v1/auth/refresh') {
    if (await refreshCloud()) return request<T>(path, init, false);
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as {
      code?: string;
      detail?: string;
    } | null;
    throw new CloudApiError(response.status, problem?.code ?? 'request_failed',
      problem?.detail ?? 'The cloud request failed.');
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

function setSession(response: AuthResponse) {
  accessToken = response.accessToken;
  return response.user;
}

export async function registerCloud(input: {
  email: string;
  password: string;
  displayName?: string;
}) {
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  return setSession(await request<AuthResponse>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ ...input, timezone, locale: navigator.language, deviceName: navigator.userAgent }),
  }));
}

export async function loginCloud(email: string, password: string) {
  return setSession(await request<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, deviceName: navigator.userAgent }),
  }));
}

export function refreshCloud() {
  if (activeRefresh) return activeRefresh;
  activeRefresh = performRefresh().finally(() => { activeRefresh = null; });
  return activeRefresh;
}

async function performRefresh() {
  const csrf = csrfCookie();
  if (!csrf) return null;
  try {
    return setSession(await request<AuthResponse>('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'X-CSRF-Token': decodeURIComponent(csrf) },
    }));
  } catch {
    accessToken = null;
    return null;
  }
}

export async function logoutCloud() {
  const csrf = csrfCookie();
  await request('/api/v1/auth/logout', {
    method: 'POST',
    headers: csrf ? { 'X-CSRF-Token': decodeURIComponent(csrf) } : {},
  });
  accessToken = null;
  localStorage.removeItem(CLOUD_USER_KEY);
}

function readJson(key: string) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) as unknown : null;
  } catch {
    return null;
  }
}

function localSnapshot() {
  return {
    tasks: readJson(STORAGE_KEYS.tasks),
    sessions: readJson(STORAGE_KEYS.sessions),
    streak: readJson(STORAGE_KEYS.streak),
    settings: readJson(STORAGE_KEYS.settings),
    activeSession: readJson(STORAGE_KEYS.activeSession),
    stopwatch: readJson(STORAGE_KEYS.stopwatch),
    theme: localStorage.getItem('theme'),
  };
}

export function hasLocalVestaData() {
  const snapshot = localSnapshot();
  return Object.values(snapshot).some((value) => value !== null);
}

export function backupLocalData() {
  const key = `vesta_pre_cloud_backup_${new Date().toISOString()}`;
  localStorage.setItem(key, JSON.stringify(localSnapshot()));
  return key;
}

export async function importLocalData(userId: string) {
  const importKeyName = `vesta_cloud_import_key_${userId}`;
  let importKey = localStorage.getItem(importKeyName);
  if (!importKey) {
    importKey = `browser.${crypto.randomUUID()}`;
    localStorage.setItem(importKeyName, importKey);
  }
  await request('/api/v1/imports/local-storage', {
    method: 'POST',
    headers: { 'Idempotency-Key': importKey },
    body: JSON.stringify(localSnapshot()),
  });
}

export async function hydrateFromCloud(userId?: string) {
  const cursorKey = userId ? `vesta_sync_cursor_${userId}` : 'vesta_sync_cursor';
  const savedCursor = localStorage.getItem(cursorKey);
  const incremental = savedCursor !== null;
  const [data, streak] = await Promise.all([
    request<SyncResponse>(incremental
      ? `/api/v1/sync/changes?cursor=${encodeURIComponent(savedCursor!)}`
      : '/api/v1/sync/bootstrap'),
    request<StreakResponse>('/api/v1/streak'),
  ]);
  type LocalTask = { id: string; text: string; completed: boolean; sessionsCount: number;
    createdAt: string; updatedAt: string };
  type LocalSession = { id: string; date: string; durationMinutes: number; taskId: string | null;
    status: 'completed' | 'abandoned'; startedAt: string; endedAt: string };
  type LocalActive = { id: string; durationMinutes: number; taskId: string | null;
    status: 'running' | 'paused'; startedAt: string; endsAt: number | null;
    pausedSecondsRemaining: number | null };
  const taskMap = new Map<string, LocalTask>((incremental
    && Array.isArray(readJson(STORAGE_KEYS.tasks))
      ? readJson(STORAGE_KEYS.tasks) as LocalTask[] : []).map((task) => [task.id, task]));
  const sessionMap = new Map<string, LocalSession>((incremental
    && Array.isArray(readJson(STORAGE_KEYS.sessions))
      ? readJson(STORAGE_KEYS.sessions) as LocalSession[] : []).map((session) => [session.id, session]));
  let active = incremental ? readJson(STORAGE_KEYS.activeSession) as LocalActive | null : null;

  for (const task of data.tasks) {
    if (task.deleted) taskMap.delete(task.id);
    else taskMap.set(task.id, {
      id: task.id,
      text: task.title,
      completed: task.completed,
      sessionsCount: 0,
      createdAt: task.createdAt,
      updatedAt: task.updatedAt,
    });
  }
  for (const session of data.sessions) {
    if (session.status === 'COMPLETED' || session.status === 'ABANDONED') {
      sessionMap.set(session.id, {
        id: session.id,
        date: session.localDate!,
        durationMinutes: Math.round(session.plannedDurationSeconds / 60),
        taskId: session.taskId,
        status: session.status.toLowerCase() as 'completed' | 'abandoned',
        startedAt: session.startedAt,
        endedAt: session.endedAt!,
      });
      if (active?.id === session.id) active = null;
    } else {
      active = {
        id: session.id,
        durationMinutes: Math.round(session.plannedDurationSeconds / 60),
        taskId: session.taskId,
        status: session.status.toLowerCase() as 'running' | 'paused',
        startedAt: session.startedAt,
        endsAt: session.deadlineAt ? new Date(session.deadlineAt).getTime() : null,
        pausedSecondsRemaining: session.status === 'PAUSED'
          ? session.plannedDurationSeconds - session.accumulatedFocusSeconds
          : null,
      };
    }
  }

  const ended = [...sessionMap.values()];
  const countByTask = ended.filter((session) => session.status === 'completed')
    .reduce<Record<string, number>>((counts, session) => {
      if (session.taskId) counts[session.taskId] = (counts[session.taskId] ?? 0) + 1;
      return counts;
    }, {});
  const tasks = [...taskMap.values()].map((task) => ({
    ...task,
    sessionsCount: countByTask[task.id] ?? 0,
  }));

  localStorage.setItem(STORAGE_KEYS.tasks, JSON.stringify(tasks));
  localStorage.setItem(STORAGE_KEYS.sessions, JSON.stringify(ended));
  localStorage.setItem(STORAGE_KEYS.streak, JSON.stringify({
    current: streak.current,
    longestEver: streak.longestEver,
    lastSessionDate: streak.lastCompletedDate,
  }));
  if (data.preferences) {
    localStorage.setItem(STORAGE_KEYS.settings, JSON.stringify({
      defaultDuration: data.preferences.defaultDurationMinutes,
      soundEnabled: data.preferences.soundEnabled,
    }));
    if (data.preferences.theme !== 'SYSTEM') {
      localStorage.setItem('theme', data.preferences.theme.toLowerCase());
    }
  }
  if (active) {
    localStorage.setItem(STORAGE_KEYS.activeSession, JSON.stringify(active));
  } else {
    localStorage.removeItem(STORAGE_KEYS.activeSession);
  }
  localStorage.setItem(cursorKey, data.cursor);
  window.dispatchEvent(new Event('vesta-cloud-hydrated'));
}

export function markCloudConnected(userId: string) {
  localStorage.setItem(CLOUD_USER_KEY, userId);
}

export function isCloudConnected(userId?: string) {
  const connected = localStorage.getItem(CLOUD_USER_KEY);
  return connected !== null && (userId === undefined || connected === userId);
}

export function getCloudDeviceId() {
  const key = 'vesta_cloud_device_id';
  let value = localStorage.getItem(key);
  if (!value) {
    value = crypto.randomUUID();
    localStorage.setItem(key, value);
  }
  return value;
}

function readOutbox(): OutboxItem[] {
  const value = readJson(OUTBOX_KEY);
  return Array.isArray(value) ? value as OutboxItem[] : [];
}

function writeOutbox(value: OutboxItem[]) {
  localStorage.setItem(OUTBOX_KEY, JSON.stringify(value));
}

export function queueCloudOperation(operation: Omit<OutboxItem, 'id' | 'attempts'>) {
  if (!CLOUD_ENABLED || !isCloudConnected()) return;
  const item: OutboxItem = { ...operation, id: crypto.randomUUID(), attempts: 0 };
  writeOutbox([...readOutbox(), item]);
  void flushCloudOutbox();
}

export function flushCloudOutbox() {
  if (activeFlush) return activeFlush;
  activeFlush = flushOutbox().finally(() => { activeFlush = null; });
  return activeFlush;
}

async function flushOutbox() {
  if (!accessToken) return false;
  while (true) {
    const pending = readOutbox();
    const item = pending[0];
    if (!item) return true;
    try {
      await request(item.path, {
        method: item.method,
        body: item.body === undefined ? undefined : JSON.stringify(item.body),
      });
      writeOutbox(readOutbox().filter((value) => value.id !== item.id));
    } catch (error) {
      if (error instanceof CloudApiError && error.status >= 400 && error.status < 500
          && error.status !== 401 && error.status !== 429) {
        if (error.status === 409) localStorage.setItem('vesta_cloud_conflict', error.code);
        writeOutbox(readOutbox().filter((value) => value.id !== item.id));
        continue;
      }
      writeOutbox(readOutbox().map((value) => value.id === item.id
        ? { ...value, attempts: value.attempts + 1 }
        : value));
      return false;
    }
  }
}
