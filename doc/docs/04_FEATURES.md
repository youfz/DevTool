# 04 模块详细规格

## JSON
四 Tab：格式化、校验、树形、对比。编辑器保留原始输入，显示行号；解析失败突出错误偏移、行列、上下文；对比按 JSON 结构而非字符串空格比较，对象键顺序不敏感、数组顺序敏感。支持剪贴板复制与 SAF 导出。

## 蓝牙
BLE 扫描页显示设备名、MAC（权限/系统允许时）、RSSI、扫描状态；设备详情显示 GATT 服务/特征 UUID、属性、读写/通知能力；终端支持 HEX 与 UTF-8、时间戳、TX/RX 标签、字节统计、清空/保存。订阅需配置 CCCD；长包写入按 MTU/设备能力分段，不能假定一次写入成功。Classic 仅支持设备提供的 RFCOMM/SPP，不等同于 BLE。

## API
请求编辑：Method、URL、Params、Headers、Body(raw JSON/text/form)、Auth（首版 Bearer/Basic，可按安全要求限制保存）、环境变量。响应：HTTP 状态、耗时、字节数、Headers、原始/格式化 Body。区分 DNS/TLS/超时/取消/HTTP 非 2xx；历史只存脱敏副本。

## ADB
连接方式：Android 11+ Wireless debugging 配对与连接、USB OTG ADB、受限本机信息。终端：命令输入、输出流、超时、取消、命令历史；Logcat：等级/Tag/关键字过滤、暂停/清空/导出；系统信息：getprop、内存、存储、进程等经目标设备授权的 Shell 可获取项。设备未授权时显示授权说明，不声称自动提权。

## 会话与记录
每次连接或请求产生 Session；事件含时间戳、类型、方向、状态、摘要；详情可搜索/过滤/复制/导出；清理可按时间或全部清理。大日志按大小上限轮转。
