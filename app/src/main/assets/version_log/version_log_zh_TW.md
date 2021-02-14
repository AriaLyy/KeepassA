#### 1.8.1 (2021/2/15)
- 修復數據庫一直加載的問題
- 修復一個附件問題
- 增加Argon2 ID 支持
- 增加keyfile 2.0 支持

#### 1.8 (2021/2/14)
- 修復一個緩存問題導致的webdav打不開的問題
- 優化打開數據庫的速度

#### 1.7 (2021/2/4)
- 增加當包名不匹配時，將會從url匹配關鍵字
- 增加參考條目提示
- 增加[屏幕鎖定，自動鎖定數據庫功能](route://keepassA.com/kpa?activity=SettingActivity&type=app)
- note增加編輯功能
- 增加用戶名歷史記錄功能

![user_drop_down_list](https://raw.githubusercontent.com/AriaLyy/KeepassA/master/img/userDropdownList.png)
- 搜索忽略回收站中的條目
- 修復夜間模式下，自動填充文字無法顯示的問題
- 修復某些應用無法自動填充的問題
- 修復一些崩潰問題

#### 1.6 (2020/11/28)
- 自動填充模塊，修復關聯條目後，返回到應用，已關聯的數據無法顯示的問題
- 自動填充模塊，修復多個輸入框時，輸入框數據填充錯誤的問題
- 自動填充模塊，增加瀏覽器填充功能
- 自動填充模塊，增加其它按鈕
- 設置模塊，增加解鎖數據庫後，主頁優先顯示所有條目功能，[點擊設置](route://keepassA.com/kpa?activity=SettingActivity&type=db)
- 條目詳情模塊，note 自動增加展開和收縮功能

![ime](https://raw.githubusercontent.com/AriaLyy/KeepassA/master/img/noteExpand.png)
- 優化主頁閃爍問題
- 感謝[@DominicDesbiens](https://github.com/DominicDesbiens)提供了加拿大法語翻譯

#### 1.5 (2020/11/5)
- 增加群組搜索功能
- 增加開放源碼協議說明
- 增加移動條目和群組的功能

![ime](https://raw.githubusercontent.com/AriaLyy/KeepassA/master/img/moveData.png)
- 修復無法增加二級群組的問題
- 修復一個webdav獲取文件信息導致的崩潰問題
- 修復一些討厭的崩潰問題

#### 1.4.1 (2020/10/29)
- 升級android studio 到4.1
- 搜索增加高亮關鍵字
- 修復快速解鎖的一個崩潰問題

#### 1.4 (2020/10/28)
- 升級kotlin版本
- 增加群組排序功能
- 增加字段引用功能
- 增加[安全鍵盤](route://keepassA.com/kpa?activity=ime)

![ime](https://raw.githubusercontent.com/AriaLyy/KeepassA/master/img/ime.png)
- 修復快速解鎖界面，無法刪除所有短密碼的問題
- 修復webdav登陸超時導致的崩潰問題
- 修復一些討厭的崩潰問題

#### 1.3 (2020/9/22)
- 增加TOTP令牌設置功能，編輯條目，點擊添加更多按鈕便可以顯示該功能界面

![otp_setting](https://raw.githubusercontent.com/AriaLyy/KeepassA/master/img/otpsetting.png)

#### 1.2 (2020/9/2)
- 修復一些討厭的崩潰問題

#### 1.1 (2020/8/21)
- KeepassA開源了，已經託管在[github](https://github.com/AriaLyy/KeepassA)上
- 修復了一些討厭的bug

#### 1.0.1.2 (2020/7/22)
- 重構[指紋解鎖設置界面](route://keepassA.com/kpa?activity=FingerprintActivity)
- 增加下拉同步數據庫功能
- 增加手機root/模擬器檢測
- 增加直接刪除條目/群組功能
- 修復無法識別含有特殊字符的http地址的問題
- 修復自動填充服務，無法保存數據的問題

#### 1.0.1.0 (2020/7/10)
- 修復數據庫已鎖定後，點擊通知欄通知跳轉錯誤的問題
- TOTP增加倒計時效果
- 修復同步數據庫時，相同條目總是提示的問題
- 修復一些討厭的崩潰問題

#### 1.0.0.9 (2020/7/11)
- 修復手動進行快速鎖定後，無法進入其它頁面的問題
- 修復通知欄圖標過小的問題
- 修復打開指紋解鎖界面崩潰的問題
- 修復自動填充服務崩潰的問題
- 優化指紋解鎖功能的邏輯

#### 1.0.0.7 (2020/6/14)
- 增加歷史版本對話框
- 增加震動反饋
- 修復dropbox打開記錄無法保存的問題

#### 1.0.0.6 (2020/6/7)
- 修復一些討厭bug
- 增加[指紋解鎖](route://keepassA.com/kpa?activity=FingerprintActivity)

#### 1.0.0.4 (2020/6/6)
- 修復一些討厭bug

#### 1.0.0.3 (2020/5/13)
- 修復一些討厭bug

#### 1.0.0.2 (2020/5/10)
- 修復一些討厭bug

#### 1.0.0.1 (2020/5/3)
- 修復一些討厭bug
