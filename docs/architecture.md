# Architecture

## 设计目标

AlgoForge 的目标不是复刻桌面 IDE，而是把“写代码 -> 编译 -> 输入 -> 运行 -> 对拍/调试”压缩到移动端的单手操作中。

核心原则：

1. **离线优先**：工作区和测试数据是本机文件，网络只用于安装 runtime。
2. **运行时与 UI 解耦**：编辑器不依赖 Python/C++/Java 的具体执行方式。
3. **工具链可替换**：Termux/PRoot 只是首个 provider，后续可加入嵌入式 runtime。
4. **协议优先**：调试使用 DAP，运行结果有统一的事件流，便于增加新语言。
5. **移动端可恢复**：编译或运行意外终止后，状态和输出仍可见。

## 分层

```text
+--------------------------------------------------+
| Compose UI                                       |
|  Editor / Testcases / Console / Settings         |
+----------------------+---------------------------+
                       |
+----------------------v---------------------------+
| Application services                             |
|  Workspace / RunCoordinator / CphService         |
+----------------------+---------------------------+
                       |
+----------------------v---------------------------+
| core (pure Kotlin)                               |
|  Language / RunPlan / CPH model / DAP codec      |
+----------------------+---------------------------+
                       |
+----------------------v---------------------------+
| runtime (Android)                                |
|  RuntimeInstaller / RuntimeExecutor / Adapters   |
+--------------------------------------------------+
```

## 工作区

默认工作区位于 `filesDir/workspaces/default`，每个项目包含：

```text
project/
  main.py | main.cpp | Main.java
  .algoforge/project.json
  .cph/<problem-key>.json
  .algoforge/out/
```

Android 的 scoped storage 不保证普通文件路径可被外部应用访问，因此首发版把工作区保存在应用私有目录，并提供 SAF 导入/导出作为交换层。后续可在用户明确授权后把外部目录作为工作区根。

## 运行模型

`RunPlan` 是纯数据：

- 语言和任务类型（运行、测试、调试）
- 源文件、工作目录、参数和环境变量
- 编译命令与运行命令
- 时间和内存限制
- stdio 策略

`runtime` 把 `RunPlan` 翻译为 PRoot 调用。编译器与解释器使用绝对路径，避免依赖 shell PATH：

```text
proot -0 -r <rootfs> -b <workspace>:/workspace -w /workspace \
  /usr/bin/clang++ -std=c++20 -O2 -g main.cpp -o .algoforge/out/main
```

编译与运行是两个独立阶段，因此可以：

- 在编译失败时保留完整 stderr；
- 在运行阶段切换 stdin；
- 对 CPH 用例复用同一可执行文件；
- 对每个测试用例设置独立 timeout。

## 运行时安装

`RuntimeInstaller` 负责：

1. 选择 ABI/架构对应的 artifact；
2. 使用 HTTPS 流式下载到临时文件；
3. 计算 SHA-256，不匹配则删除；
4. 解压 bootstrap，恢复必要符号链接和权限；
5. 原子替换 runtime 目录；
6. 写入安装记录，供 UI 展示版本、大小和状态。

runtime 的 URL、大小和校验值由 `scripts/runtime-index.json` 维护。仓库不提交二进制。

## 调试

`runtime/debug` 提供 DAP JSON-RPC 帧编解码器：

- 输入：`Content-Length: N\r\n\r\n{...}`
- 输出：初始化、断点、继续、下一步、变量请求
- 事件：stopped、output、terminated

Python 首选 `debugpy`，C/C++ 首选 `lldb-dap`，Java 使用 JDI 适配器。没有适配器时，编辑器仍可使用日志插桩和 CPH 批量测试。

## 失败策略

- runtime 下载失败：保留旧 runtime，显示校验/网络错误。
- 编译超时：杀掉整个进程组，保留已捕获输出。
- App 被系统回收：项目文件已持久化；运行输出可选写入日志。
- PRoot 不可用：禁用原生执行，提供 remote runner provider 作为后续扩展点。
