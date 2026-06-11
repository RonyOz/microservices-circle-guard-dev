import { renderHook, act } from '@testing-library/react-native';
import { useQrToken } from './useQrToken';

jest.mock('./useAuth', () => ({
  useAuth: () => ({
    token: 'jwt-token',
    anonymousId: 'test-id',
    isLoading: false,
    enroll: jest.fn(),
    logout: jest.fn(),
  }),
}));

describe('useQrToken', () => {
  const fetchMock = jest.fn();

  beforeEach(() => {
    jest.useFakeTimers();
    let counter = 0;
    fetchMock.mockImplementation(async () => ({
      ok: true,
      json: async () => ({ qrToken: `qr-${++counter}`, expiresIn: '60' }),
    }));
    global.fetch = fetchMock as unknown as typeof fetch;
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  test('should fetch a token and start the 60s timer when anonymousId is present', async () => {
    const { result } = renderHook(() => useQrToken('test-id'));

    await act(async () => {
      await Promise.resolve();
    });

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/auth/qr/generate'),
      expect.objectContaining({
        headers: { Authorization: 'Bearer jwt-token' },
      })
    );
    expect(result.current.token).toBe('qr-1');
    expect(result.current.timeLeft).toBe(60);
  });

  test('should not fetch if anonymousId is null', async () => {
    const { result } = renderHook(() => useQrToken(null));

    await act(async () => {
      await Promise.resolve();
    });

    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.token).toBeNull();
  });

  test('should decrement timer every second', async () => {
    const { result } = renderHook(() => useQrToken('test-id'));

    await act(async () => {
      await Promise.resolve();
    });

    act(() => {
      jest.advanceTimersByTime(1000);
    });

    expect(result.current.timeLeft).toBe(59);
  });

  test('should rotate token and reset timer when it reaches 0', async () => {
    const { result } = renderHook(() => useQrToken('test-id'));

    await act(async () => {
      await Promise.resolve();
    });
    const initialToken = result.current.token;

    await act(async () => {
      jest.advanceTimersByTime(60000);
      await Promise.resolve();
    });

    expect(result.current.token).not.toBe(initialToken);
    expect(result.current.timeLeft).toBe(60);
  });
});
