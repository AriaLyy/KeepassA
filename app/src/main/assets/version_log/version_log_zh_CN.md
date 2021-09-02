#### 2.1.4 (2021/9/2)
- 【新增】德语翻译
- 【优化】更新部分多语言翻译
- 【修复】密码生成工具选中数据大概率没有数字的问题
- 【修复】打开某些KDBV4失败的问题

#### 2.1.3（2021/7/13）
- 【新增】KDBV4支持
- 【新增】过期的条目增加提示（中横线）
- 【修复】群组可以移动到自己
- 【修复】安全键盘其它字段无法填充
- 【修复】历史记录没有按时间排序
- 【修复】无法启动快速解锁界面
- 【优化】安全键盘多条目去重

#### 2.1.2 (2021/5/29)
- 【新增】允许密码为空
- 【新增】OneDrive增加提示说明
- 【修复】第一次安装，没有加载等待动画
- 【修复】android 11 没有弹出自动填充选择项
- 【修复】首页创建条目到群组时，首页的条目数量提示没有增加
- 【修复】修改密码后，图标变为默认图标
- 【优化】首页滑动增加渐隐动画
- 【优化】动画细节

#### 2.1.1（2021/5/6）
- 【修复】webview崩溃问题

#### 2.1（20201/5/5）
- 【新增】OneDrive支持
- 【新增】增加法语翻译
- 【新增】兼容API30
- 【修复】保存备注出现的崩溃
- 【修复】横屏时，界面尺寸被放大
- 【修复】WebDav多次Unlock导致的问题

