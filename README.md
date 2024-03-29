## 基于[Mirai](https://github.com/mamoe/mirai)开发的的QQ机器人
### 使用了：<br/>MySQL数据库储存数据<br/>OpenNLP自然语言处理框架进行文本分类<br/>Lucene进行文本分词

<details>
<summary>功能列表</summary>

**主人系统**
- 开机
- 关机
- 加积分

**群主系统**
- 添加群管
- 删除群管
- 群管列表
- 开启发言检测 （未完成）
- 关闭发言检测 （未完成）
- 开启积分系统 （未完成）
- 关闭积分系统 （未完成）
- 开启入群验证 （未完成）
- 关闭入群验证 （未完成）
- 开启入群自动审核 （未完成）
- 关闭入群自动审核 （未完成）

**群管系统**
- 踢
- 踢黑
- 禁言
- 解禁
- 撤回
- 撤回关键词
- 封印
- 解除封印
- 封印列表
- 查询消息记录
- 开启全员禁言
- 关闭全员禁言
- 禁言列表
- 全部解禁
- 清屏

**实时发言检测系统**
- 刷屏检测
- 复读检测
- 一些违规行为检测
- NLP自然语言处理检测违规发言

**积分系统**
- 签到
- 转账
- 我的积分
- 积分排行榜
- kban
- kj
- 抢劫
- 抢劫规则

**其他功能**
- 摸鱼人日历
- 说（文字转语言）
- 实时天气
- 未来天气
- 天气指数
- 日出日落
- 月升月落
- 天气服务由和风天气驱动

</details>

使用实时发言检测系统的自然语言处理检测违规发言功能，需要在项目文件下创建一个名为 'ai' 目录，并在 'ai' 目录下放置一个 ’mode.bin‘ 的文本分类模型文件。需要训练模型，可以在ai/data1.txt 和ai/data2.txt文件里以 "文本类型 文本数据" 的格式，并且一行一条写入数据。然后运行com.bot.AiMain类训练模型。如不需要可在com.qqbot.group.msg.proc.GroupCheck类里将相关代码注释

**机器人启动示例**
- [Java](src/main/java/helloworld/java/HelloWorld.java)
- [Kotlin](src/main/java/helloworld/kotlin/HelloWorld.kt)
