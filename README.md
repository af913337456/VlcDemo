# VlcDemo

##1，第一步
&emsp;&emsp;你的 Activity 继承抽象类*VLCBasePlayerActivity.java*

##2，第二步
<pre>
/** 视频播放处父容器 */
LinearLayout container = ((LinearLayout)findViewById(R.id.videoFather))
/** 添加一个视频View进来，id 是 0 */
container.addView(addVideoView(0));

</pre>

##3，第三步
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

/** 案件事件 */
@Override
public boolean onKeyDownE(int keyCode, KeyEvent event) {
    return false;
}
</pre>
