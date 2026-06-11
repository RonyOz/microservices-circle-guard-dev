/**
 * Global Configuration for CircleGuard Mobile.
 *
 * Production web builds use same-origin URLs ('' or EXPO_PUBLIC_API_ORIGIN);
 * nginx routes each /api/v1/* prefix to its backing service. In development
 * (__DEV__) each service is reached directly on its local port.
 */
const ORIGIN = process.env.EXPO_PUBLIC_API_ORIGIN ?? '';

const base = (port: number) => (__DEV__ ? `http://localhost:${port}` : ORIGIN);

export const AUTH_BASE_URL = base(8081); // Auth Service
export const IDENTITY_BASE_URL = base(8082); // Identity Service
export const GATEWAY_BASE_URL = base(8083); // Gateway Service
export const FORM_BASE_URL = base(8084); // Form Service
export const NOTIFICATION_BASE_URL = base(8085); // Notification Service
export const PROMOTION_BASE_URL = base(8086); // Promotion Service
export const DASHBOARD_BASE_URL = base(8087); // Dashboard Service
export const FILE_BASE_URL = base(8088); // File Service
