# 智慧医院 · 预约挂号系统前端

为后端 Spring Boot 项目（`hospital`）配套的前端单页应用（SPA）。

## 技术栈

- Vue 3（Composition API + `<script setup>`）
- Vite 5
- Vue Router 4
- Pinia 2
- Element Plus 2（中文医疗风格 UI）
- Axios（统一请求封装）
- qrcode（将后端返回的支付宝二维码链接渲染为二维码图片）

## 目录结构

```
Frontend/
├── index.html
├── package.json
├── vite.config.js          # 开发代理：/user /registration /ai -> localhost:8080
└── src/
    ├── main.js             # 应用入口
    ├── App.vue
    ├── api/
    │   ├── request.js      # axios 实例 + token/userId 请求头 + Result 统一解包
    │   └── index.js        # 所有后端接口封装
    ├── store/auth.js       # 登录态 Pinia store
    ├── router/index.js     # 路由 + 登录鉴权守卫
    ├── layouts/MainLayout.vue
    ├── styles/main.css
    └── views/
        ├── Login.vue          # 登录 / 注册
        ├── AiChat.vue         # 智能导诊（流式对话 + 会话记忆）
        ├── Registration.vue   # 预约挂号（号源查询 + 扫码支付）
        └── MyAppointments.vue # 我的挂号（查询 + 取消退款）
```

## 运行方式

1. 确保已安装 Node.js（建议 18+）。
2. 安装依赖：

   ```bash
   cd Frontend
   npm install
   ```

3. 启动后端（Spring Boot，默认端口 `8080`），并确保 MySQL、Redis 已启动。
4. 启动前端开发服务器：

   ```bash
   npm run dev
   ```

   浏览器会自动打开 `http://localhost:5173`。

> 开发环境下，前端通过 Vite 代理把 `/user`、`/registration`、`/ai` 转发到
> `http://localhost:8080`，因此前端与后端可保持不同端口、无需手动处理跨域。

生产构建：

```bash
npm run build      # 产物在 dist/
npm run preview    # 本地预览构建产物
```

## 功能说明

| 页面 | 功能 | 对应后端接口 |
| --- | --- | --- |
| 登录/注册 | 用户名密码登录、患者注册 | `POST /user/login`、`POST /user/register` |
| 智能导诊 | AI 流式对话、会话列表、历史记录、删除会话 | `GET /ai/chat`、`/ai/history/*` |
| 预约挂号 | 查询号源、生成支付二维码、支付后生成挂号单 | `/registration/queryDepartment`、`/registration/generateOrder`、`/registration/addAppointment` |
| 我的挂号 | 查询挂号记录、取消预约并退款 | `/registration/queryAppointment`、`/registration/cancelAppointment` |

## 认证方式

后端 `TokenInterceptor` 通过请求头 `token` 和 `userId` 校验登录态。
前端在 `src/api/request.js` 中统一注入这两个请求头；登录成功后会把
`token`、`userId`、`username` 存入 `localStorage`。收到 `401` 会自动跳回登录页。

## 为跑通前端而做的后端改动（可回退）

后端原本存在两个会让前端无法完整运行的问题，已做最小改动：

1. **`generateOrder` 只返回二维码、不返回订单号**
   - `RegistrationcServiceImpl#generateQrCode` 由返回 `String` 改为返回
     `Map<String, String>`，成功时返回 `{ orderNo, qrCode }`，失败时返回 `{ error }`。
   - 同步修改了接口 `RegistrationcSerrvice` 与控制器 `RegistrationcController`。
   - 这样前端拿到 `orderNo` 后才能在支付完成时调用 `/registration/addAppointment`。

2. **后端未配置 CORS**
   - `WebConfig` 增加了 `addCorsMappings`，允许跨域访问。
   - `TokenInterceptor` 放行 `OPTIONS` 预检请求。

> 说明：`cancelAppointment` 当前后端实现是按用户查询第一条挂号记录退款
> （未按 `appointmentId` 精确定位），且挂号记录未持久化订单号，退款链路在
> 真实支付宝场景下仍需进一步完善。前端已按现有接口契约透传 `appointmentId` 与
> `orderNo`，后端接口完善后无需改动前端。
