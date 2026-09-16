# CPH compatibility

AlgoForge 读取 VS Code 插件 Competitive Programming Helper (CPH) 常用的工作区数据：`.cph/<problem-key>.json`。

支持的字段：

```json
{
  "name": "A. Two Sum",
  "group": "Codeforces Round",
  "url": "https://codeforces.com/...",
  "interactive": false,
  "memoryLimit": 256,
  "timeLimit": 2,
  "tests": [
    { "input": "4\n2 7 11 15\n9\n", "output": "0 1" }
  ]
}
```

实现策略：

- 读取时容忍缺失字段和额外字段；
- 名称、URL、限制和测试数组保持可编辑；
- 写回时使用稳定、可读的 JSON；
- 不依赖 CPH 的 VS Code 扩展运行时；
- 第一版只负责测试用例，不自动提交到在线评测平台。

## 批量运行

1. 编译一次，得到可执行文件；
2. 对每个测试用例复制 input 到 stdin；
3. 按 `timeLimit` 运行，捕获 stdout/stderr 和退出码；
4. 归一化行尾和尾部空白后比较 expected output；
5. 记录耗时、内存（provider 支持时）和 diff；
6. 遇到第一个失败时可配置为立即停止，或继续跑完所有测试。
