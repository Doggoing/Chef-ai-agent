# 百味智厨 AI 智能体助手

基于 Spring Boot 3 + Spring AI 的 AI 烹饪助手学习项目，从 `yu-ai-agent-master` 精简而来，聚焦核心业务代码。

> **详细代码说明（每个类、调用关系、时序图）：** 见 [docs/代码架构与类关系说明.md](docs/代码架构与类关系说明.md)  
> **登录、会话、对话落库业务流程：** 见 [docs/业务流程说明.md](docs/业务流程说明.md)  
> **RAG 入库 / 问答 / metadata / Advisor 详解：** 见 [docs/RAG流程说明.md](docs/RAG流程说明.md)

## 功能概览

- **多轮对话**：Spring AI `MessageWindowChatMemory` + PostgreSQL 持久化（`chat_message`），按 `chatId` 区分会话
- **结构化输出**：`doChatWithReport` 将 AI 回答映射为 Java `record`（`MealPlanReport`），便于后续处理
- **RAG 知识库**：Markdown 读取与切片 → 百炼 Embedding → PgVector 写入 PostgreSQL `vector_store`
- **工具调用**：联网搜索、网页抓取、文件读写、PDF 生成等
- **MCP**：百炼 QwenImage 菜品图生成（Streamable HTTP）
- **ReAct 智能体**：ChefManus 多步推理 + 工具循环执行

## 前后端端口说明

| 服务 | 端口 | 配置位置 |
|------|------|----------|
| **后端** Spring Boot | `8123` | `application.yml` → `server.port` |
| **后端** 接口前缀 | `/api` | `application.yml` → `server.servlet.context-path` |
| **前端** Vite 开发服务器 | `3000` | `wu-ai-agent-frontend/vite.config.js` → `server.port` |

浏览器地址 `localhost:3000` 是**前端页面**；前端通过 `src/api/index.js` 把请求发到 `http://localhost:8123/api`（后端），两个端口是正常现象。

## 前端与后端接口对应关系

| 前端页面 | 调用的接口 | 未使用的接口（可 Postman / 测试调用） |
|----------|------------|--------------------------------------|
| 烹饪助手 | `/ai/chef_app/chat/sse` | `sync`、`rag`、`tools`、`mcp`、`report` |
| 智厨智能体 | `/ai/chef_manus/chat` | — |

ReAct 智能体通过 **Java 注入的 `ToolCallback[]`** 调用工具，**不会**再去请求 `/chef_app/chat/tools` 这个 HTTP 地址。

---

## 项目目录总览

```
wu-ai-agent/
├── pom.xml                          # Maven 依赖与构建
├── README.md                        # 本说明文档
├── .gitignore                       # Git 忽略规则
├── src/main/java/                   # 后端 Java 源码
├── src/main/resources/              # 配置与知识库文档
├── src/test/                        # 单元 / 集成测试
└── wu-ai-agent-frontend/            # Vue3 前端
```

---

## 根目录文件说明

| 文件 | 说明 |
|------|------|
| `pom.xml` | Maven 工程定义：Spring Boot 3.4、Spring AI 1.1.2、百炼 DashScope、MCP Client、Markdown 文档读取、Knife4j 等依赖 |
| `.gitignore` | 忽略 `target/`、`application-local.yml`（含密钥）、`node_modules/`、`tmp/` 等 |
| `README.md` | 项目说明（当前文件） |

---

## 后端：`src/main/java/com/wu/aiagent/`

### 启动入口

| 文件 | 说明 |
|------|------|
| `WuAiAgentApplication.java` | Spring Boot 启动类，`main` 方法入口 |

---

### `app/` — 核心业务应用

| 文件 | 说明 |
|------|------|
| `ChefApp.java` | **百味智厨主应用**。封装 `ChatClient`，提供：普通多轮对话、流式对话、RAG 菜谱问答、Tool 工具调用、MCP 生图、结构化膳食报告（`MealPlanReport`）。前端「烹饪助手」默认只用到其中的 `doChatByStream` |

---

### `controller/` — HTTP 接口层

| 文件 | 说明 |
|------|------|
| `AiController.java` | 对外 REST 接口。映射 `/ai/chef_app/*`（对话/RAG/工具/MCP/报告）和 `/ai/chef_manus/chat`（智能体 SSE） |
| `HealthController.java` | 健康检查 `GET /health`，返回 `ok`，用于确认服务是否启动 |

**`AiController` 各接口含义：**

