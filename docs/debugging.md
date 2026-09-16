# Debugging

AlgoForge 不重新发明调试协议，编辑器侧统一面向 Debug Adapter Protocol (DAP)。

## 适配器路线

| 语言 | 适配器 | 状态 |
|---|---|---|
| Python | debugpy | 计划接入 |
| C/C++ | lldb-dap | 计划接入 |
| Java | JDI bridge | 计划接入 |
| 通用 | 日志插桩 runner | 已有运行事件基础 |

## 移动端限制

- 后台进程容易被系统回收，调试会话必须可恢复并记录最后一个事件；
- 小屏幕上不适合一次显示调用栈、变量和 watch 三栏，采用底部抽屉；
- 断点点击区域至少 32dp，行号栏同时提供语义化无障碍描述；
- 进入后台前应保存断点，而不是假定进程持续存在。

## DAP transport

当前实现提供 stdio 的 `Content-Length` 帧编解码。后续接入：

- request/response/event 类型化模型；
- initialize/launch/attach；
- setBreakpoints/configurationDone；
- continue/next/stepIn/stepOut/pause；
- stackTrace/scopes/variables/evaluate；
- terminated/exited/output/stopped 事件。
