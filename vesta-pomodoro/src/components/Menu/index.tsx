import { CloudIcon, HistoryIcon, MoonIcon, SettingsIcon, SunIcon, UserRoundIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { queueCloudOperation } from '../../api/cloud';
import { useCloudContext } from '../../context/CloudContext/UseCloudContext';
import { applyTheme, initialTheme, type Theme } from '../../theme/theme';
import styles from './style.module.css';

type MenuProps = {
  onOpenHistory?: () => void;
  onOpenSettings?: () => void;
  onOpenAccount?: () => void;
};

export function Menu({ onOpenHistory, onOpenSettings, onOpenAccount }: MenuProps) {
  const cloud = useCloudContext();
  const [theme, setTheme] = useState<Theme>(initialTheme);

  function handleThemeChange() {
    setTheme((currentTheme) => {
      const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
      queueCloudOperation({ method: 'PATCH', path: '/api/v1/preferences',
        body: { theme: nextTheme.toUpperCase() } });
      return nextTheme;
    });
  }

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  const iconByTheme = {
    dark: <SunIcon className={styles.menuLink} />,
    light: <MoonIcon className={styles.menuLink} />,
  };

  return (
    <nav className={styles.menu} aria-label='Actions'>
      {onOpenAccount && (
        <button className={styles.buttonMenu} type='button' onClick={onOpenAccount}
          aria-label={cloud.user ? `Cloud account: ${cloud.user.email}` : 'Sign in for cloud sync'}>
          {cloud.user ? <CloudIcon className={styles.connectedIcon} /> : <UserRoundIcon className={styles.menuLink} />}
        </button>
      )}
      {onOpenHistory && (
        <button
          className={`${styles.buttonMenu} ${styles.historyButton}`}
          type='button'
          onClick={onOpenHistory}
        >
          <HistoryIcon className={styles.menuLink} />
          <span className={styles.historyText}>History</span>
        </button>
      )}
      {onOpenSettings && (
        <button
          className={styles.buttonMenu}
          type='button'
          aria-label='Settings'
          onClick={onOpenSettings}
        >
          <SettingsIcon className={styles.menuLink} />
        </button>
      )}
      <button
        className={styles.buttonMenu}
        type='button'
        aria-label={`Use ${theme === 'dark' ? 'light' : 'dark'} theme`}
        onClick={handleThemeChange}
      >
        {iconByTheme[theme]}
      </button>
    </nav>
  );
}
