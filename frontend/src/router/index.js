import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/assets',
    children: [
      {
        path: 'assets',
        name: 'Assets',
        component: () => import('../views/AssetList.vue'),
        meta: { title: '工装台账' },
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('../views/TransferRecord.vue'),
        meta: { title: '移位登记' },
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('../views/InventoryCheck.vue'),
        meta: { title: '实物清点' },
      },
      {
        path: 'scrap',
        name: 'Scrap',
        component: () => import('../views/ScrapRecord.vue'),
        meta: { title: '报废归档' },
      },
      {
        path: 'trace',
        name: 'Trace',
        component: () => import('../views/ToolingTrace.vue'),
        meta: { title: '轨迹履历' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
