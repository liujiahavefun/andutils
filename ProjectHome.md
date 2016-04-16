andutils 是 Android 的常用工具类.

1. (暂时无法正常使用)日志工具类： 即使过去logcat中的日志信息（通过当前app的pid进行日志筛选，日志可分等级过滤），当日志保存到一定大小会发送到指定的邮箱。

2. 汉字转换拼音工具类：可以把汉字转换成拼音方便模糊查询，ListView分组显示。

3. android发送邮件工具类(整个开源项目 http://code.google.com/p/javamail-android/ )：可以嵌入到android程序中用来发送邮件，当程序发生错误时候将错误信息通过邮件的方式提交。

4  BitmapHelper网络图片缓冲下载：根据网络url，将图片下载到本地，然后在更新节目（不会和页面有冲突，文件一旦存在就不在重复下载，文件的名字通过 加密的方式保存）.

5 NetworkHelper网络状态：检测当前手机的联网状态工具累。

6. ApkHelper apk工具类：可以检测制定packageName的应用是否被安装，可以检测Activity 是否存在。