export interface Session {
  id: string;
  user: string;
  device: string;
  ip: string;
  location: string;
  started: string;
  current: boolean;
}
