# cycheckin API 文档

## 简介

cycheckin API 提供了 RESTful 接口，用于「学在重邮」平台的登录和签到操作。所有接口使用 JSON 格式进行数据交换。

## 基础信息

- **基础 URL**: `http://localhost:5003`
- **数据格式**: JSON
- **字符编码**: UTF-8
- **默认端口**: 5003
- **请求超时**: 15 秒

## 启动服务

```bash
# 安装依赖
pip install -r requirements.txt

# 初始化子模块
git submodule init
git submodule update

# 启动 API 服务（默认配置）
python api.py

# 使用代理启动服务
python api.py --proxy http://127.0.0.1:7890

# 使用 SOCKS5 代理启动服务
python api.py --proxy socks5://127.0.0.1:1080

# 自定义监听地址和端口
python api.py --host 0.0.0.0 --port 8080

# 启用调试模式
python api.py --debug

# 组合使用多个参数
python api.py --proxy http://127.0.0.1:7890 --host 0.0.0.0 --port 8080 --debug
```

### 命令行参数

- `--proxy`: 设置代理地址，支持 HTTP 和 SOCKS5 代理
  - HTTP 代理示例: `http://127.0.0.1:7890`
  - SOCKS5 代理示例: `socks5://127.0.0.1:1080`
- `--host`: 设置监听地址，默认为 `0.0.0.0`
- `--port`: 设置监听端口，默认为 `5003`
- `--debug`: 启用调试模式

服务将在 `http://localhost:5003` 上运行（默认配置）。

## API 端点

### 1. 密码登录

**接口地址**: `POST /api/login/password`

**请求参数**:
```json
{
  "uid": "统一认证码",
  "password": "密码"
}
```

**参数说明**:
- `uid`: 必填，统一认证码
- `password`: 必填，登录密码

**响应示例**:
```json
{
  "success": true,
  "session": "V2-1-55f2b21d-2ba4-44db-92df-75c08063248b.MjU3NzMx.1772537186844.K7oLgfKuN-jZoUslW_N20UF1PJ8",
  "message": "登录成功"
}
```

**错误响应**:
```json
{
  "success": false,
  "message": "缺少必要参数"
}
```

**重要说明**:
- 登录成功后，请保存返回的 `session` 字段
- 后续所有请求都需要使用此 `session` 值
- Session 有有效期，过期后需要重新登录

---

### 2. 二维码登录

