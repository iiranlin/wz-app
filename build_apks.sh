#!/bin/bash

# 建设物资APK构建脚本
# 用于构建正式版和测试版APK

echo "=========================================="
echo "        建设物资APK构建脚本"
echo "=========================================="

# 清理项目
echo "正在清理项目..."
./gradlew clean

if [ $? -ne 0 ]; then
    echo "❌ 项目清理失败"
    exit 1
fi

echo "✅ 项目清理完成"

# 构建正式版APK
echo ""
echo "正在构建正式版APK (建设物资)..."
echo "服务器地址: http://183.56.240.244:8081/"
./gradlew assembleProductionRelease

if [ $? -ne 0 ]; then
    echo "❌ 正式版APK构建失败"
    exit 1
fi

echo "✅ 正式版APK构建完成"

# 构建测试版APK
echo ""
echo "正在构建测试版APK (建设物资-测试)..."
echo "服务器地址: http://1.95.136.93:8080/"
./gradlew assembleStagingRelease

if [ $? -ne 0 ]; then
    echo "❌ 测试版APK构建失败"
    exit 1
fi

echo "✅ 测试版APK构建完成"

# 显示构建结果
echo ""
echo "=========================================="
echo "           构建完成！"
echo "=========================================="
echo ""
echo "📱 正式版APK (建设物资):"
echo "   文件位置: app/build/outputs/apk/production/release/app-production-release-unsigned.apk"
echo "   应用名称: 建设物资"
echo "   包名: com.cars.material"
echo "   服务器: http://183.56.240.244:8081/"
echo ""
echo "📱 测试版APK (建设物资-测试):"
echo "   文件位置: app/build/outputs/apk/staging/release/app-staging-release-unsigned.apk"
echo "   应用名称: 建设物资-测试"
echo "   包名: com.cars.material.test"
echo "   服务器: http://1.95.136.93:8080/"
echo ""
echo "🔧 Android Studio使用说明:"
echo "   1. 在Build Variants面板中可以选择不同的构建变体:"
echo "      - productionDebug/productionRelease (正式版)"
echo "      - stagingDebug/stagingRelease (测试版)"
echo "   2. 选择对应的变体后即可运行调试或打包"
echo ""
echo "✅ 所有APK构建完成！"
