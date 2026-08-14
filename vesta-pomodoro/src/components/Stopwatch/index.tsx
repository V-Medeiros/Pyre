import { FlagIcon, PauseIcon, PlayIcon, RotateCcwIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { StopwatchStatus } from '../../Models/StopwatchModel';
import {
  loadStopwatch,
  STORAGE_KEYS,
  writeStorage,
} from '../../utils/storage';
import { DefaultButton } from '../DefaultButton';
import styles from './style.module.css';

type StopwatchState = {
  status: StopwatchStatus | 'idle';
  elapsedMs: number;
  startedAt: number | null;
  laps: number[];
};

function getInitialState(): StopwatchState {
  const saved = loadStopwatch();
  return saved ?? { status: 'idle', elapsedMs: 0, startedAt: null, laps: [] };
}

function getElapsedMs(state: StopwatchState, now = Date.now()) {
  if (state.status !== 'running' || state.startedAt === null) {
    return state.elapsedMs;
  }

  return state.elapsedMs + Math.max(0, now - state.startedAt);
}

function formatTime(elapsedMs: number) {
  const totalCentiseconds = Math.floor(elapsedMs / 10);
  const centiseconds = totalCentiseconds % 100;
  const totalSeconds = Math.floor(totalCentiseconds / 100);
  const seconds = totalSeconds % 60;
  const totalMinutes = Math.floor(totalSeconds / 60);
  const minutes = totalMinutes % 60;
  const hours = Math.floor(totalMinutes / 60);

  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, '0'))
    .join(':')
    .concat(`.${String(centiseconds).padStart(2, '0')}`);
}

export function Stopwatch() {
  const [state, setState] = useState<StopwatchState>(getInitialState);
  const [now, setNow] = useState(Date.now);
  const elapsedMs = getElapsedMs(state, now);

  useEffect(() => {
    if (state.status !== 'running') return;

    const intervalId = window.setInterval(() => setNow(Date.now()), 31);
    return () => window.clearInterval(intervalId);
  }, [state.status]);

  useEffect(() => {
    if (state.status === 'idle') {
      writeStorage(STORAGE_KEYS.stopwatch, null);
      return;
    }

    writeStorage(STORAGE_KEYS.stopwatch, state);
  }, [state]);

  function start() {
    const startedAt = Date.now();
    setNow(startedAt);
    setState((current) => ({
      ...current,
      status: 'running',
      startedAt,
    }));
  }

  function pause() {
    const pausedAt = Date.now();
    setNow(pausedAt);
    setState((current) => ({
      ...current,
      status: 'paused',
      elapsedMs: getElapsedMs(current, pausedAt),
      startedAt: null,
    }));
  }

  function addLap() {
    const lapAt = Date.now();
    setNow(lapAt);
    setState((current) => ({
      ...current,
      laps: [...current.laps, getElapsedMs(current, lapAt)],
    }));
  }

  function reset() {
    setNow(Date.now());
    setState({ status: 'idle', elapsedMs: 0, startedAt: null, laps: [] });
  }

  return (
    <section className={styles.stopwatch} aria-labelledby='stopwatch-title'>
      <div className={styles.readout}>
        <span className={styles.eyebrow} id='stopwatch-title'>
          Stopwatch
        </span>
        <div
          className={styles.time}
          role='timer'
          aria-live='off'
          aria-label={`${Math.floor(elapsedMs / 1000)} seconds elapsed`}
        >
          {formatTime(elapsedMs)}
        </div>
        <span className={styles.status}>
          {state.status === 'running'
            ? 'Running'
            : state.status === 'paused'
              ? 'Paused'
              : 'Ready'}
        </span>
      </div>

      <div className={styles.controls}>
        {state.status === 'running' ? (
          <DefaultButton
            type='button'
            icon={<PauseIcon />}
            color='secondary'
            onClick={pause}
          >
            Pause
          </DefaultButton>
        ) : (
          <DefaultButton type='button' icon={<PlayIcon />} onClick={start}>
            {state.status === 'paused' ? 'Resume' : 'Start'}
          </DefaultButton>
        )}

        {state.status === 'running' && (
          <DefaultButton
            type='button'
            icon={<FlagIcon />}
            color='secondary'
            onClick={addLap}
          >
            Lap
          </DefaultButton>
        )}

        {state.status === 'paused' && (
          <DefaultButton
            type='button'
            icon={<RotateCcwIcon />}
            color='danger'
            onClick={reset}
          >
            Reset
          </DefaultButton>
        )}
      </div>

      {state.laps.length > 0 && (
        <div className={styles.laps}>
          <div className={styles.lapsHeader}>
            <h3>Laps</h3>
            <span>{state.laps.length}</span>
          </div>
          <ol className={styles.lapList} reversed>
            {[...state.laps].reverse().map((lap, reverseIndex) => {
              const lapIndex = state.laps.length - 1 - reverseIndex;
              const previousLap = state.laps[lapIndex - 1] ?? 0;

              return (
                <li key={`${lapIndex}-${lap}`}>
                  <span>Lap {lapIndex + 1}</span>
                  <strong>{formatTime(lap - previousLap)}</strong>
                  <span>{formatTime(lap)}</span>
                </li>
              );
            })}
          </ol>
        </div>
      )}
    </section>
  );
}
