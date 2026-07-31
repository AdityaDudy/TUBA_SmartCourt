export interface Task {
  id: string;
  matterId?: number;
  title: string;
  matter: string;
  matterTitle?: string;
  priority: TaskPriority;
  due: string;
  dueDate?: string;
  assign: string;
  assignedTo?: string;
  status: TaskStatus;
  done: boolean;
  type: string;
  notes?: string;
  createdBy?: string;
}

export type TaskPriority = 'Urgent' | 'High' | 'Medium' | 'Low';
export type TaskStatus =
  | 'To Do'
  | 'In Progress'
  | 'Under Review'
  | 'Done'
  | 'Overdue'
  | 'Due Today'
  | 'Open';
