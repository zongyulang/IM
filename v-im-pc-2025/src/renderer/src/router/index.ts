import type { RouteRecordRaw } from 'vue-router'
import { createRouter, createWebHashHistory } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/index',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    children: [
      {
        path: 'chat',
        name: 'chatBox',
        component: () => import('../views/chat/Index.vue')
      },
      {
        path: 'friend',
        name: 'friendBox',
        component: () => import('../views/friend/Index.vue'),
        children: [
          {
            path: 'validate',
            name: 'validate',
            component: () => import('../views/friend/friendValidate.vue')
          },
          {
            path: ':id',
            name: 'user',
            component: () => import('../views/friend/UserInfo.vue')
          }
        ]
      },
      {
        path: 'dept',
        name: 'deptBox',
        component: () => import('../views/dept/Index.vue'),
        children: [
          {
            path: ':id',
            name: 'dept',
            component: () => import('../views/dept/DeptInfo.vue')
          }
        ]
      },
      {
        path: 'group',
        name: 'groupBox',
        component: () => import('../views/group/Index.vue'),
        children: [
          {
            path: 'edit',
            name: 'edit',
            component: () => import('../views/group/Edit.vue')
          },
          {
            path: 'invite',
            name: 'invite',
            component: () => import('../views/group/Invite.vue')
          },
          {
            path: ':id',
            name: 'group',
            component: () => import('../views/group/Info.vue')
          }
        ]
      },
      {
        path: 'system',
        name: 'systemBox',
        component: () => import('../views/sys/Index.vue'),
        children: [
          {
            path: 'user',
            name: 'sys-user',
            component: () => import('../views/sys/user/Index.vue')
          },
          {
            path: 'pwd',
            name: 'pwd',
            component: () => import('../views/sys/pwd/Index.vue')
          },
          {
            path: 'info',
            name: 'info',
            component: () => import('../views/sys/info/Index.vue')
          },
          {
            path: 'setting',
            name: 'setting',
            component: () => import('../views/sys/setting/Index.vue')
          }
        ]
      }
    ]
  },
  {
    path: '/',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Register.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})


export default router
