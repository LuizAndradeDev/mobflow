import { TaskAnalytics } from '../models/task.model';

export const EMPTY_TASK_ANALYTICS: TaskAnalytics = {
  totalTasks: 0,
  completedTasks: 0,
  createdTasks: 0,
  assignedTasks: 0,
  overdueTasks: 0,
};

export interface AnalyticsSummary {
  total: number;
  done: number;
  inProgress: number;
  overdue: number;
  pct: number;
}

export function buildAnalyticsSummary(total: number, completed: number, overdue: number): AnalyticsSummary {
  return {
    total,
    done: completed,
    inProgress: calculateInProgressTasks(total, completed, overdue),
    overdue,
    pct: calculateCompletionPercentage(completed, total),
  };
}

export function calculateInProgressTasks(total: number, completed: number, overdue: number): number {
  return Math.max(0, total - completed - overdue);
}

export function calculateCompletionPercentage(completed: number, total: number): number {
  return total > 0 ? Math.round((completed / total) * 100) : 0;
}