#### 2.0.2 (2021/4/1)
- 【新增】关闭加载动画选择项[设置](route://keepassA.com/kpa?activity=SettingActivity) -> 界面设置
- 【新增】不自动锁定功能[设置](route://keepassA.com/kpa?activity=SettingActivity&type=app) -> 数据库设置
- 【新增】俄语（40%），感谢[@KovalevArtem](https://github.com/KovalevArtem)
- 【新增】挪威语（50%），感谢[@Allan Nordhøy](https://github.com/comradekingu)
- 【优化】优化英文翻译，感谢[@Allan Nordhøy](https://github.com/comradekingu)
- 【修复】一个动画关闭掉的问题
- 【修复】点击应设置崩溃的问题

#### 2.0.1（2021/3/27）
- 【修复】webdav登陆问题
- 【修复】多语言问题

#### 2.0（2021/3/23）
- 【新增】回收站不显示添加群组\条目按钮
- 【新增】是否隐藏状态栏，[点击设置](route://keepassA.com/kpa?activity=SettingActivity) -> 界面设置
- 【新增】仅密钥登陆
- 【优化】大量动画
- 【优化】默认图标全采用MD风格图标
- 【优化】图标选择将使用底部弹出的的方式
- 【修复】定时锁定数据库无效的问题
- 【修复】跳转动画丢失的问题
- 【修复】无法切换历史数据

#### 1.8.4（2021/3/4）
- 重构创建数据库的逻辑
- 增加在Webdav上创建数据库的功能

![webdavCreate](https://gitee.com/laoyuyu/blog/raw/master/keepassA/webdavCreate.png)
- 修复一些崩溃问题


#### 1.8.3 (2021/2/25)
- 修复一些崩溃问题
- 升级kotlin版本
- 移除kotlin-android-extensions

#### 1.8.2 (2021/2/17)
- 修复屏幕锁定后，点击通知进入软件出现的崩溃问题
- 修复获取steam totp 崩溃问题
- 升级kotlin 版本

#### 1.8.1 (2021/2/14)
- 修复数据库一直加载的问题
- 修复一个附件问题
- 增加 Argon2 ID 支持
- 增加 keyfile 2.0 支持

#### 1.8 (2021/2/14)
- 修复一个缓存问题导致的webdav打不开的问题
- 优化打开数据库的速度

#### 1.7 (2021/2/4)
- 增加当包名不匹配时，将会从url匹配关键字
- 增加参考条目提示
- 增加[屏幕锁定，自动锁定数据库功能](route://keepassA.com/kpa?activity=SettingActivity&type=app)
- note增加编辑功能
- 增加用户名历史记录功能

![user_drop_down_list](https://gitee.com/laoyuyu/blog/raw/master/keepassA/userDropdownList.png)
- 搜索忽略回收站中的条目
- 修复夜间模式下，自动填充文字无法显示的问题
- 修复某些应用无法自动填充的问题
- 修复一些崩溃问题


#### 1.6 (2020/11/28)
- 自动填充模块，修复关联条目后，返回到应用，已关联的数据无法显示的问题
- 自动填充模块，修复多个输入框时，输入框数据填充错误的问题
- 自动填充模块，增加浏览器填充功能
- 自动填充模块，增加其它按钮
- 设置模块，增加解锁数据库后，主页优先显示所有条目功能，[点击设置](route://keepassA.com/kpa?activity=SettingActivity&type=db)
- 条目详情模块，note 自动增加展开和收缩功能

![note_expand](https://gitee.com/laoyuyu/blog/raw/master/keepassA/noteExpand.png)
- 优化主页闪烁问题
- 感谢[@DominicDesbiens](https://github.com/DominicDesbiens)提供了加拿大法语翻译

#### 1.5 (2020/11/5)
- 增加群组搜索功能
- 增加开放源码协议说明
- 增加移动条目和群组的功能

![ime](https://gitee.com/laoyuyu/blog/raw/master/keepassA/moveData.png)
- 修复无法增加二级群组的问题
- 修复一个webdav获取文件信息导致的崩溃问题
- 修复一些讨厌的崩溃问题

#### 1.4.1 (2020/10/29)
- 升级android studio 到4.1
- 搜索增加高亮关键字
- 修复快速解锁的一个崩溃问题

#### 1.4 (2020/10/28)
- 升级kotlin版本
- 增加群组排序功能
- 增加字段引用功能
- 增加[安全键盘](route://keepassA.com/kpa?activity=ime)

![ime](https://gitee.com/laoyuyu/blog/raw/master/keepassA/ime.png)
- 修复快速解锁界面，无法删除所有短密码的问题
- 修复webdav登陆超时导致的崩溃问题
- 修复一些讨厌的崩溃问题

#### 1.3 (2020/9/22)
- 增加TOTP令牌设置功能，编辑条目，点击添加更多按钮便可以显示该功能界面

![otp_setting](https://gitee.com/laoyuyu/blog/raw/master/keepassA/otpsetting.png)

#### 1.2 (2020/9/2)
- 修复一些讨厌的崩溃问题

#### 1.1 (2020/8/21)
- KeepassA开源了，已经托管在[github](https://github.com/AriaLyy/KeepassA)上
- 修复了一些讨厌的bug

#### 1.0.1.2 (2020/8/7)
- 重构[指纹解锁设置界面](route://keepassA.com/kpa?activity=FingerprintActivity)
- 增加下拉同步数据库功能
- 增加手机root/模拟器检测
- 增加直接删除条目/群组功能
- 修复无法识别含有特殊字符的http地址的问题
- 修复自动填充服务，无法保存数据的问题

#### 1.0.1.1 (2020/7/22)
- 增加[webdav](route://keepassA.com/kpa?activity=WebDavLoginDialog)
- 修复小米手机增加指纹后，再进行指纹解锁导致的崩溃问题

#### 1.0.1.0 (2020/7/10)
- 修复数据库已锁定后，点击通知栏通知跳转错误的问题
- TOTP增加倒计时效果
- 修复同步数据库时，相同条目总是提示的问题
- 修复一些讨厌的崩溃问题

#### 1.0.0.9 (2020/7/11)
- 修复手动进行快速锁定后，无法进入其它页面的问题
- 修复通知栏图标过小的问题
- 修复打开指纹解锁界面崩溃的问题
- 修复自动填充服务崩溃的问题
- 优化指纹解锁功能的逻辑

#### 1.0.0.7 (2020/6/14)
- 增加历史版本对话框
- 增加震动反馈
- 修复dropbox打开记录无法保存的问题

#### 1.0.0.6 (2020/6/7)
- 修复一些讨厌bug
- 增加[指纹解锁](route://keepassA.com/kpa?activity=FingerprintActivity)

#### 1.0.0.4 (2020/6/6)
- 修复一些讨厌的bug

#### 1.0.0.3 (2020/5/13)
- 修复一些讨厌的bug

#### 1.0.0.2 (2020/5/10)
- 修复一些讨厌的bug

#### 1.0.0.1 (2020/5/3)
- 修复一些讨厌的bug
