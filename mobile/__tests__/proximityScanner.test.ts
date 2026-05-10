jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

describe('ProximityScanner', () => {
  it('resolves with an array of peers after scan', async () => {
    const { ProximityScanner } = await import('@/utils/proximityScanner');
    const peers = await ProximityScanner.scanForPeers(10);
    expect(Array.isArray(peers)).toBe(true);
    expect(peers.length).toBeGreaterThanOrEqual(1);
  });

  it('each peer has anonymousId, rssi, and timestamp fields', async () => {
    const { ProximityScanner } = await import('@/utils/proximityScanner');
    const peers = await ProximityScanner.scanForPeers(10);
    for (const peer of peers) {
      expect(peer).toHaveProperty('anonymousId');
      expect(peer).toHaveProperty('rssi');
      expect(peer).toHaveProperty('timestamp');
      expect(typeof peer.rssi).toBe('number');
    }
  });
});
