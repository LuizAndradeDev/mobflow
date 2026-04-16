import { BoardSummary, TaskPriority } from '../models/task.model';

const priorityClassMap: Record<TaskPriority, string> = {
  URGENT: 'bg-red-500',
  HIGH: 'bg-orange-400',
  MEDIUM: 'bg-blue-400',
  LOW: 'bg-gray-300 dark:bg-gray-600',
};

export function getBoardTaskCount(board: BoardSummary): number {
  return board.lists.reduce((taskCount, list) => taskCount + list.taskCount, 0);
}

export function getPriorityDotClass(priority: TaskPriority): string {
  return priorityClassMap[priority];
}
