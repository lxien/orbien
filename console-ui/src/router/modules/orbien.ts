import { AppRouteRecord } from '@/types/router'

export const penetrationRoutes: AppRouteRecord[] = [
  {
    name: 'HTTP',
    path: '/http',
    component: '/orbien/http',
    meta: {
      title: 'menus.orbien.http',
      icon: 'ri:global-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'HTTPS',
    path: '/https',
    component: '/orbien/https',
    meta: {
      title: 'menus.orbien.https',
      icon: 'ri:shield-keyhole-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'TCP',
    path: '/tcp',
    component: '/orbien/tcp',
    meta: {
      title: 'menus.orbien.tcp',
      icon: 'ri:server-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'UDP',
    path: '/udp',
    component: '/orbien/udp',
    meta: {
      title: 'menus.orbien.udp',
      icon: 'ri:share-forward-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Agent',
    path: '/agent',
    component: '/orbien/agent',
    meta: {
      title: 'menus.orbien.agent',
      icon: 'ri:computer-line',
      roles: ['R_SUPER']
    }
  },
    {
    name: 'PortPool',
    path: '/port-pool',
    component: '/orbien/port-pool',
    meta: {
      title: 'menus.orbien.portPool',
      icon: 'ri:database-2-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Domain',
    path: '/domain',
    component: '/orbien/domain',
    meta: {
      title: 'menus.orbien.domain',
      icon: 'ri:link',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'SSL',
    path: '/ssl',
    component: '/orbien/ssl',
    meta: {
      title: 'menus.orbien.ssl',
      icon: 'ri:shield-check-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Token',
    path: '/token',
    component: '/orbien/token',
    meta: {
      title: 'menus.orbien.token',
      icon: 'ri:key-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'ScheduledJob',
    path: '/scheduled-job',
    component: '/orbien/scheduled-job',
    meta: {
      title: 'menus.orbien.scheduledJob',
      icon: 'ri:time-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Stats',
    path: '/stats',
    component: '/orbien/stats',
    meta: {
      title: 'menus.orbien.stats',
      icon: 'ri:bar-chart-line',
      roles: ['R_SUPER']
    }
  }
]