| 接口路径 | 对应方法 | 用途 |
|----------|----------|------|
| `/chef_app/chat/sync` | `doChatSync` | 同步一次性返回全文（调试 / 非流式） |
| `/chef_app/chat/sse` | `doChatSSE` | **前端烹饪助手使用**，Flux 流式输出 |
| `/chef_app/chat/server_sent_event` | `doChatServerSentEvent` | SSE 另一种封装写法（教程示例） |
| `/chef_app/chat/sse_emitter` | `doChatSseEmitter` | SSE 第三种写法（SseEmitter） |
| `/chef_app/chat/rag` | `doChatWithRag` | 走 RAG 知识库 + 查询重写 |
| `/chef_app/chat/tools` | `doChatWithTools` | 单次对话中调用 WebSearch、PDF 等工具 |
| `/chef_app/chat/mcp` | `doChatWithMcp` | 调用百炼 QwenImage MCP 生成菜品图 |
| `/chef_app/chat/report` | `doChatWithReport` | 返回结构化 JSON 膳食计划 |
| `/chef_manus/chat` | `doChatWithChefManus` | **前端智能体页使用**，ReAct 多步执行 + SSE |

---

### `agent/` — ReAct 自主智能体

| 文件 | 说明 |
|------|------|
| `BaseAgent.java` | 智能体基类：`run()` / `runStream()` 循环执行 `step()`，维护消息列表、步数上限、状态 |
| `ReActAgent.java` | ReAct 抽象类：每步先 `think()` 再 `act()` |
| `ToolCallAgent.java` | 实现 think/act：`think` 让大模型选工具，`act` 用 `ToolCallingManager` 执行工具 |
| `ChefManus.java` | **百味智厨智能体**实例：配置烹饪相关 System Prompt，注入 `allTools`，最多 20 步 |
| `model/AgentState.java` | 智能体状态枚举：`IDLE`、`RUNNING`、`FINISHED`、`ERROR` |

---

### `rag/` — 检索增强生成（知识库）

| 文件 | 说明 |
|------|------|
| `ChefDocumentLoader.java` | 从 `classpath:document/*.md` 加载菜谱 Markdown，并写入 `category` 等元数据 |
| `ChefVectorStoreConfig.java` | 创建 **PgVector 向量库** `PgVectorStore`，空表时从 Markdown 导入菜谱向量 |
| `MyKeywordEnricher.java` | 用 AI 为每篇文档自动提取关键词，写入 metadata，便于检索 |
| `QueryRewriter.java` | 用户问题查询重写（`RewriteQueryTransformer`），提升 RAG 召回效果 |

---

### `tools/` — Tool Calling 工具集

| 文件 | 说明 |
|------|------|
| `ToolRegistration.java` | Spring 配置类：注册所有 `@Tool` 为 `ToolCallback[]` Bean，供 `ChefApp` 与 `ChefManus` 使用 |
| `WebSearchTool.java` | 联网搜索（SearchAPI），查菜谱、食材信息等 |
| `WebScrapingTool.java` | 网页抓取（Jsoup），解析页面正文 |
| `FileOperationTool.java` | 读写 `tmp/file/` 下文本文件，保存食谱笔记等 |
| `ResourceDownloadTool.java` | 按 URL 下载图片等资源到本地 |
| `TerminalOperationTool.java` | 执行终端命令（需注意安全，仅学习用） |
| `PDFGenerationTool.java` | 用 iText 生成 PDF（如食谱、购物清单） |
| `TerminateTool.java` | 终止智能体循环（`doTerminate`），供 ReAct 正常结束 |

---

### `config/` — 全局配置

| 文件 | 说明 |
|------|------|
| `CorsConfig.java` | 跨域配置，允许前端 `localhost:3000` 访问后端 API |
| `DashScopeMcpConfig.java` | 为 MCP HTTP 请求注入 `Authorization: Bearer {api-key}`，连接百炼 QwenImage |

---

### `advisor/` — ChatClient 拦截增强

| 文件 | 说明 |
|------|------|
| `MyLoggerAdvisor.java` | 自定义 Advisor：打印每次 AI 请求与回复日志，便于调试 |

---

### `constant/` — 常量

| 文件 | 说明 |
|------|------|
| `FileConstant.java` | 文件工具保存根目录：`{项目目录}/tmp` |

---

## 后端：`src/main/resources/`

| 文件 / 目录 | 说明 |
|-------------|------|
| `application.yml` | 主配置：端口 8123、context-path `/api`、百炼模型、MCP 连接地址、`initialized: false` 等 |
| `application-local.yml` | **本地私密配置**（已 gitignore）：真实 `api-key`、SearchAPI Key，覆盖主配置 |
| `application-local.yml.example` | 本地配置模板，复制后改名为 `application-local.yml` 使用 |
| `document/家常菜谱知识库 - 快手篇.md` | RAG 知识库：快手菜、简单家常菜问答 |
| `document/家常菜谱知识库 - 家常篇.md` | RAG 知识库：红烧、炒菜、炖汤等 |
| `document/家常菜谱知识库 - 健康篇.md` | RAG 知识库：减脂、控糖、营养搭配等 |

