import { createContext } from 'react';
import type { CloudUser } from '../../api/cloud';

export type CloudStatus = 'loading' | 'anonymous' | 'connected' | 'syncing' | 'error';

export type CloudContextValue = {
  user: CloudUser | null;
  status: CloudStatus;
  error: string | null;
  signIn: (email: string, password: string, importData: boolean) => Promise<void>;
  register: (email: string, password: string, displayName: string,
             importData: boolean) => Promise<void>;
  signOut: () => Promise<void>;
  syncNow: () => Promise<void>;
};

export const CloudContext = createContext<CloudContextValue | null>(null);

