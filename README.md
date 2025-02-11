# Enterprise Authentication Service

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![JDK](https://img.shields.io/badge/JDK-17-green.svg)
![Javalin](https://img.shields.io/badge/Javalin-Latest-orange.svg)

[English](readme.en.md)

---

### 项目简介
基于 Javalin 框架开发的企业认证服务，对接银联 3060 接口，提供 RESTful API 接口，支持 Swagger 文档，使用 Bearer Token 进行身份验证。

### 环境要求
- OpenJDK 17.0.2+
- Maven

### 项目结构
```
.
├── conf/                 # 配置文件目录
│   ├── cert/            # 证书文件
│   └── security.properties # 银联 3060 加密配置文件
├── src/
│   └── main/
│       ├── java/        # 源代码
│       └── resources/   # 资源文件
├── .env.example        # 环境变量文件
└── pom.xml             # Maven 配置文件
```

### 核心模块
- **Config**: 应用配置管理，包含环境变量、错误处理和 Swagger 配置
- **Exception**: 全局统一异常处理
- **Model**: 数据模型定义，包含 POJO 类和统一响应格式
- **Middleware**: 中间件组件，如认证中间件
- **Router**: 路由注册管理
- **Handler**: 请求处理器，负责调用 Service 处理业务逻辑
- **Service**: 无状态的业务逻辑实现层

### 设计原则
- Handler 负责状态管理（如维持数据库连接等有状态维护）
- Service 保持无状态设计
- 统一的错误处理和响应格式
- 模块化的项目结构

### 使用方法
1. 复制 .env.example 文件为 .env 文件，并配置相关环境变量
2. 运行 `mvn clean package assembly:single` 进行编译和打包
3. 运行 `java -jar target/unionpay3060-1.0-SNAPSHOT-with-dependencies.jar` 启动应用
