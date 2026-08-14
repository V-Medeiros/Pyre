export type StopwatchStatus = 'running' | 'paused';

export type StopwatchModel = {
  status: StopwatchStatus;
  elapsedMs: number;
  startedAt: number | null;
  laps: number[];
};
