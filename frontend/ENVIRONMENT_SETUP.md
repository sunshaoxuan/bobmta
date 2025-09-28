# Frontend Offline Environment Setup

由于当前交付环境无法访问公共 NPM 仓库，前端项目采用离线依赖方案，所有运行时组件和样式均预置在 `vendor/` 目录并通过相对路径引入。为避免后续开发再次遇到安装失败的问题，请遵循以下流程：

1. **安装依赖**
   - 执行 `npm install` 仅用于生成或更新 `package-lock.json`，不会连接外网，也无需额外的代理设置。
   - 若 `package-lock.json` 出现异常，请直接删除后重新运行 `npm install`。

2. **编译、测试与预览**
   - 运行 `npm run build` 触发 TypeScript 离线编译，产物输出至 `dist/` 目录。
   - 运行 `npm run test` 会先执行离线编译，再通过 Node 内置测试框架校验 API 客户端与 Mock 数据逻辑，无需额外依赖。
   - 通过 `npm run preview` 启动内置的 Node 静态服务器（默认端口 `4173`），浏览器访问 `http://localhost:4173` 即可加载 `index.html` 与离线依赖。
   - `npm run dev` 会开启 TypeScript watch 编译，适合调试状态下增量构建。

3. **目录结构约束**
   - 不得直接改动 `vendor/` 中的第三方实现，如需升级组件需在本地验证后统一提交。
   - 所有业务代码必须通过相对路径引用依赖，例如 `../vendor/antd/index.js`，禁止重新配置外部模块解析以免触发网络访问。

4. **常见问题**
   - 如果浏览器访问页面时报 404，请确认执行了 `npm run build` 且 `dist/` 与 `vendor/` 位于项目根目录同级。
   - 当需要新增工具库时，应优先评估是否可以自行实现；确需引入第三方库时，请在 `frontend/FRONTEND_REQUIREMENTS.md` 中登记并与后端沟通离线镜像方案。

遵守以上规范可以保障在受限网络环境下持续迭代前端功能，并确保所有团队成员能够快速复现编译与预览过程。