---

## 后端：`src/test/` — 测试代码

| 文件 | 说明 |
|------|------|
| `resources/application-test.yml` | 测试环境配置：关闭 MCP，避免启动失败 |
| `WuAiAgentApplicationTests.java` | 验证 Spring 容器能正常启动 |
| `controller/HealthControllerTest.java` | MockMvc 测试 `/api/health` |
| `rag/ChefDocumentLoaderTest.java` | 测试 Markdown 菜谱是否加载成功 |
| `app/ChefAppTest.java` | 集成测试：对话、RAG、膳食报告（需有效 api-key） |
| `agent/ChefManusTest.java` | 智能体测试（默认 `@Disabled`，耗时长） |

---

## 前端：`wu-ai-agent-frontend/`

### 根目录

| 文件 | 说明 |
|------|------|
| `package.json` | 前端依赖：Vue3、Vue Router、Vite、Axios、@vueuse/head |
| `vite.config.js` | Vite 配置：**开发端口 3000**、Vue 插件、路径别名 `@` |
| `index.html` | 页面入口 HTML |

---

### `src/` — 前端源码

| 文件 | 说明 |
|------|------|
| `main.js` | Vue 应用入口：挂载 App、注册路由与 head |
| `App.vue` | 根组件，仅包含 `<router-view />` |
| `style.css` | 全局样式 |
| `api/index.js` | **API 封装**：开发环境请求 `http://localhost:8123/api`；`chatWithChefApp`、`chatWithChefManus` |
| `router/index.js` | 路由：`/` 首页、`/chef-assistant` 烹饪助手、`/chef-manus` 智能体 |

---

### `src/views/` — 页面

| 文件 | 说明 |
|------|------|
| `Home.vue` | 首页：两个入口卡片（烹饪助手 / 智厨智能体） |
| `ChefAssistant.vue` | 烹饪助手聊天页，调用 **`/ai/chef_app/chat/sse`**，带 `chatId` 多轮记忆 |
| `ChefManus.vue` | 智能体聊天页，调用 **`/ai/chef_manus/chat`**，展示多步 SSE 结果 |

---

### `src/components/` — 公共组件

| 文件 | 说明 |
|------|------|
| `ChatRoom.vue` | 聊天 UI：消息列表、输入框、发送按钮、流式打字效果 |
| `AiAvatarFallback.vue` | AI 头像占位（烹饪助手 🍳 / 智能体 👨‍🍳） |
| `AppFooter.vue` | 页脚 |

---

## 运行时数据目录（自动生成）

| 路径 | 说明 |
|------|------|
| `tmp/file/` | `FileOperationTool` 读写文件的目录 |
| `target/` | Maven 编译输出，可删除后重新 `mvn compile` |

---

## 快速开始

### 1. 配置密钥

```bash
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

编辑 `application-local.yml`，填写：

- `spring.ai.dashscope.api-key`（必填）
- `search-api.api-key`（使用联网搜索工具时必填）

### 2. 启动后端

```bash
cd E:\wu-ai-agent
mvn spring-boot:run
```

接口文档：http://localhost:8123/api/swagger-ui.html

### 3. 启动前端（可选）

```bash
cd wu-ai-agent-frontend
npm install
npm run dev
```

访问：http://localhost:3000

---

## 单元 / 集成测试

```bash
# 不耗 API 的测试
mvn test -Dtest=WuAiAgentApplicationTests,HealthControllerTest,ChefDocumentLoaderTest

# 会调用百炼大模型（需 application-local.yml 里有效 api-key）
mvn test -Dtest=ChefAppTest
```

---

## 相对原项目 `yu-ai-agent-master` 未包含的内容

- Demo 学习类（`demo/invoke/*`、`MultiQueryExpanderDemo` 等）
- PgVector / 云知识库 / 高德 MCP / Pexels 图片 MCP 服务
- Ollama、LangChain4j、恋爱主题文档与 `LoveApp`

---

## 在 Cursor 中继续开发

若对话记录在 `yu-ai-agent-master` 工作区，而代码在 `E:\wu-ai-agent`：

1. 用 Cursor **打开文件夹** → 选择 `E:\wu-ai-agent`；或  
2. 继续在当前工作区聊天，说明「请修改 E:\wu-ai-agent 下的文件」。

本 README 已汇总目录与程序职责，换工作区后也可快速上手。
