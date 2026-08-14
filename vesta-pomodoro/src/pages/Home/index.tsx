import { useCallback, useEffect, useRef, useState } from 'react';
import { CountDown } from '../../components/CountDown';
import { FocusFlame } from '../../components/FocusFlame';
import { HistoryPanel } from '../../components/HistoryPanel';
import { MainForm } from '../../components/MainForm';
import { ModePlaceholder } from '../../components/ModePlaceholder';
import { SettingsPanel } from '../../components/SettingsPanel';
import { Stopwatch } from '../../components/Stopwatch';
import { TaskPanel } from '../../components/TaskPanel';
import { useTaskContext } from '../../context/TaskContext/UseTaskContext';
import type { AppMode } from '../../Models/AppMode';
import { MainTemplate } from '../../templates/MainTemplate';
import { playCompletionSound } from '../../utils/playCompletionSound';

export function Home() {
  const { ContextState, dismissFeedback, toggleTask } = useTaskContext();
  const [activeMode, setActiveMode] = useState<AppMode>('pomodoro');
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const playedSessionId = useRef<string | null>(null);
  const closeHistory = useCallback(() => setIsHistoryOpen(false), []);
  const closeSettings = useCallback(() => setIsSettingsOpen(false), []);
  const selectedTask = ContextState.tasks.find(
    (task) => task.id === ContextState.selectedTaskId,
  );
  const latestSession = ContextState.sessions.at(-1);
  const completedTask =
    ContextState.sessionStatus === 'completed' && latestSession?.taskId
      ? ContextState.tasks.find((task) => task.id === latestSession.taskId)
      : null;

  useEffect(() => {
    if (
      ContextState.sessionStatus !== 'completed' ||
      latestSession?.status !== 'completed' ||
      playedSessionId.current === latestSession.id
    ) {
      return;
    }

    playedSessionId.current = latestSession.id;
    if (ContextState.settings.soundEnabled) playCompletionSound();
  }, [
    ContextState.sessionStatus,
    ContextState.settings.soundEnabled,
    latestSession,
  ]);

  return (
    <MainTemplate
      aside={activeMode === 'pomodoro' ? <TaskPanel /> : undefined}
      activeMode={activeMode}
      onModeChange={setActiveMode}
      onOpenHistory={() => setIsHistoryOpen(true)}
      onOpenSettings={() => setIsSettingsOpen(true)}
    >
      {activeMode === 'pomodoro' ? (
        <div className='home-layout'>
          <section className='timer-area' aria-labelledby='session-title'>
            <FocusFlame />
            <div className='timer-readout'>
              <h2 className='session-title' id='session-title'>
                Focus
              </h2>
              {selectedTask && (
                <p className='active-task-label'>{selectedTask.text}</p>
              )}
              <CountDown />
            </div>

            {ContextState.feedbackMessage && (
              <div className='session-feedback' role='status'>
                <span>
                  {ContextState.feedbackMessage}
                  {completedTask && !completedTask.completed && (
                    <button
                      className='complete-task-action'
                      type='button'
                      onClick={() => toggleTask(completedTask.id)}
                    >
                      Mark “{completedTask.text}” as completed
                    </button>
                  )}
                </span>
                <button type='button' onClick={dismissFeedback}>
                  Close
                </button>
              </div>
            )}

            <div className='session-controls'>
              <MainForm />
            </div>
          </section>
        </div>
      ) : activeMode === 'stopwatch' ? (
        <Stopwatch />
      ) : (
        <ModePlaceholder mode='timer' />
      )}
      {isHistoryOpen && <HistoryPanel onClose={closeHistory} />}
      {isSettingsOpen && <SettingsPanel onClose={closeSettings} />}
    </MainTemplate>
  );
}
