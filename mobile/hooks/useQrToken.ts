import { useState, useEffect } from 'react';
import { AUTH_BASE_URL } from '@/constants/Config';
import { useAuth } from './useAuth';

/**
 * Hook to fetch and rotate short-lived Campus Entry QR tokens.
 * Implements Story 2.2: Rotating Token logic.
 */
export const useQrToken = (anonymousId: string | null) => {
  const { token: authToken } = useAuth();
  const [token, setToken] = useState<string | null>(null);
  const [timeLeft, setTimeLeft] = useState(60);

  useEffect(() => {
    if (!anonymousId || !authToken) return;

    fetchToken();
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          fetchToken();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [anonymousId, authToken]);

  const fetchToken = async () => {
    try {
      const res = await fetch(`${AUTH_BASE_URL}/api/v1/auth/qr/generate`, {
        headers: { Authorization: `Bearer ${authToken}` },
      });
      if (!res.ok) {
        throw new Error(`QR generation failed: ${res.status}`);
      }
      const data = await res.json();
      setToken(data.qrToken);
      setTimeLeft(Number(data.expiresIn) || 60);
    } catch (e) {
      console.error('QR Fetch Failed', e);
    }
  };

  return { token, timeLeft };
};
