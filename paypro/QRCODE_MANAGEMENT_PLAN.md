# 二维码管理系统重构计划

## 当前问题
1. ❌ 二维码管理页面不在后台框架内
2. ❌ 下拉菜单文字颜色为白色，看不清
3. ❌ 固定金额二维码上传后无法管理（无法设置金额、编号、删除）
4. ❌ 上传的图片刷新后消失（未持久化）

## 解决方案

### 1. 数据库设计
创建 `t_qrcode_file` 表存储二维码文件信息：
- id: 主键
- pay_type: 支付类型（alipay/wechat）
- amount: 金额
- qr_num: 二维码编号
- file_path: 文件路径
- file_name: 原始文件名
- enabled: 是否启用

### 2. 后端 API
- GET /admin/api/qrcode/list - 获取所有二维码列表
- POST /admin/api/qrcode/upload - 批量上传二维码
- PUT /admin/api/qrcode/update/{id} - 更新二维码信息（金额、编号）
- DELETE /admin/api/qrcode/delete/{id} - 删除二维码

### 3. 前端页面
- 使用统一的后台布局（sidebar + main）
- 修复 CSS 样式（select option 背景色）
- 批量上传 + 图片预览
- 每个图片可编辑金额、编号
- 可删除图片
- 持久化显示

### 4. 集成到后台菜单
- 添加到侧边栏菜单
- 使用统一的样式和布局

## 实施步骤
1. ✅ 创建数据库表
2. ⏳ 创建实体类和 Mapper
3. ⏳ 实现后端 API
4. ⏳ 修复 CSS 样式
5. ⏳ 重新设计前端页面
6. ⏳ 集成到后台框架
