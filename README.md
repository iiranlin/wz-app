# 建设物资管理系统 (wz-App)

本项目是一个基于 Android 开发的物资管理系统移动客户端，主要用于建设工程中的物资流转、监管与数据统计。项目采用原生与 H5 混合开发的模式，集成了安全加密、动画展示及多环境部署等功能。

全程使用VibingCode进行代码开发与审查，使用Antigravity进行代码开发，结合Codex与Cloud Code进行代码审查
审查提示词：
`使用 Git 查看当前暂存区的代码，根据 [Google code review guide](https://google.github.io/eng-practices/review/reviewer/standard.html) 来 review code 指出代码的问题并给出建议`

## 项目特点

- **多环境支持**：内置 `production` (正式) 和 `staging` (测试) 两个构建变体（Build Flavors），支持包名隔离与服务器地址自动切换。
- **混合开发**：核心业务逻辑通过 WebView 加载 H5 页面实现，Native 端提供底层硬件/系统能力支持。
- **安全加固**：集成了 Bouncy Castle 与 Hutool，支持 SM2 等国密算法。
- **流畅体验**：集成 Lottie 动画库，提升用户交互视觉体验。

## 技术栈

- **构建工具**: Gradle
- **开发语言**: Java (JDK 1.8)
- **最低支持**: Android 5.0 (API 21)
- **核心组件**:
  - **网络**: OkHttp 3, FastJSON / GSON
  - **动画**: Lottie
  - **工具库**: Hutool (SM2 加密)
  - **其他**: MultiDex, Android Support Library (28.0.0)

## 项目结构

```text
app/src/main/java/com/cars/material/
├── activity/      # UI 活动页面（WebView 容器、主入口等）
├── application/   # 全局 Application 类，初始化配置
├── base/          # 基础 Activity/Fragment 基类
├── bean/          # 数据实体模型
├── custom/        # 自定义视图与 UI 组件
├── interceptor/   # 网络拦截器（日志、动态 Token 等）
├── manager/       # 业务逻辑管理器（下载、更新等）
├── net/           # 网络请求封装层
└── utils/         # 通用工具类（日志、权限、解密等）
```

## 快速上手

### 环境要求

- Android Studio Chipmunk 或更高版本
- JDK 1.8
- Android SDK 28

### 构建变体选择

在 Android Studio 中打开 **Build Variants** 面板，根据需要选择：

- `productionDebug`: 正式环境调试版
- `stagingDebug`: 测试环境调试版

### 签名配置

项目调试版使用内置的 `debug.keystore`：

- **路径**: `debug.keystore`
- **别名**: `key`
- **密码**: `123456`

## 常用命令

```bash
# 构建所有版本 APK
./build_apks.sh

# 构建正式环境发布包
./gradlew assembleProductionRelease

# 构建测试环境发布包
./gradlew assembleStagingRelease
```

## 注意事项

- 正式版与测试版包名不同（`com.cars.material` vs `com.cars.material.test`），可同时安装在同一台设备上。
- 服务器地址（`SERVER_URL`）会随构建变体自动调整，无需手动修改代码。
