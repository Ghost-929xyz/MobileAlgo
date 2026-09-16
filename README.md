# AlgoForge

> 一个为 Android 设计的离线算法 IDE：Python、C/C++、Java，本地编译运行、调试，以及 CPH 兼容的测试用例工作流。

AlgoForge 面向手机和平板上的刷题、笔试准备和轻量算法开发。它不是把桌面 VS Code 简单缩小，而是围绕移动端交互重新组织：编辑器、测试用例、输入输出、运行状态和调试控制都放在一个可切换的工作台中。

## 核心能力

- **多语言工作区**：Python、C++、C 和 Java 模板与文件识别。
- **本地执行**：通过可插拔 runtime 执行编译、运行和批量测试。
- **环境管理**：App 内下载、校验、更新、修复和删除工具链。
- **CPH 兼容**：读取和写入工作区 `.cph/*.json` 测试文件，支持单测、批量测试和自定义输入。
- **调试协议**：面向 DAP 的会话抽象；运行时提供适配器时支持断点、单步和变量查看。
- **移动端工作台**：文件树、代码编辑、行号、问题面板、测试结果、终端输出和可配置运行命令。
- **隐私优先**：代码和测试默认留在本机；不使用云端编译。

## 重要平台说明

Android 10 起，部分设备会阻止应用从可写私有目录直接执行原生二进制文件。AlgoForge 采用两层策略：

1. **兼容模式（首发）**：`targetSdk = 28`，可侧载安装。下载经过校验的 Termux bootstrap 与 PRoot，在应用私有目录中运行 Python、Clang、OpenJDK。
2. **商店模式（路线图）**：`targetSdk = 35+`，使用嵌入式 Python/JVM 编译器和 `dlopen` 式 native sandbox；C/C++ 远程或自托管编译作为回退。该模式不会伪装成通用 Linux 终端。

这意味着首发版本适合 GitHub Releases 侧载，而不适合直接提交 Google Play。详见 [架构设计](docs/architecture.md) 和 [运行时说明](docs/runtime.md)。

## 快速开始

### 构建

```bash
./gradlew :app:assembleDebug
```

仓库当前暂未提交 `gradle-wrapper.jar`。可以用 Android Studio 打开仓库，或在已安装 Gradle 8.10.2 的环境中先生成 Wrapper：

```bash
gradle wrapper --gradle-version 8.10.2
```

生成后再执行 `./gradlew :app:assembleDebug`。GitHub Actions 已配置为直接使用 Gradle 8.10.2，不依赖仓库内的 Wrapper。

### 首次运行

1. 在 “环境” 页面选择 CPU 架构。
2. 安装 Termux bootstrap 和 PRoot。
3. 打开或创建项目，选择语言模板。
4. 在 “运行” 页填写 stdin，点击运行。
5. 在 “测试” 页导入或创建 CPH 测试用例，点击“全部测试”。

## 仓库结构

```text
app/       Android UI、导航、应用容器
core/      纯 Kotlin 领域模型、CPH 解析、运行计划、DAP 帧协议
runtime/   Android 运行时安装器、PRoot 执行器和调试适配器
docs/      架构、运行时、调试和路线图
scripts/   工具链清单与辅助脚本
```

## 当前状态

这是可编译工程的首个可运行骨架。UI、工作区、CPH 读写、运行计划、runtime 安装流程和 DAP 帧编解码均已落地；真实 APK 仍需 Android SDK、联网下载工具链，并在 arm64 设备上验证。见 [ROADMAP.md](docs/ROADMAP.md)。

## License

Apache-2.0。第三方运行时和工具链保留各自许可证。
