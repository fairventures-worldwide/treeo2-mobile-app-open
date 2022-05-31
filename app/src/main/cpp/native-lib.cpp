#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include "ObjectDetector.h"
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <opencv2/imgproc.hpp>
#include <android/bitmap.h>
#include <fstream>
#include <sstream>
#include <unistd.h>
#include <__mutex_base>
#define  LOG_TAG    "TREEO-JNI"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))



namespace {

    constexpr char *RES_RAW_CONFIG_PATH_ENV_VAR = "RES_RAW_CONFIG_PATH";
    constexpr char *RES_CARD_FILE_NAME = "treeo_card.png";
    constexpr char *RES_SAMPLE_FILE_NAME = "tree.jpeg";
    constexpr char *TREEO_CARD_FRONT = "karta39.png";
    constexpr char *TREEO_CARD_BACK = "karta40.png";


    jobject getAssetManagerFromJava(JNIEnv *env, jobject obj);

    cv::Mat readFileFromAsset(JNIEnv *env, jobject obj);

    cv::Mat readSampleImage(JNIEnv *env, jobject obj);

    std::string readFile(std::string filePath);

}


namespace {

    cv::Mat readFileFromAsset(JNIEnv *env, jobject obj) {

        jobject jam = getAssetManagerFromJava(env, obj);
        cv::Mat h;
        if (jam) {
            AAssetManager *am = AAssetManager_fromJava(env, jam);
            if (am) {
                AAsset *assetFile = AAssetManager_open(am, TREEO_CARD_FRONT, AASSET_MODE_BUFFER);

                const void *buf = AAsset_getBuffer(assetFile);


                LOGD("%s:\n%s", TREEO_CARD_FRONT, static_cast<const char *>(buf));

                long sizeOfImg = AAsset_getLength(assetFile);
                char *buffer = (char *) malloc(sizeof(char) * sizeOfImg);
                AAsset_read(assetFile, buffer, sizeOfImg);

                std::vector<char> data(buffer, buffer + sizeOfImg);

                h = cv::imdecode(data, -1);

            }
        }
        return h;
    }

    cv::Mat readSampleImage(JNIEnv *env, jobject obj) {

        jobject jam = getAssetManagerFromJava(env, obj);
        cv::Mat h;
        if (jam) {
            AAssetManager *am = AAssetManager_fromJava(env, jam);
            if (am) {
                AAsset *assetFile = AAssetManager_open(am, RES_SAMPLE_FILE_NAME,
                                                       AASSET_MODE_BUFFER);

                const void *buf = AAsset_getBuffer(assetFile);


                LOGD("%s:\n%s", RES_SAMPLE_FILE_NAME, static_cast<const char *>(buf));

                long sizeOfImg = AAsset_getLength(assetFile);
                char *buffer = (char *) malloc(sizeof(char) * sizeOfImg);
                AAsset_read(assetFile, buffer, sizeOfImg);

                std::vector<char> data(buffer, buffer + sizeOfImg);

                h = cv::imdecode(data, -1);
            }
        }
        return h;
    }

    jobject getAssetManagerFromJava(JNIEnv *env, jobject obj) {
        jclass clazz = env->GetObjectClass(obj);
        jmethodID method = env->GetMethodID(clazz, "getAssetManager",
                                            "()Landroid/content/res/AssetManager;");
        jobject ret = env->CallObjectMethod(obj, method);

        return ret;
    }

    std::string readFile(std::string filePath) {
        std::ifstream ifs(filePath);
        std::stringstream ss;

        ss << ifs.rdbuf();

        return ss.str();
    }

}


jfieldID getPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID ptrFieldId = nullptr;
    if (!ptrFieldId) {
        jclass c = env->GetObjectClass(obj);
        ptrFieldId = env->GetFieldID(c, "ptrObjectHolder", "J");
        env->DeleteLocalRef(c);
    }
    return ptrFieldId;
}


extern "C"
JNIEXPORT void JNICALL
Java_org_treeo_treeo_ui_treemeasurement_TMViewModel_init(JNIEnv *env, jobject thiz) {
    cv::Mat cardImageFile = readFileFromAsset(env, thiz);
    auto *cls = new ObjectDetector(cardImageFile);
    env->SetLongField(thiz, getPtrFieldId(env, thiz), reinterpret_cast<jlong>(cls));
}


extern "C"
JNIEXPORT void JNICALL
Java_org_treeo_treeo_ui_treemeasurement_TMViewModel_cleanup(JNIEnv *env, jobject thiz) {
    auto *cppObj = (ObjectDetector *) env->GetLongField(thiz, getPtrFieldId(env, thiz));
    delete cppObj;
}


extern "C"
JNIEXPORT jstring JNICALL
Java_org_treeo_treeo_ui_treemeasurement_TMViewModel_getTreeData(JNIEnv *env,
                                                                jobject thiz,
                                                                jlong mat, jfloat roi_x,
                                                                jfloat roi_y, jfloat roi_x2,
                                                                jfloat roi_y2, jstring stage) {

    auto *objectDetector = (ObjectDetector *) env->GetLongField(thiz, getPtrFieldId(
            env, thiz));

    cv::Mat input = *(cv::Mat *) mat;

    double diameter;
    double _diameter;
    std::vector<cv::Point> card;
    std::vector<cv::Point> tree;
    cv::Rect roi;

    string _cardPolygonString;
    string _treeLinesString;

    int x = input.cols;
    int y = input.rows;


    string leStage = "stage1";
    jstring leString = env->NewStringUTF(leStage.c_str());


    const char *nativeString1 = (*env).GetStringUTFChars(stage, nullptr);
    const char *nativeString2 = (*env).GetStringUTFChars(leString, nullptr);

    int xd = strcmp(nativeString1, nativeString2);

    if(xd == 0){
        roi = cv::Rect(static_cast<int>(x*roi_x),static_cast<int>(y*roi_y),static_cast<int>(x * roi_x2 - x*roi_x),static_cast<int>(y * roi_y2 -  y*roi_y));
    }

    objectDetector->measureTree(input, roi, card, tree, diameter);

    _diameter = objectDetector->getDiameterValue();
    std::vector<cv::Point2f> _cardPolygon = objectDetector->getCardPolygon();
    std::vector<cv::Point2f> _treeLines = objectDetector->getTreeLines();


    int cardPolygonSize = _cardPolygon.size();
    for (int k = 0; k < cardPolygonSize; k++) {
        _cardPolygonString +=
                "_" + to_string(_cardPolygon[k].x) + "," + to_string(_cardPolygon[k].y);
    }
    int treeLinesSize = _treeLines.size();
    for (int k = 0; k < treeLinesSize; k++) {
        _treeLinesString +=
                "_" + to_string(_treeLines[k].x) + "," + to_string(_treeLines[k].y);
    }

    string sdf =
            "{diameter* " + std::to_string(_diameter) + " *card_polygon* " + _cardPolygonString +
            "*, tree_polygon* " + _treeLinesString + "*}";

    return env->NewStringUTF(sdf.c_str());
}



