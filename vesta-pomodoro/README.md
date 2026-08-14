# Vesta

Vesta is a local-first intentional-focus app. It turns a task into a focused
session, keeps a lightweight history, and makes daily consistency visible
through a streak ritual.

## Current status

The Pomodoro and `Stopwatch` modes are complete. `Timer` is available in the
navigation but intentionally shows a minimal **Page in progress** view until
its dedicated experience is implemented.

The interface is currently in English. No account, backend, or network
connection is required.

## MVP features

### Focus sessions

- Configurable focus duration from 5 to 120 minutes.
- Start, pause, resume, and stop controls.
- Immediate stop without a confirmation dialog.
- Recovery of an active session after a page reload.
- Completed and abandoned session history.
- Optional link between the active session and a task.

### Stopwatch

- Start, pause, resume, and reset controls.
- Centisecond readout based on wall-clock time for background-tab accuracy.
- Lap splits with individual and total elapsed times.
- Recovery of the running stopwatch and its laps after a page reload.

### Tasks

- Create tasks with a concise title.
- Rename, complete, reopen, or delete a task.
- Deletion is immediate and retains any existing session history.

### Progress and visual identity

- Daily streak calculated from completed sessions.
- Fourteen-day campfire history.
- Streak labels: Spark, Kindling, Flame, Torch, and Bonfire.
- The central flame uses the original V1 image asset
  (`vesta-flame-symbol-redesign-512.png`).

### Settings

- Default duration, focus presets, and audio preferences.
- Start, completion, and tick sound controls.
- Light, dark, and system theme choices.
- Reset limited to application data.

## Local data

All data stays in browser `localStorage`:

| Key | Purpose |
| --- | --- |
| `vesta_tasks` | Tasks and completion status |
| `vesta_sessions` | Completed and abandoned sessions |
| `vesta_streak` | Daily streak progress |
| `vesta_settings` | Experience settings |
| `vesta_active_session` | Session currently in progress |
| `vesta_stopwatch` | Stopwatch time, status, and laps |
| `theme` | Selected visual theme |

## Technology

- React 19
- TypeScript
- Vite
- CSS Modules
- Context API
- Lucide React
- Web Audio API
- ESLint

## Run locally

From `vesta-pomodoro`:

```powershell
npm.cmd install
npm.cmd run dev
```

Vite normally serves the app at `http://localhost:5173`.

## Scripts

| Command | Description |
| --- | --- |
| `npm.cmd run dev` | Starts the development server |
| `npm.cmd run build` | Validates TypeScript and builds the production app |
| `npm.cmd run lint` | Runs ESLint |
| `npm.cmd run preview` | Serves the production build locally |

## Project structure

```text
src/
|-- components/          # Timer, flame, tasks, history, and settings
|-- context/             # Shared state and session rules
|-- Models/              # Domain types
|-- pages/               # Screen composition and mode views
|-- templates/           # Shared interface structure
|-- theme/               # Light and dark themes
`-- utils/               # Dates, streaks, storage, sound, and formatting
```

The MVP state remains centralized in `TaskContextProvider` so interface
components can reuse the same business rules.

## Verification

Before delivering a code change, run:

```powershell
npm.cmd run lint
npm.cmd run build
```

The complete product scope is documented in the
[root README](../README.md).
