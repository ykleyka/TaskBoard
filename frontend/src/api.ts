import type {
  ApiErrorPayload,
  AsyncTaskMetricsResponse,
  AsyncTaskStatusResponse,
  AsyncTaskSubmissionResponse,
  AuthResponse,
  CommentResponse,
  DashboardResponse,
  Priority,
  ProjectDetailsResponse,
  ProjectResponse,
  ProjectRole,
  ProjectSummaryReportResponse,
  ProjectUserSummaryResponse,
  TagResponse,
  TaskDetailsResponse,
  TaskResponse,
  TaskStatus,
  UserResponse
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

let token: string | null = null;
let csrfToken: string | null = null;
let csrfHeaderName = "X-XSRF-TOKEN";

export function setApiToken(nextToken: string | null) {
  if (token !== nextToken) {
    resetCsrfToken();
  }
  token = nextToken;
}

export class ApiError extends Error {
  status: number;
  payload: ApiErrorPayload | null;

  constructor(status: number, payload: ApiErrorPayload | null, fallback: string) {
    super(payload?.message ?? fallback);
    this.status = status;
    this.payload = payload;
  }
}

type JsonBody = Record<string, unknown> | Array<Record<string, unknown>>;
type CsrfResponse = {
  headerName: string;
  token: string;
};

const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

function isUnsafeMethod(method?: string) {
  return UNSAFE_METHODS.has((method ?? "GET").toUpperCase());
}

function resetCsrfToken() {
  csrfToken = null;
  csrfHeaderName = "X-XSRF-TOKEN";
}

async function ensureCsrfToken() {
  if (csrfToken) {
    return;
  }

  const response = await fetch(`${API_BASE}/api/auth/csrf`, {
    method: "GET",
    headers: { Accept: "application/json" },
    credentials: "include"
  });

  if (!response.ok) {
    throw new ApiError(response.status, null, "Could not load CSRF token");
  }

  const body = (await response.json()) as CsrfResponse;
  csrfHeaderName = body.headerName;
  csrfToken = body.token;
}

async function sendRequest(
  path: string,
  options: RequestInit & { bodyJson?: JsonBody },
  unsafe: boolean
) {
  if (unsafe) {
    await ensureCsrfToken();
  }

  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.bodyJson !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (unsafe && csrfToken) {
    headers.set(csrfHeaderName, csrfToken);
  }

  return fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: "include",
    body: options.bodyJson === undefined ? options.body : JSON.stringify(options.bodyJson)
  });
}

async function request<T>(path: string, options: RequestInit & { bodyJson?: JsonBody } = {}) {
  const unsafe = isUnsafeMethod(options.method);
  let response = await sendRequest(path, options, unsafe);

  if (unsafe && response.status === 403) {
    resetCsrfToken();
    response = await sendRequest(path, options, unsafe);
  }

  if (!response.ok) {
    let payload: ApiErrorPayload | null = null;
    try {
      payload = (await response.json()) as ApiErrorPayload;
    } catch {
      payload = null;
    }
    throw new ApiError(response.status, payload, response.statusText);
  }

  if (response.status === 204) {
    return null as T;
  }

  return (await response.json()) as T;
}

function cleanBody<T extends Record<string, unknown>>(body: T): T {
  return Object.fromEntries(
    Object.entries(body).filter(([, value]) => value !== "" && value !== undefined)
  ) as T;
}

