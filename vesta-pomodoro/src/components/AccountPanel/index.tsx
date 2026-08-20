import { CloudIcon, LogOutIcon, RefreshCwIcon, UserRoundIcon, XIcon } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { hasLocalVestaData } from '../../api/cloud';
import { useCloudContext } from '../../context/CloudContext/UseCloudContext';
import styles from './style.module.css';

type Props = { onClose: () => void };

export function AccountPanel({ onClose }: Props) {
  const cloud = useCloudContext();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [importData, setImportData] = useState(hasLocalVestaData);
  const [localError, setLocalError] = useState<string | null>(null);
  const busy = cloud.status === 'syncing';

  useEffect(() => {
    function keydown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !busy) onClose();
    }
    window.addEventListener('keydown', keydown);
    return () => window.removeEventListener('keydown', keydown);
  }, [busy, onClose]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLocalError(null);
    try {
      if (mode === 'login') await cloud.signIn(email, password, importData);
      else await cloud.register(email, password, displayName, importData);
    } catch (reason) {
      setLocalError(reason instanceof Error ? reason.message : 'Account request failed.');
    }
  }

  return (
    <div className={styles.overlay} onMouseDown={(event) => {
      if (event.target === event.currentTarget && !busy) onClose();
    }}>
      <section className={styles.dialog} role='dialog' aria-modal='true' aria-labelledby='account-title'>
        <header className={styles.header}>
          <div className={styles.titleIcon}><UserRoundIcon /></div>
          <div>
            <span className={styles.eyebrow}>Cloud continuity</span>
            <h2 id='account-title'>{cloud.user ? 'Your account' : 'Vesta account'}</h2>
          </div>
          <button type='button' className={styles.close} onClick={onClose}
                  disabled={busy} aria-label='Close account panel'><XIcon /></button>
        </header>

        {cloud.user ? (
          <div className={styles.connected}>
            <CloudIcon />
            <div><strong>{cloud.user.displayName || cloud.user.email}</strong><small>{cloud.user.email}</small></div>
            <span>Connected</span>
          </div>
        ) : (
          <form className={styles.form} onSubmit={submit}>
            <div className={styles.tabs}>
              <button type='button' className={mode === 'login' ? styles.activeTab : ''}
                      onClick={() => setMode('login')}>Sign in</button>
              <button type='button' className={mode === 'register' ? styles.activeTab : ''}
                      onClick={() => setMode('register')}>Create account</button>
            </div>
            {mode === 'register' && <label>Display name<input value={displayName} maxLength={80}
              autoComplete='name' onChange={(event) => setDisplayName(event.target.value)} /></label>}
            <label>Email<input type='email' required value={email} autoComplete='email'
              onChange={(event) => setEmail(event.target.value)} /></label>
            <label>Password<input type='password' required minLength={10} maxLength={72}
              value={password} autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              onChange={(event) => setPassword(event.target.value)} /></label>
            <label className={styles.importChoice}>
              <input type='checkbox' checked={importData}
                onChange={(event) => setImportData(event.target.checked)} />
              <span><strong>Bring this browser’s Vesta data</strong>
                <small>If disabled, a local JSON backup is kept before cloud data loads.</small></span>
            </label>
            {(localError || cloud.error) && <p className={styles.error} role='alert'>{localError || cloud.error}</p>}
            <button className={styles.primary} type='submit' disabled={busy}>
              {busy ? 'Connecting…' : mode === 'login' ? 'Sign in and sync' : 'Create and sync'}
            </button>
          </form>
        )}

        {cloud.user && <div className={styles.actions}>
          {(localError || cloud.error) && <p className={styles.error} role='alert'>{localError || cloud.error}</p>}
          <button type='button' onClick={() => void cloud.syncNow().catch(() => undefined)} disabled={busy}>
            <RefreshCwIcon /> Sync now
          </button>
          <button type='button' className={styles.danger}
            onClick={() => void cloud.signOut().catch(() => undefined)} disabled={busy}>
            <LogOutIcon /> Sign out
          </button>
        </div>}
      </section>
    </div>
  );
}

