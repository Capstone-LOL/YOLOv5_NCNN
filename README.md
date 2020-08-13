## :rocket: 编码不易，点个star！ ##

### 移动端目标检测，项目支持YOLOv5s与YOLOv4-tiny模型，摄像头实时捕获视频流进行检测。

## iOS:
- Xcode 11.5
- macOS 10.15.4
- iPhone 5s 12.4.5

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
```
> Android
直接在界面选择即可。

## 模型速度
* YOLOv5s输入尺寸减小，解码过程使用了大量的 for 循环与 NMS 表现出来会比较慢。
* YOLOv4-tiny使用默认尺寸，解码过程没有大量的 for 与 NMS 所以速度会快些。
* YOLOv3-nano与v4-tiny一样。

Note：由于手机性能、图像尺寸等因素导致FPS在不同手机上相差比较大。该项目主要测试NCNN框架的使用，具体模型的转换可以去NCNN官方查看转换教程。

:art: 截图<br/>
<div>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_mobilenetv2_yolov3_nano.jpg"/>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_yolov4_tiny.jpg"/>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_Meizu16x_yolov5s.jpg"/>
</div>
<div>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/iOS_iPhone5s.jpg"/>
<div/>

