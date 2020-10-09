//
// Created by WZTENG on 2020/09/21 028.
//

#include "MbnFCN.h"
#include "SimplePose.h"
#include <android/log.h>


bool MbnFCN::hasGPU = true;
MbnFCN *MbnFCN::detector = nullptr;

MbnFCN::MbnFCN(AAssetManager *mgr, bool useGPU) {

    MBNFCNsim = new ncnn::Net();
    // opt 需要在加载前设置
    hasGPU = ncnn::get_gpu_count() > 0;
    MBNFCNsim->opt.use_vulkan_compute = hasGPU && useGPU;  // gpu
    MBNFCNsim->opt.use_fp16_arithmetic = true;  // fp16运算加速
    MBNFCNsim->load_param(mgr, "fcn_mbv2-sim-opt.param");
    MBNFCNsim->load_model(mgr, "fcn_mbv2-sim-opt.bin");
//    LOGD("mbnfcn_detector");

}

MbnFCN::~MbnFCN() {
    MBNFCNsim->clear();
    delete MBNFCNsim;
}

ncnn::Mat MbnFCN::detect_mbnfcn(JNIEnv *env, jobject image) {
    AndroidBitmapInfo img_size;
    AndroidBitmap_getInfo(env, image, &img_size);
    ncnn::Mat in_net = ncnn::Mat::from_android_bitmap_resize(env, image, ncnn::Mat::PIXEL_RGBA2RGB, target_size_w,
                                                             target_size_h);
    float mean[3] = {123.68f, 116.28f, 103.53f};
    float norm[3] = {1.0 / 58.40f, 1.0 / 57.12f, 1.0 / 57.38f};
    in_net.substract_mean_normalize(mean, norm);

    ncnn::Mat maskout;

    auto ex = MBNFCNsim->create_extractor();
    ex.set_light_mode(true);
    ex.set_num_threads(4);
    hasGPU = ncnn::get_gpu_count() > 0;
    ex.set_vulkan_compute(hasGPU);
    ex.input("input.1", in_net);
    ex.extract("581", maskout);

    int mask_c = maskout.c;
    int mask_w = maskout.w;
    int mask_h = maskout.h;
//    LOGD("jni fcn mask c:%d w:%d h:%d", mask_c, mask_w, mask_h);

    cv::Mat prediction = cv::Mat::zeros(mask_h, mask_w, CV_8UC1);
    ncnn::Mat chn[mask_c];
    for (int i = 0; i < mask_c; i++) {
        chn[i] = maskout.channel(i);
    }
    for (int i = 0; i < mask_h; i++) {
        const float *pChn[mask_c];
        for (int c = 0; c < mask_c; c++) {
            pChn[c] = chn[c].row(i);
        }

        auto *pCowMask = prediction.ptr<uchar>(i);

        for (int j = 0; j < mask_w; j++) {
            int maxindex = -1;
            float maxvalue = -100.0f;
            for (int n = 0; n < mask_c; n++) {
                if (pChn[n][j] > maxvalue) {
                    maxindex = n;
                    maxvalue = pChn[n][j];
                }
            }
            pCowMask[j] = maxindex;
        }

    }

//    ncnn::Mat maskMat;
//    maskMat = ncnn::Mat::from_pixels(prediction.data, ncnn::Mat::PIXEL_GRAY, prediction.cols, prediction.rows);

    cv::Mat pred_resize;
    cv::resize(prediction, pred_resize, cv::Size(img_size.width, img_size.height), 0, 0, cv::INTER_NEAREST);

    ncnn::Mat maskMat;
    maskMat = ncnn::Mat::from_pixels_resize(pred_resize.data, ncnn::Mat::PIXEL_GRAY,
                                            pred_resize.cols, pred_resize.rows,
                                            img_size.width, img_size.height);

//    LOGD("jni fcn maskMat 0:%f", maskMat.channel(0).row(0)[0]);
//    LOGD("jni fcn maskMat end w:%d h:%d", maskMat.w, maskMat.h);

    return maskMat;

}

