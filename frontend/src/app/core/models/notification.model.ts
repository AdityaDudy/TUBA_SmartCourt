export interface Notification {
  id: string;
  userId?: number;
  title: string;
  message: string;
  type: string;
  link?: string;
  read: boolean;
  createdAt?: string;
}

