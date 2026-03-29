@echo off
chcp 65001 >nul
echo ================================================
echo    工资记录 App - 一键编译脚本
echo ================================================
echo.

REM 检查 Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java！
    echo 请先安装 JDK 17: https://adoptium.net/
    pause
    exit /b 1
)

REM 检查 Android SDK
if not exist "local.properties" (
    echo [提示] 首次运行，请设置 Android SDK 路径
    echo 例如: C:\Users\你的用户名\AppData\Local\Android\Sdk
    echo.
    set /p SDK_PATH="请输入 Android SDK 路径: "
    echo sdk.dir=%SDK_PATH% > local.properties
    echo [OK] SDK 路径已保存
)

REM 检查 Gradle Wrapper
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [错误] gradle-wrapper.jar 缺失，正在下载...
    powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/nicerobot/nicerobot.github.io/refs/heads/master/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)

REM 编译
echo.
echo [开始] 编译中，请耐心等待（首次编译需下载依赖，约5-10分钟）...
echo.

gradlew.bat assembleDebug

if errorlevel 1 (
    echo.
    echo [失败] 编译出错！请检查上方错误信息
    echo.
    echo 常见问题：
    echo 1. Android SDK 未安装 → https://developer.android.com/studio
    echo 2. 网络无法下载依赖 → 检查VPN/代理设置
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo  编译成功！
echo  APK 文件: app\build\outputs\apk\debug\app-debug.apk
echo ================================================
echo.
echo 复制 APK 到手机安装即可使用
echo.
pause