**接口地址**: `POST /api/login/qr`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "session": "session_id",
  "qr_code": "base64编码的二维码图片",
  "message": "请扫描二维码"
}
```

**使用说明**:
1. 调用此接口获取二维码
2. 将 `qr_code` 字段的 base64 数据解码为图片
3. 使用企业微信扫描二维码
4. 调用 `/api/login/qr/check` 检查登录状态

---

### 3. 检查二维码登录状态

**接口地址**: `POST /api/login/qr/check`

**请求参数**:
```json
{
  "session": "session_id"
}
```

**响应示例** (已登录):
```json
{
  "success": true,
  "logged_in": true,
  "session": "session_id",
  "message": "登录成功"
}
```

**响应示例** (未登录):
```json
{
  "success": true,
  "logged_in": false,
  "message": "等待扫码"
}
```

---

### 4. 搜索签到

**接口地址**: `POST /api/rollcalls`

**请求参数**:
```json
{
  "session": "session_id"
}
```

**响应示例**:
```json
{
  "success": true,
  "rollcalls": [
    {
      "id": 123,
      "name": "课程名称",
      "teacher_name": "教师姓名",
      "type": "qr",
      "is_checked_in": false
    },
    {
      "id": 124,
      "name": "另一门课程",
      "teacher_name": "另一教师",
      "type": "number",
      "is_checked_in": true
    }
  ]
}
```

**签到类型**:
- `qr`: 二维码签到
- `number`: 数字签到
- `radar`: 雷达签到

---

### 5. 二维码签到

**接口地址**: `POST /api/checkin/qr`

**请求参数**:
```json
{
  "session": "session_id",
  "rollcall_id": 123,
  "code": "二维码解码后的内容"
}
```

**参数说明**:
- `session`: 必填，登录后获取的 session
- `rollcall_id`: 可选，指定的签到 ID。如果不提供，则自动获取第一个未签到的签到任务
- `code`: 必填，二维码解码后的内容

**响应示例**:
```json
{
  "success": true,
  "message": "签到成功",
  "rollcall_id": 123
}
```

---

### 6. 数字签到

**接口地址**: `POST /api/checkin/number`

**请求参数**:
```json
{
  "session": "session_id",
  "rollcall_id": 123,
  "number": "1234"
}
```

**参数说明**:
- `session`: 必填，登录后获取的 session
- `rollcall_id`: 可选，指定的签到 ID。如果不提供，则自动获取第一个未签到的签到任务
- `number`: 必填，数字签到码

**响应示例**:
```json
{
  "success": true,
  "message": "签到成功",
  "rollcall_id": 123
}
```

---

### 7. 雷达签到

**接口地址**: `POST /api/checkin/radar`

**请求参数**:
```json
{
  "session": "session_id",
  "rollcall_id": 123,
  "location": {
    "latitude": 29.5630,
    "longitude": 106.5516,
    "altitude": 0,
    "accuracy": 10,
    "verticalAccuracy": 0,
    "speed": 0
  }
}
```

**参数说明**:
- `session`: 必填，登录后获取的 session
- `rollcall_id`: 可选，指定的签到 ID。如果不提供，则自动获取第一个未签到的签到任务
- `location`: 必填，位置信息
  - `latitude`: 纬度
  - `longitude`: 经度
  - `altitude`: 海拔高度
  - `accuracy`: 水平精度
  - `verticalAccuracy`: 垂直精度
  - `speed`: 速度

**响应示例**:
```json
{
  "success": true,
  "message": "签到成功",
  "rollcall_id": 123
}
```

---

### 8. 健康检查

**接口地址**: `GET /api/health`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "message": "API 服务正常运行"
}
```

## 使用示例

### Python 示例

