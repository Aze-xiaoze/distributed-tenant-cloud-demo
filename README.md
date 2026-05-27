# 分布式多租户云平台

## 项目概述

这是一个企业级的分布式多租户云平台，采用微服务架构，支持多租户数据隔离，提供完整的认证授权、服务治理、监控日志等功能。

## 技术栈

- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.2
- MyBatis-Plus 3.5.5
- Nacos 作为注册中心和配置中心
- Gateway 作为API网关
- JWT 用于认证授权
- Prometheus + Grafana 用于监控
- SkyWalking 用于链路追踪
- MySQL 数据库

## 模块说明

### 1. tenant-common (公共模块)
- 工具类、常量、枚举
- 全局异常处理
- 统一响应结果封装

### 2. tenant-core (核心模块)
- 多租户数据隔离实现
- Redis 配置
- MyBatis-Plus 配置
- 权限相关核心功能

### 3. tenant-api (接口定义模块)
- Feign 接口定义
- DTO 实体类

### 4. tenant-auth (认证授权服务)
- 用户登录、注册
- JWT 认证
- 密码加密

### 5. tenant-system (系统管理服务)
- 用户、角色、菜单管理
- 系统配置管理

### 6. tenant-gateway (网关服务)
- 路由转发
- 跨域处理
- 租户请求转发

## 多租户实现

### 数据隔离策略
- 通过 MyBatis-Plus 多租户插件实现
- 在每个数据表中添加 tenant_id 字段
- 自动在 SQL 查询中添加租户过滤条件

### 租户识别
- 通过 HTTP 请求头 X-Tenant-ID 识别租户
- 使用 ThreadLocal 存储当前租户上下文

## 监控与运维

### 监控指标
- 应用健康状况
- 接口响应时间
- 错误率统计
- JVM 性能指标

### 日志系统
- 结构化日志输出
- SkyWalking 链路追踪
- 日志分级存储

## 配置说明

### Nacos 配置
- 服务注册与发现
- 配置中心管理
- 动态配置更新

### 安全配置
- JWT 认证
- 密码加密
- 接口权限控制

## 部署说明

1. 启动 Nacos 服务
2. 配置数据库连接
3. 启动各微服务模块
4. 配置监控组件（可选）

## 开发规范

- 统一使用中文注释
- 遵循 RESTful API 规范
- 统一异常处理
- 统一响应格式