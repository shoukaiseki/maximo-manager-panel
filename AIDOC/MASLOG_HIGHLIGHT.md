# Maximo 日志查看器 - Java 日志高亮方案

## 🎨 高亮效果预览

### 1. 日志级别高亮

```
DEBUG   → 蓝色 (#6a9fb5) + 浅蓝背景
INFO    → 绿色 (#90a959) + 浅绿背景  
WARN    → 橙色 (#d28445) + 浅橙背景
ERROR   → 红色 (#ac4142) + 浅红背景
FATAL   → 鲜红 (#f00) + 深红背景 + 脉冲动画 ⚡
```

### 2. 其他元素高亮

| 元素类型 | 颜色 | 样式 |
|---------|------|------|
| **时间戳** | 灰色 (#75715e) | 斜体 |
| **类名** | 青色 (#66d9ef) | 中等粗细 |
| **异常类型** | 粉红 (#f92672) | 粗体 + 下划线 |
| **数字** | 紫色 (#ae81ff) | 正常 |
| **关键词** (null/true/false) | 粉红 (#f92672) | 粗体 |
| **过滤关键词** | 黄色 (#ffeb3b) | 粗体 + 阴影 + 发光效果 ✨ |

## 📝 示例日志

### 输入日志
```
2024-01-15 10:30:45.123 INFO com.maximo.service.UserService - User login successful for user admin
2024-01-15 10:30:46.456 ERROR com.maximo.dao.DatabaseDAO - Connection timeout after 30 seconds
2024-01-15 10:30:47.789 WARN com.maximo.cache.CacheManager - Cache memory usage at 85%
2024-01-15 10:30:48.012 DEBUG com.maximo.util.Logger - Processing request id=12345
java.lang.NullPointerException: Cannot invoke method on null object
    at com.maximo.service.OrderService.processOrder(OrderService.java:156)
```

### 高亮效果

#### 第一行 (INFO)
- `2024-01-15 10:30:45.123` → <span style="color:#75715e;font-style:italic">灰色斜体时间戳</span>
- `INFO` → <span style="color:#90a959;background:rgba(144,169,89,0.1);padding:2px 4px;border-radius:2px;font-weight:bold">绿色 INFO 标签</span>
- `com.maximo.service.UserService` → <span style="color:#66d9ef;font-weight:500">青色类名</span>

#### 第二行 (ERROR)
- `ERROR` → <span style="color:#ac4142;background:rgba(172,65,66,0.15);padding:2px 4px;border-radius:2px;font-weight:bold">红色 ERROR 标签</span>
- `30` → <span style="color:#ae81ff">紫色数字</span>

#### 第三行 (WARN)
- `WARN` → <span style="color:#d28445;background:rgba(210,132,69,0.1);padding:2px 4px;border-radius:2px;font-weight:bold">橙色 WARN 标签</span>
- `85` → <span style="color:#ae81ff">紫色数字</span>

#### 第四行 (DEBUG)
- `DEBUG` → <span style="color:#6a9fb5;background:rgba(106,159,181,0.1);padding:2px 4px;border-radius:2px;font-weight:bold">蓝色 DEBUG 标签</span>
- `12345` → <span style="color:#ae81ff">紫色数字</span>

#### 第五行 (异常)
- `NullPointerException` → <span style="color:#f92672;font-weight:bold;text-decoration:underline">粉红异常名 + 下划线</span>
- `null` → <span style="color:#f92672;font-weight:bold">粉红关键词</span>

## 🎯 特性说明

### 1. 智能识别
- ✅ 自动识别 Java 标准日志格式
- ✅ 支持多种时间戳格式
- ✅ 识别完整的包名.类名结构
- ✅ 检测所有 Java 异常类型

### 2. 视觉层次
- **主要信息**（日志级别）→ 最醒目，带背景色
- **次要信息**（类名、异常）→ 彩色突出
- **辅助信息**（时间戳、数字）→ 低调显示
- **用户关注**（过滤词）→ 黄色高亮 + 发光

### 3. 交互效果
- 鼠标悬停行 → 左侧出现指示条 + 背景微亮
- FATAL 级别 → 持续脉冲动画吸引注意
- 过滤关键词 → 黄色发光效果

### 4. 性能优化
- 使用 CSS 类而非 inline styles
- 正则表达式预编译
- 虚拟滚动支持大量日志

## 🔧 配置选项

### 超时时间选择
在连接前可选择 SSE 超时时间：
- 5秒、10秒、20秒、30秒、50秒、80秒、100秒
- 连接后隐藏，避免误操作

### 自动重连
- 断开后 2 秒自动重连
- 除非手动点击"停止接收"
- 重连时保持所有设置

### 实时过滤
- 输入关键词即时过滤
- 高亮显示匹配内容
- 支持大小写不敏感

## 💡 使用建议

1. **开发环境**: 使用较短超时时间 (5-10秒) 快速发现问题
2. **生产环境**: 使用较长超时时间 (30-100秒) 减少连接频率
3. **调试错误**: 过滤 "ERROR" 或 "Exception" 快速定位问题
4. **性能分析**: 过滤时间戳分析响应时间

## 🎨 配色方案

采用 **Monokai** 主题配色，适合长时间阅读：
- 深色背景减少眼睛疲劳
- 高对比度确保可读性
- 语义化颜色便于快速识别
- 柔和的过渡动画提升体验

---

**提示**: 这个高亮方案专为 Java 日志优化，同时也适用于其他语言的类似格式日志。
