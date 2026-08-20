import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { SessionModel } from '../../Models/SessionModel';
import type { SettingsModel, StreakModel } from '../../Models/TaskStateModel';
import type { TaskModel } from '../../Models/TaskModel';
import { getCloudDeviceId, queueCloudOperation } from '../../api/cloud';
import { differenceInCalendarDays, toLocalDateKey } from '../../utils/date';
import { formatSecondsToMinutes } from '../../utils/formatSecondsToMinutes';
import { STORAGE_KEYS, writeStorage } from '../../utils/storage';
import { TaskContext } from './TaskContext';
import { createInitialTaskState } from './initialTaskState';

type TaskContextProviderProps = {
  children: ReactNode;
};

const COMPLETION_MESSAGE =
  'Session complete. Your flame is stronger — keep the momentum going.';
const ABANDONED_MESSAGE =
  'Session stopped. Your flame will be here when you come back.';

function createId(prefix: string) {
  const randomId =
    typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}_${randomId}`;
}

function updateStreak(streak: StreakModel, sessionDate: string): StreakModel {
  if (streak.lastSessionDate === sessionDate) return streak;

  const isConsecutiveDay =
    streak.lastSessionDate !== null &&
    differenceInCalendarDays(streak.lastSessionDate, sessionDate) === 1;
  const current = isConsecutiveDay ? streak.current + 1 : 1;

  return {
    current,
    lastSessionDate: sessionDate,
    longestEver: Math.max(streak.longestEver, current),
  };
}

export function TaskContextProvider({ children }: TaskContextProviderProps) {
  const [ContextState, SetState] = useState(createInitialTaskState);

  useEffect(() => {
    function hydrate() {
      SetState(createInitialTaskState());
    }
    window.addEventListener('vesta-cloud-hydrated', hydrate);
    return () => window.removeEventListener('vesta-cloud-hydrated', hydrate);
  }, []);

  const completeSession = useCallback(() => {
    SetState((state) => {
      const activeSession = state.activeSession;
      if (!activeSession || activeSession.status !== 'running') return state;

      const endedAt = new Date().toISOString();
      const date = toLocalDateKey(endedAt);
      const session: SessionModel = {
        id: activeSession.id,
        date,
        durationMinutes: activeSession.durationMinutes,
        taskId: activeSession.taskId,
        status: 'completed',
        startedAt: activeSession.startedAt,
        endedAt,
      };

      return {
        ...state,
        activeSession: null,
        sessionStatus: 'completed',
        secondsRemaining: 0,
        sessions: [...state.sessions, session],
        streak: updateStreak(state.streak, date),
        tasks: state.tasks.map((task) =>
          task.id === activeSession.taskId
            ? {
                ...task,
                sessionsCount: task.sessionsCount + 1,
                updatedAt: endedAt,
              }
            : task,
        ),
        feedbackMessage: COMPLETION_MESSAGE,
      };
    });
  }, []);

  useEffect(() => {
    if (
      ContextState.sessionStatus !== 'running' ||
      !ContextState.activeSession?.endsAt
    ) {
      return;
    }

    const endsAt = ContextState.activeSession.endsAt;
    const intervalId = window.setInterval(() => {
      const secondsRemaining = Math.max(
        0,
        Math.ceil((endsAt - Date.now()) / 1000),
      );

      if (secondsRemaining === 0) {
        window.clearInterval(intervalId);
        completeSession();
        return;
      }

      SetState((state) =>
        state.secondsRemaining === secondsRemaining
          ? state
          : {
              ...state,
              secondsRemaining,
            },
      );
    }, 250);

    return () => window.clearInterval(intervalId);
  }, [
    ContextState.activeSession?.endsAt,
    ContextState.sessionStatus,
    completeSession,
  ]);

  useEffect(() => {
    document.title = `${formatSecondsToMinutes(
      ContextState.secondsRemaining,
    )} · Vesta`;
  }, [ContextState.secondsRemaining]);

  useEffect(() => {
    writeStorage(STORAGE_KEYS.tasks, ContextState.tasks);
    writeStorage(STORAGE_KEYS.sessions, ContextState.sessions);
    writeStorage(STORAGE_KEYS.streak, ContextState.streak);
    writeStorage(STORAGE_KEYS.settings, ContextState.settings);
    writeStorage(STORAGE_KEYS.activeSession, ContextState.activeSession);
  }, [
    ContextState.activeSession,
    ContextState.sessions,
    ContextState.settings,
    ContextState.streak,
    ContextState.tasks,
  ]);

  const setDuration = useCallback((minutes: number) => {
    const nextDuration = Math.min(120, Math.max(5, Math.round(minutes)));

    SetState((state) => {
      if (state.sessionStatus === 'running' || state.sessionStatus === 'paused') {
        return state;
      }

      return {
        ...state,
        durationMinutes: nextDuration,
        secondsRemaining: nextDuration * 60,
        sessionStatus: 'idle',
        feedbackMessage: null,
      };
    });
  }, []);

  const startSession = useCallback(() => {
    if (ContextState.sessionStatus === 'running' || ContextState.sessionStatus === 'paused') return;
    const now = Date.now();
    const id = createId('session');
    const durationInSeconds = ContextState.durationMinutes * 60;
    SetState((state) => ({
      ...state,
      activeSession: {
        id,
        durationMinutes: ContextState.durationMinutes,
        taskId: ContextState.selectedTaskId,
        status: 'running',
        startedAt: new Date(now).toISOString(),
        endsAt: now + durationInSeconds * 1000,
        pausedSecondsRemaining: null,
      },
      sessionStatus: 'running',
      secondsRemaining: durationInSeconds,
      feedbackMessage: null,
    }));
    queueCloudOperation({
      method: 'POST',
      path: '/api/v1/focus-sessions',
      body: {
        id,
        durationMinutes: ContextState.durationMinutes,
        taskId: ContextState.selectedTaskId,
        deviceId: getCloudDeviceId(),
      },
    });
  }, [ContextState.durationMinutes, ContextState.selectedTaskId, ContextState.sessionStatus]);

  const pauseSession = useCallback(() => {
    if (ContextState.sessionStatus !== 'running' || !ContextState.activeSession) return;
    const sessionId = ContextState.activeSession.id;
    SetState((state) => {
      if (
        state.sessionStatus !== 'running' ||
        !state.activeSession?.endsAt
      ) {
        return state;
      }

      const secondsRemaining = Math.max(
        1,
        Math.ceil((state.activeSession.endsAt - Date.now()) / 1000),
      );

      return {
        ...state,
        activeSession: {
          ...state.activeSession,
          status: 'paused',
          endsAt: null,
          pausedSecondsRemaining: secondsRemaining,
        },
        sessionStatus: 'paused',
        secondsRemaining,
      };
    });
    queueCloudOperation({ method: 'POST', path: `/api/v1/focus-sessions/${sessionId}/pause`,
      body: { deviceId: getCloudDeviceId() } });
  }, [ContextState.activeSession, ContextState.sessionStatus]);

  const resumeSession = useCallback(() => {
    if (ContextState.sessionStatus !== 'paused' || !ContextState.activeSession) return;
    const sessionId = ContextState.activeSession.id;
    SetState((state) => {
      if (
        state.sessionStatus !== 'paused' ||
        !state.activeSession ||
        state.secondsRemaining <= 0
      ) {
        return state;
      }

      return {
        ...state,
        activeSession: {
          ...state.activeSession,
          status: 'running',
          endsAt: Date.now() + state.secondsRemaining * 1000,
          pausedSecondsRemaining: null,
        },
        sessionStatus: 'running',
      };
    });
    queueCloudOperation({ method: 'POST', path: `/api/v1/focus-sessions/${sessionId}/resume`,
      body: { deviceId: getCloudDeviceId() } });
  }, [ContextState.activeSession, ContextState.sessionStatus]);

  const abandonSession = useCallback(() => {
    if (!ContextState.activeSession) return;
    const sessionId = ContextState.activeSession.id;
    SetState((state) => {
      if (!state.activeSession) return state;

      const endedAt = new Date().toISOString();
      const abandonedSession: SessionModel = {
        id: state.activeSession.id,
        date: toLocalDateKey(endedAt),
        durationMinutes: state.activeSession.durationMinutes,
        taskId: state.activeSession.taskId,
        status: 'abandoned',
        startedAt: state.activeSession.startedAt,
        endedAt,
      };

      return {
        ...state,
        activeSession: null,
        sessionStatus: 'abandoned',
        secondsRemaining: state.durationMinutes * 60,
        sessions: [...state.sessions, abandonedSession],
        feedbackMessage: ABANDONED_MESSAGE,
      };
    });
    queueCloudOperation({ method: 'POST', path: `/api/v1/focus-sessions/${sessionId}/abandon`,
      body: { deviceId: getCloudDeviceId() } });
  }, [ContextState.activeSession]);

  const dismissFeedback = useCallback(() => {
    SetState((state) => ({
      ...state,
      sessionStatus:
        state.sessionStatus === 'completed' ||
        state.sessionStatus === 'abandoned'
          ? 'idle'
          : state.sessionStatus,
      secondsRemaining:
        state.sessionStatus === 'completed'
          ? state.durationMinutes * 60
          : state.secondsRemaining,
      feedbackMessage: null,
    }));
  }, []);

  const addTask = useCallback((text: string) => {
    const normalizedText = text.trim();
    if (!normalizedText) return '';

    const now = new Date().toISOString();
    const task: TaskModel = {
      id: createId('task'),
      text: normalizedText,
      completed: false,
      sessionsCount: 0,
      createdAt: now,
      updatedAt: now,
    };

    SetState((state) => ({
      ...state,
      tasks: [task, ...state.tasks],
      selectedTaskId: task.id,
    }));

    queueCloudOperation({ method: 'POST', path: '/api/v1/tasks',
      body: { id: task.id, title: task.text } });

    return task.id;
  }, []);

  const selectTask = useCallback((taskId: string | null) => {
    SetState((state) => {
      if (state.sessionStatus === 'running' || state.sessionStatus === 'paused') {
        return state;
      }

      const taskExists =
        taskId === null ||
        state.tasks.some((task) => task.id === taskId && !task.completed);

      return taskExists ? { ...state, selectedTaskId: taskId } : state;
    });
  }, []);

  const toggleTask = useCallback((taskId: string) => {
    const task = ContextState.tasks.find((item) => item.id === taskId);
    if (!task || ContextState.activeSession?.taskId === taskId) return;
    const updatedAt = new Date().toISOString();
    SetState((state) => {
      if (state.activeSession?.taskId === taskId) return state;

      return {
        ...state,
        selectedTaskId:
          state.selectedTaskId === taskId ? null : state.selectedTaskId,
        tasks: state.tasks.map((task) =>
          task.id === taskId
            ? { ...task, completed: !task.completed, updatedAt }
            : task,
        ),
      };
    });
    queueCloudOperation({ method: 'POST',
      path: `/api/v1/tasks/${taskId}/${task.completed ? 'reopen' : 'complete'}`, body: {} });
  }, [ContextState.activeSession?.taskId, ContextState.tasks]);

  const deleteTask = useCallback((taskId: string) => {
    if (ContextState.activeSession?.taskId === taskId) return;
    SetState((state) => {
      if (state.activeSession?.taskId === taskId) return state;

      return {
        ...state,
        selectedTaskId:
          state.selectedTaskId === taskId ? null : state.selectedTaskId,
        tasks: state.tasks.filter((task) => task.id !== taskId),
      };
    });
    queueCloudOperation({ method: 'DELETE', path: `/api/v1/tasks/${taskId}` });
  }, [ContextState.activeSession?.taskId]);

  const updateSettings = useCallback((settings: Partial<SettingsModel>) => {
    SetState((state) => {
      const nextSettings = { ...state.settings, ...settings };
      const canUpdateTimer =
        state.sessionStatus !== 'running' && state.sessionStatus !== 'paused';

      return {
        ...state,
        settings: nextSettings,
        durationMinutes: canUpdateTimer
          ? nextSettings.defaultDuration
          : state.durationMinutes,
        secondsRemaining: canUpdateTimer
          ? nextSettings.defaultDuration * 60
          : state.secondsRemaining,
      };
    });
    queueCloudOperation({ method: 'PATCH', path: '/api/v1/preferences', body: {
      defaultDurationMinutes: settings.defaultDuration,
      soundEnabled: settings.soundEnabled,
    } });
  }, []);

  const value = useMemo(
    () => ({
      ContextState,
      setDuration,
      startSession,
      pauseSession,
      resumeSession,
      abandonSession,
      dismissFeedback,
      addTask,
      selectTask,
      toggleTask,
      deleteTask,
      updateSettings,
    }),
    [
      ContextState,
      abandonSession,
      addTask,
      deleteTask,
      dismissFeedback,
      pauseSession,
      resumeSession,
      selectTask,
      setDuration,
      startSession,
      toggleTask,
      updateSettings,
    ],
  );

  return <TaskContext.Provider value={value}>{children}</TaskContext.Provider>;
}
