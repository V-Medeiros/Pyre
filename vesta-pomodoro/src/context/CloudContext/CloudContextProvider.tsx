import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  backupLocalData,
  CLOUD_ENABLED,
  flushCloudOutbox,
  hydrateFromCloud,
  importLocalData,
  isCloudConnected,
  loginCloud,
  logoutCloud,
  markCloudConnected,
  refreshCloud,
  registerCloud,
  type CloudUser,
} from '../../api/cloud';
import { CloudContext, type CloudStatus } from './CloudContext';

type Props = { children: ReactNode };

export function CloudContextProvider({ children }: Props) {
  const [user, setUser] = useState<CloudUser | null>(null);
  const [status, setStatus] = useState<CloudStatus>('loading');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      if (!CLOUD_ENABLED) {
        setStatus('anonymous');
        return;
      }
      const restored = await refreshCloud();
      if (cancelled) return;
      if (!restored) {
        setStatus('anonymous');
        return;
      }
      setUser(restored);
      if (isCloudConnected(restored.id)) {
        await flushCloudOutbox();
        await hydrateFromCloud(restored.id);
      }
      if (!cancelled) setStatus('connected');
    })().catch((reason: unknown) => {
      if (!cancelled) {
        setError(reason instanceof Error ? reason.message : 'Cloud restore failed.');
        setStatus('error');
      }
    });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (status !== 'connected') return;
    let running = false;
    async function reconcile() {
      if (running) return;
      running = true;
      try {
        if (await flushCloudOutbox()) await hydrateFromCloud(user?.id);
      } catch {
        // Local state remains available; the next online event retries safely.
      } finally {
        running = false;
      }
    }
    function visibility() {
      if (document.visibilityState === 'visible') void reconcile();
    }
    window.addEventListener('online', reconcile);
    document.addEventListener('visibilitychange', visibility);
    return () => {
      window.removeEventListener('online', reconcile);
      document.removeEventListener('visibilitychange', visibility);
    };
  }, [status, user?.id]);

  const finishConnection = useCallback(async (nextUser: CloudUser, importData: boolean) => {
    if (importData) await importLocalData(nextUser.id);
    else backupLocalData();
    await hydrateFromCloud(nextUser.id);
    markCloudConnected(nextUser.id);
    setUser(nextUser);
    setStatus('connected');
  }, []);

  const signIn = useCallback(async (email: string, password: string, importData: boolean) => {
    setStatus('syncing');
    setError(null);
    try {
      await finishConnection(await loginCloud(email, password), importData);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Sign in failed.');
      setStatus('anonymous');
      throw reason;
    }
  }, [finishConnection]);

  const register = useCallback(async (email: string, password: string, displayName: string,
                                      importData: boolean) => {
    setStatus('syncing');
    setError(null);
    try {
      await finishConnection(await registerCloud({ email, password, displayName }), importData);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Registration failed.');
      setStatus('anonymous');
      throw reason;
    }
  }, [finishConnection]);

  const signOut = useCallback(async () => {
    setStatus('syncing');
    setError(null);
    if (!await flushCloudOutbox()) {
      setStatus('connected');
      setError('Some changes are still waiting to sync. Try again while online.');
      throw new Error('Pending cloud changes');
    }
    await logoutCloud();
    setUser(null);
    setStatus('anonymous');
  }, []);

  const syncNow = useCallback(async () => {
    setStatus('syncing');
    setError(null);
    try {
      if (!await flushCloudOutbox()) throw new Error('Changes could not be uploaded.');
      await hydrateFromCloud(user?.id);
      setStatus('connected');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Sync failed.');
      setStatus('connected');
      throw reason;
    }
  }, [user]);

  const value = useMemo(() => ({
    user, status, error, signIn, register, signOut, syncNow,
  }), [error, register, signIn, signOut, status, syncNow, user]);

  if (status === 'loading') {
    return <div className='cloud-loading' role='status'>Lighting Vesta…</div>;
  }

  return <CloudContext.Provider value={value}>{children}</CloudContext.Provider>;
}
