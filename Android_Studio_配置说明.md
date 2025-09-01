# Android Studio 调试配置说明

## 问题解决方案

您遇到的错误 `Unable to determine application id: com.android.tools.idea.run.ApkProvisionException: No outputs for the main artifact of variant: debug` 是因为项目现在使用了 productFlavors，需要在 Android Studio 中选择具体的构建变体。

## 解决步骤

### 1. 打开 Build Variants 面板
- 在 Android Studio 底部状态栏找到 **"Build Variants"** 标签
- 如果没有看到，可以通过菜单 `View` → `Tool Windows` → `Build Variants` 打开

### 2. 选择构建变体
在 Build Variants 面板中，您会看到以下选项：

**Module: app**
- `productionDebug` - 正式版调试模式
- `productionRelease` - 正式版发布模式  
- `stagingDebug` - 测试版调试模式
- `stagingRelease` - 测试版发布模式

### 3. 选择您要调试的版本

#### 调试正式版 (连接到 183.56.240.244:8081)
- 选择 `productionDebug`
- 应用名称显示为：**建设物资**
- 包名：`com.cars.material`

#### 调试测试版 (连接到 1.95.136.93:8080)  
- 选择 `stagingDebug`
- 应用名称显示为：**建设物资-测试**
- 包名：`com.cars.material.test`

### 4. 运行调试
选择好构建变体后，点击运行按钮 ▶️ 或调试按钮 🐛 即可正常在模拟器中运行。

## 构建变体说明

| 构建变体 | 应用名称 | 包名 | 服务器地址 | 用途 |
|---------|---------|------|-----------|------|
| productionDebug | 建设物资 | com.cars.material | http://183.56.240.244:8081/ | 正式环境调试 |
| productionRelease | 建设物资 | com.cars.material | http://183.56.240.244:8081/ | 正式环境发布 |
| stagingDebug | 建设物资-测试 | com.cars.material.test | http://1.95.136.93:8080/ | 测试环境调试 |
| stagingRelease | 建设物资-测试 | com.cars.material.test | http://1.95.136.93:8080/ | 测试环境发布 |

## 打包APK

### 方法1：使用命令行
```bash
# 构建所有版本
./build_apks.sh

# 或单独构建
./gradlew assembleProductionRelease    # 正式版
./gradlew assembleStagingRelease       # 测试版
```

### 方法2：使用Android Studio
1. 选择菜单 `Build` → `Generate Signed Bundle / APK`
2. 选择 `APK`
3. 在构建变体中选择您需要的版本
4. 完成签名配置后构建

## 注意事项

1. **两个版本可以同时安装**：因为包名不同，正式版和测试版可以同时安装在同一设备上
2. **服务器地址自动切换**：代码会根据构建变体自动连接到对应的服务器
3. **应用名称区分**：安装后可以通过应用名称区分是正式版还是测试版

## 运行配置 (Run Configurations)

现在项目已经包含了两个预配置的运行配置：

### 1. 建设物资-正式环境
- **配置名称**: `建设物资-正式环境`
- **构建变体**: `productionDebug`
- **服务器**: http://183.56.240.244:8081/
- **应用名称**: 建设物资
- **包名**: com.cars.material

### 2. 建设物资-测试环境
- **配置名称**: `建设物资-测试环境`
- **构建变体**: `stagingDebug`
- **服务器**: http://1.95.136.93:8080/
- **应用名称**: 建设物资-测试
- **包名**: com.cars.material.test

### 如何使用运行配置

1. **选择运行配置**：
   - 在Android Studio顶部工具栏的运行配置下拉菜单中选择
   - 或者通过 `Run` → `Edit Configurations...` 管理配置

2. **直接运行**：
   - 选择对应的配置后，直接点击运行▶️或调试🐛按钮
   - 无需手动切换Build Variants

## 签名配置

项目已配置debug签名：
- **Keystore文件**: `debug.keystore`
- **密码**: 123456
- **别名**: key

## 如果仍有问题

如果选择构建变体后仍然无法运行，请尝试：
1. `Build` → `Clean Project`
2. `Build` → `Rebuild Project`
3. 重新选择构建变体或运行配置
4. 重启 Android Studio
