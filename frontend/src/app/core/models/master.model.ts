export interface Masters {
  courts: string[];
  matterTypes: string[];
  practiceAreas: string[];
  stages: string[];
  taskTypes: string[];
  priorities: string[];
  docTypes: string[];
  filingStages: string[];
  clientTypes: string[];
  designations: string[];
  departments: string[];
  roles: string[];
}

export interface MasterCard {
  key: keyof Masters;
  title: string;
  icon: string;
  bg: string;
  tc: string;
}

export const MASTER_CARDS: MasterCard[] = [
  { key: 'courts',       title: 'Courts & Forums',   icon: 'fa-landmark',     bg: '#dcfce7', tc: 'var(--g1)' },
  { key: 'matterTypes',  title: 'Matter Types',       icon: 'fa-briefcase',    bg: '#dbeafe', tc: 'var(--blue)' },
  { key: 'practiceAreas',title: 'Practice Areas',     icon: 'fa-balance-scale',bg: '#ede9fe', tc: 'var(--purple)' },
  { key: 'stages',       title: 'Hearing Stages',     icon: 'fa-gavel',        bg: '#fef3c7', tc: 'var(--acc)' },
  { key: 'taskTypes',    title: 'Task Types',         icon: 'fa-tasks',        bg: '#ccfbf1', tc: 'var(--teal)' },
  { key: 'priorities',   title: 'Priority Levels',    icon: 'fa-flag',         bg: '#fee2e2', tc: 'var(--red)' },
  { key: 'docTypes',     title: 'Document Types',     icon: 'fa-file-alt',     bg: '#dcfce7', tc: 'var(--g1)' },
  { key: 'filingStages', title: 'Filing Stages',      icon: 'fa-file-import',  bg: '#fce7f3', tc: '#be185d' },
  { key: 'clientTypes',  title: 'Client Types',       icon: 'fa-users',        bg: '#dbeafe', tc: 'var(--blue)' },
  { key: 'designations', title: 'Designations',       icon: 'fa-id-badge',     bg: '#ede9fe', tc: 'var(--purple)' },
  { key: 'departments',  title: 'Departments',        icon: 'fa-building',     bg: '#fef3c7', tc: 'var(--acc)' },
  { key: 'roles',        title: 'Roles',              icon: 'fa-user-tag',     bg: '#dcfce7', tc: 'var(--g1)' },
];
