# Runtime design

## 为什么不把 NDK/JDK 直接塞进 APK

完整的 Android LLVM 工具链、Python 标准库和 JDK 会让 APK 增长到数百 MB，并且 ABI、许可和更新策略都很复杂。AlgoForge 因此把 runtime 做成独立的已校验 artifact，首次安装后即可离线使用。

## 首发 provider：Termux bootstrap + PRoot

首发版提供：

- Python 3
- Clang/Clang++
- OpenJDK（根据 Termux 仓库可用版本）
- Git、GDB/LLDB（可选包）

安装目录：

```text
filesDir/runtimes/termux/
  rootfs/
  bin/proot
  runtime.json
```

工作区通过 bind mount 暴露给 guest：

```text
<workspace> -> /workspace
```

## 兼容性关键点

Android 的应用私有目录是否允许 `execve` 取决于系统版本、SELinux 策略和 `targetSdk`。首发工程使用：

```kotlin
targetSdk = 28
```

这是为了保持侧载场景下与 Termux 类似的执行环境。若未来要上架 Google Play，应切换到嵌入式 runtime provider，不能只修改 `targetSdk` 后继续依赖 PRoot。

## artifact 信任链

`scripts/runtime-index.json` 是唯一允许写入下载地址和校验值的位置。release 前必须由离线 CI 下载 artifact、计算 SHA-256、记录大小和许可证，再更新索引。

示例：

```json
{
  "schemaVersion": 1,
  "artifacts": {
    "termux-bootstrap-aarch64": {
      "version": "pinned-by-release",
      "url": "https://...",
      "sha256": "...",
      "size": 0,
      "license": "GPL-3.0-or-later"
    }
  }
}
```

当前仓库中的索引是安全占位结构，不包含可用的未校验二进制。发布前由维护者填入固定版本和摘要。

## 推荐开发镜像

为了减少首次下载量，CI 可以构建一个带 Python、clang、openjdk 的精简 prefix：

1. 在 GitHub Actions 的 Ubuntu runner 中使用 Android NDK 交叉编译；
2. 生成每个 ABI 的 bootstrap；
3. 对产物做签名与 SHA-256；
4. 上传到 GitHub Releases；
5. 把 immutable URL 写入 manifest。

## 许可

Termux、Python、LLVM、OpenJDK 和其他工具链有各自许可证。App 关于页必须展示 runtime manifest 中的 license 字段，并链接对应的原始许可证。
