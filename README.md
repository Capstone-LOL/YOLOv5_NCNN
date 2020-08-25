## :rocket: 编码不易，点个star！ ##

### 移动端NCNN部署，项目支持YOLOv5s、YOLOv4-tiny、MobileNetV2-YOLOv3-nano、Simple-Pose与Yolact模型，摄像头实时捕获视频流进行检测。

## iOS:
- Xcode 11.5
- macOS 10.15.4
- iPhone 6s 13.5.1

## Android:
- android studio 4.0
- win10 1909
- meizu 16x 8.1.0

部分代码参考自：sunnyden 的开源项目。
安卓已经增加权限申请，但如果还是闪退请手动确认下相关权限是否允许。

> iOS
```code
YOLOv5s:     ViewController.mm->USE_YOLOV5 = YES;
YOLOv4-tiny: ViewController.mm->USE_YOLOV5 = NO;
YOLOv3-nano: Due to time constraints, you need to modify it yourself.
                 1. Copy 2 files from Android library: .param and .bin
                 2. Just modify the loading name of yolov4-tiny.
```
> Android
```
Select the model to be tested directly on the interface.
```
## 模型
* YOLOv5s 输入尺寸减小，解码过程使用了大量的 for 循环与 NMS 表现出来会比较慢。
* YOLOv4-tiny 使用默认尺寸，解码过程没有大量的 for 与 NMS 所以速度会快些。
* YOLOv3-nano 与 v4-tiny 一样。
* Simple-Pose 暂时只写了安卓版本，iOS目前还没有增加。内部原理是先检测人再用人的区域再次进行姿态检测，即2步过程。
* Yolact 暂时只写了安卓版本，iOS目前还没有增加。

Note：<br/>
* 由于手机性能、图像尺寸等因素导致FPS在不同手机上相差比较大。该项目主要测试NCNN框架的使用，具体模型的转换可以去NCNN官方查看转换教程。<br/>
* 增加 Simple-Pose 模型时增加了opencv，由于库太大只保留 arm64-v8a/armeabi-v7a 有需要的自己去官方下载。
* ncnn暂时使用vulkan版本，在加载前需要打开加速，本项目中没有打开。如果要用ncnn版本需要修改CMakeLists.txt配置。

已知部分ncnn大佬网名：.... nihui qiuqiu

:art: 截图<br/>

<div>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_mobilenetv2_yolov3_nano.jpg"/>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_yolov4_tiny.jpg"/>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_yolov5s.jpg"/>
</div>
<div>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_simple_pose.jpg"/>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_yolact.jpg"/>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_meizu16x_chineseocr_lite_01.jpg"/>
</div>
<div>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_meizu16x_chineseocr_lite_02.jpg"/>
</div>

<div>
<img width="270" height="500" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/iOS_iPhone6s_yolov4_tiny.jpg"/>
<div/>

