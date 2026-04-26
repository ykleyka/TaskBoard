import {
  Activity,
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  ChevronDown,
  Clock3,
  Edit3,
  FolderOpen,
  Languages,
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
  AsyncTaskMetricsResponse,
  AsyncTaskStatusResponse,
  AuthResponse,
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
const LANGUAGE_KEY = "taskboard.language";

const STATUSES: TaskStatus[] = ["TODO", "IN_PROGRESS", "COMPLETED"];
const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const ROLES: ProjectRole[] = ["OWNER", "MANAGER", "MEMBER"];

type Language = "en" | "ru";
type View = "dashboard" | "board" | "reports" | "team" | "profile";
type Modal = "project" | "task" | "member" | "taskDetails" | "profile" | null;

const STATUS_LABELS: Record<Language, Record<TaskStatus, string>> = {
  en: {
    TODO: "To Do",
    IN_PROGRESS: "In Progress",
    COMPLETED: "Completed"
  },
  ru: {
    TODO: "К выполнению",
    IN_PROGRESS: "В работе",
    COMPLETED: "Готово"
  }
};

const PRIORITY_LABELS: Record<Language, Record<Priority, string>> = {
  en: {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
    URGENT: "Urgent"
  },
  ru: {
    LOW: "Низкий",
    MEDIUM: "Средний",
    HIGH: "Высокий",
    URGENT: "Срочный"
  }
};

const ROLE_LABELS: Record<Language, Record<ProjectRole, string>> = {
  en: {
    OWNER: "Owner",
    MANAGER: "Manager",
    MEMBER: "Member"
  },
  ru: {
    OWNER: "Владелец",
    MANAGER: "Менеджер",
    MEMBER: "Участник"
  }
};

const COPY = {
  en: {
    appTitle: "TaskBoard",
    brand: "TaskBoard",
    workspace: "Workspace",
    workspaceSubtitle: "Engineering",
    nav: {
      dashboard: "Dashboard",
      board: "Projects",
      reports: "Reports",
      team: "Team",
      profile: "Profile",
      settings: "Settings",
      logout: "Logout"
    },
    actions: {
      createTask: "Create Task",
      newTask: "New Task",
      createProject: "Create Project",
      newProject: "New Project",
      addMember: "Add Member",
      assign: "Assign",
      comment: "Comment",
      deleteTask: "Delete Task",
      editProfile: "Edit Profile",
      saveProfile: "Save Profile",
      cancel: "Cancel",
      project: "Project",
      newReport: "New Report",
      exportPdf: "Export PDF"
    },
    common: {
      loadingWorkspace: "Loading workspace",
      dismiss: "Dismiss",
      close: "Close",
      noDate: "No date",
      open: "Open",
      project: "Project",
      taskboard: "TaskBoard",
      none: "None",
      active: "Active",
      currentUser: "Current user",
      unknown: "Unknown",
      unassigned: "Unassigned",
      chooseProjectFirst: "Choose a project first",
      noDescription: "No description",
      searchWorkspace: "Search workspace",
      notifications: "Notifications",
      language: "Language",
      selectLanguage: "Interface language",
      asyncStatus: "Async status",
      detected: "Detected",
      clear: "Clear"
    },
    auth: {
      introTitle: "Plan projects without noise",
      introCopy:
        "A focused board for teams, tasks, roles, comments, tags, and project reports.",
      featureBoards: "Project boards",
      featureBoardsCopy: "Membership-scoped boards for focused execution.",
      featurePrecision: "Roles and access",
      featurePrecisionCopy: "Manage metadata with minimal friction.",
      featureVault: "Reports",
      featureVaultCopy: "Generate project summaries and track async execution.",
      loginTitle: "Sign in",
      loginSubtitle: "Use your username or email to continue.",
      registerTitle: "Create Account",
      registerSubtitle: "Join the next generation of task management.",
      loginTab: "Login",
      registerTab: "Register",
      loginField: "Username or email",
      username: "Username",
      email: "Email",
      firstName: "First name",
      lastName: "Last name",
      password: "Password",
      remember: "Persist session on this device",
      submitLogin: "Sign in",
      submitRegister: "Create account",
      switchToRegister: "Initialize new account",
      switchToLogin: "Return to system access",
      nodeStatus: "Node Status",
      encrypted: "Encrypted",
      architecture: "Mode"
    },
    dashboard: {
      eyebrow: "Operational command",
      title: "Welcome back.",
      loadingCopy: "Workspace metrics are loading.",
      activeSummary: (activeTasks: number, totalProjects: number) =>
        `${activeTasks} active tasks across ${totalProjects} projects.`,
      totalProjects: "Total Projects",
      activeTasks: "Active Tasks",
      dueToday: "Due Today",
      collaborators: "Collaborators",
      recentProjects: "Recent Projects",
      recentSubtitle: "Membership-scoped portfolio",
      upcomingTasks: "Upcoming Tasks",
      upcomingSubtitle: "Nearest active deadlines",
      emptyProjects: "No projects yet",
      emptyUpcoming: "No upcoming deadlines"
    },
    board: {
      title: "Project Board",
      fallbackSubtitle: "Central command for task execution",
      empty: "Create a project to start the board"
    },
    reports: {
      title: "Project Reports & Metrics",
      subtitle: "Real-time performance analytics and project health",
      completedTasks: "Completed Tasks",
      overdueTasks: "Overdue Tasks",
      highPriority: "High Priority",
      projectSummary: "Project Summary",
      generateHint: "Generate a report to load metrics",
      distribution: "Tasks Distribution by Status",
      asyncRuntime: "Async Runtime",
      executionCounters: "Execution counters",
      submitted: "Submitted",
      running: "Running",
      completed: "Completed",
      failed: "Failed",
      raceCondition: "Race Condition",
      members: "Members",
      unassigned: "Unassigned",
      nearestDueDate: "Nearest Due Date"
    },
    team: {
      title: "Team",
      subtitle: "Manage roles, permissions, and collaborative access",
      totalMembers: "Total Members",
      tasks: "Tasks",
      yourRole: "Your Role",
      name: "Name",
      role: "Role",
      status: "Status",
      actions: "Actions",
      removeMember: "Remove member"
    },
    profile: {
      title: "User Profile",
      subtitle: "Manage your account details and security settings.",
      projects: "Projects",
      activeTasks: "Active",
      collaborators: "Collaborators",
      identityData: "Identity Data",
      userId: "User ID",
      firstName: "First Name",
      lastName: "Last Name",
      contactVector: "Contact Vector",
      primaryEmail: "Primary Email",
      verified: "Verified",
      accessLevel: "Access Level",
      memberAccess: "Workspace Member",
      registrationTimestamp: "Registration Timestamp",
      updatedAt: "Updated At",
      uptimeStatus: "Uptime Status",
      operational: "Operational",
      languagePanel: "Interface Language",
      languageSubtitle: "Switch all labels, forms, statuses, and dates.",
      accountSecurity: "Account Security",
      accountSecurityCopy:
        "Profile updates are scoped to your authenticated user. Password is optional when editing.",
      passwordHint: "Leave blank to keep the current password"
    },
    forms: {
      projectName: "Name",
      projectDescription: "Description",
      projectSelect: "Project",
      chooseProject: "Choose project",
      taskTitle: "Title",
      taskDescription: "Description",
      assignee: "Assignee",
      priority: "Priority",
      dueDate: "Due Date",
      chooseUser: "Choose user",
      role: "Role",
      user: "User",
      assignTag: "Assign tag",
      addComment: "Add a comment"
    },
    modals: {
      newProject: "New Project",
      createTask: "Create Task",
      addMember: "Add Member",
      editProfile: "Edit Profile",
      taskDetails: "Task Details",
      comments: "Comments"
    }
  },
  ru: {
    appTitle: "Обсидиановый архитектор",
    brand: "TaskBoard",
    workspace: "Рабочая область",
    workspaceSubtitle: "Инжиниринг",
    nav: {
      dashboard: "Дашборд",
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
      assign: "Назначить",
      comment: "Комментировать",
      deleteTask: "Удалить задачу",
      editProfile: "Редактировать",
      saveProfile: "Сохранить профиль",
      cancel: "Отмена",
      project: "Проект",
      newReport: "Новый отчет",
      exportPdf: "Экспорт PDF"
    },
    common: {
      loadingWorkspace: "Загружаем рабочую область",
      dismiss: "Скрыть",
      close: "Закрыть",
      noDate: "Без даты",
      open: "Открыто",
      project: "Проект",
      taskboard: "TaskBoard",
      none: "Нет",
      active: "Активен",
      currentUser: "Текущий пользователь",
      unknown: "Неизвестно",
      unassigned: "Не назначено",
      chooseProjectFirst: "Сначала выберите проект",
      noDescription: "Без описания",
      searchWorkspace: "Поиск по рабочей области",
      notifications: "Уведомления",
      language: "Язык",
      selectLanguage: "Язык интерфейса",
      asyncStatus: "Статус async",
      detected: "Обнаружено",
      clear: "Чисто"
    },
    auth: {
      introTitle: "Управляйте рабочим потоком",
      introCopy:
        "Высокопроизводительная среда для архитекторов цифровых систем: точность, скорость и фокус в единой доске.",
      featureBoards: "Асимметричные доски",
      featureBoardsCopy: "Доски с доступом по участию для сфокусированной работы.",
      featurePrecision: "Хирургическая точность",
      featurePrecisionCopy: "Управляйте метаданными без лишнего трения.",
      featureVault: "Хранилище",
      featureVaultCopy: "Безопасное место для завершенных рабочих циклов.",
      loginTitle: "Доступ к системе",
      loginSubtitle: "Введите учетные данные, чтобы синхронизировать рабочую область.",
      registerTitle: "Создать аккаунт",
      registerSubtitle: "Подключитесь к следующему поколению управления задачами.",
      loginTab: "Вход",
      registerTab: "Регистрация",
      loginField: "Имя пользователя или email",
      username: "Имя пользователя",
      email: "Email",
      firstName: "Имя",
      lastName: "Фамилия",
      password: "Пароль",
      remember: "Сохранять сессию на этом устройстве",
      submitLogin: "Войти",
      submitRegister: "Зарегистрироваться",
      switchToRegister: "Создать новый аккаунт",
      switchToLogin: "Вернуться ко входу",
      nodeStatus: "Статус узла",
      encrypted: "Зашифровано",
      architecture: "Архитектура"
    },
    dashboard: {
      eyebrow: "Оперативный центр",
      title: "С возвращением, архитектор.",
      loadingCopy: "Метрики рабочей области загружаются.",
      activeSummary: (activeTasks: number, totalProjects: number) =>
        `${activeTasks} активных задач в ${totalProjects} проектах.`,
      totalProjects: "Всего проектов",
      activeTasks: "Активные задачи",
      dueToday: "Срок сегодня",
      collaborators: "Участники",
      recentProjects: "Недавние проекты",
      recentSubtitle: "Портфель с учетом ваших ролей",
      upcomingTasks: "Ближайшие задачи",
      upcomingSubtitle: "Активные дедлайны рядом",
      emptyProjects: "Проектов пока нет",
      emptyUpcoming: "Ближайших дедлайнов нет"
    },
    board: {
      title: "Доска проекта",
      fallbackSubtitle: "Центр управления выполнением задач",
      empty: "Создайте проект, чтобы открыть доску"
    },
    reports: {
      title: "Отчеты и метрики проекта",
      subtitle: "Аналитика производительности и состояния проекта в реальном времени",
      completedTasks: "Завершенные задачи",
      overdueTasks: "Просроченные задачи",
      highPriority: "Высокий приоритет",
      projectSummary: "Сводка проекта",
      generateHint: "Сгенерируйте отчет, чтобы загрузить метрики",
      distribution: "Распределение задач по статусам",
      asyncRuntime: "Async выполнение",
      executionCounters: "Счетчики выполнения",
      submitted: "Отправлено",
      running: "В работе",
      completed: "Завершено",
      failed: "Ошибки",
      raceCondition: "Race condition",
      members: "Участники",
      unassigned: "Без исполнителя",
      nearestDueDate: "Ближайший срок"
    },
    team: {
      title: "Архитектор команды",
      subtitle: "Управляйте ролями, полномочиями и совместным доступом",
      totalMembers: "Всего участников",
      tasks: "Задачи",
      yourRole: "Ваша роль",
      name: "Имя",
      role: "Роль",
      status: "Статус",
      actions: "Действия",
      removeMember: "Удалить участника"
    },
    profile: {
      title: "Профиль пользователя",
      subtitle: "Управляйте данными аккаунта и настройками безопасности.",
      projects: "Проекты",
      activeTasks: "Активные",
      collaborators: "Участники",
      identityData: "Идентификация",
      userId: "ID пользователя",
      firstName: "Имя",
      lastName: "Фамилия",
      contactVector: "Контактные данные",
      primaryEmail: "Основной email",
      verified: "Проверен",
      accessLevel: "Уровень доступа",
      memberAccess: "Участник рабочей области",
      registrationTimestamp: "Дата регистрации",
      updatedAt: "Обновлен",
      uptimeStatus: "Состояние",
      operational: "В норме",
      languagePanel: "Язык интерфейса",
      languageSubtitle: "Переключает все подписи, формы, статусы и даты.",
      accountSecurity: "Безопасность аккаунта",
      accountSecurityCopy:
        "Обновление профиля доступно только для вашего авторизованного пользователя. Пароль при редактировании необязателен.",
      passwordHint: "Оставьте пустым, чтобы сохранить текущий пароль"
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
      assignTag: "Назначить тег",
      addComment: "Добавить комментарий"
    },
    modals: {
      newProject: "Новый проект",
      createTask: "Создать задачу",
      addMember: "Добавить участника",
      editProfile: "Редактировать профиль",
      taskDetails: "Детали задачи",
      comments: "Комментарии"
    }
  }
} as const;

type WidenCopy<T> = T extends (...args: infer Args) => infer Return
  ? (...args: Args) => Return
  : T extends string
    ? string
    : T extends object
      ? { -readonly [Key in keyof T]: WidenCopy<T[Key]> }
      : T;

type Copy = WidenCopy<typeof COPY.en>;

const UI_COPY: Record<Language, Copy> = {
  en: {
    ...COPY.en,
    appTitle: "TaskBoard",
    brand: "TaskBoard",
    workspace: "TaskBoard",
    workspaceSubtitle: "Projects",
    auth: {
      ...COPY.en.auth,
      introTitle: "Plan projects without noise",
      introCopy: "A focused board for teams, tasks, roles, comments, tags, and project reports.",
      featureBoards: "Project boards",
      featureBoardsCopy: "Only project members see and edit their work.",
      featurePrecision: "Roles and access",
      featurePrecisionCopy: "OWNER, MANAGER, and MEMBER permissions stay visible.",
      featureVault: "Reports",
      featureVaultCopy: "Generate project summaries and track async execution.",
      loginTitle: "Sign in",
      loginSubtitle: "Use your username or email to continue.",
      submitLogin: "Sign in",
      submitRegister: "Create account",
      nodeStatus: "API",
      encrypted: "Online",
      architecture: "Mode"
    },
    dashboard: {
      ...COPY.en.dashboard,
      eyebrow: "Dashboard",
      title: "",
      recentSubtitle: "Projects available to you",
      upcomingSubtitle: "Nearest deadlines"
    },
    board: {
      ...COPY.en.board,
      fallbackSubtitle: "Tasks by status"
    },
    team: {
      ...COPY.en.team,
      title: "Team",
      subtitle: "Manage members and project roles"
    }
  },
  ru: {
    ...COPY.en,
    appTitle: "TaskBoard",
    brand: "TaskBoard",
    workspace: "TaskBoard",
    workspaceSubtitle: "Projects",
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
      ...COPY.en.actions,
      createTask: "Создать задачу",
      newTask: "Новая задача",
      createProject: "Создать проект",
      newProject: "Новый проект",
      addMember: "Добавить",
      assign: "Добавить",
      comment: "Отправить",
      deleteTask: "Удалить задачу",
      editProfile: "Редактировать",
      saveProfile: "Сохранить",
      project: "Проект",
      newReport: "Новый отчет"
    },
    common: {
      ...COPY.en.common,
      loadingWorkspace: "Загрузка",
      dismiss: "Скрыть",
      close: "Закрыть",
      noDate: "Без даты",
      open: "Открыто",
      project: "Проект",
      none: "Нет",
      active: "Active",
      currentUser: "Вы",
      unknown: "Unknown",
      unassigned: "Без исполнителя",
      chooseProjectFirst: "Сначала выберите проект",
      noDescription: "Без описания",
      notifications: "Уведомления",
      language: "Язык",
      selectLanguage: "Язык интерфейса",
      detected: "Detected",
      clear: "Clear"
    },
    auth: {
      ...COPY.en.auth,
      introTitle: "Планируйте проекты без шума",
      introCopy: "Рабочая доска для задач, ролей, комментариев, тегов и отчетов по проектам.",
      featureBoards: "Project boards",
      featureBoardsCopy: "Проекты видят только участники.",
      featurePrecision: "Roles and access",
      featurePrecisionCopy: "OWNER, MANAGER и MEMBER остаются прозрачными.",
      featureVault: "Reports",
      featureVaultCopy: "Сводки проекта и состояние async-задач.",
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
      remember: "Оставаться в системе",
      submitLogin: "Войти",
      submitRegister: "Создать аккаунт",
      nodeStatus: "API",
      encrypted: "Online",
      architecture: "Mode"
    },
    dashboard: {
      ...COPY.en.dashboard,
      eyebrow: "Обзор",
      title: "",
      activeSummary: (activeTasks: number, totalProjects: number) =>
        `${activeTasks} активных задач в ${totalProjects} проектах.`,
      totalProjects: "Проекты",
      activeTasks: "Active tasks",
      dueToday: "Due today",
      recentProjects: "Последние проекты",
      recentSubtitle: "Проекты, доступные вам",
      upcomingTasks: "Ближайшие задачи",
      upcomingSubtitle: "Ближайшие сроки",
      emptyProjects: "Проектов пока нет",
      emptyUpcoming: "Ближайших задач нет"
    },
    board: {
      ...COPY.en.board,
      title: "Доска проекта",
      fallbackSubtitle: "Задачи по статусам",
      empty: "Создайте проект, чтобы начать"
    },
    reports: {
      ...COPY.en.reports,
      title: "Отчеты",
      subtitle: "Метрики проекта и async-отчеты",
      completedTasks: "Completed",
      overdueTasks: "Overdue",
      highPriority: "High priority",
      projectSummary: "Project summary",
      generateHint: "Создайте отчет, чтобы увидеть метрики",
      members: "Members",
      unassigned: "Unassigned"
    },
    team: {
      ...COPY.en.team,
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
      ...COPY.en.profile,
      title: "Профиль",
      subtitle: "Данные аккаунта и язык интерфейса.",
      projects: "Проекты",
      activeTasks: "Active tasks",
      identityData: "Профиль",
      firstName: "Имя",
      lastName: "Фамилия",
      primaryEmail: "Email",
      languagePanel: "Язык",
      languageSubtitle: "Меняет только основные элементы интерфейса.",
      accountSecurity: "Безопасность",
      accountSecurityCopy: "Профиль может редактировать только текущий пользователь.",
      passwordHint: "Оставьте пустым, чтобы не менять пароль"
    },
    forms: {
      ...COPY.en.forms,
      projectName: "Название",
      projectDescription: "Описание",
      projectSelect: "Проект",
      chooseProject: "Выберите проект",
      taskTitle: "Заголовок",
      taskDescription: "Описание",
      assignee: "Исполнитель",
      dueDate: "Срок",
      chooseUser: "Выберите пользователя",
      user: "Пользователь",
      assignTag: "Тег",
      addComment: "Комментарий"
    },
    modals: {
      ...COPY.en.modals,
      newProject: "Новый проект",
      createTask: "Новая задача",
      addMember: "Добавить участника",
      editProfile: "Редактировать профиль",
      taskDetails: "Задача",
      comments: "Комментарии"
    }
  }
};

