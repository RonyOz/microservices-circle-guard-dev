import { Platform } from 'react-native';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

const mockLocalStorage: Record<string, string> = {};

beforeEach(() => {
  Object.keys(mockLocalStorage).forEach(k => delete mockLocalStorage[k]);
  (Platform as any).OS = 'web';
  (global as any).localStorage = {
    getItem: jest.fn((k: string) => mockLocalStorage[k] ?? null),
    setItem: jest.fn((k: string, v: string) => { mockLocalStorage[k] = v; }),
    removeItem: jest.fn((k: string) => { delete mockLocalStorage[k]; }),
  };
});

describe('storage web fallback', () => {
  it('uses localStorage.getItem on web', async () => {
    const { storage } = await import('@/utils/storage');
    mockLocalStorage['test-key'] = 'test-value';
    const val = await storage.getItem('test-key');
    expect(val).toBe('test-value');
  });

  it('uses localStorage.setItem on web', async () => {
    const { storage } = await import('@/utils/storage');
    await storage.setItem('foo', 'bar');
    expect(mockLocalStorage['foo']).toBe('bar');
  });

  it('uses localStorage.removeItem on web', async () => {
    mockLocalStorage['baz'] = 'qux';
    const { storage } = await import('@/utils/storage');
    await storage.deleteItem('baz');
    expect(mockLocalStorage['baz']).toBeUndefined();
  });
});

describe('storage native fallback', () => {
  beforeEach(() => {
    (Platform as any).OS = 'ios';
  });

  it('returns null when SecureStore returns null', async () => {
    const SecureStore = require('expo-secure-store');
    SecureStore.getItemAsync.mockResolvedValue(null);

    const { storage } = await import('@/utils/storage');
    const val = await storage.getItem('missing-key');
    expect(val).toBeNull();
  });
});
