## :rocket: 编码不易，点个star！ ##

### 移动端目标检测，项目支持YOLOv5s与YOLOv4-tiny模型，摄像头实时捕获视频流进行检测。

## iOS:
- Xcode 11.5
- macOS 10.15.4
- iPhone 5s 12.4.5

## android:
- android studio 4.0
- win10 1909
- meizu 16x 8.1.0

部分代码参考自：sunnyden 的开源项目。

安卓请自行确认相关权限是否允许，代码中没有做过多的处理。

YOLOv5s:mainactivity->USE_YOLOV5 = true or ViewController.mm->USE_YOLOV5 = YES;

YOLOv4-tiny:mainactivity->USE_YOLOV5 = false or ViewController.mm->USE_YOLOV5 = NO;

YOLOv5s输入尺寸减小，解码过程使用了大量的 for 循环与 NMS 表现出来会比较慢。

YOLOv4-tiny使用默认尺寸，解码过程没有大量的 for 与 NMS 所以速度会快些。

:art: 截图<br/>
<div>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/Android_16X.jpg"/>
<img width="288" height="512" src="https://github.com/WZTENG/YOLOv5_NCNN/blob/master/Screenshots/iOS_iPhone5s.jpg"/>
</div>