const UI_LANGUAGE_NAMES: Record<Language, string> = {
  en: "English",
  ru: "Русский"
};

function getInitialLanguage(): Language {
  const stored = localStorage.getItem(LANGUAGE_KEY);
  return stored === "ru" || stored === "en" ? stored : "ru";
}

function formatDate(value: string | null | undefined, language: Language) {
  if (!value) {
    return UI_COPY[language].common.noDate;
  }
  return new Intl.DateTimeFormat(language === "ru" ? "ru-RU" : "en-US", {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatShortDate(value: string | null | undefined, language: Language) {
  if (!value) {
    return UI_COPY[language].common.open;
  }
  return new Intl.DateTimeFormat(language === "ru" ? "ru-RU" : "en-US", {
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

function welcomeText(language: Language, user: UserResponse | null) {
  const name = displayName(user);
  return language === "ru" ? `С возвращением, ${name}` : `Welcome back, ${name}`;
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
  const [language, setLanguageState] = useState<Language>(() => getInitialLanguage());
  const [currentUser, setCurrentUser] = useState<UserResponse | null>(null);
  const [view, setView] = useState<View>("dashboard");
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
  const [metrics, setMetrics] = useState<AsyncTaskMetricsResponse | null>(null);
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
  const [tagSelect, setTagSelect] = useState("");
  const [newTagName, setNewTagName] = useState("");

  const copy = UI_COPY[language];
  const statusLabels = STATUS_LABELS[language];
  const priorityLabels = PRIORITY_LABELS[language];
  const roleLabels = ROLE_LABELS[language];

  function setLanguage(nextLanguage: Language) {
    localStorage.setItem(LANGUAGE_KEY, nextLanguage);
    setLanguageState(nextLanguage);
  }

  useEffect(() => {
    setApiToken(accessToken);
    if (!accessToken) {
      setBusy(false);
      return;
    }
    void bootstrap();
  }, [accessToken]);

  useEffect(() => {
    if (view !== "reports" || !accessToken) {
      return;
    }
    void refreshMetrics();
  }, [view, accessToken]);

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
  const canEditSelectedProject = currentRole !== null;
  const canEditSelectedMembers = isEditableRole(currentRole);
  const canManageSelectedMembers = canManageRole(currentRole);
  const canExtendActiveTaskDeadline = Boolean(
    activeTask &&
      currentUser &&
      (isEditableRole(currentRole) || activeTask.creatorId === currentUser.id)
  );

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

  async function refreshMetrics() {
    try {
      setMetrics(await api.asyncMetrics());
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

  async function submitProject(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await api.createProject(projectForm);
      setProjectForm({ name: "", description: "" });
      setModal(null);
      await bootstrap(created.id);
      setView("board");
    } catch (error) {
      setNotice(errorMessage(error));
    }
  }

  async function submitTask(event: FormEvent) {
    event.preventDefault();
    const projectId = Number(taskForm.projectId || selectedProjectId);
    if (!projectId) {
      setNotice(copy.common.chooseProjectFirst);
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

  async function moveTask(task: ProjectTaskSummaryResponse, status: TaskStatus) {
    if (!selectedProjectId || task.status === status) {
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
        language === "ru" ? "Укажите корректный дедлайн." : "Provide a valid deadline."
      );
      return;
    }
    if (activeTask.dueDate && nextDueDate.getTime() <= new Date(activeTask.dueDate).getTime()) {
      setTaskDetailError(
        language === "ru"
          ? "Новый дедлайн должен быть позже текущего."
          : "The new deadline must be later than the current one."
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
    if (!selectedProjectId) {
      return;
    }
    try {
      await api.deleteTask(taskId);
      setTaskDetailError(null);
      setActiveTask(null);
      setModal(null);
      await loadProject(selectedProjectId);
      await refreshDashboard();
    } catch (error) {
      setTaskDetailError(errorMessage(error));
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
      setTagSelect("");
      setNewTagName("");
      setModal("taskDetails");
    } catch (error) {
      setNotice(errorMessage(error));
    }
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
      await openTask(activeTask.id);
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
      await openTask(activeTask.id);
    } catch (error) {
      setTaskDetailError(errorMessage(error));
    }
  }

  async function assignTagToActiveTask() {
    if (!activeTask || (!tagSelect && !newTagName.trim())) {
      return;
    }
    try {
      const tagId = tagSelect
        ? Number(tagSelect)
        : (tags.find((tag) => tag.name.toLowerCase() === newTagName.trim().toLowerCase())?.id
          ?? (await api.createTag(newTagName.trim())).id);
      if (!tagSelect) {
        setTags(await api.tags());
      }
      await api.assignTag(activeTask.id, tagId);
      setTaskDetailError(null);
      setTagSelect("");
      setNewTagName("");
      await openTask(activeTask.id);
      await refreshDashboard();
    } catch (error) {
      setTaskDetailError(errorMessage(error));
    }
  }

  async function removeTagFromActiveTask(tagId: number) {
    if (!activeTask) {
      return;
    }
    try {
      await api.removeTag(activeTask.id, tagId);
      setTaskDetailError(null);
      await openTask(activeTask.id);
      await refreshDashboard();
    } catch (error) {
      setTaskDetailError(errorMessage(error));
    }
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
      await refreshMetrics();
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
        copy={copy}
        language={language}
        onAuthenticated={handleAuth}
        onLanguageChange={setLanguage}
      />
    );
  }

  return (
    <div className="app-shell">
      <Sidebar
        copy={copy}
        currentView={view}
        onChangeView={setView}
        onCreateTask={() => {
          setTaskForm((form) => ({
            ...form,
            projectId: selectedProjectId ? String(selectedProjectId) : form.projectId
          }));
          setModal("task");
        }}
        onLogout={logout}
      />
      <div className="content-shell">
        <Topbar
          copy={copy}
          currentUser={currentUser}
          language={language}
          onCreateProject={() => setModal("project")}
          onOpenProfile={() => setView("profile")}
          onLanguageChange={setLanguage}
        />
        <main className="workspace">
          {notice && (
            <div className="notice">
              <span>{notice}</span>
              <button
                className="icon-button"
                onClick={() => setNotice(null)}
                aria-label={copy.common.dismiss}
              >
                <X size={16} />
              </button>
            </div>
          )}
          {busy ? (
            <LoadingState copy={copy} />
          ) : (
            <>
              {view === "dashboard" && (
                <DashboardView
                  copy={copy}
                  currentUser={currentUser}
                  dashboard={dashboard}
                  language={language}
                  projects={projects}
                  onSelectProject={(id) => {
                    void loadProject(id);
                    setView("board");
                  }}
                  onCreateProject={() => setModal("project")}
                  onOpenBoard={() => setView("board")}
                  onOpenTask={(taskId) => void openTask(taskId)}
                />
              )}
              {view === "board" && (
                <BoardView
                  copy={copy}
                  language={language}
                  priorityLabels={priorityLabels}
                  projects={projects}
                  selectedProject={selectedProject}
                  selectedProjectId={selectedProjectId}
                  loading={loadingProject}
                  canEdit={canEditSelectedProject}
                  statusLabels={statusLabels}
                  onSelectProject={(id) => void loadProject(id)}
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
                  copy={copy}
                  dashboard={dashboard}
                  language={language}
                  metrics={metrics}
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
                  copy={copy}
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
                  copy={copy}
                  currentUser={currentUser}
                  dashboard={dashboard}
                  language={language}
                  onEditProfile={openProfileEditor}
                  onLanguageChange={setLanguage}
                />
              )}
            </>
          )}
        </main>
      </div>
      {modal === "project" && (
        <Modal copy={copy} title={copy.modals.newProject} onClose={() => setModal(null)}>
          <form className="form-stack" onSubmit={(event) => void submitProject(event)}>
            <label>
              <span>{copy.forms.projectName}</span>
              <input
                required
                value={projectForm.name}
                onChange={(event) =>
                  setProjectForm((form) => ({ ...form, name: event.target.value }))
                }
              />
            </label>
            <label>
              <span>{copy.forms.projectDescription}</span>
              <textarea
                rows={4}
                value={projectForm.description}
                onChange={(event) =>
                  setProjectForm((form) => ({ ...form, description: event.target.value }))
                }
              />
            </label>
            <button className="primary-button" type="submit">
              <Plus size={16} />
              {copy.actions.createProject}
            </button>
          </form>
        </Modal>
      )}
      {modal === "task" && (
        <Modal copy={copy} title={copy.modals.createTask} onClose={() => setModal(null)}>
          <form className="form-stack" onSubmit={(event) => void submitTask(event)}>
            <label>
              <span>{copy.forms.projectSelect}</span>
              <select
                required
                value={taskForm.projectId || selectedProjectId || ""}
                onChange={(event) =>
                  setTaskForm((form) => ({ ...form, projectId: event.target.value }))
                }
              >
                <option value="">{copy.forms.chooseProject}</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>{copy.forms.taskTitle}</span>
              <input
                required
                value={taskForm.title}
                onChange={(event) =>
                  setTaskForm((form) => ({ ...form, title: event.target.value }))
                }
              />
            </label>
            <label>
              <span>{copy.forms.taskDescription}</span>
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
                <span>{copy.forms.assignee}</span>
                <select
                  value={taskForm.assigneeId}
                  onChange={(event) =>
                    setTaskForm((form) => ({ ...form, assigneeId: event.target.value }))
                  }
                >
                  <option value="">{copy.common.unassigned}</option>
                  {projectMembers.map((member) => (
                    <option key={member.id} value={member.id}>
                      {member.username}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>{copy.forms.priority}</span>
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
              <span>{copy.forms.dueDate}</span>
              <input
                type="datetime-local"
                value={taskForm.dueDate}
                onChange={(event) =>
                  setTaskForm((form) => ({ ...form, dueDate: event.target.value }))
                }
              />
            </label>
            <label>
              <span>Tags</span>
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
              <span>{language === "ru" ? "Новый тег" : "New tag"}</span>
              <input
                value={taskForm.newTagName}
                onChange={(event) =>
                  setTaskForm((form) => ({ ...form, newTagName: event.target.value }))
                }
              />
            </label>
            <button className="primary-button" type="submit">
              <Plus size={16} />
              {copy.actions.createTask}
            </button>
          </form>
        </Modal>
      )}
      {modal === "member" && selectedProject && (
        <Modal copy={copy} title={copy.modals.addMember} onClose={() => setModal(null)}>
          <form className="form-stack" onSubmit={(event) => void submitMember(event)}>
            <label>
              <span>{copy.forms.user}</span>
              <select
                required
                value={memberForm.userId}
                onChange={(event) =>
                  setMemberForm((form) => ({ ...form, userId: event.target.value }))
                }
              >
                <option value="">{copy.forms.chooseUser}</option>
                {availableUsers.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.username} · {user.email}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>{copy.forms.role}</span>
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
              {copy.actions.addMember}
            </button>
          </form>
        </Modal>
      )}
      {modal === "taskDetails" && activeTask && (
        <Modal copy={copy} title={copy.modals.taskDetails} onClose={() => setModal(null)} wide>
          <div className="task-detail-grid">
            <section className={`detail-panel task-hero priority-edge-${activeTask.priority.toLowerCase()}`}>
              <div className="task-detail-meta">
                <span className={`status-chip status-${activeTask.status.toLowerCase()}`}>
                  {statusLabels[activeTask.status]}
                </span>
                <span className={`priority-chip priority-${activeTask.priority.toLowerCase()}`}>
                  {priorityLabels[activeTask.priority]}
                </span>
                {isOverdueTask(activeTask.dueDate, activeTask.status) && (
                  <span className="status-chip status-overdue">
                    {language === "ru" ? "Просрочено" : "Overdue"}
                  </span>
                )}
                <span>{formatDate(activeTask.dueDate, language)}</span>
              </div>
              <h2>{activeTask.title}</h2>
              <p className="detail-copy">{activeTask.description}</p>
              <div className="tag-row">
                {activeTask.tags.map((tag) => (
                  <button
                    key={tag.id}
                    className="tag-chip"
                    onClick={() => void removeTagFromActiveTask(tag.id)}
                    disabled={!canEditSelectedProject}
                  >
                    <Tag size={13} />
                    {tag.name}
                  </button>
                ))}
              </div>
              {canEditSelectedProject && (
                <div className="task-edit-panel">
                  <div className="task-tag-fields">
                    <select value={tagSelect} onChange={(event) => setTagSelect(event.target.value)}>
                      <option value="">{copy.forms.assignTag}</option>
                      {tags
                        .filter((tag) => !activeTask.tags.some((assigned) => assigned.id === tag.id))
                        .map((tag) => (
                          <option key={tag.id} value={tag.id}>
                            {tag.name}
                          </option>
                        ))}
                    </select>
                    <input
                      value={newTagName}
                      onChange={(event) => setNewTagName(event.target.value)}
                      placeholder={language === "ru" ? "Новый тег" : "New tag"}
                    />
                  </div>
                  <div className="task-detail-actions">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => void assignTagToActiveTask()}
                      disabled={!tagSelect && !newTagName.trim()}
                    >
                      <Plus size={15} />
                      {copy.actions.assign}
                    </button>
                    <button
                      className="danger-button"
                      type="button"
                      onClick={() => void removeTask(activeTask.id)}
                    >
                      <Trash2 size={16} />
                      {copy.actions.deleteTask}
                    </button>
                  </div>
                </div>
              )}
              {(canEditSelectedProject || canExtendActiveTaskDeadline) && (
                <section className="task-inline-controls">
                  <SectionHeader
                    title={language === "ru" ? "Управление задачей" : "Task Controls"}
                    subtitle={
                      language === "ru"
                        ? "Статус, исполнитель и дедлайн"
                        : "Status, assignee, and deadline"
                    }
                  />
                  {canEditSelectedProject && (
                    <div className="task-control-grid">
                      <label className="detail-control">
                        <span>{copy.team.status}</span>
                        <select
                          value={activeTask.status}
                          onChange={(event) =>
                            void changeActiveTaskStatus(event.target.value as TaskStatus)
                          }
                        >
                          {STATUSES.map((status) => (
                            <option key={status} value={status}>
                              {statusLabels[status]}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="detail-control">
                        <span>{copy.forms.assignee}</span>
                        <select
                          value={activeTask.assigneeId ?? ""}
                          onChange={(event) => void changeActiveTaskAssignee(event.target.value)}
                        >
                          <option value="">{copy.common.unassigned}</option>
                          {projectMembers.map((member) => (
                            <option key={member.id} value={member.id}>
                              {member.username}
                            </option>
                          ))}
                        </select>
                      </label>
                    </div>
                  )}
                  {canExtendActiveTaskDeadline && (
                    <div className="deadline-extend-panel">
                      <label className="detail-control">
                        <span>{language === "ru" ? "Продлить дедлайн" : "Extend deadline"}</span>
                        <input
                          type="datetime-local"
                          value={deadlineDraft}
                          onChange={(event) => setDeadlineDraft(event.target.value)}
                        />
                      </label>
                      <button
                        className="secondary-button"
                        type="button"
                        disabled={!deadlineDraft}
                        onClick={() => void extendActiveTaskDeadline()}
                      >
                        <Clock3 size={15} />
                        {language === "ru" ? "Обновить дедлайн" : "Update deadline"}
                      </button>
                    </div>
                  )}
                  {taskDetailError && <p className="form-error task-detail-error">{taskDetailError}</p>}
                </section>
              )}
            </section>
            <section className="detail-panel task-side-panel">
              <div className="task-side-stack">
                {activeTask && false && (canEditSelectedProject || canExtendActiveTaskDeadline) && (
                  <section className="task-side-section">
                    <SectionHeader
                      title={language === "ru" ? "Управление задачей" : "Task Controls"}
                      subtitle={
                        language === "ru"
                          ? "Статус, исполнитель и дедлайн"
                          : "Status, assignee, and deadline"
                      }
                    />
                    {canEditSelectedProject && (
                      <div className="task-control-grid">
                        <label className="detail-control">
                          <span>{copy.team.status}</span>
                          <select
                            value={activeTask!.status}
                            onChange={(event) =>
                              void changeActiveTaskStatus(event.target.value as TaskStatus)
                            }
                          >
                            {STATUSES.map((status) => (
                              <option key={status} value={status}>
                                {statusLabels[status]}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label className="detail-control">
                          <span>{copy.forms.assignee}</span>
                          <select
                            value={activeTask!.assigneeId ?? ""}
                            onChange={(event) => void changeActiveTaskAssignee(event.target.value)}
                          >
                            <option value="">{copy.common.unassigned}</option>
                            {projectMembers.map((member) => (
                              <option key={member.id} value={member.id}>
                                {member.username}
                              </option>
                            ))}
                          </select>
                        </label>
                      </div>
                    )}
                    {canExtendActiveTaskDeadline && (
                      <div className="deadline-extend-panel">
                        <label className="detail-control">
                          <span>{language === "ru" ? "Продлить дедлайн" : "Extend deadline"}</span>
                          <input
                            type="datetime-local"
                            value={deadlineDraft}
                            onChange={(event) => setDeadlineDraft(event.target.value)}
                          />
                        </label>
                        <button
                          className="secondary-button"
                          type="button"
                          disabled={!deadlineDraft}
                          onClick={() => void extendActiveTaskDeadline()}
                        >
                          <Clock3 size={15} />
                          {language === "ru" ? "Обновить дедлайн" : "Update deadline"}
                        </button>
                      </div>
                    )}
                    {taskDetailError && <p className="form-error task-detail-error">{taskDetailError}</p>}
                  </section>
                )}

                <section className="task-side-section">
                  <SectionHeader
                    title={language === "ru" ? "Сведения" : "Task Details"}
                    subtitle={selectedProject?.name ?? copy.common.project}
                  />
                  <div className="metric-list task-meta-list">
                    {!canEditSelectedProject && (
                      <Metric
                        label={copy.forms.assignee}
                        value={activeTask.assigneeUsername ?? copy.common.unassigned}
                      />
                    )}
                    <Metric
                      label={copy.forms.projectSelect}
                      value={selectedProject?.name ?? copy.common.project}
                    />
                    <Metric
                      label={copy.forms.dueDate}
                      value={formatShortDate(activeTask.dueDate, language)}
                    />
                  </div>
                </section>

                <section className="task-side-section comments-section">
                  <SectionHeader title={copy.modals.comments} subtitle={`${activeTask.comments.length}`} />
                  <div className="comment-list">
                    {activeTask.comments.map((comment) => (
                      <article key={comment.id} className="comment-row">
                        <div>
                          <strong>{comment.authorUsername ?? copy.common.unknown}</strong>
                          <p>{comment.text}</p>
                        </div>
                        {(comment.authorId === currentUser?.id || canEditSelectedMembers) && (
                          <button
                            className="icon-button"
                            onClick={() => void removeComment(comment.id)}
                            aria-label={copy.actions.deleteTask}
                          >
                            <Trash2 size={15} />
                          </button>
                        )}
                      </article>
                    ))}
                  </div>
                  <form className="comment-form" onSubmit={(event) => void submitComment(event)}>
                    <textarea
                      rows={3}
                      value={commentText}
                      onChange={(event) => setCommentText(event.target.value)}
                      placeholder={copy.forms.addComment}
                    />
                    <button className="primary-button" type="submit">
                      <MessageSquare size={16} />
                      {copy.actions.comment}
                    </button>
                  </form>
                </section>
              </div>
            </section>
          </div>
        </Modal>
      )}
      {modal === "profile" && currentUser && (
        <Modal copy={copy} title={copy.modals.editProfile} onClose={() => setModal(null)}>
          <form className="form-stack" onSubmit={(event) => void submitProfile(event)}>
            <label>
              <span>{copy.auth.username}</span>
              <input
                required
                value={profileForm.username}
                onChange={(event) =>
                  setProfileForm((form) => ({ ...form, username: event.target.value }))
                }
              />
            </label>
            <label>
              <span>{copy.auth.email}</span>
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
                <span>{copy.auth.firstName}</span>
                <input
                  required
                  value={profileForm.firstName}
                  onChange={(event) =>
                    setProfileForm((form) => ({ ...form, firstName: event.target.value }))
                  }
                />
              </label>
              <label>
                <span>{copy.auth.lastName}</span>
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
              <span>{copy.auth.password}</span>
              <input
                type="password"
                placeholder={copy.profile.passwordHint}
                value={profileForm.password}
                onChange={(event) =>
                  setProfileForm((form) => ({ ...form, password: event.target.value }))
                }
              />
            </label>
            <button className="primary-button" type="submit">
              <Edit3 size={16} />
              {copy.actions.saveProfile}
            </button>
          </form>
        </Modal>
      )}
    </div>
  );
}

function AuthScreen({
  copy,
  language,
  onAuthenticated,
  onLanguageChange
}: {
  copy: Copy;
  language: Language;
  onAuthenticated: (response: AuthResponse) => void;
  onLanguageChange: (language: Language) => void;
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
              <strong>{copy.brand}</strong>
            </div>
            <h1>{copy.auth.introTitle}</h1>
            <p>{copy.auth.introCopy}</p>
          </div>
          <div className="auth-feature-list">
            <AuthFeature
              icon={LayoutDashboard}
              title={copy.auth.featureBoards}
              text={copy.auth.featureBoardsCopy}
            />
            <AuthFeature
              icon={Shield}
              title={copy.auth.featurePrecision}
              text={copy.auth.featurePrecisionCopy}
            />
            <AuthFeature
              icon={FolderOpen}
              title={copy.auth.featureVault}
              text={copy.auth.featureVaultCopy}
            />
          </div>
          <div className="auth-metadata">
            <Metric label={copy.auth.nodeStatus} value={copy.auth.encrypted} />
            <Metric label={copy.auth.architecture} value="SPA + API" />
          </div>
        </aside>
        <section className="auth-panel">
          <div className="auth-panel-top">
            <div>
              <h1>{mode === "login" ? copy.auth.loginTitle : copy.auth.registerTitle}</h1>
              <p>{mode === "login" ? copy.auth.loginSubtitle : copy.auth.registerSubtitle}</p>
            </div>
            <LanguageSwitch
              copy={copy}
              language={language}
              onLanguageChange={onLanguageChange}
            />
          </div>
          <div className="segmented">
            <button
              className={mode === "login" ? "active" : ""}
              onClick={() => setMode("login")}
              type="button"
            >
              {copy.auth.loginTab}
            </button>
            <button
              className={mode === "register" ? "active" : ""}
              onClick={() => setMode("register")}
              type="button"
            >
              {copy.auth.registerTab}
            </button>
          </div>
          <form className="form-stack" onSubmit={(event) => void submit(event)}>
            {mode === "login" ? (
              <label>
                <span>{copy.auth.loginField}</span>
                <input
                  required
                  value={form.login}
                  onChange={(event) => setForm((next) => ({ ...next, login: event.target.value }))}
                />
              </label>
            ) : (
              <>
                <label>
                  <span>{copy.auth.username}</span>
                  <input
                    required
                    value={form.username}
                    onChange={(event) =>
                      setForm((next) => ({ ...next, username: event.target.value }))
                    }
                  />
                </label>
                <label>
                  <span>{copy.auth.email}</span>
                  <input
                    required
                    type="email"
                    value={form.email}
                    onChange={(event) => setForm((next) => ({ ...next, email: event.target.value }))}
                  />
                </label>
                <div className="form-grid">
                  <label>
                    <span>{copy.auth.firstName}</span>
                    <input
                      required
                      value={form.firstName}
                      onChange={(event) =>
                        setForm((next) => ({ ...next, firstName: event.target.value }))
                      }
                    />
                  </label>
                  <label>
                    <span>{copy.auth.lastName}</span>
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
              <span>{copy.auth.password}</span>
              <input
                required
                type="password"
                value={form.password}
                onChange={(event) => setForm((next) => ({ ...next, password: event.target.value }))}
              />
            </label>
            <label className="checkbox-line">
              <input type="checkbox" />
              <span>{copy.auth.remember}</span>
            </label>
            {error && <p className="form-error">{error}</p>}
            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? <Loader2 className="spin" size={16} /> : <Shield size={16} />}
              {mode === "login" ? copy.auth.submitLogin : copy.auth.submitRegister}
            </button>
          </form>
        </section>
      </section>
    </main>
  );
}

function AuthFeature({
  icon: Icon,
  title,
  text
}: {
  icon: typeof LayoutDashboard;
  title: string;
  text: string;
}) {
  return (
    <div className="auth-feature">
      <div>
        <Icon size={18} />
      </div>
      <div>
        <strong>{title}</strong>
        <span>{text}</span>
      </div>
    </div>
  );
}

function LanguageSwitch({
  copy,
  language,
  onLanguageChange
}: {
  copy: Copy;
  language: Language;
  onLanguageChange: (language: Language) => void;
}) {
  return (
    <label className="language-switch" title={copy.common.selectLanguage}>
      <Languages size={16} />
      <select
        aria-label={copy.common.selectLanguage}
        value={language}
        onChange={(event) => onLanguageChange(event.target.value as Language)}
      >
        {(["ru", "en"] as Language[]).map((item) => (
          <option key={item} value={item}>
            {UI_LANGUAGE_NAMES[item]}
          </option>
        ))}
      </select>
    </label>
  );
}

function Sidebar({
  copy,
  currentView,
  onChangeView,
  onCreateTask,
  onLogout
}: {
  copy: Copy;
  currentView: View;
  onChangeView: (view: View) => void;
  onCreateTask: () => void;
  onLogout: () => void;
}) {
  const items: { view: View; label: string; icon: typeof LayoutDashboard }[] = [
    { view: "dashboard", label: copy.nav.dashboard, icon: LayoutDashboard },
    { view: "board", label: copy.nav.board, icon: FolderOpen },
    { view: "reports", label: copy.nav.reports, icon: BarChart3 },
    { view: "team", label: copy.nav.team, icon: Users },
    { view: "profile", label: copy.nav.profile, icon: UserCircle }
  ];
  return (
    <aside className="sidebar">
      <div className="workspace-logo">
        <div className="logo-cube">
          <Sparkles size={17} />
        </div>
        <div>
          <strong>{copy.workspace}</strong>
          <span>{copy.workspaceSubtitle}</span>
        </div>
      </div>
      <nav>
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <button
              key={item.view}
              className={`nav-item ${currentView === item.view ? "active" : ""}`}
              onClick={() => onChangeView(item.view)}
            >
              <Icon size={19} />
              {item.label}
            </button>
          );
        })}
      </nav>
      <div className="sidebar-footer">
        <button className="primary-button compact" onClick={onCreateTask}>
          <Plus size={16} />
          <span>{copy.actions.createTask}</span>
        </button>
        <button className="nav-item ghost" onClick={onLogout}>
          <LogOut size={18} />
          <span>{copy.nav.logout}</span>
        </button>
      </div>
    </aside>
  );
}

function Topbar({
  copy,
  currentUser,
  language,
  onCreateProject,
  onOpenProfile,
  onLanguageChange
}: {
  copy: Copy;
  currentUser: UserResponse | null;
  language: Language;
  onCreateProject: () => void;
  onOpenProfile: () => void;
  onLanguageChange: (language: Language) => void;
}) {
  return (
    <header className="topbar">
      <div>
        <h1>{copy.appTitle}</h1>
      </div>
      <button className="secondary-button top-action" onClick={onCreateProject}>
        <Plus size={15} />
        {copy.actions.project}
      </button>
      <LanguageSwitch copy={copy} language={language} onLanguageChange={onLanguageChange} />
      <button className="avatar avatar-button" onClick={onOpenProfile} aria-label={copy.nav.profile}>
        {initials(currentUser?.username ?? "User")}
      </button>
    </header>
  );
}

function DashboardView({
  copy,
  currentUser,
  dashboard,
  language,
  projects,
  onSelectProject,
  onCreateProject,
  onOpenBoard,
  onOpenTask
}: {
  copy: Copy;
  currentUser: UserResponse | null;
  dashboard: DashboardResponse | null;
  language: Language;
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
          <p className="eyebrow">{copy.dashboard.eyebrow}</p>
          <h2>{welcomeText(language, currentUser)}</h2>
          <p>
            {dashboard
              ? copy.dashboard.activeSummary(dashboard.activeTasks, dashboard.totalProjects)
              : copy.dashboard.loadingCopy}
          </p>
        </div>
        <button className="primary-button" onClick={onCreateProject}>
          <Plus size={16} />
          {copy.actions.newProject}
        </button>
      </section>
      <section className="stat-grid three">
        <StatCard
          icon={FolderOpen}
          label={copy.dashboard.totalProjects}
          value={dashboard?.totalProjects ?? 0}
          accent="primary"
          onClick={onOpenBoard}
        />
        <StatCard
          icon={Activity}
          label={copy.dashboard.activeTasks}
          value={dashboard?.activeTasks ?? 0}
          accent="tertiary"
          onClick={onOpenBoard}
        />
        <StatCard
          icon={AlertTriangle}
          label={copy.dashboard.dueToday}
          value={dashboard?.dueTodayTasks ?? 0}
          accent="error"
          onClick={onOpenBoard}
        />
      </section>
      <section className="dashboard-grid">
        <div>
          <SectionHeader title={copy.dashboard.recentProjects} subtitle={copy.dashboard.recentSubtitle} />
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
                    <span>{project.description || copy.common.noDescription}</span>
                  </div>
                  <span>{formatShortDate(project.updatedAt, language)}</span>
                </button>
              ))
            ) : (
              <EmptyState title={copy.dashboard.emptyProjects} />
            )}
          </div>
        </div>
        <div>
          <SectionHeader title={copy.dashboard.upcomingTasks} subtitle={copy.dashboard.upcomingSubtitle} />
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
                      {task.projectName ?? copy.common.project} - {formatDate(task.dueDate, language)}
                    </span>
                    {task.overdue && (
                      <em className="task-row-flag">
                        {language === "ru" ? "Дедлайн просрочен" : "Deadline overdue"}
                      </em>
                    )}
                  </div>
                </button>
              ))
            ) : (
              <EmptyState title={copy.dashboard.emptyUpcoming} />
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

function BoardView({
  copy,
  language,
  priorityLabels,
  projects,
  selectedProject,
  selectedProjectId,
  loading,
  canEdit,
  statusLabels,
  onSelectProject,
  onCreateTask,
  onMoveTask,
  onOpenTask
}: {
  copy: Copy;
  language: Language;
  priorityLabels: Record<Priority, string>;
  projects: ProjectResponse[];
  selectedProject: ProjectDetailsResponse | null;
  selectedProjectId: number | null;
  loading: boolean;
  canEdit: boolean;
  statusLabels: Record<TaskStatus, string>;
  onSelectProject: (projectId: number) => void;
  onCreateTask: () => void;
  onMoveTask: (task: ProjectTaskSummaryResponse, status: TaskStatus) => void;
  onOpenTask: (taskId: number) => void;
}) {
  const [draggingTaskId, setDraggingTaskId] = useState<number | null>(null);
  const [dragOverStatus, setDragOverStatus] = useState<TaskStatus | null>(null);

  function dropTask(status: TaskStatus) {
    if (!selectedProject || draggingTaskId === null) {
      return;
    }
    const task = selectedProject.tasks.find((item) => item.id === draggingTaskId);
    if (task) {
      onMoveTask(task, status);
    }
    setDraggingTaskId(null);
    setDragOverStatus(null);
  }

  return (
    <div className="view-stack">
      <Toolbar
        title={copy.board.title}
        subtitle={selectedProject?.description ?? copy.board.fallbackSubtitle}
        selectValue={selectedProjectId ?? ""}
        projects={projects}
        copy={copy}
        onSelectProject={onSelectProject}
        action={
          <button className="primary-button" onClick={onCreateTask} disabled={!canEdit}>
            <Plus size={16} />
            {copy.actions.newTask}
          </button>
        }
      />
      {loading ? (
        <LoadingState copy={copy} />
      ) : selectedProject ? (
        <section className="kanban">
          {STATUSES.map((status) => {
            const tasks = selectedProject.tasks.filter((task) => task.status === status);
            return (
              <div
                className={`kanban-column ${dragOverStatus === status ? "drag-target" : ""}`}
                key={status}
                onDragOver={(event) => {
                  if (canEdit) {
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
                  {tasks.map((task) => (
                    <article
                      className="task-card"
                      draggable={canEdit}
                      key={task.id}
                      onClick={() => onOpenTask(task.id)}
                      onDragEnd={() => {
                        setDraggingTaskId(null);
                        setDragOverStatus(null);
                      }}
                      onDragStart={(event) => {
                        event.dataTransfer.effectAllowed = "move";
                        setDraggingTaskId(task.id);
                      }}
                    >
                      <div className="task-card-top">
                        <span className={`priority-chip priority-${task.priority.toLowerCase()}`}>
                          {priorityLabels[task.priority]}
                        </span>
                        <span>{formatShortDate(task.dueDate, language)}</span>
                      </div>
                      <h3>{task.title}</h3>
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
                        <span>{task.assigneeUsername ?? copy.common.unassigned}</span>
                        {canEdit && (
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
                  ))}
                </div>
              </div>
            );
          })}
        </section>
      ) : (
        <EmptyState title={copy.board.empty} />
      )}
    </div>
  );
}

function ReportsView({
  copy,
  dashboard,
  language,
  metrics,
  projects,
  selectedProject,
  selectedProjectId,
  report,
  statusLabels,
  onSelectProject,
  onGenerateReport
}: {
  copy: Copy;
  dashboard: DashboardResponse | null;
  language: Language;
  metrics: AsyncTaskMetricsResponse | null;
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
        title={copy.reports.title}
        subtitle={copy.reports.subtitle}
        selectValue={selectedProjectId ?? ""}
        projects={projects}
        copy={copy}
        onSelectProject={onSelectProject}
        action={
          <button className="primary-button" onClick={onGenerateReport} disabled={!selectedProject}>
            {report && report.status !== "COMPLETED" && report.status !== "FAILED" ? (
              <Loader2 className="spin" size={16} />
            ) : (
              <BarChart3 size={16} />
            )}
            {copy.actions.newReport}
          </button>
        }
      />
      <section className="stat-grid three">
        <StatCard
          icon={CheckCircle2}
          label={copy.reports.completedTasks}
          value={result?.completedTasksCount ?? dashboard?.completedTasks ?? 0}
          accent="secondary"
        />
        <StatCard
          icon={AlertTriangle}
          label={copy.reports.overdueTasks}
          value={result?.overdueTasksCount ?? dashboard?.overdueTasks ?? 0}
          accent="error"
        />
        <StatCard
          icon={Shield}
          label={copy.reports.highPriority}
          value={result?.highPriorityTasksCount ?? 0}
          accent="tertiary"
        />
      </section>
      <section className="reports-grid">
        <div className="surface-panel analytics-panel">
          <SectionHeader
            title={result?.projectName ?? selectedProject?.name ?? copy.reports.projectSummary}
            subtitle={
              report
                ? `${copy.common.asyncStatus}: ${report.status}`
                : copy.reports.generateHint
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
            <Metric label={copy.reports.members} value={result?.membersCount ?? selectedProject?.membersCount ?? 0} />
            <Metric label={copy.reports.unassigned} value={result?.unassignedTasksCount ?? 0} />
            <Metric
              label={copy.reports.nearestDueDate}
              value={formatShortDate(result?.nearestDueDate ?? null, language)}
            />
          </div>
        </div>
        <div className="surface-panel">
          <SectionHeader title={copy.reports.asyncRuntime} subtitle={copy.reports.executionCounters} />
          <div className="metric-list">
            <Metric label={copy.reports.submitted} value={metrics?.submittedCount ?? 0} />
            <Metric label={copy.reports.running} value={metrics?.runningCount ?? 0} />
            <Metric label={copy.reports.completed} value={metrics?.completedCount ?? 0} />
            <Metric label={copy.reports.failed} value={metrics?.failedCount ?? 0} />
            <Metric
              label={copy.reports.raceCondition}
              value={metrics?.raceConditionDetected ? copy.common.detected : copy.common.clear}
            />
          </div>
        </div>
      </section>
      <section className="surface-panel project-insights">
        <SectionHeader
          title={language === "ru" ? "Полезные показатели" : "Project Insights"}
          subtitle={language === "ru" ? "Состояние выбранного проекта" : "Selected project health"}
        />
        <div className="insight-grid">
          <Metric label={language === "ru" ? "Всего задач" : "Total tasks"} value={totalTasks} />
          <Metric label={language === "ru" ? "В работе" : "Active tasks"} value={activeTasks} />
          <Metric label={language === "ru" ? "Готово" : "Completed"} value={completedTasks} />
          <Metric label={language === "ru" ? "Completion rate" : "Completion rate"} value={`${completionRate}%`} />
          <Metric label={language === "ru" ? "Overdue rate" : "Overdue rate"} value={`${overdueRate}%`} />
          <Metric label={language === "ru" ? "High priority" : "High priority"} value={highPriorityTasks} />
          <Metric label={language === "ru" ? "Без исполнителя" : "Unassigned"} value={unassignedTasks} />
          <Metric label={language === "ru" ? "Участники" : "Team size"} value={teamSize} />
        </div>
      </section>
    </div>
  );
}

function TeamView({
  copy,
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
  copy: Copy;
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
        title={copy.team.title}
        subtitle={copy.team.subtitle}
        selectValue={selectedProjectId ?? ""}
        projects={projects}
        copy={copy}
        onSelectProject={onSelectProject}
        action={
          <button className="primary-button" onClick={onAddMember} disabled={!canEditMembers}>
            <UserPlus size={16} />
            {copy.actions.addMember}
          </button>
        }
      />
      <section className="stat-grid three">
        <StatCard
          icon={Users}
          label={copy.team.totalMembers}
          value={selectedProject?.membersCount ?? 0}
          accent="primary"
        />
        <StatCard
          icon={FolderOpen}
          label={copy.team.tasks}
          value={selectedProject?.tasksCount ?? 0}
          accent="tertiary"
        />
        <StatCard
          icon={Shield}
          label={copy.team.yourRole}
          value={currentRole ? roleLabels[currentRole] : copy.common.none}
          accent="secondary"
        />
      </section>
      <div className="table-panel">
        <table>
          <thead>
            <tr>
              <th>{copy.team.name}</th>
              <th>{copy.team.role}</th>
              <th>{copy.team.status}</th>
              <th>{copy.team.actions}</th>
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
                        {member.id === currentUser?.id ? copy.common.currentUser : `ID ${member.id}`}
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
                  <span className="status-chip status-in_progress">{copy.common.active}</span>
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
                    aria-label={copy.team.removeMember}
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
  copy,
  currentUser,
  dashboard,
  language,
  onEditProfile,
  onLanguageChange
}: {
  copy: Copy;
  currentUser: UserResponse;
  dashboard: DashboardResponse | null;
  language: Language;
  onEditProfile: () => void;
  onLanguageChange: (language: Language) => void;
}) {
  const displayName = `${currentUser.firstName} ${currentUser.lastName}`.trim() || currentUser.username;

  return (
    <div className="view-stack profile-view">
      <section className="toolbar profile-toolbar">
        <div>
          <p className="eyebrow">{copy.nav.profile}</p>
          <h2>{copy.profile.title}</h2>
          <p>{copy.profile.subtitle}</p>
        </div>
        <button className="primary-button" onClick={onEditProfile}>
          <Edit3 size={16} />
          {copy.actions.editProfile}
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
              <span>{copy.profile.projects}</span>
            </div>
            <div>
              <strong>{dashboard?.activeTasks ?? 0}</strong>
              <span>{copy.profile.activeTasks}</span>
            </div>
          </div>
        </article>

        <article className="profile-card wide">
          <ProfileCardHeader icon={UserCircle} title={copy.profile.identityData} />
          <div className="profile-fields">
            <ProfileField label={copy.auth.username} value={currentUser.username} />
            <ProfileField label={copy.profile.primaryEmail} value={currentUser.email} />
            <ProfileField label={copy.profile.firstName} value={currentUser.firstName} />
            <ProfileField label={copy.profile.lastName} value={currentUser.lastName} />
          </div>
        </article>

        <article className="profile-card wide">
          <ProfileCardHeader icon={Languages} title={copy.profile.languagePanel} />
          <p className="profile-muted">{copy.profile.languageSubtitle}</p>
          <LanguageSwitch copy={copy} language={language} onLanguageChange={onLanguageChange} />
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
  copy,
  title,
  subtitle,
  selectValue,
  projects,
  onSelectProject,
  action
}: {
  copy: Copy;
  title: string;
  subtitle: string;
  selectValue: number | "";
  projects: ProjectResponse[];
  onSelectProject: (projectId: number) => void;
  action: ReactNode;
}) {
  return (
    <section className="toolbar">
      <div>
        <p className="eyebrow">{copy.common.taskboard}</p>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      <div className="toolbar-actions">
        <label className="select-shell">
          <select
            value={selectValue}
            onChange={(event) => onSelectProject(Number(event.target.value))}
          >
            {!projects.length && <option value="">{copy.forms.chooseProject}</option>}
            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
          <ChevronDown size={15} />
        </label>
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

function SectionHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="section-header">
      <h3>{title}</h3>
      <p>{subtitle}</p>
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
  copy,
  title,
  children,
  onClose,
  wide = false
}: {
  copy: Copy;
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
          <button className="icon-button" onClick={onClose} aria-label={copy.common.close}>
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

function LoadingState({ copy }: { copy: Copy }) {
  return (
    <div className="loading-state">
      <Loader2 className="spin" size={24} />
      <span>{copy.common.loadingWorkspace}</span>
    </div>
  );
}
