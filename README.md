# VlcDemo

##有什么功能(Function list)
<pre>
&emsp;&emsp;1.支持多种视频播放格式，解码快；
&emsp;&emsp;2.支持多路播放；
&emsp;&emsp;3.能进行视频直播，性能高；
&emsp;&emsp;4.颜值高，还能选择画布比例。
&emsp;&emsp;5.vlc 能倍速播放，还很多，很屌! -_- 。
</pre>
<p></p>
<pre>
在上面的基础上已添加如下功能拓展:
&emsp;&emsp;1.全屏；
&emsp;&emsp;2.上下滑调亮度、声音；
&emsp;&emsp;3.左右滑快进，退；
&emsp;&emsp;4.锁屏与解锁；
&emsp;&emsp;5.像素切换，应对高、超清；
&emsp;&emsp;//6.网络状态广播监听及处理；
&emsp;&emsp;.....
</pre>

##怎样使用(How to use)

###1，第一步
&emsp;&emsp;你的 Activity 继承抽象类*VLCBasePlayerActivity.java*

###2，第二步
<pre>
/** 视频播放处父容器 */
LinearLayout container = ((LinearLayout)findViewById(R.id.videoFather))
/** 添加一个视频View进来，id 是 0 */
container.addView(addVideoView(0));

</pre>

###3，第三步
&emsp;&emsp;重写3个函数
<pre>
/** 设置全屏时装载的父容器 */
@Override
public RelativeLayout setFullViewContainer() {
    return (RelativeLayout)findViewById(R.id.videoFullSizeFather);
}

/** 设置视频的播放路径 */
@Override
public Map<Integer, String> setVideoPaths() {
    Map<Integer, String> paths = new HashMap<>();
    paths.put(0,"http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8");
    return paths;
}

/** 按键事件 */
@Override
public boolean onKeyDownE(int keyCode, KeyEvent event) {
    return false;
}
</pre>
