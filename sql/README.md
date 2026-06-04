# 数据库脚本说明

## 1. 执行建表（任选一种）

### 方式 A：复制进容器后执行（推荐，避免 Windows 管道乱码）

```powershell
docker cp sql\init_schema.sql postgres-pgvector:/tmp/init_schema.sql
docker exec postgres-pgvector psql -U root -d mydb -f /tmp/init_schema.sql
```

### 方式 B：在已打开的 psql 里

```powershell
docker exec -it postgres-pgvector psql -U root -d mydb
```

然后手动粘贴 `init_schema.sql` 内容执行。

> 不建议用 `Get-Content ... | docker exec`，在 Windows 下容易因编码导致 SQL 解析失败。

## 2. 验证表是否建好

```sql
\dt
SELECT extname FROM pg_extension WHERE extname = 'vector';
```

## 3. 运行 Java 测试类

确保容器 `postgres-pgvector` 已启动，且已执行上述建表脚本：

```bash
mvn test -Dtest=PostgreSqlConnectionTest
```

运行后在 IDE 控制台或日志里搜索 **`[DB-TEST]`**，可看到 JDBC URL、表列表、pgvector 是否安装等汇总信息。

Hikari / JDBC 细节可在 `application-test.yml` 的 `logging.level` 下开 DEBUG 查看。

## RAG 向量库（PgVector）

- 表名：`vector_store`（Spring AI 首次启动时自动建表，需 `vector` 扩展；当前代码 **1024 维**）
- 详细流程说明：**[`docs/RAG流程说明.md`](../docs/RAG流程说明.md)**
- 首次启动且表为空：会从 `src/main/resources/document/*.md` 导入并调用 embedding
- 配置项（`application.yml`）：
  - `rag.initialize-on-empty: true` — 仅空表时导入（推荐）
  - `rag.skip-startup-load: true` — 测试环境跳过导入

测试：

```bash
# 仅检查 PgVector 接线（不导入文档、不调大模型）
mvn test -Dtest=PgVectorStoreTest

# RAG 全流程：加载文档 → 转向量入库 → 检索增强对话（需百炼 api-key + Docker PG）
mvn test -Dtest=ChefRagPipelineTest
```
