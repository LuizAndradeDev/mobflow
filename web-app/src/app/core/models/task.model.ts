export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Task {
  id: string;
  listId: string;
  workspaceId: string;
  title: string;
  description: string | null;
  priority: TaskPriority;
  assigneeAuthId: string | null;
  assigneeDisplayName: string | null;
  assigneeAvatarUrl: string | null;
  createdByAuthId: string;
  dueDate: string | null;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskList {
  id: string;
  boardId: string;
  name: string;
  position: number;
  tasks: Task[];
  createdAt: string;
  updatedAt: string;
}

export interface Board {
  id: string;
  workspaceId: string;
  name: string;
  color: string;
  position: number;
  lists: TaskList[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateBoardRequest {
  name: string;
  color?: string;
}

export interface UpdateBoardRequest {
  name?: string;
  color?: string;
}

export interface CreateTaskListRequest {
  name: string;
}

export interface UpdateTaskListRequest {
  name: string;
}

export interface ReorderListsRequest {
  orderedIds: string[];
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority?: TaskPriority;
  assigneeAuthId?: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  assigneeAuthId?: string;
  dueDate?: string;
}

export interface MoveTaskRequest {
  targetListId: string;
  position: number;
}

// ---- Workspace Summary (masonry overview) ----

export interface TaskCardPreview {
  id: string;
  title: string;
  priority: TaskPriority;
  dueDate: string | null;
  assigneeAvatarUrl: string | null;
}

export interface ListSummary {
  id: string;
  name: string;
  position: number;
  taskCount: number;
  previewTasks: TaskCardPreview[];
}

export interface BoardSummary {
  id: string;
  name: string;
  color: string;
  position: number;
  lists: ListSummary[];
}

export interface WorkspaceSummary {
  workspaceId: string;
  boards: BoardSummary[];
}
