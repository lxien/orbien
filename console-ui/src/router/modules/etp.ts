import { AppRouteRecord } from '@/types/router'

export const penetrationRoutes: AppRouteRecord[] = [
  {
    name: 'HTTP',
    path: '/http',
    component: '/etp/http',
    meta: {
      title: 'menus.etp.http',
      icon: 'ri:global-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'HTTPS',
    path: '/https',
    component: '/etp/https',
    meta: {
      title: 'menus.etp.https',
      icon: 'ri:shield-keyhole-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'TCP',
    path: '/tcp',
    component: '/etp/tcp',
    meta: {
      title: 'menus.etp.tcp',
      icon: 'ri:server-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Agent',
    path: '/agent',
    component: '/etp/agent',
    meta: {
      title: 'menus.etp.agent',
      icon: 'ri:computer-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Domain',
    path: '/domain',
    component: '/etp/domain',
    meta: {
      title: 'menus.etp.domain',
      icon: 'ri:link',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'SSL',
    path: '/ssl',
    component: '/etp/ssl',
    meta: {
      title: 'menus.etp.ssl',
      icon: 'ri:shield-check-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Token',
    path: '/token',
    component: '/etp/token',
    meta: {
      title: 'menus.etp.token',
      icon: 'ri:key-line',
      roles: ['R_SUPER']
    }
  },
  {
    name: 'Stats',
    path: '/stats',
    component: '/etp/stats',
    meta: {
      title: 'menus.etp.stats',
      icon: 'ri:bar-chart-line',
      roles: ['R_SUPER']
    }
  }
]
