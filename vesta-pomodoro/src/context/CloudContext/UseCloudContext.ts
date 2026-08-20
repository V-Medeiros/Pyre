import { useContext } from 'react';
import { CloudContext } from './CloudContext';

export function useCloudContext() {
  const value = useContext(CloudContext);
  if (!value) throw new Error('useCloudContext must be used inside CloudContextProvider');
  return value;
}

