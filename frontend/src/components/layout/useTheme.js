import { useCallback, useEffect, useState } from 'react';

const KEY = 'edusphere-theme';

export function useTheme() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'));
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
  }, [dark]);
  const toggle = useCallback(() => {
    setDark((d) => {
      const next = !d;
      try { localStorage.setItem(KEY, next ? 'dark' : 'light'); } catch { /* storage may be unavailable */ }
      return next;
    });
  }, []);
  return { dark, toggle };
}

export function useOnlineStatus() {
  const [online, setOnline] = useState(typeof navigator === 'undefined' ? true : navigator.onLine);
  useEffect(() => {
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    return () => { window.removeEventListener('online', on); window.removeEventListener('offline', off); };
  }, []);
  return online;
}