```python
import requests
import base64
from PIL import Image
import io

BASE_URL = "http://localhost:5003"

# 1. 密码登录
def password_login(uid, password):
    response = requests.post(f"{BASE_URL}/api/login/password", json={
        "uid": uid,
        "password": password
    })
    return response.json()

# 2. 二维码登录
def qr_login():
    response = requests.post(f"{BASE_URL}/api/login/qr")
    data = response.json()
    
    if data['success']:
        # 显示二维码
        qr_data = base64.b64decode(data['qr_code'])
        image = Image.open(io.BytesIO(qr_data))
        image.show()
        
        # 检查登录状态
        while True:
            check_response = requests.post(f"{BASE_URL}/api/login/qr/check", json={
                "session": data['session']
            })
            check_data = check_response.json()
            
            if check_data['logged_in']:
                return check_data['session']
            
            import time
            time.sleep(2)
    
    return None

# 3. 搜索签到
def search_rollcalls(session):
    response = requests.post(f"{BASE_URL}/api/rollcalls", json={
        "session": session
    })
    return response.json()

# 4. 二维码签到
def qr_checkin(session, rollcall_id=None, code=None):
    response = requests.post(f"{BASE_URL}/api/checkin/qr", json={
        "session": session,
        "rollcall_id": rollcall_id,
        "code": code
    })
    return response.json()

# 5. 数字签到
def number_checkin(session, rollcall_id=None, number=None):
    response = requests.post(f"{BASE_URL}/api/checkin/number", json={
        "session": session,
        "rollcall_id": rollcall_id,
        "number": number
    })
    return response.json()

# 6. 雷达签到
def radar_checkin(session, rollcall_id=None, location=None):
    response = requests.post(f"{BASE_URL}/api/checkin/radar", json={
        "session": session,
        "rollcall_id": rollcall_id,
        "location": location
    })
    return response.json()

# 完整示例
if __name__ == "__main__":
    # 密码登录
    login_result = password_login("your_uid", "your_password")
    if login_result['success']:
        session = login_result['session']
        print(f"登录成功，session: {session}")
        
        # 方式1: 搜索签到并手动选择
        rollcalls_result = search_rollcalls(session)
        if rollcalls_result['success']:
            rollcalls = rollcalls_result['rollcalls']
            print(f"找到 {len(rollcalls)} 个签到")
            
            for rollcall in rollcalls:
                if not rollcall['is_checked_in']:
                    print(f"签到: {rollcall['name']} ({rollcall['type']})")
                    
                    # 根据签到类型执行不同的签到操作
                    if rollcall['type'] == 'qr':
                        # 二维码签到
                        code = input("请输入二维码内容: ")
                        result = qr_checkin(session, rollcall['id'], code)
                    elif rollcall['type'] == 'number':
                        # 数字签到
                        number = input("请输入数字签到码: ")
                        result = number_checkin(session, rollcall['id'], number)
                    elif rollcall['type'] == 'radar':
                        # 雷达签到
                        location = {
                            "latitude": 29.5630,
                            "longitude": 106.5516,
                            "altitude": 0,
                            "accuracy": 10,
                            "verticalAccuracy": 0,
                            "speed": 0
                        }
                        result = radar_checkin(session, rollcall['id'], location)
                    
                    if result['success']:
                        print("签到成功！")
                    else:
                        print(f"签到失败: {result['message']}")
        
        # 方式2: 自动签到（不指定 rollcall_id）
        print("\n尝试自动签到...")
        
        # 二维码签到（自动获取第一个未签到的签到）
        code = input("请输入二维码内容: ")
        result = qr_checkin(session, code=code)
        if result['success']:
            print(f"自动签到成功！签到ID: {result['rollcall_id']}")
        else:
            print(f"自动签到失败: {result['message']}")
```

### cURL 示例

```bash
# 1. 密码登录
curl -X POST http://localhost:5003/api/login/password \
  -H "Content-Type: application/json" \
  -d '{"uid":"your_uid","password":"your_password"}'

# 2. 二维码登录
curl -X POST http://localhost:5003/api/login/qr

# 3. 检查二维码登录状态
curl -X POST http://localhost:5003/api/login/qr/check \
  -H "Content-Type: application/json" \
  -d '{"session":"session_id"}'

# 4. 搜索签到
curl -X POST http://localhost:5003/api/rollcalls \
  -H "Content-Type: application/json" \
  -d '{"session":"session_id"}'

# 5. 二维码签到
curl -X POST http://localhost:5003/api/checkin/qr \
  -H "Content-Type: application/json" \
  -d '{"session":"session_id","rollcall_id":123,"code":"qr_code"}'

# 6. 数字签到
curl -X POST http://localhost:5003/api/checkin/number \
  -H "Content-Type: application/json" \
  -d '{"session":"session_id","rollcall_id":123,"number":"1234"}'

# 7. 雷达签到
curl -X POST http://localhost:5003/api/checkin/radar \
  -H "Content-Type: application/json" \
  -d '{"session":"session_id","rollcall_id":123,"location":{"latitude":29.5630,"longitude":106.5516,"altitude":0,"accuracy":10,"verticalAccuracy":0,"speed":0}}'

# 8. 健康检查
curl http://localhost:5003/api/health
```

## 错误码说明

- `400`: 请求参数错误
- `404`: 资源未找到
- `500`: 服务器内部错误

所有错误响应都包含 `success: false` 和 `message` 字段，说明错误原因。

## 注意事项

1. Session 在服务器端有有效期，请妥善保存
2. 雷达签到需要提供准确的位置信息
3. 数字签到的爆破功能在 API 中未实现，如需使用请使用命令行版本
4. 请合理使用 API，避免频繁请求
5. 所有请求超时时间为 15 秒
6. 默认端口为 5003

## 许可证

MIT License
