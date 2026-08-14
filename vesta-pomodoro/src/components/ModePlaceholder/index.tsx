import { Clock3Icon } from 'lucide-react';
import type { AppMode } from '../../Models/AppMode';
import styles from './style.module.css';

type ModePlaceholderProps = {
  mode: Extract<AppMode, 'timer'>;
};

const MODE_LABELS = {
  timer: 'Timer',
} as const;

export function ModePlaceholder({ mode }: ModePlaceholderProps) {
  const title = MODE_LABELS[mode];

  return (
    <section className={styles.page} aria-labelledby='mode-title'>
      <Clock3Icon className={styles.icon} aria-hidden='true' />
      <h2 id='mode-title'>{title}</h2>
      <p className={styles.status}>Page in progress</p>
    </section>
  );
}
