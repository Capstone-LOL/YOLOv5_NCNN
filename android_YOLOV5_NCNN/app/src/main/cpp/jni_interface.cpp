#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "YoloV5.h"
#include "YoloV4.h"
#include "SimplePose.h"
#include "Yolact.h"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        YoloV5::hasGPU = true;
        YoloV4::hasGPU = true;
        SimplePose::hasGPU = true;
    }
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
}

extern "C" JNIEXPORT void JNICALL
Java_com_wzt_yolov5_YOLOv5_init(JNIEnv *env, jclass, jobject assetManager) {
    if (YoloV5::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        YoloV5::detector = new YoloV5(mgr, "yolov5.param", "yolov5.bin");
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_wzt_yolov5_YOLOv5_detect(JNIEnv *env, jclass, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = YoloV5::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/wzt/yolov5/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label, box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}

/*********************************************************************************************
                                         YOLOv4-tiny
 yolov4官方ncnn模型下载地址
 darknet2ncnn:https://drive.google.com/drive/folders/1YzILvh0SKQPS_lrb33dmGNq7aVTKPWS0
 ********************************************************************************************/

// 20200813 增加 MobileNetV2-YOLOv3-Nano-coco

extern "C" JNIEXPORT void JNICALL
Java_com_wzt_yolov5_YOLOv4_init(JNIEnv *env, jclass, jobject assetManager, jboolean v4tiny) {
    if (YoloV4::detector != nullptr) {
        delete YoloV4::detector;
        YoloV4::detector = nullptr;
    }
    if (YoloV4::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        if (v4tiny == 1) {
            YoloV4::detector = new YoloV4(mgr, "yolov4-tiny-opt.param", "yolov4-tiny-opt.bin");
        } else if (v4tiny == 0) {
            YoloV4::detector = new YoloV4(mgr, "MobileNetV2-YOLOv3-Nano-coco.param", "MobileNetV2-YOLOv3-Nano-coco.bin");
//            YoloV4::detector = new YoloV4(mgr,"export_demo.param","export_demo.bin");
        }
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_wzt_yolov5_YOLOv4_detect(JNIEnv *env, jclass, jobject image, jdouble threshold, jdouble nms_threshold) {
    auto result = YoloV4::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/wzt/yolov5/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label, box.score);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}


/*********************************************************************************************
                                         SimplePose
 ********************************************************************************************/

extern "C" JNIEXPORT void JNICALL
Java_com_wzt_yolov5_SimplePose_init(JNIEnv *env, jclass clazz, jobject assetManager) {
    if (SimplePose::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        SimplePose::detector = new SimplePose(mgr);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_wzt_yolov5_SimplePose_detect(JNIEnv *env, jclass clazz, jobject image) {
    auto result = SimplePose::detector->detect(env, image);

    auto box_cls = env->FindClass("com/wzt/yolov5/KeyPoint");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FF)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;
    for (auto &keypoint : result) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, keypoint.p.x, keypoint.p.y);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;

}

/*********************************************************************************************
                                         Yolact
 ********************************************************************************************/
jintArray matToBitmapIntArray(JNIEnv *env, const cv::Mat &image) {
    jintArray resultImage = env->NewIntArray(image.total());
    auto *_data = new jint[image.total()];
    for (int i = 0; i < image.total(); i++) {  // =========== 注意这里再确认下要不要除3
        char r = image.data[3 * i + 2];
        char g = image.data[3 * i + 1];
        char b = image.data[3 * i + 0];
        char a = (char) 255;
        _data[i] = (((jint) a << 24) & 0xFF000000) + (((jint) r << 16) & 0x00FF0000) +
                   (((jint) g << 8) & 0x0000FF00) + ((jint) b & 0x000000FF);
    }
    env->SetIntArrayRegion(resultImage, 0, image.total(), _data);
    delete[] _data;
    return resultImage;
}

jcharArray matToBitmapCharArray(JNIEnv *env, const cv::Mat &image) {
    jcharArray resultImage = env->NewCharArray(image.total());
    auto *_data = new jchar[image.total()];
    for (int i = 0; i < image.total(); i++) {
        char m = image.data[i];
        _data[i] = (m & 0xFF);
    }
    env->SetCharArrayRegion(resultImage, 0, image.total(), _data);
    delete[] _data;
    return resultImage;
}

extern "C" JNIEXPORT void JNICALL
Java_com_wzt_yolov5_Yolact_init(JNIEnv *env, jclass clazz, jobject assetManager) {
    if (Yolact::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        Yolact::detector = new Yolact(mgr);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_wzt_yolov5_Yolact_detect(JNIEnv *env, jclass clazz, jobject image) {
    auto result = Yolact::detector->detect_yolact(env, image);

    auto yolact_mask = env->FindClass("com/wzt/yolov5/YolactMask");
//    auto cid = env->GetMethodID(yolact_mask, "<init>", "(FFFFIF[F[I)V");
    auto cid = env->GetMethodID(yolact_mask, "<init>", "(FFFFIF[F[C)V");
    jobjectArray ret = env->NewObjectArray(result.size(), yolact_mask, nullptr);
    int i = 0;
    for (auto &mask : result) {
//        LOGD("jni yolact mask rect x:%f y:%f", mask.rect.x, mask.rect.y);
//        LOGD("jni yolact maskdata size:%d", mask.maskdata.size());
//        LOGD("jni yolact mask size:%d", mask.mask.cols * mask.mask.rows);
//        jintArray jintmask = matToBitmapIntArray(env, mask.mask);
        jcharArray jcharmask = matToBitmapCharArray(env, mask.mask);

        env->PushLocalFrame(1);
        jfloatArray maskdata = env->NewFloatArray(mask.maskdata.size());
        jfloat *jnum = new jfloat[mask.maskdata.size()];
        for (int i = 0; i < mask.maskdata.size(); ++i) {
            *(jnum + i) = mask.maskdata[i];
        }
        env->SetFloatArrayRegion(maskdata, 0, mask.maskdata.size(), jnum);
        delete[] jnum;

        jobject obj = env->NewObject(yolact_mask, cid,
                mask.rect.x, mask.rect.y, mask.rect.x + mask.rect.width, mask.rect.y + mask.rect.height,
                mask.label, mask.prob, maskdata, jcharmask);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}

