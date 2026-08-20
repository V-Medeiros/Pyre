import type { ReactNode } from 'react';
import { FlameKindling } from 'lucide-react';
import '../../App.css';
import { Heading } from '../../components/Heading';
import { Menu } from '../../components/Menu';
import { StreakBadge } from '../../components/StreakBadge';
import type { AppMode } from '../../Models/AppMode';

type MainTemplateProps = {
  children: ReactNode;
  aside?: ReactNode;
  activeMode?: AppMode;
  onModeChange?: (mode: AppMode) => void;
  onOpenHistory?: () => void;
  onOpenSettings?: () => void;
  onOpenAccount?: () => void;
};

export function MainTemplate({
  children,
  aside,
  activeMode = 'pomodoro',
  onModeChange,
  onOpenHistory,
  onOpenSettings,
  onOpenAccount,
}: MainTemplateProps) {
  const modes: { id: AppMode; label: string }[] = [
    { id: 'pomodoro', label: 'Pomodoro' },
    { id: 'stopwatch', label: 'Stopwatch' },
    { id: 'timer', label: 'Timer' },
  ];

  return (
    <div className={`app-shell ${aside ? 'app-shell-with-sidebar' : ''}`}>
      <div className='app'>
        <header className='app-header'>
          <Heading>
            <span className='logo-mark' aria-hidden='true'>
              <FlameKindling className='logo-icon' />
            </span>
            VESTA
          </Heading>

          <div className='header-center'>
            <nav className='mode-menu' aria-label='Timer modes'>
              {modes.map((mode) => (
                <button
                  key={mode.id}
                  className={`mode-button ${
                    activeMode === mode.id ? 'active-mode' : ''
                  }`}
                  type='button'
                  aria-current={activeMode === mode.id ? 'page' : undefined}
                  onClick={() => onModeChange?.(mode.id)}
                >
                  {mode.label}
                </button>
              ))}
            </nav>
            <StreakBadge />
          </div>

          <Menu
            onOpenHistory={onOpenHistory}
            onOpenSettings={onOpenSettings}
            onOpenAccount={onOpenAccount}
          />
        </header>

        <main className='app-main'>{children}</main>

        <footer className='app-footer'>
          <div className='footer-content'>
            <span className='footer-divider' aria-hidden='true' />
            <span className='footer-copy'>One spark at a time.</span>
          </div>
        </footer>
      </div>

      {aside}
    </div>
  );
}