export const api = {
  login: (login: string, password: string) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      bodyJson: { login, password }
    }),

  register: (body: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }) =>
    request<AuthResponse>("/api/auth/register", {
      method: "POST",
      bodyJson: body
    }),

  me: () => request<UserResponse>("/api/auth/me"),

  patchUser: (
    id: number,
    body: Partial<{
      username: string;
      email: string;
      password: string;
      firstName: string;
      lastName: string;
    }>
  ) =>
    request<UserResponse>(`/api/users/${id}`, {
      method: "PATCH",
      bodyJson: cleanBody(body)
    }),

  dashboard: () => request<DashboardResponse>("/api/dashboard"),

  projects: () => request<ProjectResponse[]>("/api/projects?size=100&sort=updatedAt,desc"),

  project: (id: number) => request<ProjectDetailsResponse>(`/api/projects/${id}`),

  createProject: (body: { name: string; description?: string }) =>
    request<ProjectResponse>("/api/projects", {
      method: "POST",
      bodyJson: cleanBody(body)
    }),

  patchProject: (id: number, body: { name?: string; description?: string }) =>
    request<ProjectResponse>(`/api/projects/${id}`, {
      method: "PATCH",
      bodyJson: cleanBody(body)
    }),

  deleteProject: (id: number) =>
    request<ProjectResponse>(`/api/projects/${id}`, { method: "DELETE" }),

  users: () => request<UserResponse[]>("/api/users?size=200&sort=username,asc"),

  addMember: (projectId: number, userId: number, role: ProjectRole) =>
    request<ProjectUserSummaryResponse>(`/api/projects/${projectId}/members`, {
      method: "POST",
      bodyJson: { userId, role }
    }),

  updateMember: (projectId: number, userId: number, role: ProjectRole) =>
    request<ProjectUserSummaryResponse>(`/api/projects/${projectId}/members/${userId}`, {
      method: "PUT",
      bodyJson: { role }
    }),

  removeMember: (projectId: number, userId: number) =>
    request<ProjectUserSummaryResponse>(`/api/projects/${projectId}/members/${userId}`, {
      method: "DELETE"
    }),

  tasks: () => request<TaskResponse[]>("/api/tasks?size=200&sort=id,asc"),

  task: (id: number) => request<TaskDetailsResponse>(`/api/tasks/${id}`),

  createTask: (body: {
    title: string;
    description: string;
    projectId: number;
    assigneeId?: number;
    status?: TaskStatus;
    priority: Priority;
    dueDate?: string | null;
  }) =>
    request<TaskResponse>("/api/tasks", {
      method: "POST",
      bodyJson: cleanBody(body)
    }),

  updateTask: (
    id: number,
    body: {
      title: string;
      description: string;
      projectId: number;
      assigneeId?: number | null;
      status: TaskStatus;
      priority: Priority;
      dueDate?: string | null;
    }
  ) =>
    request<TaskResponse>(`/api/tasks/${id}`, {
      method: "PUT",
      bodyJson: cleanBody(body)
    }),

  patchTask: (
    id: number,
    body: Partial<{
      title: string;
      description: string;
      projectId: number;
      assigneeId: number;
      status: TaskStatus;
      priority: Priority;
      dueDate: string;
    }>
  ) =>
    request<TaskResponse>(`/api/tasks/${id}`, {
      method: "PATCH",
      bodyJson: cleanBody(body)
    }),

  deleteTask: (id: number) => request<TaskResponse>(`/api/tasks/${id}`, { method: "DELETE" }),

  tags: () => request<TagResponse[]>("/api/tags?size=100&sort=name,asc"),

  createTag: (name: string) =>
    request<TagResponse>("/api/tags", {
      method: "POST",
      bodyJson: { name }
    }),

  assignTag: (taskId: number, tagId: number) =>
    request<TagResponse>(`/api/tasks/${taskId}/tags/${tagId}`, { method: "POST" }),

  removeTag: (taskId: number, tagId: number) =>
    request<TagResponse>(`/api/tasks/${taskId}/tags/${tagId}`, { method: "DELETE" }),

  createComment: (taskId: number, text: string) =>
    request<CommentResponse>(`/api/tasks/${taskId}/comments`, {
      method: "POST",
      bodyJson: { text }
    }),

  updateComment: (commentId: number, text: string) =>
    request<CommentResponse>(`/api/comments/${commentId}`, {
      method: "PUT",
      bodyJson: { text }
    }),

  deleteComment: (commentId: number) =>
    request<CommentResponse>(`/api/comments/${commentId}`, { method: "DELETE" }),

  startReport: (projectId: number) =>
    request<AsyncTaskSubmissionResponse>(`/api/projects/${projectId}/summary-report`, {
      method: "POST"
    }),

  reportStatus: (asyncTaskId: string) =>
    request<AsyncTaskStatusResponse<ProjectSummaryReportResponse>>(
      `/api/async-tasks/${asyncTaskId}`
    ),

  asyncMetrics: () => request<AsyncTaskMetricsResponse>("/api/async-tasks/metrics")
};
