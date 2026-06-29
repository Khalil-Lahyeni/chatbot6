export type IconName =
  | 'grid' | 'radio' | 'wrench' | 'alert' | 'settings'
  | 'bell' | 'search' | 'user' | 'logout' | 'chevron'
  | 'chevronD' | 'check' | 'dot' | 'arrow' | 'train' | 'filter' | 'sort' | 'edit' | 'plus';

export interface NavItem {
  id: string;
  label: string;
  icon: IconName;
  route: string;
  badge?: string | null;
}

export interface AlertItem {
  id: string;
  level: 'critical' | 'warning' | 'info';
  train: string;
  msg: string;
  time: string;
  line: string;
}
