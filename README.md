# SimpleSchedule

一个简洁、现代的课程表应用，用于查看和管理课程安排。

<<<<<<< HEAD
## 功能特性

- 🎨 **现代化UI** - 圆角设计，简洁美观的界面
- 📱 **响应式布局** - 适配各种屏幕尺寸
- 📡 **在线同步** - 自动从服务器获取最新课程数据
- 💾 **本地缓存** - 离线也能查看课程表
- 🔄 **自动登录** - 记住登录状态，无需重复输入
- 📅 **周次切换** - 方便查看不同周的课程
- 🎯 **课程详情** - 点击课程查看详细信息
- ⚙️ **设置选项** - 支持退出登录

## 技术栈

- **开发语言**: Kotlin
- **UI框架**: Android Material Design 3
- **网络请求**: OkHttp
- **JSON解析**: Gson
- **异步处理**: Kotlin Coroutines
- **数据绑定**: ViewBinding
- **导航**: BottomNavigationView + Fragments

## 安装说明

### 方法一：直接安装APK
1. 下载 `app/build/outputs/apk/debug/app-debug.apk`
2. 在Android设备上安装APK文件
=======
## 安装说明

### 方法一：直接安装APK
release里面下载安装
>>>>>>> b57aec2c0427f9dc8ddd0e627d4a5f9cbd7b12f3

### 方法二：从源码构建
1. 克隆仓库
   ```bash
<<<<<<< HEAD
   git clone https://github.com/你的用户名/SimpleSchedule.git
=======
   git clone https://github.com/Rycarl-Furry/CQUPT-SimpleSchedule.git
>>>>>>> b57aec2c0427f9dc8ddd0e627d4a5f9cbd7b12f3
   cd SimpleSchedule
   ```
2. 使用Android Studio打开项目
3. 构建并运行
<<<<<<< HEAD

## 使用方法

1. **首次登录**：输入学号，点击登录
2. **查看课表**：默认显示当前周的课程
3. **切换周次**：使用顶部的左右箭头切换周次
4. **查看详情**：点击课程卡片查看详细信息
5. **退出登录**：在设置页面点击退出登录

## 数据来源

课程数据来源于 `https://cqupt.ishub.top/api/curriculum/学号/curriculum.json`

## 项目结构

```
app/src/main/
├── java/com/example/myapplication/
│   ├── cache/          # 缓存管理
│   ├── model/          # 数据模型
│   ├── network/        # 网络请求
│   ├── ui/             # 界面组件
│   ├── LoginActivity.kt
│   └── MainActivity.kt
└── res/
    ├── anim/           # 动画资源
    ├── drawable/       # 图片和背景
    ├── layout/         # 布局文件
    ├── mipmap-*/       # 应用图标
    └── values/         # 字符串和样式
```

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 联系方式

如有问题或建议，欢迎联系。

---

*SimpleSchedule - 让课程管理更简单*
=======
>>>>>>> b57aec2c0427f9dc8ddd0e627d4a5f9cbd7b12f3
