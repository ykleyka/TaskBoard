export type ProjectRole = "OWNER" | "MANAGER" | "MEMBER";
export type TaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED";
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type AsyncTaskStatus = "SUBMITTED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  tokenType: "Bearer";
  accessToken: string;
  expiresAt: string;
  user: UserResponse;
}

export interface ProjectResponse {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectUserSummaryResponse {
  id: number;
  username: string;
  role: ProjectRole;
}

export interface ProjectTaskSummaryResponse {
  id: number;
  title: string;
  status: TaskStatus;
  priority: Priority;
  creatorId: number | null;
  creatorUsername: string | null;
  assigneeId: number | null;
  assigneeUsername: string | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  tags: { id: number; name: string }[];
}

export interface ProjectDetailsResponse extends ProjectResponse {
  membersCount: number;
  tasksCount: number;
  completedTasksCount: number;
  users: ProjectUserSummaryResponse[];
  tasks: ProjectTaskSummaryResponse[];
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: Priority;
  projectId: number | null;
  projectName: string | null;
  creatorId: number | null;
  creatorUsername: string | null;
  assigneeId: number | null;
  assigneeUsername: string | null;
  dueDate: string | null;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TaskDetailsResponse {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: Priority;
  projectId: number | null;
  creatorId: number | null;
  creatorUsername: string | null;
  assigneeId: number | null;
  assigneeUsername: string | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  tags: { id: number; name: string }[];
  comments: {
    id: number;
    text: string;
    authorId: number | null;
    authorUsername: string | null;
    createdAt: string;
    updatedAt: string | null;
  }[];
}

export interface TagResponse {
  id: number;
  name: string;
  usageCount: number;
}

export interface CommentResponse {
  id: number;
  text: string;
  taskId: number | null;
  authorId: number | null;
  authorUsername: string | null;
  authorFullName: string | null;
  edited: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export interface DashboardResponse {
  totalProjects: number;
  totalTasks: number;
  activeTasks: number;
  completedTasks: number;
  overdueTasks: number;
  dueTodayTasks: number;
  collaboratorsCount: number;
  recentProjects: ProjectResponse[];
  upcomingTasks: TaskResponse[];
}

export interface ProjectSummaryReportResponse {
  projectId: number;
  projectName: string;
  projectDescription: string | null;
  generatedAt: string;
  membersCount: number;
  tasksCount: number;
  completedTasksCount: number;
  overdueTasksCount: number;
  unassignedTasksCount: number;
  highPriorityTasksCount: number;
  nearestDueDate: string | null;
  tasksByStatus: Record<TaskStatus, number>;
  members: ProjectUserSummaryResponse[];
}

export interface AsyncTaskSubmissionResponse {
  asyncTaskId: string;
  status: AsyncTaskStatus;
  createdAt: string;
}

export interface AsyncTaskStatusResponse<T = unknown> extends AsyncTaskSubmissionResponse {
  startedAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
  result: T | null;
}

export interface ApiErrorPayload {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errors: { field: string; message: string; rejectedValue: string | null }[];
}
