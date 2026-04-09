# 修改项目配置
重点修改：application.yml

paypro配置下:
alipayCustomQrUrl:替换为自己的支付宝收款二维码链接，获取方式：打开支付宝点击收款将二维码截图保存，使用任意解码工具解析二维码内容得到url。
alipayUserId: 扫描本项目中的`获取支付宝userId.jpg`获取
alipayDfmAppId: 申请成功之后在支付宝界面获取
alipayDfmAppPrivateKey: 参考支付宝文档获取
alipayDfmPublicKey: 参考支付宝文档获取
site: 替换为自己的域名，用于生成收款链接
receiver: 替换为管理员接收审批的邮箱地址，用于接收支付通知
sender: 与下方mail.username配置一致
`paypr下其他配置不影响你的收款，可以酌情看注释修改`

mail.username: 替换为自己的发件邮箱地址。
mail.password: 替换为自己的发件邮箱密码,获取方式各个邮箱不同，以qq邮箱为例：
1. 登录qq邮箱，点击设置
2. 点击右上角设置 -》跳转后点击左下角 账号与安全
3. 点击安全设置，开启SMTP服务，获取授权码
4. 替换为获取到的授权码
datasource.url: 替换为自己的数据库连接url
datasource.password: 替换为自己的数据库密码
redis.host: 替换为自己的redis主机地址
redis.port: 替换为自己的redis端口号
redis.password: 替换为自己的redis密码

# 项目编译方式
与普通maven项目编译方式相同，使用`mvn clean install`命令编译项目。

# 项目启动方式
 1. 确保项目已编译成功
 2. 使用`java -jar paypro-0.0.1-SNAPSHOT.jar`命令启动项目
 3. 项目启动后，默认监听端口为8889，可通过`http://localhost:8889`访问项目

# 关于自动确认程序
自动确认辅助程序以exe格式交付，只支持windows，绑定机器。需要电脑端微信保持在线（离线后重新上线数据会自动回补），不是市面上常见的监听通知和监听ui的方案。

目前仅支持微信，后续会新增支付宝支持，微信多开支持，新增后捐助门槛会上调，但早期已经获取的可以向我获取。

若你有意向获取，请先获取本机序列号。获取方式：
在cmd中执行以下命令：wmic cpu get processorid

邮件点击win图标，点击运行。

![cmd](.\cmd.png)

在输入框内输入cmd，点击确定
![cmd](.\cmd.png)

复制这段内容后续授权使用
![cmd](.\cpu.png)

获取地址：https://paypro.codewendao.com/recharge-wx.html

