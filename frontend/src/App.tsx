import {
    Activity,
    AlertTriangle,
    BarChart3,
    CheckCircle2,
    ChevronDown,
    Clock3,
    Edit3,
    FolderOpen,
    LayoutDashboard,
    Loader2,
    LogOut,
    MessageSquare,
    Plus,
    Shield,
    Sparkles,
    Tag,
    Trash2,
    UserCircle,
    UserPlus,
    Users,
    X
} from "lucide-react";
import type { FormEvent, ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { ApiError, api, setApiToken } from "./api";
import type {
    AsyncTaskStatusResponse,
    AuthResponse,
    CommentResponse,
    DashboardResponse,
    Priority,
    ProjectDetailsResponse,
    ProjectResponse,
    ProjectRole,
    ProjectSummaryReportResponse,
    ProjectTaskSummaryResponse,
    TagResponse,
    TaskDetailsResponse,
    TaskStatus,
    UserResponse
} from "./types";

const TOKEN_KEY = "taskboard.accessToken";
const COMMENTS_PAGE_SIZE = 5;

const STATUSES: TaskStatus[] = ["TODO", "IN_PROGRESS", "COMPLETED"];
const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const ROLES: ProjectRole[] = ["OWNER", "MANAGER", "MEMBER"];

type View = "dashboard" | "board" | "reports" | "team" | "profile" | "taskDetails";
type Modal = "project" | "task" | "member" | "profile" | "deleteProject" | null;
type TaskDetailsMode = "view" | "edit";
type TaskEditForm = {
    title: string;
    description: string;
    status: TaskStatus;
    priority: Priority;
    assigneeId: string;
    dueDate: string;
};

type TaskEditTag = {
    id: number;
    name: string;
    isNew?: boolean;
};

const STATUS_LABELS: Record<TaskStatus, string> = {
    TODO: "К выполнению",
    IN_PROGRESS: "В работе",
    COMPLETED: "Готово"
};

const PRIORITY_LABELS: Record<Priority, string> = {
    LOW: "Низкий",
    MEDIUM: "Средний",
    HIGH: "Высокий",
    URGENT: "Срочный"
};

const ROLE_LABELS: Record<ProjectRole, string> = {
    OWNER: "Владелец",
    MANAGER: "Менеджер",
    MEMBER: "Участник"
};

const TEXT = {
    appTitle: "TaskBoard",
    brand: "TaskBoard",
    workspace: "TaskBoard",
    workspaceSubtitle: "Проекты",
    nav: {
        dashboard: "Обзор",
        board: "Проекты",
        reports: "Отчеты",
        team: "Команда",
        profile: "Профиль",
        settings: "Настройки",
        logout: "Выйти"
    },
    actions: {
        createTask: "Создать задачу",
        newTask: "Новая задача",
        createProject: "Создать проект",
        newProject: "Новый проект",
        addMember: "Добавить участника",
        assign: "Добавить",
        comment: "Отправить",
        deleteTask: "Удалить задачу",
        editProfile: "Редактировать",
        saveProfile: "Сохранить",
        cancel: "Отмена",
        project: "Проект",
        newReport: "Новый отчет",
        exportPdf: "Экспорт PDF"
    },
    common: {
        loadingWorkspace: "Загрузка рабочей области",
        dismiss: "Скрыть",
        close: "Закрыть",
        noDate: "Без даты",
        open: "Открыто",
        project: "Проект",
        taskboard: "TaskBoard",
        none: "Нет",
        active: "Активен",
        currentUser: "Вы",
        unknown: "Неизвестно",
        unassigned: "Без исполнителя",
        chooseProjectFirst: "Сначала выберите проект",
        noDescription: "Без описания",
        searchWorkspace: "Поиск по рабочей области",
        notifications: "Уведомления",
        asyncStatus: "Статус async",
        detected: "Обнаружено",
        clear: "Чисто"
    },
    auth: {
        introTitle: "Планируйте проекты без шума",
        introText: "Рабочая доска для задач, ролей, комментариев, тегов и отчетов по проектам.",
        loginTitle: "Вход",
        loginSubtitle: "Введите username или email.",
        registerTitle: "Регистрация",
        registerSubtitle: "Создайте аккаунт для работы с проектами.",
        loginTab: "Вход",
        registerTab: "Регистрация",
        loginField: "Username или email",
        username: "Username",
        email: "Email",
        firstName: "Имя",
        lastName: "Фамилия",
        password: "Пароль",
        submitLogin: "Войти",
        submitRegister: "Создать аккаунт"
    },
    dashboard: {
        eyebrow: "Обзор",
        title: "",
        loadingText: "Метрики рабочей области загружаются.",
        activeSummary: (activeTasks: number, totalProjects: number) => `${activeTasks} активных задач в ${totalProjects} проектах.`,
        totalProjects: "Проекты",
        activeTasks: "Активные задачи",
        dueToday: "Срок сегодня",
        collaborators: "Участники",
        recentProjects: "Последние проекты",
        recentSubtitle: "Проекты, доступные вам",
        upcomingTasks: "Ближайшие задачи",
        upcomingSubtitle: "Ближайшие сроки",
        emptyProjects: "Проектов пока нет",
        emptyUpcoming: "Ближайших задач нет"
    },
    board: {
        title: "Доска проекта",
        fallbackSubtitle: "Задачи по статусам",
        empty: "Создайте проект, чтобы начать"
    },
    reports: {
        title: "Отчеты",
        subtitle: "Метрики проекта и async-отчеты",
        completedTasks: "Готово",
        overdueTasks: "Просрочено",
        highPriority: "Высокий приоритет",
        projectSummary: "Сводка проекта",
        generateHint: "Создайте отчет, чтобы увидеть метрики",
        distribution: "Распределение задач по статусам",
        asyncRuntime: "Async выполнение",
        executionCounters: "Счетчики выполнения",
        projectSummaryRequests: "Запросы отчета",
        running: "В работе",
        completed: "Завершено",
        failed: "Ошибки",
        members: "Участники",
        unassigned: "Без исполнителя",
        nearestDueDate: "Ближайший срок"
    },
    team: {
        title: "Команда",
        subtitle: "Участники и роли проекта",
        totalMembers: "Участники",
        tasks: "Задачи",
        yourRole: "Ваша роль",
        name: "Имя",
        role: "Роль",
        status: "Статус",
        actions: "Действия",
        removeMember: "Удалить"
    },
    profile: {
        title: "Профиль",
        subtitle: "Данные аккаунта.",
        projects: "Проекты",
        activeTasks: "Активные задачи",
        collaborators: "Участники",
        identityData: "Профиль",
        userId: "ID пользователя",
        firstName: "Имя",
        lastName: "Фамилия",
        contactVector: "Контактные данные",
        primaryEmail: "Email",
        verified: "Проверен",
        accessLevel: "Уровень доступа",
        memberAccess: "Участник рабочей области",
        registrationTimestamp: "Дата регистрации",
        updatedAt: "Обновлен",
        uptimeStatus: "Состояние",
        operational: "В норме",
        accountSecurity: "Безопасность",
        accountSecurityText: "Профиль может редактировать только текущий пользователь.",
        passwordHint: "Оставьте пустым, чтобы не менять пароль"
    },
    forms: {
        projectName: "Название",
        projectDescription: "Описание",
        projectSelect: "Проект",
        chooseProject: "Выберите проект",
        taskTitle: "Заголовок",
        taskDescription: "Описание",
        assignee: "Исполнитель",
        priority: "Приоритет",
        dueDate: "Срок",
        chooseUser: "Выберите пользователя",
        role: "Роль",
        user: "Пользователь",
        assignTag: "Тег",
        addComment: "Комментарий"
    },
    modals: {
        newProject: "Новый проект",
        createTask: "Новая задача",
        addMember: "Добавить участника",
        editProfile: "Редактировать профиль",
        taskDetails: "Задача",
        comments: "Комментарии"
    }
} as const;

type Text = typeof TEXT;

function formatDate(value: string | null | undefined) {
    if (!value) {
        return TEXT.common.noDate;
    }
    return new Intl.DateTimeFormat("ru-RU", {
        month: "short",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function formatShortDate(value: string | null | undefined) {
    if (!value) {
        return TEXT.common.open;
    }
    return new Intl.DateTimeFormat("ru-RU", {
        month: "short",
        day: "2-digit"
    }).format(new Date(value));
}

function toInputDateTime(value: string | null | undefined) {
    if (!value) {
        return "";
    }
    const date = new Date(value);
    const offsetMinutes = date.getTimezoneOffset();
    const localDate = new Date(date.getTime() - offsetMinutes * 60_000);
    return localDate.toISOString().slice(0, 16);
}

function toIsoFromInput(value: string) {
    return value ? new Date(value).toISOString() : undefined;
}

function isOverdueTask(dueDate: string | null | undefined, status?: TaskStatus) {
    return Boolean(dueDate && status !== "COMPLETED" && new Date(dueDate).getTime() < Date.now());
}

function initials(name: string) {
    return name
        .split(/[\s._-]+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase())
        .join("");
}

function displayName(user: UserResponse | null) {
    if (!user) {
        return "User";
    }
    return `${user.firstName} ${user.lastName}`.trim() || user.username;
}

function welcomeText(user: UserResponse | null) {
    const name = displayName(user);
    return `С возвращением, ${name}`;
}

function errorMessage(error: unknown) {
    if (error instanceof ApiError) {
        const validation = error.payload?.errors
            ?.map((item) => `${item.field}: ${item.message}`)
            .join("; ");
        return validation || error.message;
    }
    if (error instanceof Error) {
        return error.message;
    }
    return "Unexpected error";
}

function isEditableRole(role: ProjectRole | null) {
    return role === "OWNER" || role === "MANAGER";
}

function canManageRole(role: ProjectRole | null) {
    return role === "OWNER";
}

export default function App() {
    const [accessToken, setAccessToken] = useState<string | null>(() =>
        localStorage.getItem(TOKEN_KEY)
    );
    const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);
    const [view, setView] = useState<View>("dashboard");
    const [taskDetailsBackView, setTaskDetailsBackView] = useState<View>("board");
    const [taskDetailsMode, setTaskDetailsMode] = useState<TaskDetailsMode>("view");
    const [busy, setBusy] = useState(Boolean(accessToken));
    const [loadingProject, setLoadingProject] = useState(false);
    const [notice, setNotice] = useState<string | null>(null);
    const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
    const [projects, setProjects] = useState<ProjectResponse[]>([]);
    const [users, setUsers] = useState<UserResponse[]>([]);
    const [tags, setTags] = useState<TagResponse[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [selectedProject, setSelectedProject] = useState<ProjectDetailsResponse | null>(null);
    const [modal, setModal] = useState<Modal>(null);
    const [activeTask, setActiveTask] = useState<TaskDetailsResponse | null>(null);
    const [report, setReport] =
        useState<AsyncTaskStatusResponse<ProjectSummaryReportResponse> | null>(null);
    const [projectFormMode, setProjectFormMode] = useState<"create" | "edit">("create");
    const [projectForm, setProjectForm] = useState({ name: "", description: "" });
    const [taskForm, setTaskForm] = useState({
        title: "",
        description: "",
        projectId: "",
        assigneeId: "",
        priority: "MEDIUM" as Priority,
        dueDate: "",
        tagIds: [] as string[],
        newTagName: ""
    });
    const [taskEditForm, setTaskEditForm] = useState<TaskEditForm>({
        title: "",
        description: "",
        status: "TODO",
        priority: "MEDIUM",
        assigneeId: "",
        dueDate: ""
    });
    const [taskEditTags, setTaskEditTags] = useState<TaskEditTag[]>([]);
    const [memberForm, setMemberForm] = useState({
        userId: "",
        role: "MEMBER" as ProjectRole
    });
    const [profileForm, setProfileForm] = useState({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: ""
    });
    const [deadlineDraft, setDeadlineDraft] = useState("");
    const [taskDetailError, setTaskDetailError] = useState<string | null>(null);
    const [commentText, setCommentText] = useState("");
    const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
    const [editingCommentText, setEditingCommentText] = useState("");
    const [taskComments, setTaskComments] = useState<CommentResponse[]>([]);
    const [commentsPage, setCommentsPage] = useState(0);
    const [hasMoreComments, setHasMoreComments] = useState(false);
    const [loadingMoreComments, setLoadingMoreComments] = useState(false);
    const [tagSelect, setTagSelect] = useState("");
    const [newTagName, setNewTagName] = useState("");

    const text = TEXT;
    const statusLabels = STATUS_LABELS;
    const priorityLabels = PRIORITY_LABELS;
    const roleLabels = ROLE_LABELS;


    useEffect(() => {
        setApiToken(accessToken);
        if (!accessToken) {
            setBusy(false);
            return;
        }
        void bootstrap();
    }, [accessToken]);

    useEffect(() => {
        if (!report || report.status === "COMPLETED" || report.status === "FAILED") {
            return;
        }
        const timer = window.setInterval(() => {
            void refreshReport(report.asyncTaskId);
        }, 1800);
        return () => window.clearInterval(timer);
    }, [report?.asyncTaskId, report?.status]);

    const currentRole = useMemo(() => {
        if (!selectedProject || !currentUser) {
            return null;
        }
        return selectedProject.users.find((member) => member.id === currentUser.id)?.role ?? null;
    }, [currentUser, selectedProject]);

    const projectMembers = selectedProject?.users ?? [];
    const availableUsers = users.filter(
        (user) => !projectMembers.some((member) => member.id === user.id)
    );
    const canEditSelectedProject = isEditableRole(currentRole);
    const canEditSelectedMembers = isEditableRole(currentRole);
    const canManageSelectedMembers = canManageRole(currentRole);
    const canEditActiveTask = Boolean(
        activeTask &&
        currentUser &&
        (isEditableRole(currentRole) || activeTask.creatorId === currentUser.id)
    );
    const canChangeActiveTaskStatus = Boolean(
        activeTask &&
        currentUser &&
        (
            isEditableRole(currentRole) ||
            activeTask.creatorId === currentUser.id ||
            activeTask.assigneeId === currentUser.id
        )
    );
    const canExtendActiveTaskDeadline = canEditActiveTask;

    async function bootstrap(preferredProjectId?: number) {
        setBusy(true);
        try {
            const [me, nextDashboard, nextProjects, nextUsers, nextTags] = await Promise.all([
                api.me(),
                api.dashboard(),
                api.projects(),
                api.users(),
                api.tags()
            ]);
            setCurrentUser(me);
            setDashboard(nextDashboard);
            setProjects(nextProjects);
            setUsers(nextUsers);
            setTags(nextTags);
            const nextProjectId = preferredProjectId ?? selectedProjectId ?? nextProjects[0]?.id ?? null;
            setSelectedProjectId(nextProjectId);
            if (nextProjectId) {
                await loadProject(nextProjectId);
            } else {
                setSelectedProject(null);
            }
        } catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                logout();
            } else {
                setNotice(errorMessage(error));
            }
        } finally {
            setBusy(false);
        }
    }

    async function loadProject(projectId: number) {
        setLoadingProject(true);
        try {
            const nextProject = await api.project(projectId);
            setSelectedProject(nextProject);
            setSelectedProjectId(projectId);
        } catch (error) {
            setNotice(errorMessage(error));
        } finally {
            setLoadingProject(false);
        }
    }

    async function refreshDashboard() {
        try {
            setDashboard(await api.dashboard());
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    function handleAuth(response: AuthResponse) {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        setApiToken(response.accessToken);
        setAccessToken(response.accessToken);
        setCurrentUser(response.user);
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        setApiToken(null);
        setAccessToken(null);
        setCurrentUser(null);
        setDashboard(null);
        setProjects([]);
        setSelectedProject(null);
        setSelectedProjectId(null);
        setReport(null);
    }

    function openCreateProjectModal() {
        setProjectFormMode("create");
        setProjectForm({ name: "", description: "" });
        setModal("project");
    }

    function openEditProjectModal() {
        if (!selectedProject) {
            return;
        }
        setProjectFormMode("edit");
        setProjectForm({
            name: selectedProject.name,
            description: selectedProject.description ?? ""
        });
        setModal("project");
    }

    async function submitProject(event: FormEvent) {
        event.preventDefault();
        try {
            if (projectFormMode === "edit" && selectedProjectId) {
                await api.patchProject(selectedProjectId, projectForm);
                setProjectForm({ name: "", description: "" });
                setProjectFormMode("create");
                setModal(null);
                await bootstrap(selectedProjectId);
                setView("board");
                return;
            }
            const created = await api.createProject(projectForm);
            setProjectForm({ name: "", description: "" });
            setProjectFormMode("create");
            setModal(null);
            await bootstrap(created.id);
            setView("board");
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function deleteProject() {
        if (!selectedProjectId) {
            return;
        }
        try {
            await api.deleteProject(selectedProjectId);
            setSelectedProject(null);
            setSelectedProjectId(null);
            setModal(null);
            await bootstrap();
            setView("board");
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function submitTask(event: FormEvent) {
        event.preventDefault();
        const projectId = Number(taskForm.projectId || selectedProjectId);
        if (!projectId) {
            setNotice(text.common.chooseProjectFirst);
            return;
        }
        try {
            const createdTask = await api.createTask({
                title: taskForm.title,
                description: taskForm.description,
                projectId,
                assigneeId: taskForm.assigneeId ? Number(taskForm.assigneeId) : undefined,
                priority: taskForm.priority,
                status: "TODO",
                dueDate: toIsoFromInput(taskForm.dueDate)
            });
            const tagIds = taskForm.tagIds.map(Number);
            if (taskForm.newTagName.trim()) {
                const tagName = taskForm.newTagName.trim();
                const existingTag = tags.find((tag) => tag.name.toLowerCase() === tagName.toLowerCase());
                if (existingTag) {
                    tagIds.push(existingTag.id);
                } else {
                    const createdTag = await api.createTag(tagName);
                    setTags((currentTags) =>
                        [...currentTags, createdTag].sort((a, b) => a.name.localeCompare(b.name))
                    );
                    tagIds.push(createdTag.id);
                }
            }
            await Promise.all([...new Set(tagIds)].map((tagId) => api.assignTag(createdTask.id, tagId)));
            setTaskForm({
                title: "",
                description: "",
                projectId: String(projectId),
                assigneeId: "",
                priority: "MEDIUM",
                dueDate: "",
                tagIds: [],
                newTagName: ""
            });
            setModal(null);
            await loadProject(projectId);
            await refreshDashboard();
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function submitMember(event: FormEvent) {
        event.preventDefault();
        if (!selectedProjectId || !memberForm.userId) {
            return;
        }
        try {
            await api.addMember(selectedProjectId, Number(memberForm.userId), memberForm.role);
            setMemberForm({ userId: "", role: "MEMBER" });
            setModal(null);
            await loadProject(selectedProjectId);
            await refreshDashboard();
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function updateMemberRole(userId: number, role: ProjectRole) {
        if (!selectedProjectId) {
            return;
        }
        try {
            await api.updateMember(selectedProjectId, userId, role);
            await loadProject(selectedProjectId);
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function removeMember(userId: number) {
        if (!selectedProjectId) {
            return;
        }
        try {
            await api.removeMember(selectedProjectId, userId);
            await loadProject(selectedProjectId);
            await refreshDashboard();
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    function canChangeTaskStatus(task: Pick<ProjectTaskSummaryResponse, "creatorId" | "assigneeId">) {
        return Boolean(
            currentUser &&
            (
                isEditableRole(currentRole) ||
                task.creatorId === currentUser.id ||
                task.assigneeId === currentUser.id
            )
        );
    }

    async function moveTask(task: ProjectTaskSummaryResponse, status: TaskStatus) {
        if (!selectedProjectId || task.status === status) {
            return;
        }
        if (!canChangeTaskStatus(task)) {
            setNotice("Вы можете менять статус только задач, назначенных вам, созданных вами, либо доступных по роли проекта.");
            return;
        }
        try {
            await api.patchTask(task.id, { status });
            await loadProject(selectedProjectId);
            await refreshDashboard();
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function changeActiveTaskStatus(status: TaskStatus) {
        if (!activeTask || activeTask.status === status) {
            return;
        }
        if (!canChangeActiveTaskStatus) {
            setTaskDetailError("Вы можете менять статус только задач, назначенных вам, созданных вами, либо доступных по роли проекта.");
            return;
        }
        const projectId = activeTask.projectId ?? selectedProjectId;
        try {
            await api.patchTask(activeTask.id, { status });
            setTaskDetailError(null);
            await openTask(activeTask.id);
            if (projectId) {
                await loadProject(projectId);
            }
            await refreshDashboard();
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function changeActiveTaskAssignee(value: string) {
        if (!activeTask) {
            return;
        }
        const projectId = activeTask.projectId ?? selectedProjectId;
        if (!projectId) {
            return;
        }
        const assigneeId = value ? Number(value) : null;
        try {
            if (assigneeId === null) {
                await api.updateTask(activeTask.id, {
                    title: activeTask.title,
                    description: activeTask.description,
                    projectId,
                    assigneeId: null,
                    status: activeTask.status,
                    priority: activeTask.priority,
                    dueDate: activeTask.dueDate
                });
            } else {
                await api.patchTask(activeTask.id, { assigneeId });
            }
            setTaskDetailError(null);
            await openTask(activeTask.id);
            await loadProject(projectId);
            await refreshDashboard();
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function extendActiveTaskDeadline() {
        if (!activeTask || !deadlineDraft) {
            return;
        }
        const nextDueDate = new Date(deadlineDraft);
        if (Number.isNaN(nextDueDate.getTime())) {
            setTaskDetailError(
                "Укажите корректный дедлайн."
            );
            return;
        }
        if (activeTask.dueDate && nextDueDate.getTime() <= new Date(activeTask.dueDate).getTime()) {
            setTaskDetailError(
                "Новый дедлайн должен быть позже текущего."
            );
            return;
        }
        const projectId = activeTask.projectId ?? selectedProjectId;
        try {
            await api.patchTask(activeTask.id, { dueDate: nextDueDate.toISOString() });
            setTaskDetailError(null);
            await openTask(activeTask.id);
            if (projectId) {
                await loadProject(projectId);
            }
            await refreshDashboard();
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function removeTask(taskId: number) {
        const projectId = selectedProjectId ?? activeTask?.projectId ?? null;
        try {
            await api.deleteTask(taskId);
            setTaskDetailError(null);
            setActiveTask(null);
            setTaskDetailsMode("view");
            setModal(null);
            setView("board");
            if (projectId) {
                await loadProject(projectId);
            }
            await refreshDashboard();
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function refreshActiveTask(taskId: number) {
        const task = await api.task(taskId);
        setActiveTask(task);
        return task;
    }

    function fillTaskEditForm(task: TaskDetailsResponse) {
        setTaskEditForm({
            title: task.title,
            description: task.description,
            status: task.status,
            priority: task.priority,
            assigneeId: task.assigneeId ? String(task.assigneeId) : "",
            dueDate: toInputDateTime(task.dueDate)
        });
        setTaskEditTags((task.tags ?? []).map((tag) => ({ id: tag.id, name: tag.name })));
        setTagSelect("");
        setNewTagName("");
    }

    function startTaskEdit() {
        if (!activeTask || !canEditActiveTask) {
            return;
        }
        fillTaskEditForm(activeTask);
        setTaskDetailError(null);
        setTaskDetailsMode("edit");
    }

    function cancelTaskEdit() {
        if (activeTask) {
            fillTaskEditForm(activeTask);
        }
        setTaskDetailError(null);
        setTaskDetailsMode("view");
    }

    async function applyTaskTagChanges(taskId: number) {
        if (!activeTask) {
            return;
        }

        const originalTagIds = new Set((activeTask.tags ?? []).map((tag) => tag.id));
        const desiredExistingTagIds = new Set(
            taskEditTags.filter((tag) => tag.id > 0).map((tag) => tag.id)
        );

        const tagIdsToRemove = [...originalTagIds].filter((tagId) => !desiredExistingTagIds.has(tagId));
        const tagIdsToAssign = [...desiredExistingTagIds].filter((tagId) => !originalTagIds.has(tagId));

        for (const tagId of tagIdsToRemove) {
            await api.removeTag(taskId, tagId);
        }

        for (const tagId of tagIdsToAssign) {
            await api.assignTag(taskId, tagId);
        }

        const newTagNames = [
            ...new Set(
                taskEditTags
                    .filter((tag) => tag.id < 0)
                    .map((tag) => tag.name.trim())
                    .filter(Boolean)
            )
        ];

        const createdTags: TagResponse[] = [];
        for (const tagName of newTagNames) {
            const existingTag = tags.find((tag) => tag.name.toLowerCase() === tagName.toLowerCase());
            const tag = existingTag ?? (await api.createTag(tagName));
            if (!existingTag) {
                createdTags.push(tag);
            }
            await api.assignTag(taskId, tag.id);
        }

        if (createdTags.length) {
            setTags((currentTags) =>
                [...currentTags, ...createdTags].sort((left, right) => left.name.localeCompare(right.name))
            );
        }
    }

    async function saveActiveTaskEdit() {
        if (!activeTask) {
            return;
        }

        const projectId = activeTask.projectId ?? selectedProjectId;
        try {
            const payload: Partial<{
                title: string;
                description: string;
                status: TaskStatus;
                priority: Priority;
                assigneeId: number;
                dueDate: string;
            }> = {
                title: taskEditForm.title,
                description: taskEditForm.description,
                status: taskEditForm.status,
                priority: taskEditForm.priority
            };

            if (taskEditForm.assigneeId) {
                payload.assigneeId = Number(taskEditForm.assigneeId);
            }

            if (taskEditForm.dueDate) {
                payload.dueDate = toIsoFromInput(taskEditForm.dueDate);
            }

            await api.patchTask(activeTask.id, payload);
            await applyTaskTagChanges(activeTask.id);
            const updatedTask = await refreshActiveTask(activeTask.id);
            fillTaskEditForm(updatedTask);
            setTaskDetailsMode("view");
            setTaskDetailError(null);

            if (projectId) {
                await loadProject(projectId);
            }
            await refreshDashboard();
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function loadFirstCommentsPage(taskId: number) {
        const firstPage = await api.comments(taskId, 0, COMMENTS_PAGE_SIZE);

        setTaskComments(firstPage);
        setCommentsPage(0);
        setHasMoreComments(firstPage.length === COMMENTS_PAGE_SIZE);
    }

    async function loadMoreComments() {
        if (!activeTask || loadingMoreComments) {
            return;
        }

        setLoadingMoreComments(true);
        try {
            const nextPage = commentsPage + 1;
            const nextComments = await api.comments(activeTask.id, nextPage, COMMENTS_PAGE_SIZE);

            setTaskComments((currentComments) => [...currentComments, ...nextComments]);
            setCommentsPage(nextPage);
            setHasMoreComments(nextComments.length === COMMENTS_PAGE_SIZE);
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        } finally {
            setLoadingMoreComments(false);
        }
    }

    async function openTask(taskId: number) {
        try {
            const task = await api.task(taskId);
            if (task.projectId && task.projectId !== selectedProjectId) {
                await loadProject(task.projectId);
            }
            setActiveTask(task);
            setDeadlineDraft(toInputDateTime(task.dueDate));
            setTaskDetailError(null);
            setCommentText("");
            setEditingCommentId(null);
            setEditingCommentText("");
            setTagSelect("");
            setNewTagName("");

            await loadFirstCommentsPage(task.id);
            fillTaskEditForm(task);
            setTaskDetailsMode("view");
            setTaskDetailsBackView(view === "taskDetails" ? taskDetailsBackView : view);
            setModal(null);
            setView("taskDetails");
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    function startCommentEdit(commentId: number, text: string) {
        setEditingCommentId(commentId);
        setEditingCommentText(text);
        setTaskDetailError(null);
    }

    function cancelCommentEdit() {
        setEditingCommentId(null);
        setEditingCommentText("");
    }

    async function submitComment(event: FormEvent) {
        event.preventDefault();
        if (!activeTask || !commentText.trim()) {
            return;
        }
        try {
            await api.createComment(activeTask.id, commentText.trim());
            setTaskDetailError(null);
            setCommentText("");
            await loadFirstCommentsPage(activeTask.id);

            if (selectedProjectId) {
                await loadProject(selectedProjectId);
            }
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function removeComment(commentId: number) {
        if (!activeTask) {
            return;
        }
        try {
            await api.deleteComment(commentId);
            setTaskDetailError(null);
            await loadFirstCommentsPage(activeTask.id);
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    async function saveCommentEdit() {
        if (!editingCommentId || !activeTask || !editingCommentText.trim()) {
            return;
        }
        try {
            await api.updateComment(editingCommentId, editingCommentText.trim());
            setTaskDetailError(null);
            setEditingCommentId(null);
            setEditingCommentText("");
            await loadFirstCommentsPage(activeTask.id);
        } catch (error) {
            setTaskDetailError(errorMessage(error));
        }
    }

    function assignTagToActiveTask() {
        if (!tagSelect && !newTagName.trim()) {
            return;
        }

        const nextTag: TaskEditTag | null = tagSelect
            ? (() => {
                const selectedTag = tags.find((tag) => tag.id === Number(tagSelect));
                return selectedTag ? { id: selectedTag.id, name: selectedTag.name } : null;
            })()
            : { id: -Date.now(), name: newTagName.trim(), isNew: true };

        if (!nextTag) {
            return;
        }

        setTaskEditTags((currentTags) => {
            const exists = currentTags.some(
                (tag) => tag.name.toLowerCase() === nextTag.name.toLowerCase()
            );
            return exists ? currentTags : [...currentTags, nextTag];
        });
        setTaskDetailError(null);
        setTagSelect("");
        setNewTagName("");
    }

    function removeTagFromActiveTask(tagId: number) {
        setTaskEditTags((currentTags) => currentTags.filter((tag) => tag.id !== tagId));
        setTaskDetailError(null);
    }

    async function generateReport() {
        if (!selectedProjectId) {
            return;
        }
        try {
            const submission = await api.startReport(selectedProjectId);
            setReport({
                ...submission,
                startedAt: null,
                completedAt: null,
                errorMessage: null,
                result: null
            });
            await refreshReport(submission.asyncTaskId);
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    async function refreshReport(asyncTaskId: string) {
        try {
            setReport(await api.reportStatus(asyncTaskId));
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    function openProfileEditor() {
        if (!currentUser) {
            return;
        }
        setProfileForm({
            username: currentUser.username,
            email: currentUser.email,
            firstName: currentUser.firstName,
            lastName: currentUser.lastName,
            password: ""
        });
        setModal("profile");
    }

    async function submitProfile(event: FormEvent) {
        event.preventDefault();
        if (!currentUser) {
            return;
        }
        try {
            const updated = await api.patchUser(currentUser.id, {
                username: profileForm.username,
                email: profileForm.email,
                firstName: profileForm.firstName,
                lastName: profileForm.lastName,
                password: profileForm.password || undefined
            });
            setCurrentUser(updated);
            setUsers((nextUsers) =>
                nextUsers.map((user) => (user.id === updated.id ? updated : user))
            );
            setProfileForm((form) => ({ ...form, password: "" }));
            setModal(null);
        } catch (error) {
            setNotice(errorMessage(error));
        }
    }

    if (!accessToken || (!busy && !currentUser)) {
        return (
            <AuthScreen
                text={text}
                onAuthenticated={handleAuth}
            />
        );
    }

    return (
        <div className="app-shell">
            <Sidebar
                text={text}
                currentView={view}
                onChangeView={setView}
                onLogout={logout}
            />
            <div className="content-shell">
                <Topbar
                    text={text}
                    currentUser={currentUser}
                    onCreateProject={openCreateProjectModal}
                    onOpenProfile={() => setView("profile")}
                />
                <main className="workspace">
                    {notice && (
                        <div className="notice">
                            <span>{notice}</span>
                            <button
                                className="icon-button"
                                onClick={() => setNotice(null)}
                                aria-label={text.common.dismiss}
                            >
                                <X size={16} />
                            </button>
                        </div>
                    )}
                    {busy ? (
                        <LoadingState text={text} />
                    ) : (
                        <>
                            {view === "dashboard" && (
                                <DashboardView
                                    text={text}
                                    currentUser={currentUser}
                                    dashboard={dashboard}
                                    projects={projects}
                                    onSelectProject={(id) => {
                                        void loadProject(id);
                                        setView("board");
                                    }}
                                    onCreateProject={openCreateProjectModal}
                                    onOpenBoard={() => setView("board")}
                                    onOpenTask={(taskId) => void openTask(taskId)}
                                />
                            )}
                            {view === "board" && (
                                <BoardView
                                    text={text}
                                    priorityLabels={priorityLabels}
                                    projects={projects}
                                    selectedProject={selectedProject}
                                    selectedProjectId={selectedProjectId}
                                    currentUser={currentUser}
                                    loading={loadingProject}
                                    canEdit={canEditSelectedProject}
                                    canDeleteProject={currentRole === "OWNER"}
                                    currentRole={currentRole}
                                    roleLabels={roleLabels}
                                    statusLabels={statusLabels}
                                    canChangeTaskStatus={canChangeTaskStatus}
                                    onSelectProject={(id) => void loadProject(id)}
                                    onEditProject={openEditProjectModal}
                                    onDeleteProject={() => setModal("deleteProject")}
                                    onCreateTask={() => {
                                        setTaskForm((form) => ({
                                            ...form,
                                            projectId: selectedProjectId ? String(selectedProjectId) : form.projectId
                                        }));
                                        setModal("task");
                                    }}
                                    onMoveTask={(task, status) => void moveTask(task, status)}
                                    onOpenTask={(taskId) => void openTask(taskId)}
                                />
                            )}
                            {view === "reports" && (
                                <ReportsView
                                    text={text}
                                    dashboard={dashboard}
                                    projects={projects}
                                    selectedProject={selectedProject}
                                    selectedProjectId={selectedProjectId}
                                    report={report}
                                    statusLabels={statusLabels}
                                    onSelectProject={(id) => void loadProject(id)}
                                    onGenerateReport={() => void generateReport()}
                                />
                            )}
                            {view === "team" && (
                                <TeamView
                                    text={text}
                                    projects={projects}
                                    roleLabels={roleLabels}
                                    selectedProject={selectedProject}
                                    selectedProjectId={selectedProjectId}
                                    currentUser={currentUser}
                                    currentRole={currentRole}
                                    canManageMembers={canManageSelectedMembers}
                                    canEditMembers={canEditSelectedMembers}
                                    onSelectProject={(id) => void loadProject(id)}
                                    onAddMember={() => setModal("member")}
                                    onUpdateRole={(userId, role) => void updateMemberRole(userId, role)}
                                    onRemoveMember={(userId) => void removeMember(userId)}
                                />
                            )}
                            {view === "profile" && currentUser && (
                                <ProfileView
                                    text={text}
                                    currentUser={currentUser}
                                    dashboard={dashboard}
                                    onEditProfile={openProfileEditor}
                                />
                            )}
                            {view === "taskDetails" && activeTask && (
                                <TaskDetailsView
                                    text={text}
                                    mode={taskDetailsMode}
                                    activeTask={activeTask}
                                    selectedProject={selectedProject}
                                    currentUser={currentUser}
                                    canEditTask={canEditActiveTask}
                                    canChangeStatus={canChangeActiveTaskStatus}
                                    canEditComments={canEditSelectedMembers}
                                    statusLabels={statusLabels}
                                    priorityLabels={priorityLabels}
                                    projectMembers={projectMembers}
                                    tags={tags}
                                    taskComments={taskComments}
                                    commentsPage={commentsPage}
                                    hasMoreComments={hasMoreComments}
                                    loadingMoreComments={loadingMoreComments}
                                    commentText={commentText}
                                    editingCommentId={editingCommentId}
                                    editingCommentText={editingCommentText}
                                    taskEditForm={taskEditForm}
                                    taskEditTags={taskEditTags}
                                    taskDetailError={taskDetailError}
                                    tagSelect={tagSelect}
                                    newTagName={newTagName}
                                    onBack={() => setView(taskDetailsBackView === "taskDetails" ? "board" : taskDetailsBackView)}
                                    onStartEdit={startTaskEdit}
                                    onCancelEdit={cancelTaskEdit}
                                    onSaveEdit={() => void saveActiveTaskEdit()}
                                    onDeleteTask={(taskId) => void removeTask(taskId)}
                                    onStatusChange={(status) => void changeActiveTaskStatus(status)}
                                    onEditFormChange={(patch) => setTaskEditForm((form) => ({ ...form, ...patch }))}
                                    onTagSelectChange={setTagSelect}
                                    onNewTagNameChange={setNewTagName}
                                    onAssignTag={() => void assignTagToActiveTask()}
                                    onRemoveTag={(tagId) => void removeTagFromActiveTask(tagId)}
                                    onCommentTextChange={setCommentText}
                                    onSubmitComment={(event) => void submitComment(event)}
                                    onStartCommentEdit={startCommentEdit}
                                    onCancelCommentEdit={cancelCommentEdit}
                                    onEditingCommentTextChange={setEditingCommentText}
                                    onSaveCommentEdit={() => void saveCommentEdit()}
                                    onRemoveComment={(commentId) => void removeComment(commentId)}
                                    onLoadMoreComments={() => void loadMoreComments()}
                                />
                            )}
                        </>
                    )}
                </main>
            </div>
            {modal === "project" && (
                <Modal
                    text={text}
                    title={
                        projectFormMode === "edit"
                            ? "Редактировать проект"
                            : text.modals.newProject
                    }
                    onClose={() => {
                        setProjectFormMode("create");
                        setProjectForm({ name: "", description: "" });
                        setModal(null);
                    }}
                >
                    <form className="form-stack" onSubmit={(event) => void submitProject(event)}>
                        <label>
                            <span>{text.forms.projectName}</span>
                            <input
                                required
                                value={projectForm.name}
                                onChange={(event) =>
                                    setProjectForm((form) => ({ ...form, name: event.target.value }))
                                }
                            />
                        </label>
                        <label>
                            <span>{text.forms.projectDescription}</span>
                            <textarea
                                rows={4}
                                value={projectForm.description}
                                onChange={(event) =>
                                    setProjectForm((form) => ({ ...form, description: event.target.value }))
                                }
                            />
                        </label>
                        <button className="primary-button" type="submit">
                            {projectFormMode === "edit" ? <Edit3 size={16} /> : <Plus size={16} />}
                            {projectFormMode === "edit"
                                ? "Сохранить проект"
                                : text.actions.createProject}
                        </button>
                    </form>
                </Modal>
            )}
            {modal === "deleteProject" && selectedProject && (
                <Modal
                    text={text}
                    title={"Удалить проект"}
                    onClose={() => setModal(null)}
                >
                    <div className="form-stack">
                        <p className="modal-warning-text">
                            {`Проект "${selectedProject.name}" будет удален вместе с задачами, участниками, тегами и комментариями. Это действие нельзя отменить.`}
                        </p>
                        <div className="modal-action-row">
                            <button className="secondary-button" type="button" onClick={() => setModal(null)}>
                                {"Отмена"}
                            </button>
                            <button className="danger-button" type="button" onClick={() => void deleteProject()}>
                                <Trash2 size={16} />
                                {"Удалить проект"}
                            </button>
                        </div>
                    </div>
                </Modal>
            )}
            {modal === "task" && (
                <Modal text={text} title={text.modals.createTask} onClose={() => setModal(null)}>
                    <form className="form-stack" onSubmit={(event) => void submitTask(event)}>
                        <label>
                            <span>{text.forms.projectSelect}</span>
                            <select
                                required
                                value={taskForm.projectId || selectedProjectId || ""}
                                onChange={(event) =>
                                    setTaskForm((form) => ({ ...form, projectId: event.target.value }))
                                }
                            >
                                <option value="">{text.forms.chooseProject}</option>
                                {projects.map((project) => (
                                    <option key={project.id} value={project.id}>
                                        {project.name}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label>
                            <span>{text.forms.taskTitle}</span>
                            <input
                                required
                                value={taskForm.title}
                                onChange={(event) =>
                                    setTaskForm((form) => ({ ...form, title: event.target.value }))
                                }
                            />
                        </label>
                        <label>
                            <span>{text.forms.taskDescription}</span>
                            <textarea
                                required
                                rows={4}
                                value={taskForm.description}
                                onChange={(event) =>
                                    setTaskForm((form) => ({ ...form, description: event.target.value }))
                                }
                            />
                        </label>
                        <div className="form-grid">
                            <label>
                                <span>{text.forms.assignee}</span>
                                <select
                                    value={taskForm.assigneeId}
                                    onChange={(event) =>
                                        setTaskForm((form) => ({ ...form, assigneeId: event.target.value }))
                                    }
                                >
                                    <option value="">{text.common.unassigned}</option>
                                    {projectMembers.map((member) => (
                                        <option key={member.id} value={member.id}>
                                            {member.username}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label>
                                <span>{text.forms.priority}</span>
                                <select
                                    value={taskForm.priority}
                                    onChange={(event) =>
                                        setTaskForm((form) => ({
                                            ...form,
                                            priority: event.target.value as Priority
                                        }))
                                    }
                                >
                                    {PRIORITIES.map((priority) => (
                                        <option key={priority} value={priority}>
                                            {priorityLabels[priority]}
                                        </option>
                                    ))}
                                </select>
                            </label>
                        </div>
                        <label>
                            <span>{text.forms.dueDate}</span>
                            <input
                                type="datetime-local"
                                value={taskForm.dueDate}
                                onChange={(event) =>
                                    setTaskForm((form) => ({ ...form, dueDate: event.target.value }))
                                }
                            />
                        </label>
                        <label>
                            <span>{"Теги"}</span>
                            <div className="tag-picker">
                                {tags.map((tag) => {
                                    const selected = taskForm.tagIds.includes(String(tag.id));
                                    return (
                                        <button
                                            key={tag.id}
                                            className={`tag-chip tag-toggle ${selected ? "selected" : ""}`}
                                            type="button"
                                            onClick={() =>
                                                setTaskForm((form) => ({
                                                    ...form,
                                                    tagIds: selected
                                                        ? form.tagIds.filter((tagId) => tagId !== String(tag.id))
                                                        : [...form.tagIds, String(tag.id)]
                                                }))
                                            }
                                        >
                                            <Tag size={13} />
                                            {tag.name}
                                        </button>
                                    );
                                })}
                            </div>
                        </label>
                        <label>
                            <span>{"Новый тег"}</span>
                            <input
                                value={taskForm.newTagName}
                                onChange={(event) =>
                                    setTaskForm((form) => ({ ...form, newTagName: event.target.value }))
                                }
                            />
                        </label>
                        <button className="primary-button" type="submit">
                            <Plus size={16} />
                            {text.actions.createTask}
                        </button>
                    </form>
                </Modal>
            )}
            {modal === "member" && selectedProject && (
                <Modal text={text} title={text.modals.addMember} onClose={() => setModal(null)}>
                    <form className="form-stack" onSubmit={(event) => void submitMember(event)}>
                        <label>
                            <span>{text.forms.user}</span>
                            <select
                                required
                                value={memberForm.userId}
                                onChange={(event) =>
                                    setMemberForm((form) => ({ ...form, userId: event.target.value }))
                                }
                            >
                                <option value="">{text.forms.chooseUser}</option>
                                {availableUsers.map((user) => (
                                    <option key={user.id} value={user.id}>
                                        {user.username} · {user.email}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label>
                            <span>{text.forms.role}</span>
                            <select
                                value={memberForm.role}
                                onChange={(event) =>
                                    setMemberForm((form) => ({
                                        ...form,
                                        role: event.target.value as ProjectRole
                                    }))
                                }
                            >
                                {((currentRole === "MANAGER" ? ["MEMBER"] : ROLES) as ProjectRole[]).map((role) => (
                                    <option key={role} value={role}>
                                        {roleLabels[role]}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <button className="primary-button" type="submit" disabled={!availableUsers.length}>
                            <UserPlus size={16} />
                            {text.actions.addMember}
                        </button>
                    </form>
                </Modal>
            )}
            {modal === "profile" && currentUser && (
                <Modal text={text} title={text.modals.editProfile} onClose={() => setModal(null)}>
                    <form className="form-stack" onSubmit={(event) => void submitProfile(event)}>
                        <label>
                            <span>{text.auth.username}</span>
                            <input
                                required
                                value={profileForm.username}
                                onChange={(event) =>
                                    setProfileForm((form) => ({ ...form, username: event.target.value }))
                                }
                            />
                        </label>
                        <label>
                            <span>{text.auth.email}</span>
                            <input
                                required
                                type="email"
                                value={profileForm.email}
                                onChange={(event) =>
                                    setProfileForm((form) => ({ ...form, email: event.target.value }))
                                }
                            />
                        </label>
                        <div className="form-grid">
                            <label>
                                <span>{text.auth.firstName}</span>
                                <input
                                    required
                                    value={profileForm.firstName}
                                    onChange={(event) =>
                                        setProfileForm((form) => ({ ...form, firstName: event.target.value }))
                                    }
                                />
                            </label>
                            <label>
                                <span>{text.auth.lastName}</span>
                                <input
                                    required
                                    value={profileForm.lastName}
                                    onChange={(event) =>
                                        setProfileForm((form) => ({ ...form, lastName: event.target.value }))
                                    }
                                />
                            </label>
                        </div>
                        <label>
                            <span>{text.auth.password}</span>
                            <input
                                type="password"
                                placeholder={text.profile.passwordHint}
                                value={profileForm.password}
                                onChange={(event) =>
                                    setProfileForm((form) => ({ ...form, password: event.target.value }))
                                }
                            />
                        </label>
                        <button className="primary-button" type="submit">
                            <Edit3 size={16} />
                            {text.actions.saveProfile}
                        </button>
                    </form>
                </Modal>
            )}
        </div>
    );
}

function AuthScreen({
                        text,
                        onAuthenticated
                    }: {
    text: Text;
    onAuthenticated: (response: AuthResponse) => void;
}) {
    const [mode, setMode] = useState<"login" | "register">("login");
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        login: "",
        username: "",
        email: "",
        password: "",
        firstName: "",
        lastName: ""
    });

    async function submit(event: FormEvent) {
        event.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const response =
                mode === "login"
                    ? await api.login(form.login, form.password)
                    : await api.register({
                        username: form.username,
                        email: form.email,
                        password: form.password,
                        firstName: form.firstName,
                        lastName: form.lastName
                    });
            onAuthenticated(response);
        } catch (submitError) {
            setError(errorMessage(submitError));
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="auth-canvas">
            <section className="auth-frame">
                <aside className="auth-story">
                    <div>
                        <div className="auth-brand">
                            <div className="brand-mark">
                                <Sparkles size={18} />
                            </div>
                            <strong>{text.brand}</strong>
                        </div>
                        <h1>{text.auth.introTitle}</h1>
                        <p>{text.auth.introText}</p>
                    </div>
                </aside>
                <section className="auth-panel">
                    <div className="auth-panel-top">
                        <div>
                            <h1>{mode === "login" ? text.auth.loginTitle : text.auth.registerTitle}</h1>
                            <p>{mode === "login" ? text.auth.loginSubtitle : text.auth.registerSubtitle}</p>
                        </div>
                    </div>
                    <div className="segmented">
                        <button
                            className={mode === "login" ? "active" : ""}
                            onClick={() => setMode("login")}
                            type="button"
                        >
                            {text.auth.loginTab}
                        </button>
                        <button
                            className={mode === "register" ? "active" : ""}
                            onClick={() => setMode("register")}
                            type="button"
                        >
                            {text.auth.registerTab}
                        </button>
                    </div>
                    <form className="form-stack" onSubmit={(event) => void submit(event)}>
                        {mode === "login" ? (
                            <label>
                                <span>{text.auth.loginField}</span>
                                <input
                                    required
                                    value={form.login}
                                    onChange={(event) => setForm((next) => ({ ...next, login: event.target.value }))}
                                />
                            </label>
                        ) : (
                            <>
                                <label>
                                    <span>{text.auth.username}</span>
                                    <input
                                        required
                                        value={form.username}
                                        onChange={(event) =>
                                            setForm((next) => ({ ...next, username: event.target.value }))
                                        }
                                    />
                                </label>
                                <label>
                                    <span>{text.auth.email}</span>
                                    <input
                                        required
                                        type="email"
                                        value={form.email}
                                        onChange={(event) => setForm((next) => ({ ...next, email: event.target.value }))}
                                    />
                                </label>
                                <div className="form-grid">
                                    <label>
                                        <span>{text.auth.firstName}</span>
                                        <input
                                            required
                                            value={form.firstName}
                                            onChange={(event) =>
                                                setForm((next) => ({ ...next, firstName: event.target.value }))
                                            }
                                        />
                                    </label>
                                    <label>
                                        <span>{text.auth.lastName}</span>
                                        <input
                                            required
                                            value={form.lastName}
                                            onChange={(event) =>
                                                setForm((next) => ({ ...next, lastName: event.target.value }))
                                            }
                                        />
                                    </label>
                                </div>
                            </>
                        )}
                        <label>
                            <span>{text.auth.password}</span>
                            <input
                                required
                                type="password"
                                value={form.password}
                                onChange={(event) => setForm((next) => ({ ...next, password: event.target.value }))}
                            />
                        </label>
                        {error && <p className="form-error">{error}</p>}
                        <button className="primary-button" type="submit" disabled={loading}>
                            {loading ? <Loader2 className="spin" size={16} /> : <Shield size={16} />}
                            {mode === "login" ? text.auth.submitLogin : text.auth.submitRegister}
                        </button>
                    </form>
                </section>
            </section>
        </main>
    );
}

function Sidebar({
                     text,
                     currentView,
                     onChangeView,
                     onLogout
                 }: {
    text: Text;
    currentView: View;
    onChangeView: (view: View) => void;
    onLogout: () => void;
}) {
    const items: { view: View; label: string; icon: typeof LayoutDashboard }[] = [
        { view: "dashboard", label: text.nav.dashboard, icon: LayoutDashboard },
        { view: "board", label: text.nav.board, icon: FolderOpen },
        { view: "reports", label: text.nav.reports, icon: BarChart3 },
        { view: "team", label: text.nav.team, icon: Users },
        { view: "profile", label: text.nav.profile, icon: UserCircle }
    ];
    return (
        <aside className="sidebar">
            <div className="workspace-logo">
                <div className="logo-cube">
                    <Sparkles size={17} />
                </div>
                <div>
                    <strong>{text.workspace}</strong>
                    <span>{text.workspaceSubtitle}</span>
                </div>
            </div>
            <nav>
                {items.map((item) => {
                    const Icon = item.icon;
                    const active = currentView === item.view || (currentView === "taskDetails" && item.view === "board");
                    return (
                        <button
                            key={item.view}
                            className={`nav-item ${active ? "active" : ""}`}
                            onClick={() => onChangeView(item.view)}
                        >
                            <Icon size={19} />
                            {item.label}
                        </button>
                    );
                })}
            </nav>
            <div className="sidebar-footer">
                <button className="nav-item ghost" onClick={onLogout}>
                    <LogOut size={18} />
                    <span>{text.nav.logout}</span>
                </button>
            </div>
        </aside>
    );
}

function Topbar({
                    text,
                    currentUser,
                    onCreateProject,
                    onOpenProfile
                }: {
    text: Text;
    currentUser: UserResponse | null;
    onCreateProject: () => void;
    onOpenProfile: () => void;
}) {
    return (
        <header className="topbar">
            <div>
                <h1>{text.appTitle}</h1>
            </div>
            <button className="secondary-button top-action" onClick={onCreateProject}>
                <Plus size={15} />
                {text.actions.project}
            </button>
            <button className="avatar avatar-button" onClick={onOpenProfile} aria-label={text.nav.profile}>
                {initials(currentUser?.username ?? "User")}
            </button>
        </header>
    );
}

function DashboardView({
                           text,
                           currentUser,
                           dashboard,
                           projects,
                           onSelectProject,
                           onCreateProject,
                           onOpenBoard,
                           onOpenTask
                       }: {
    text: Text;
    currentUser: UserResponse | null;
    dashboard: DashboardResponse | null;
    projects: ProjectResponse[];
    onSelectProject: (projectId: number) => void;
    onCreateProject: () => void;
    onOpenBoard: () => void;
    onOpenTask: (taskId: number) => void;
}) {
    return (
        <div className="view-stack">
            <section className="hero-slab">
                <div>
                    <p className="eyebrow">{text.dashboard.eyebrow}</p>
                    <h2>{welcomeText(currentUser)}</h2>
                    <p>
                        {dashboard
                            ? text.dashboard.activeSummary(dashboard.activeTasks, dashboard.totalProjects)
                            : text.dashboard.loadingText}
                    </p>
                </div>
                <button className="primary-button" onClick={onCreateProject}>
                    <Plus size={16} />
                    {text.actions.newProject}
                </button>
            </section>
            <section className="stat-grid three">
                <StatCard
                    icon={FolderOpen}
                    label={text.dashboard.totalProjects}
                    value={dashboard?.totalProjects ?? 0}
                    accent="primary"
                    onClick={onOpenBoard}
                />
                <StatCard
                    icon={Activity}
                    label={text.dashboard.activeTasks}
                    value={dashboard?.activeTasks ?? 0}
                    accent="tertiary"
                    onClick={onOpenBoard}
                />
                <StatCard
                    icon={AlertTriangle}
                    label={text.dashboard.dueToday}
                    value={dashboard?.dueTodayTasks ?? 0}
                    accent="error"
                    onClick={onOpenBoard}
                />
            </section>
            <section className="dashboard-grid">
                <div>
                    <SectionHeader title={text.dashboard.recentProjects} subtitle={text.dashboard.recentSubtitle} />
                    <div className="list-stack">
                        {projects.length ? (
                            projects.slice(0, 6).map((project) => (
                                <button
                                    key={project.id}
                                    className="project-row"
                                    onClick={() => onSelectProject(project.id)}
                                >
                                    <div>
                                        <strong>{project.name}</strong>
                                        <span>{project.description || text.common.noDescription}</span>
                                    </div>
                                    <span>{formatShortDate(project.updatedAt)}</span>
                                </button>
                            ))
                        ) : (
                            <EmptyState title={text.dashboard.emptyProjects} />
                        )}
                    </div>
                </div>
                <div>
                    <SectionHeader title={text.dashboard.upcomingTasks} subtitle={text.dashboard.upcomingSubtitle} />
                    <div className="list-stack">
                        {dashboard?.upcomingTasks.length ? (
                            dashboard.upcomingTasks.map((task) => (
                                <button
                                    key={task.id}
                                    className={`task-row ${task.overdue ? "task-row-overdue" : ""}`}
                                    onClick={() => onOpenTask(task.id)}
                                >
                                    <span className={`priority-dot priority-${task.priority.toLowerCase()}`} />
                                    <div>
                                        <strong>{task.title}</strong>
                                        <span>
                      {task.projectName ?? text.common.project} - {formatDate(task.dueDate)}
                    </span>
                                        {task.overdue && (
                                            <em className="task-row-flag">
                                                {"Дедлайн просрочен"}
                                            </em>
                                        )}
                                    </div>
                                </button>
                            ))
                        ) : (
                            <EmptyState title={text.dashboard.emptyUpcoming} />
                        )}
                    </div>
                </div>
            </section>
        </div>
    );
}

function BoardView({
                       text,
                       priorityLabels,
                       projects,
                       selectedProject,
                       selectedProjectId,
                       currentUser,
                       loading,
                       canEdit,
                       canDeleteProject,
                       currentRole,
                       roleLabels,
                       statusLabels,
                       canChangeTaskStatus,
                       onSelectProject,
                       onEditProject,
                       onDeleteProject,
                       onCreateTask,
                       onMoveTask,
                       onOpenTask
                   }: {
    text: Text;
    priorityLabels: Record<Priority, string>;
    projects: ProjectResponse[];
    selectedProject: ProjectDetailsResponse | null;
    selectedProjectId: number | null;
    currentUser: UserResponse | null;
    loading: boolean;
    canEdit: boolean;
    canDeleteProject: boolean;
    currentRole: ProjectRole | null;
    roleLabels: Record<ProjectRole, string>;
    statusLabels: Record<TaskStatus, string>;
    canChangeTaskStatus: (task: ProjectTaskSummaryResponse) => boolean;
    onSelectProject: (projectId: number) => void;
    onEditProject: () => void;
    onDeleteProject: () => void;
    onCreateTask: () => void;
    onMoveTask: (task: ProjectTaskSummaryResponse, status: TaskStatus) => void;
    onOpenTask: (taskId: number) => void;
}) {
    const [draggingTaskId, setDraggingTaskId] = useState<number | null>(null);
    const [dragOverStatus, setDragOverStatus] = useState<TaskStatus | null>(null);
    const [filters, setFilters] = useState({
        query: "",
        status: "ALL" as TaskStatus | "ALL",
        priority: "ALL" as Priority | "ALL",
        assigneeId: "ALL",
        tagId: "ALL",
        overdueOnly: false
    });

    useEffect(() => {
        setFilters({
            query: "",
            status: "ALL",
            priority: "ALL",
            assigneeId: "ALL",
            tagId: "ALL",
            overdueOnly: false
        });
    }, [selectedProjectId]);

    function dropTask(status: TaskStatus) {
        if (!selectedProject || draggingTaskId === null) {
            return;
        }
        const task = selectedProject.tasks.find((item) => item.id === draggingTaskId);
        if (task && canChangeTaskStatus(task)) {
            onMoveTask(task, status);
        }
        setDraggingTaskId(null);
        setDragOverStatus(null);
    }

    const availableTagOptions = useMemo(() => {
        if (!selectedProject) {
            return [];
        }
        const seen = new Map<number, string>();
        selectedProject.tasks.forEach((task) => {
            task.tags.forEach((tag) => {
                if (!seen.has(tag.id)) {
                    seen.set(tag.id, tag.name);
                }
            });
        });
        return [...seen.entries()]
            .map(([id, name]) => ({ id, name }))
            .sort((left, right) => left.name.localeCompare(right.name));
    }, [selectedProject]);

    const filteredTasks = useMemo(() => {
        if (!selectedProject) {
            return [];
        }
        const query = filters.query.trim().toLowerCase();
        return selectedProject.tasks.filter((task) => {
            if (filters.status !== "ALL" && task.status !== filters.status) {
                return false;
            }
            if (filters.priority !== "ALL" && task.priority !== filters.priority) {
                return false;
            }
            if (filters.assigneeId !== "ALL") {
                if (filters.assigneeId === "unassigned") {
                    if (task.assigneeId !== null) {
                        return false;
                    }
                } else if (String(task.assigneeId ?? "") !== filters.assigneeId) {
                    return false;
                }
            }
            if (filters.tagId !== "ALL" && !task.tags.some((tag) => String(tag.id) === filters.tagId)) {
                return false;
            }
            if (filters.overdueOnly && !isOverdueTask(task.dueDate, task.status)) {
                return false;
            }
            if (!query) {
                return true;
            }
            const haystack = [
                task.title,
                task.assigneeUsername ?? "",
                task.creatorUsername ?? "",
                ...task.tags.map((tag) => tag.name)
            ]
                .join(" ")
                .toLowerCase();
            return haystack.includes(query);
        });
    }, [filters, selectedProject]);

    const hasActiveFilters =
        filters.query !== "" ||
        filters.status !== "ALL" ||
        filters.priority !== "ALL" ||
        filters.assigneeId !== "ALL" ||
        filters.tagId !== "ALL" ||
        filters.overdueOnly;

    return (
        <div className="view-stack">
            <Toolbar
                title={text.board.title}
                subtitle={selectedProject?.description ?? text.board.fallbackSubtitle}
                selectValue={selectedProjectId ?? ""}
                projects={projects}
                text={text}
                onSelectProject={onSelectProject}
                meta={
                    selectedProject && currentRole ? (
                        <div className={`project-role-badge role-${currentRole.toLowerCase()}`}>
                            {roleLabels[currentRole]}
                        </div>
                    ) : null
                }
                action={
                    <div className="board-toolbar-actions">
                        <div className="project-management-actions">
                            {selectedProject && canEdit && (
                                <button className="secondary-button" onClick={onEditProject}>
                                    <Edit3 size={16} />
                                    {"Изменить проект"}
                                </button>
                            )}
                            {selectedProject && canDeleteProject && (
                                <button className="danger-button" onClick={onDeleteProject}>
                                    <Trash2 size={16} />
                                    {"Удалить проект"}
                                </button>
                            )}
                        </div>
                        <button className="primary-button" onClick={onCreateTask} disabled={!canEdit}>
                            <Plus size={16} />
                            {text.actions.newTask}
                        </button>
                    </div>
                }
            />
            {loading ? (
                <LoadingState text={text} />
            ) : selectedProject ? (
                <>
                    <section className="board-filter-panel">
                        <div className="board-filter-grid">
                            <label className="detail-control board-filter-span-two">
                                <span>{"Поиск"}</span>
                                <input
                                    value={filters.query}
                                    onChange={(event) =>
                                        setFilters((current) => ({ ...current, query: event.target.value }))
                                    }
                                    placeholder={
                                        "Название, исполнитель или тег"
                                    }
                                />
                            </label>
                            <label className="detail-control">
                                <span>{"Статус"}</span>
                                <select
                                    value={filters.status}
                                    onChange={(event) =>
                                        setFilters((current) => ({
                                            ...current,
                                            status: event.target.value as TaskStatus | "ALL"
                                        }))
                                    }
                                >
                                    <option value="ALL">{"Все статусы"}</option>
                                    {STATUSES.map((status) => (
                                        <option key={status} value={status}>
                                            {statusLabels[status]}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="detail-control">
                                <span>{"Приоритет"}</span>
                                <select
                                    value={filters.priority}
                                    onChange={(event) =>
                                        setFilters((current) => ({
                                            ...current,
                                            priority: event.target.value as Priority | "ALL"
                                        }))
                                    }
                                >
                                    <option value="ALL">{"Все приоритеты"}</option>
                                    {PRIORITIES.map((priority) => (
                                        <option key={priority} value={priority}>
                                            {priorityLabels[priority]}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="detail-control">
                                <span>{text.forms.assignee}</span>
                                <select
                                    value={filters.assigneeId}
                                    onChange={(event) =>
                                        setFilters((current) => ({ ...current, assigneeId: event.target.value }))
                                    }
                                >
                                    <option value="ALL">{"Все исполнители"}</option>
                                    <option value="unassigned">{text.common.unassigned}</option>
                                    {selectedProject.users.map((member) => (
                                        <option key={member.id} value={member.id}>
                                            {member.username}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="detail-control">
                                <span>{"Тег"}</span>
                                <select
                                    value={filters.tagId}
                                    onChange={(event) =>
                                        setFilters((current) => ({ ...current, tagId: event.target.value }))
                                    }
                                >
                                    <option value="ALL">{"Все теги"}</option>
                                    {availableTagOptions.map((tag) => (
                                        <option key={tag.id} value={tag.id}>
                                            {tag.name}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="checkbox-line board-filter-toggle">
                                <input
                                    type="checkbox"
                                    checked={filters.overdueOnly}
                                    onChange={(event) =>
                                        setFilters((current) => ({ ...current, overdueOnly: event.target.checked }))
                                    }
                                />
                                <span>{"Только просроченные"}</span>
                            </label>
                            {hasActiveFilters && (
                                <button
                                    className="secondary-button"
                                    type="button"
                                    onClick={() =>
                                        setFilters({
                                            query: "",
                                            status: "ALL",
                                            priority: "ALL",
                                            assigneeId: "ALL",
                                            tagId: "ALL",
                                            overdueOnly: false
                                        })
                                    }
                                >
                                    {"Сбросить фильтры"}
                                </button>
                            )}
                        </div>
                    </section>

                    {filteredTasks.length ? (
                        <section className="kanban">
                            {STATUSES.map((status) => {
                                const tasks = filteredTasks.filter((task) => task.status === status);
                                return (
                                    <div
                                        className={`kanban-column ${dragOverStatus === status ? "drag-target" : ""}`}
                                        key={status}
                                        onDragOver={(event) => {
                                            const draggingTask = selectedProject.tasks.find((item) => item.id === draggingTaskId);
                                            if (draggingTask && canChangeTaskStatus(draggingTask)) {
                                                event.preventDefault();
                                                setDragOverStatus(status);
                                            }
                                        }}
                                        onDragLeave={() => setDragOverStatus(null)}
                                        onDrop={(event) => {
                                            event.preventDefault();
                                            dropTask(status);
                                        }}
                                    >
                                        <div className="column-title">
                                            <span className={`status-dot status-${status.toLowerCase()}`} />
                                            <strong>{statusLabels[status]}</strong>
                                            <em>{tasks.length}</em>
                                        </div>
                                        <div className="task-card-stack">
                                            {tasks.map((task) => {
                                                const isCreatedByCurrentUser = Boolean(
                                                    currentUser && task.creatorId === currentUser.id
                                                );
                                                const isAssignedToCurrentUser = Boolean(
                                                    currentUser && task.assigneeId === currentUser.id
                                                );
                                                const canChangeStatus = canChangeTaskStatus(task);

                                                return (
                                                    <article
                                                        className={`task-card ${isOverdueTask(task.dueDate, task.status) ? "task-card-overdue" : ""}`}
                                                        draggable={canChangeStatus}
                                                        key={task.id}
                                                        onClick={() => onOpenTask(task.id)}
                                                        onDragEnd={() => {
                                                            setDraggingTaskId(null);
                                                            setDragOverStatus(null);
                                                        }}
                                                        onDragStart={(event) => {
                                                            if (!canChangeStatus) {
                                                                event.preventDefault();
                                                                return;
                                                            }
                                                            event.dataTransfer.effectAllowed = "move";
                                                            setDraggingTaskId(task.id);
                                                        }}
                                                    >
                                                        <div className="task-card-top">
                              <span className={`priority-chip priority-${task.priority.toLowerCase()}`}>
                                {priorityLabels[task.priority]}
                              </span>
                                                            <span>{formatShortDate(task.dueDate)}</span>
                                                        </div>
                                                        <h3>{task.title}</h3>
                                                        {(isAssignedToCurrentUser || isCreatedByCurrentUser) && (
                                                            <div className="task-personal-badges">
                                                                {isAssignedToCurrentUser && (
                                                                    <span className="task-personal-badge assigned">Назначена вам</span>
                                                                )}
                                                                {isCreatedByCurrentUser && (
                                                                    <span className="task-personal-badge creator">Вы создали</span>
                                                                )}
                                                            </div>
                                                        )}
                                                        {!!task.tags.length && (
                                                            <div className="tag-row task-card-tags">
                                                                {task.tags.map((tag) => (
                                                                    <span className="tag-chip" key={tag.id}>
                                    <Tag size={12} />
                                                                        {tag.name}
                                  </span>
                                                                ))}
                                                            </div>
                                                        )}
                                                        <div className="task-card-bottom">
                                                            <span>{task.assigneeUsername ?? text.common.unassigned}</span>
                                                            {canChangeStatus && (
                                                                <select
                                                                    value={task.status}
                                                                    onClick={(event) => event.stopPropagation()}
                                                                    onChange={(event) =>
                                                                        onMoveTask(task, event.target.value as TaskStatus)
                                                                    }
                                                                >
                                                                    {STATUSES.map((nextStatus) => (
                                                                        <option key={nextStatus} value={nextStatus}>
                                                                            {statusLabels[nextStatus]}
                                                                        </option>
                                                                    ))}
                                                                </select>
                                                            )}
                                                        </div>
                                                    </article>
                                                );
                                            })}
                                        </div>
                                    </div>
                                );
                            })}
                        </section>
                    ) : (
                        <EmptyState
                            title={
                                "Фильтры не нашли ни одной задачи"
                            }
                        />
                    )}
                </>
            ) : (
                <EmptyState title={text.board.empty} />
            )}
        </div>
    );
}

function ReportsView({
                         text,
                         dashboard,
                         projects,
                         selectedProject,
                         selectedProjectId,
                         report,
                         statusLabels,
                         onSelectProject,
                         onGenerateReport
                     }: {
    text: Text;
    dashboard: DashboardResponse | null;
    projects: ProjectResponse[];
    selectedProject: ProjectDetailsResponse | null;
    selectedProjectId: number | null;
    report: AsyncTaskStatusResponse<ProjectSummaryReportResponse> | null;
    statusLabels: Record<TaskStatus, string>;
    onSelectProject: (projectId: number) => void;
    onGenerateReport: () => void;
}) {
    const result = report?.result;
    const totalTasks = result?.tasksCount ?? selectedProject?.tasksCount ?? 0;
    const completedTasks = result?.completedTasksCount ?? dashboard?.completedTasks ?? 0;
    const overdueTasks = result?.overdueTasksCount ?? dashboard?.overdueTasks ?? 0;
    const highPriorityTasks = result?.highPriorityTasksCount ?? 0;
    const activeTasks = Math.max(0, totalTasks - completedTasks);
    const completionRate = totalTasks ? Math.round((completedTasks / totalTasks) * 100) : 0;
    const overdueRate = totalTasks ? Math.round((overdueTasks / totalTasks) * 100) : 0;
    const unassignedTasks = result?.unassignedTasksCount ?? 0;
    const teamSize = result?.membersCount ?? selectedProject?.membersCount ?? 0;
    return (
        <div className="view-stack">
            <Toolbar
                title={text.reports.title}
                subtitle={text.reports.subtitle}
                selectValue={selectedProjectId ?? ""}
                projects={projects}
                text={text}
                onSelectProject={onSelectProject}
                action={
                    <button className="primary-button" onClick={onGenerateReport} disabled={!selectedProject}>
                        {report && report.status !== "COMPLETED" && report.status !== "FAILED" ? (
                            <Loader2 className="spin" size={16} />
                        ) : (
                            <BarChart3 size={16} />
                        )}
                        {text.actions.newReport}
                    </button>
                }
            />
            <section className="stat-grid three">
                <StatCard
                    icon={CheckCircle2}
                    label={text.reports.completedTasks}
                    value={result?.completedTasksCount ?? dashboard?.completedTasks ?? 0}
                    accent="secondary"
                />
                <StatCard
                    icon={AlertTriangle}
                    label={text.reports.overdueTasks}
                    value={result?.overdueTasksCount ?? dashboard?.overdueTasks ?? 0}
                    accent="error"
                />
                <StatCard
                    icon={Shield}
                    label={text.reports.highPriority}
                    value={result?.highPriorityTasksCount ?? 0}
                    accent="tertiary"
                />
            </section>
            <section className="reports-grid">
                <div className="surface-panel analytics-panel reports-main-panel">
                    <SectionHeader
                        title={result?.projectName ?? selectedProject?.name ?? text.reports.projectSummary}
                        subtitle={
                            report
                                ? `${text.common.asyncStatus}: ${report.status}`
                                : text.reports.generateHint
                        }
                    />
                    <div className="report-bars">
                        {STATUSES.map((status) => {
                            const value = result?.tasksByStatus?.[status] ?? 0;
                            const max = Math.max(result?.tasksCount ?? selectedProject?.tasksCount ?? 1, 1);
                            return (
                                <div key={status} className="bar-row">
                                    <span>{statusLabels[status]}</span>
                                    <div>
                                        <i style={{ width: `${Math.min(100, (value / max) * 100)}%` }} />
                                    </div>
                                    <strong>{value}</strong>
                                </div>
                            );
                        })}
                    </div>
                    <div className="analytics-strip">
                        <Metric label={text.reports.members} value={result?.membersCount ?? selectedProject?.membersCount ?? 0} />
                        <Metric label={text.reports.unassigned} value={result?.unassignedTasksCount ?? 0} />
                        <Metric
                            label={text.reports.nearestDueDate}
                            value={formatShortDate(result?.nearestDueDate ?? null)}
                        />
                    </div>
                </div>
            </section>
            <section className="surface-panel project-insights">
                <SectionHeader
                    title={"Полезные показатели"}
                    subtitle={"Состояние выбранного проекта"}
                />
                <div className="insight-grid">
                    <Metric label={"Всего задач"} value={totalTasks} />
                    <Metric label={"В работе"} value={activeTasks} />
                    <Metric label={"Готово"} value={completedTasks} />
                    <Metric label={"Completion rate"} value={`${completionRate}%`} />
                    <Metric label={"Overdue rate"} value={`${overdueRate}%`} />
                    <Metric label={"High priority"} value={highPriorityTasks} />
                    <Metric label={"Без исполнителя"} value={unassignedTasks} />
                    <Metric label={"Участники"} value={teamSize} />
                </div>
            </section>
        </div>
    );
}


function TaskDetailsView({
                             text,
                             mode,
                             activeTask,
                             selectedProject,
                             currentUser,
                             canEditTask,
                             canChangeStatus,
                             canEditComments,
                             statusLabels,
                             priorityLabels,
                             projectMembers,
                             tags,
                             taskComments,
                             commentsPage,
                             hasMoreComments,
                             loadingMoreComments,
                             commentText,
                             editingCommentId,
                             editingCommentText,
                             taskEditForm,
                             taskEditTags,
                             taskDetailError,
                             tagSelect,
                             newTagName,
                             onBack,
                             onStartEdit,
                             onCancelEdit,
                             onSaveEdit,
                             onDeleteTask,
                             onStatusChange,
                             onEditFormChange,
                             onTagSelectChange,
                             onNewTagNameChange,
                             onAssignTag,
                             onRemoveTag,
                             onCommentTextChange,
                             onSubmitComment,
                             onStartCommentEdit,
                             onCancelCommentEdit,
                             onEditingCommentTextChange,
                             onSaveCommentEdit,
                             onRemoveComment,
                             onLoadMoreComments
                         }: {
    text: Text;
    mode: TaskDetailsMode;
    activeTask: TaskDetailsResponse;
    selectedProject: ProjectDetailsResponse | null;
    currentUser: UserResponse | null;
    canEditTask: boolean;
    canChangeStatus: boolean;
    canEditComments: boolean;
    statusLabels: Record<TaskStatus, string>;
    priorityLabels: Record<Priority, string>;
    projectMembers: ProjectDetailsResponse["users"];
    tags: TagResponse[];
    taskComments: CommentResponse[];
    commentsPage: number;
    hasMoreComments: boolean;
    loadingMoreComments: boolean;
    commentText: string;
    editingCommentId: number | null;
    editingCommentText: string;
    taskEditForm: TaskEditForm;
    taskEditTags: TaskEditTag[];
    taskDetailError: string | null;
    tagSelect: string;
    newTagName: string;
    onBack: () => void;
    onStartEdit: () => void;
    onCancelEdit: () => void;
    onSaveEdit: () => void;
    onDeleteTask: (taskId: number) => void;
    onStatusChange: (status: TaskStatus) => void;
    onEditFormChange: (patch: Partial<TaskEditForm>) => void;
    onTagSelectChange: (value: string) => void;
    onNewTagNameChange: (value: string) => void;
    onAssignTag: () => void;
    onRemoveTag: (tagId: number) => void;
    onCommentTextChange: (value: string) => void;
    onSubmitComment: (event: FormEvent) => void;
    onStartCommentEdit: (commentId: number, text: string) => void;
    onCancelCommentEdit: () => void;
    onEditingCommentTextChange: (value: string) => void;
    onSaveCommentEdit: () => void;
    onRemoveComment: (commentId: number) => void;
    onLoadMoreComments: () => void;
}) {
    const projectName = selectedProject?.name ?? text.common.project;
    const taskTags = activeTask.tags ?? [];
    const visibleEditTags = taskEditTags;
    const availableTags = tags.filter(
        (tag) => !visibleEditTags.some(
            (taskTag) => taskTag.id === tag.id || taskTag.name.toLowerCase() === tag.name.toLowerCase()
        )
    );

    return (
        <div className="task-details-page">
            <header className="task-details-header">
                <div className="task-details-heading">
                    <div className="task-details-breadcrumb">
                        <span>{text.common.taskboard}</span>
                        <span>/</span>
                        <span>{projectName}</span>
                        {mode === "edit" && (
                            <>
                                <span>/</span>
                                <span>{"Редактирование"}</span>
                            </>
                        )}
                    </div>
                    <div className="task-details-titlebar">
                        <h2>{activeTask.title}</h2>
                        {mode === "edit" && (
                            <span className="task-details-mode-badge">
                {"Режим редактирования"}
              </span>
                        )}
                    </div>
                    {mode === "view" && (
                        <div className="task-details-status-row">
                            {canChangeStatus ? (
                                <select
                                    className="task-status-quick-select"
                                    aria-label="Изменить статус задачи"
                                    value={activeTask.status}
                                    onChange={(event) => onStatusChange(event.target.value as TaskStatus)}
                                >
                                    {STATUSES.map((status) => (
                                        <option key={status} value={status}>
                                            {statusLabels[status]}
                                        </option>
                                    ))}
                                </select>
                            ) : (
                                <span className={`status-chip status-${activeTask.status.toLowerCase()}`}>
                  {statusLabels[activeTask.status]}
                </span>
                            )}
                            <span className={`priority-chip priority-${activeTask.priority.toLowerCase()}`}>
                {priorityLabels[activeTask.priority]}
              </span>
                        </div>
                    )}
                </div>
                <div className="task-details-actions">
                    {mode === "view" ? (
                        <>
                            <button className="secondary-button" type="button" onClick={onBack}>
                                <ChevronDown className="rotate-left" size={16} />
                                {"Назад к доске"}
                            </button>
                            {canEditTask && (
                                <button className="primary-button" type="button" onClick={onStartEdit}>
                                    <Edit3 size={16} />
                                    {"Редактировать задачу"}
                                </button>
                            )}
                        </>
                    ) : (
                        <>
                            <button className="secondary-button" type="button" onClick={onCancelEdit}>
                                {text.actions.cancel}
                            </button>
                            <button className="primary-button" type="button" onClick={onSaveEdit}>
                                {"Сохранить"}
                            </button>
                        </>
                    )}
                </div>
            </header>

            <div className="task-details-layout">
                <main className="task-details-main">
                    {mode === "view" ? (
                        <>
                            <section className="task-details-card">
                                <SectionHeader title={"Описание"} />
                                <p className="task-details-description">{activeTask.description}</p>
                            </section>

                            <section className="task-details-card compact">
                                <SectionHeader title={"Теги"} />
                                <div className="task-details-tag-row">
                                    {taskTags.length ? (
                                        taskTags.map((tag) => (
                                            <span className="tag-chip task-details-tag" key={tag.id}>
                        <Tag size={12} />
                                                {tag.name}
                      </span>
                                        ))
                                    ) : (
                                        <span className="task-details-muted">{text.common.none}</span>
                                    )}
                                </div>
                            </section>

                            <section className="task-details-card comments-card">
                                <SectionHeader title={text.modals.comments} />
                                {taskDetailError && <p className="form-error task-detail-error">{taskDetailError}</p>}
                                <div className="task-comments-list">
                                    {taskComments.length ? (
                                        taskComments.map((comment) => (
                                            <article key={comment.id} className="task-comment-row">
                                                <div className="task-comment-avatar">
                                                    {initials(comment.authorUsername ?? text.common.unknown) || "?"}
                                                </div>
                                                <div className="task-comment-body">
                                                    <div className="task-comment-meta">
                                                        <strong>{comment.authorUsername ?? text.common.unknown}</strong>
                                                        <span>·</span>
                                                        <span>{formatDate(comment.createdAt)}</span>
                                                        {comment.updatedAt && comment.updatedAt !== comment.createdAt && (
                                                            <span>{"изменено"}</span>
                                                        )}
                                                    </div>
                                                    {editingCommentId === comment.id ? (
                                                        <div className="comment-editor">
                              <textarea
                                  rows={3}
                                  value={editingCommentText}
                                  onChange={(event) => onEditingCommentTextChange(event.target.value)}
                              />
                                                            <div className="comment-editor-actions">
                                                                <button
                                                                    className="primary-button"
                                                                    type="button"
                                                                    disabled={!editingCommentText.trim()}
                                                                    onClick={onSaveCommentEdit}
                                                                >
                                                                    {"Сохранить"}
                                                                </button>
                                                                <button className="secondary-button" type="button" onClick={onCancelCommentEdit}>
                                                                    {text.actions.cancel}
                                                                </button>
                                                            </div>
                                                        </div>
                                                    ) : (
                                                        <p>{comment.text}</p>
                                                    )}
                                                </div>
                                                {(comment.authorId === currentUser?.id || canEditComments) && (
                                                    <div className="task-comment-actions">
                                                        {editingCommentId !== comment.id && (
                                                            <button
                                                                className="icon-button"
                                                                type="button"
                                                                onClick={() => onStartCommentEdit(comment.id, comment.text)}
                                                                aria-label={"Редактировать комментарий"}
                                                            >
                                                                <Edit3 size={15} />
                                                            </button>
                                                        )}
                                                        <button
                                                            className="icon-button"
                                                            type="button"
                                                            onClick={() => onRemoveComment(comment.id)}
                                                            aria-label={text.actions.deleteTask}
                                                        >
                                                            <Trash2 size={15} />
                                                        </button>
                                                    </div>
                                                )}
                                            </article>
                                        ))
                                    ) : (
                                        <p className="task-details-muted">
                                            {"Комментариев пока нет."}
                                        </p>
                                    )}
                                </div>
                                <div className="task-comments-pagination">
                                    {hasMoreComments && (
                                        <button
                                            className="secondary-button"
                                            type="button"
                                            disabled={loadingMoreComments}
                                            onClick={onLoadMoreComments}
                                        >
                                            {loadingMoreComments
                                                ? "Загрузка..."
                                                : "Показать ещё"}
                                        </button>
                                    )}
                                    <span>
                    {`Показано ${taskComments.length} комментариев${commentsPage > 0 ? ` · страница ${commentsPage + 1}` : ""}`}
                  </span>
                                </div>
                                <form className="task-comment-form" onSubmit={onSubmitComment}>
                  <textarea
                      rows={2}
                      value={commentText}
                      onChange={(event) => onCommentTextChange(event.target.value)}
                      placeholder={text.forms.addComment}
                  />
                                    <button className="primary-button" type="submit" disabled={!commentText.trim()}>
                                        <MessageSquare size={16} />
                                        {text.actions.comment}
                                    </button>
                                </form>
                            </section>
                        </>
                    ) : (
                        <section className="task-details-card edit-card">
                            <SectionHeader title={"Редактирование задачи"} />
                            <div className="task-edit-form">
                                <label className="detail-control task-edit-full">
                                    <span>{text.forms.taskTitle}</span>
                                    <input
                                        required
                                        value={taskEditForm.title}
                                        onChange={(event) => onEditFormChange({ title: event.target.value })}
                                    />
                                </label>
                                <label className="detail-control task-edit-full">
                                    <span>{text.forms.taskDescription}</span>
                                    <textarea
                                        required
                                        rows={7}
                                        value={taskEditForm.description}
                                        onChange={(event) => onEditFormChange({ description: event.target.value })}
                                    />
                                </label>
                                <label className="detail-control">
                                    <span>{text.team.status}</span>
                                    <select
                                        value={taskEditForm.status}
                                        onChange={(event) => onEditFormChange({ status: event.target.value as TaskStatus })}
                                    >
                                        {STATUSES.map((status) => (
                                            <option key={status} value={status}>
                                                {statusLabels[status]}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label className="detail-control">
                                    <span>{text.forms.priority}</span>
                                    <select
                                        value={taskEditForm.priority}
                                        onChange={(event) => onEditFormChange({ priority: event.target.value as Priority })}
                                    >
                                        {PRIORITIES.map((priority) => (
                                            <option key={priority} value={priority}>
                                                {priorityLabels[priority]}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label className="detail-control">
                                    <span>{text.forms.assignee}</span>
                                    <select
                                        value={taskEditForm.assigneeId}
                                        onChange={(event) => onEditFormChange({ assigneeId: event.target.value })}
                                    >
                                        <option value="">{text.common.unassigned}</option>
                                        {projectMembers.map((member) => (
                                            <option key={member.id} value={member.id}>
                                                {member.username}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label className="detail-control">
                                    <span>{text.forms.dueDate}</span>
                                    <input
                                        type="datetime-local"
                                        value={taskEditForm.dueDate}
                                        onChange={(event) => onEditFormChange({ dueDate: event.target.value })}
                                    />
                                </label>
                                <section className="task-edit-full task-edit-tags">
                                    <div className="detail-control">
                                        <span>{"Теги"}</span>
                                    </div>
                                    <div className="task-tag-edit-row">
                                        {visibleEditTags.map((tag) => (
                                            <button
                                                className="task-tag-edit-chip"
                                                key={tag.id}
                                                type="button"
                                                onClick={() => onRemoveTag(tag.id)}
                                            >
                                                <Tag size={12} />
                                                {tag.name}
                                                <X size={12} />
                                            </button>
                                        ))}
                                    </div>
                                    <div className="task-tag-add-row">
                                        <select value={tagSelect} onChange={(event) => onTagSelectChange(event.target.value)}>
                                            <option value="">{text.forms.assignTag}</option>
                                            {availableTags.map((tag) => (
                                                <option key={tag.id} value={tag.id}>
                                                    {tag.name}
                                                </option>
                                            ))}
                                        </select>
                                        <input
                                            value={newTagName}
                                            onChange={(event) => onNewTagNameChange(event.target.value)}
                                            placeholder={"Новый тег"}
                                        />
                                        <button
                                            className="secondary-button"
                                            type="button"
                                            onClick={onAssignTag}
                                            disabled={!tagSelect && !newTagName.trim()}
                                        >
                                            <Plus size={15} />
                                            {text.actions.assign}
                                        </button>
                                    </div>
                                </section>
                                <div className="task-edit-actions task-edit-full">
                                    <button
                                        className="primary-button"
                                        type="button"
                                        disabled={!taskEditForm.title.trim() || !taskEditForm.description.trim()}
                                        onClick={onSaveEdit}
                                    >
                                        {"Сохранить изменения"}
                                    </button>
                                    <button className="danger-link-button" type="button" onClick={() => onDeleteTask(activeTask.id)}>
                                        <Trash2 size={15} />
                                        {"Удалить задачу"}
                                    </button>
                                </div>
                                {taskDetailError && <p className="form-error task-detail-error task-edit-full">{taskDetailError}</p>}
                            </div>
                        </section>
                    )}
                </main>
                <aside className="task-details-sidebar">
                    <TaskDetailsInfoCard
                        text={text}
                        task={activeTask}
                        selectedProject={selectedProject}
                    />
                </aside>
            </div>
        </div>
    );
}

function TaskDetailsInfoCard({
                                 text,
                                 task,
                                 selectedProject
                             }: {
    text: Text;
    task: TaskDetailsResponse;
    selectedProject: ProjectDetailsResponse | null;
}) {
    return (
        <section className="task-details-card task-info-card">
            <SectionHeader title={"Информация о задаче"} />
            <div className="task-info-list">
                <TaskInfoRow icon={FolderOpen} label={text.forms.projectSelect} value={selectedProject?.name ?? text.common.project} highlight />
                <TaskInfoRow icon={UserCircle} label={"Создатель"} value={task.creatorUsername ?? text.common.unknown} />
                <TaskInfoRow icon={UserCircle} label={text.forms.assignee} value={task.assigneeUsername ?? text.common.unassigned} />
                <TaskInfoRow icon={Clock3} label={text.forms.dueDate} value={formatShortDate(task.dueDate)} accent={Boolean(task.dueDate)} />
                <TaskInfoRow icon={Clock3} label={"Создано"} value={formatDate(task.createdAt)} />
                <TaskInfoRow icon={Clock3} label={"Обновлено"} value={formatDate(task.updatedAt)} />
            </div>
        </section>
    );
}

function TaskInfoRow({
                         icon: Icon,
                         label,
                         value,
                         highlight = false,
                         accent = false
                     }: {
    icon: typeof FolderOpen;
    label: string;
    value: string;
    highlight?: boolean;
    accent?: boolean;
}) {
    return (
        <div className="task-info-row">
            <Icon size={17} />
            <span>{label}</span>
            <strong className={`${highlight ? "highlight" : ""} ${accent ? "accent" : ""}`}>{value}</strong>
        </div>
    );
}

function TeamView({
                      text,
                      projects,
                      roleLabels,
                      selectedProject,
                      selectedProjectId,
                      currentUser,
                      currentRole,
                      canManageMembers,
                      canEditMembers,
                      onSelectProject,
                      onAddMember,
                      onUpdateRole,
                      onRemoveMember
                  }: {
    text: Text;
    projects: ProjectResponse[];
    roleLabels: Record<ProjectRole, string>;
    selectedProject: ProjectDetailsResponse | null;
    selectedProjectId: number | null;
    currentUser: UserResponse | null;
    currentRole: ProjectRole | null;
    canManageMembers: boolean;
    canEditMembers: boolean;
    onSelectProject: (projectId: number) => void;
    onAddMember: () => void;
    onUpdateRole: (userId: number, role: ProjectRole) => void;
    onRemoveMember: (userId: number) => void;
}) {
    return (
        <div className="view-stack">
            <Toolbar
                title={text.team.title}
                subtitle={text.team.subtitle}
                selectValue={selectedProjectId ?? ""}
                projects={projects}
                text={text}
                onSelectProject={onSelectProject}
                action={
                    <button className="primary-button" onClick={onAddMember} disabled={!canEditMembers}>
                        <UserPlus size={16} />
                        {text.actions.addMember}
                    </button>
                }
            />
            <section className="stat-grid three">
                <StatCard
                    icon={Users}
                    label={text.team.totalMembers}
                    value={selectedProject?.membersCount ?? 0}
                    accent="primary"
                />
                <StatCard
                    icon={FolderOpen}
                    label={text.team.tasks}
                    value={selectedProject?.tasksCount ?? 0}
                    accent="tertiary"
                />
                <StatCard
                    icon={Shield}
                    label={text.team.yourRole}
                    value={currentRole ? roleLabels[currentRole] : text.common.none}
                    accent="secondary"
                />
            </section>
            <div className="table-panel">
                <table>
                    <thead>
                    <tr>
                        <th>{text.team.name}</th>
                        <th>{text.team.role}</th>
                        <th>{text.team.status}</th>
                        <th>{text.team.actions}</th>
                    </tr>
                    </thead>
                    <tbody>
                    {selectedProject?.users.map((member) => (
                        <tr key={member.id}>
                            <td>
                                <div className="person-cell">
                                    <div className="avatar small">{initials(member.username)}</div>
                                    <div>
                                        <strong>{member.username}</strong>
                                        <span>
                        {member.id === currentUser?.id ? text.common.currentUser : `ID ${member.id}`}
                      </span>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <select
                                    value={member.role}
                                    disabled={!canManageMembers || member.id === currentUser?.id}
                                    onChange={(event) =>
                                        onUpdateRole(member.id, event.target.value as ProjectRole)
                                    }
                                >
                                    {ROLES.map((role) => (
                                        <option key={role} value={role}>
                                            {roleLabels[role]}
                                        </option>
                                    ))}
                                </select>
                            </td>
                            <td>
                                <span className="status-chip status-in_progress">{text.common.active}</span>
                            </td>
                            <td>
                                <button
                                    className="icon-button"
                                    disabled={
                                        !canEditMembers ||
                                        member.id === currentUser?.id ||
                                        (!canManageMembers && member.role !== "MEMBER")
                                    }
                                    onClick={() => onRemoveMember(member.id)}
                                    aria-label={text.team.removeMember}
                                >
                                    <Trash2 size={16} />
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

function ProfileView({
                         text,
                         currentUser,
                         dashboard,
                         onEditProfile
                     }: {
    text: Text;
    currentUser: UserResponse;
    dashboard: DashboardResponse | null;
    onEditProfile: () => void;
}) {
    const displayName = `${currentUser.firstName} ${currentUser.lastName}`.trim() || currentUser.username;

    return (
        <div className="view-stack profile-view">
            <section className="toolbar profile-toolbar">
                <div>
                    <p className="eyebrow">{text.nav.profile}</p>
                    <h2>{text.profile.title}</h2>
                    <p>{text.profile.subtitle}</p>
                </div>
                <button className="primary-button" onClick={onEditProfile}>
                    <Edit3 size={16} />
                    {text.actions.editProfile}
                </button>
            </section>

            <section className="profile-grid">
                <article className="profile-identity">
                    <div className="profile-avatar">
                        <span>{initials(displayName)}</span>
                    </div>
                    <h3>{displayName}</h3>
                    <p>@{currentUser.username}</p>
                    <div className="profile-stats">
                        <div>
                            <strong>{dashboard?.totalProjects ?? 0}</strong>
                            <span>{text.profile.projects}</span>
                        </div>
                        <div>
                            <strong>{dashboard?.activeTasks ?? 0}</strong>
                            <span>{text.profile.activeTasks}</span>
                        </div>
                    </div>
                </article>

                <article className="profile-card wide">
                    <ProfileCardHeader icon={UserCircle} title={text.profile.identityData} />
                    <div className="profile-fields">
                        <ProfileField label={text.auth.username} value={currentUser.username} />
                        <ProfileField label={text.profile.primaryEmail} value={currentUser.email} />
                        <ProfileField label={text.profile.firstName} value={currentUser.firstName} />
                        <ProfileField label={text.profile.lastName} value={currentUser.lastName} />
                    </div>
                </article>

            </section>
        </div>
    );
}

function ProfileCardHeader({
                               icon: Icon,
                               title
                           }: {
    icon: typeof UserCircle;
    title: string;
}) {
    return (
        <div className="profile-card-header">
            <div>
                <Icon size={18} />
            </div>
            <h3>{title}</h3>
        </div>
    );
}

function ProfileField({
                          label,
                          value,
                          mono = false
                      }: {
    label: string;
    value: string;
    mono?: boolean;
}) {
    return (
        <div className="profile-field">
            <span>{label}</span>
            <strong className={mono ? "mono" : ""}>{value}</strong>
        </div>
    );
}

function Toolbar({
                     text,
                     title,
                     subtitle,
                     selectValue,
                     projects,
                     onSelectProject,
                     action,
                     meta = null
                 }: {
    text: Text;
    title: string;
    subtitle: string;
    selectValue: number | "";
    projects: ProjectResponse[];
    onSelectProject: (projectId: number) => void;
    action: ReactNode;
    meta?: ReactNode;
}) {
    return (
        <section className="toolbar">
            <div>
                <p className="eyebrow">{text.common.taskboard}</p>
                <h2>{title}</h2>
                <p>{subtitle}</p>
            </div>
            <div className="toolbar-actions">
                <div className="toolbar-select-stack">
                    <label className="select-shell">
                        <select
                            value={selectValue}
                            onChange={(event) => onSelectProject(Number(event.target.value))}
                        >
                            {!projects.length && <option value="">{text.forms.chooseProject}</option>}
                            {projects.map((project) => (
                                <option key={project.id} value={project.id}>
                                    {project.name}
                                </option>
                            ))}
                        </select>
                        <ChevronDown size={15} />
                    </label>
                    {meta}
                </div>
                {action}
            </div>
        </section>
    );
}

function StatCard({
                      icon: Icon,
                      label,
                      value,
                      accent,
                      onClick
                  }: {
    icon: typeof Activity;
    label: string;
    value: number | string;
    accent: "primary" | "secondary" | "tertiary" | "error";
    onClick?: () => void;
}) {
    const content = (
        <>
            <div>
                <Icon size={21} />
            </div>
            <span>{label}</span>
            <strong>{value}</strong>
        </>
    );

    if (onClick) {
        return (
            <button className={`stat-card stat-card-button accent-${accent}`} onClick={onClick}>
                {content}
            </button>
        );
    }

    return (
        <article className={`stat-card accent-${accent}`}>
            {content}
        </article>
    );
}

function SectionHeader({ title, subtitle }: { title: string; subtitle?: string }) {
    return (
        <div className="section-header">
            <h3>{title}</h3>
            {subtitle && <p>{subtitle}</p>}
        </div>
    );
}

function Metric({ label, value }: { label: string; value: number | string }) {
    return (
        <div className="metric-row">
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

function Modal({
                   text,
                   title,
                   children,
                   onClose,
                   wide = false
               }: {
    text: Text;
    title: string;
    children: ReactNode;
    onClose: () => void;
    wide?: boolean;
}) {
    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
            <section
                className={`modal-panel ${wide ? "wide" : ""}`}
                role="dialog"
                aria-modal="true"
                aria-label={title}
                onMouseDown={(event) => event.stopPropagation()}
            >
                <div className="modal-header">
                    <h2>{title}</h2>
                    <button className="icon-button" onClick={onClose} aria-label={text.common.close}>
                        <X size={16} />
                    </button>
                </div>
                {children}
            </section>
        </div>
    );
}

function EmptyState({ title }: { title: string }) {
    return (
        <div className="empty-state">
            <Clock3 size={22} />
            <span>{title}</span>
        </div>
    );
}

function LoadingState({ text }: { text: Text }) {
    return (
        <div className="loading-state">
            <Loader2 className="spin" size={24} />
            <span>{text.common.loadingWorkspace}</span>
        </div>
    );
}
