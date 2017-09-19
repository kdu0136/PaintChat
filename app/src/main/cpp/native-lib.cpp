#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/objdetect.hpp>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;
using namespace std;

extern "C" {
    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_loadImage(JNIEnv *env,
                                                                       jobject instance,
                                                                       jstring imageFileName,
                                                                       jlong addrImage) {
        Mat &img_input = *(Mat *) addrImage;

        const char *nativeFileNameString = env->GetStringUTFChars(imageFileName, JNI_FALSE);

        string filePath(nativeFileNameString);
        const char *pathDir = filePath.c_str();

        img_input = imread(pathDir, IMREAD_UNCHANGED);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterDefault(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage) {
        Mat &matInput = *(Mat *)inputImage;
        Mat &matResult = *(Mat *)outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGBA);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterGray(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage) {
        Mat &matInput = *(Mat *)inputImage;
        Mat &matResult = *(Mat *)outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGB);
        cvtColor(matResult, matResult, CV_RGB2GRAY);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterBlur(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage,
                                                                                       jint filterScale) {

        Mat &matInput = *(Mat *) inputImage;
        Mat &matResult = *(Mat *) outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGBA);
        blur( matResult, matResult, Size(filterScale, filterScale));
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterSketch(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage,
                                                                                        jint filterScale) {
        Mat &matInput = *(Mat *) inputImage;
        Mat &matResult = *(Mat *) outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGB);
        cvtColor( matResult, matResult, CV_RGB2GRAY);
        //노이즈 제거
        medianBlur(matResult, matResult, 7);
        //라플라시안 필터
        Laplacian(matResult, matResult, CV_8U, 5);
        //이진화
        threshold(matResult, matResult, filterScale, 255, THRESH_BINARY_INV);
        //노이즈 7, 라플라시안 5, 이진화 10 이거 조절
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterEmboss(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage,
                                                                                        jint filterScale) {

        Mat &matInput = *(Mat *) inputImage;
        Mat &matResult = *(Mat *) outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGB);
        cvtColor( matResult, matResult, CV_RGB2GRAY);

        Mat mask(3, 3, CV_32F, Scalar(0));
        mask.at<float>(0, 0) = -1.0 * filterScale;
        mask.at<float>(2, 2) = 1.0 * filterScale;

        filter2D(matResult, matResult, CV_16S, mask);
        matResult.convertTo(matResult, CV_8U, 1, 128);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_imageFilterWaterColor(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jlong inputImage,
                                                                                     jlong outputImage) {
        Mat &matInput = *(Mat *) inputImage;
        Mat &matResult = *(Mat *) outputImage;

        cvtColor( matInput, matResult, CV_BGRA2RGB);

        Mat t1 = matResult.clone();

        for(int i = 0; i< 15; i++){
            if(i%2 == 0)
                bilateralFilter(t1, matResult, 20, 32, 32);
            else
                bilateralFilter(matResult, t1, 20, 32, 32);
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_CameraActivity_saveImage(JNIEnv *env, jobject instance,
                                                                           jstring saveFilepath,
                                                                           jstring saveFileName,
                                                                           jlong saveImg) {
        Mat &img_file = *(Mat *) saveImg;
        const char *nativeMaskFilePathString = env->GetStringUTFChars(saveFilepath, JNI_FALSE); //파일 저장 경로
        const char *nativeMaskFileNameString = env->GetStringUTFChars(saveFileName, JNI_FALSE); //파일 저장 이름

        string fileFullName(nativeMaskFilePathString); //파일 이름 (경로 포함)
        fileFullName.append("/");
        fileFullName.append(nativeMaskFileNameString);
        const char *fileFullNameChar = fileFullName.c_str();

        cvtColor(img_file, img_file, CV_BGRA2RGBA);

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "이미지 저장 경로:  %s", fileFullNameChar);

        imwrite(fileFullNameChar, img_file);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_CameraActivity_loadImage(JNIEnv *env, jobject instance,
                                                                    jstring maskFileName,
                                                                    jlong maskImg) {
        Mat &img_mask = *(Mat *) maskImg;
        const char *nativeMaskFileNameString = env->GetStringUTFChars(maskFileName, JNI_FALSE);
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "마스크 이름:  %s", nativeMaskFileNameString);
        img_mask = imread(nativeMaskFileNameString, IMREAD_UNCHANGED);
        cvtColor(img_mask, img_mask, CV_BGRA2RGBA);
    }

    JNIEXPORT jlong JNICALL
    Java_com_example_kimdongun_paintchat_activity_CameraActivity_loadCascade(JNIEnv *env, jclass type,
                                                                         jstring cascadeFileName_) {
        const char *nativeFileNameString = env->GetStringUTFChars(cascadeFileName_, 0);

        jlong ret = 0;
        ret = (jlong) new CascadeClassifier(nativeFileNameString);
        if (((CascadeClassifier *) ret)->empty()) {
            __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
        }
        else
            __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);


        env->ReleaseStringUTFChars(cascadeFileName_, nativeFileNameString);

        return ret;
    }

    //처리할 이미지 크기를 리사이징 해주는 함수
    float resize(Mat img_src, Mat &img_resize, int resize_width){
        float scale = resize_width / (float)img_src.cols ;
        if (img_src.cols > resize_width) { //리사이즈 할 크기보다 원본 이미지가 더 큰 경우 리사이징 (비율 맞춰서)
            int new_height = cvRound(img_src.rows * scale);
            resize(img_src, img_resize, Size(resize_width, new_height));
        } else { //리사이즈 할 크기보다 원본 이미지가 작거나 같은 경우
            img_resize = img_src; //원본 이미지를 바로 사용
        }
        return scale;
    }

    //배경에 이미지 오버레이 해주는 함수 (덫칠)
    void overlayImage(const cv::Mat &background, const cv::Mat &mask, cv::Point2i location)
    {
        //씌워 줄 마스크의 y 좌표부터 (화면 맨 끝 or 마스크 맨끝 - 둘 중 더 작은 값) 까지
        for(int y = location.y; y < min(background.rows, location.y + mask.rows); y++)
        {
            int maskAbY = y - location.y; //마스크 절대 y값 (0~마스크 높이)
            //씌워 줄 마스크의 x 좌표부터 (화면 맨 아래 or 마스크 맨 아래 - 둘 중 더 작은 값) 까지
            for(int x = location.x; x < min(background.cols, location.x + mask.cols); x++)
            {
                int maskAbX = x - location.x; //마스크 절대 x값 (0~마스크 넓이)
                for(int channel = 0; channel < background.channels(); channel++) //RGB채널 모두 변경
                {
                    //마스크 알파값
                    double opacity = ((double)mask.data[maskAbY * mask.step + maskAbX * mask.channels() + 3])/ 255.;
                    unsigned char maskPx = mask.data[maskAbY * mask.step + maskAbX * mask.channels() + channel];
                    unsigned char backgroundPx = background.data[y * background.step + x * background.channels() + channel];
                    background.data[y * background.step + background.channels() * x + channel] = backgroundPx * (1. - opacity) + maskPx * opacity;
                }
            }
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_CameraActivity_detect(JNIEnv *env, jclass type,
                                                                        jlong cascadeClassifier_face,
                                                                        jlong matAddrInput,
                                                                        jlong matAddrResult,
                                                                        jlong maskInput,
                                                                        jint maskSizeX,
                                                                        jint maskSizeY,
                                                                        jint maskPosX,
                                                                        jint maskPosY,
                                                                        jboolean isMask,
                                                                        jint imgResize) {
        Mat &img_mask = *(Mat *)maskInput;

        Mat &img_input = *(Mat *) matAddrInput; //인풋 프레임
        Mat &img_result = *(Mat *) matAddrResult; //아웃풋 프레임

        img_result = img_input.clone();

        std::vector<Rect> faces; //얼굴을 담을 벡터
        Mat img_gray;

        cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
        equalizeHist(img_gray, img_gray); //히스토그램 평활화 (평활화 - 명암의 분포를 재 분해 하는 작업)
        //https://m.blog.naver.com/PostView.nhn?blogId=rladnduf87&logNo=220402286769&proxyReferer=https%3A%2F%2Fwww.google.co.kr%2F - 설명

        Mat img_resize;
        float resizeRatio = resize(img_gray, img_resize, imgResize); //리사이징 크기가 너무 크면 처리속도 오래 걸리고 작으면 얼굴 인식을 못함

        //-- Detect faces
        ((CascadeClassifier *) cascadeClassifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );

    //        __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",
    //                            (char *) "face %d found ", faces.size());

        for (int i = 0; i < faces.size(); i++) {
            double maskSizeUpX = faces[i].width * 0.1 * maskSizeX; //늘릴 얼굴 길이 X
            double defaultSizeUpY = faces[i].height * 0.3; //기본으로 늘릴 얼굴 길이 Y
            double maskSizeUpY = faces[i].height * 0.1 * maskSizeY; //늘릴 얼굴 길이 Y
            double realFaceX = (faces[i].x - (maskSizeUpX*0.5) + maskPosX) / resizeRatio; //얼굴의 x좌표(비율)
            double realFaceY = (faces[i].y - defaultSizeUpY - (maskSizeUpY*0.5) - maskPosY) / resizeRatio; //얼굴의 y좌표(비율)
            double realFaceWidth = (faces[i].width + maskSizeUpX) / resizeRatio; //얼굴의 width(비율)
            double realFaceHeight = (faces[i].height + defaultSizeUpY + maskSizeUpY) / resizeRatio; //얼굴의 height(비율)

            if(realFaceX < 0 ) realFaceX = 0;
            if(realFaceY < 0 ) realFaceY = 0;

            Rect face_area(realFaceX, realFaceY, realFaceWidth, realFaceHeight); //인식한 얼굴 크기만큼 rect 생성

//            rectangle(img_result, face_area, Scalar(255, 0, 0), 2); //얼굴에 네모 그리기
            if(isMask != 0) { //마스크 있으면 마스크 씌우기
                Mat mask_resize = img_mask.clone(); //리사이즈 할 마스크 매트리스
                resize(img_mask, mask_resize, face_area.size(), 0, 0,
                       CV_INTER_NN); //얼굴 사이즈 만큼 마스크 이미지 리사이즈
                overlayImage(img_result, mask_resize,
                             Point(face_area.x, face_area.y)); //얼굴에 마스크 씌우기
            }
        }
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ChatRoomActivity_loadImage(JNIEnv *env,
                                                                         jobject instance,
                                                                         jstring imageFileName,
                                                                         jlong addrImage,
                                                                             jint resizing) {
        Mat &img_input = *(Mat *) addrImage;

        const char *nativeFileNameString = env->GetStringUTFChars(imageFileName, JNI_FALSE);

        string filePath(nativeFileNameString);
        const char *pathDir = filePath.c_str();

        img_input = imread(pathDir, IMREAD_UNCHANGED);
        cvtColor( img_input, img_input, CV_BGRA2RGBA);

        resize(img_input, img_input, resizing); //리사이징
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ChatRoomActivity_saveImage(JNIEnv *env, jobject instance,
                                                                            jstring saveFilepath,
                                                                            jstring saveFileName,
                                                                            jlong saveImg) {
        Mat &img_file = *(Mat *) saveImg;
        const char *nativeMaskFilePathString = env->GetStringUTFChars(saveFilepath, JNI_FALSE); //파일 저장 경로
        const char *nativeMaskFileNameString = env->GetStringUTFChars(saveFileName, JNI_FALSE); //파일 저장 이름

        string fileFullName(nativeMaskFilePathString); //파일 이름 (경로 포함)
        fileFullName.append("/");
        fileFullName.append(nativeMaskFileNameString);
        const char *fileFullNameChar = fileFullName.c_str();

        cvtColor(img_file, img_file, CV_BGRA2RGBA);

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                        "이미지 저장 경로:  %s", fileFullNameChar);

        imwrite(fileFullNameChar, img_file);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_fragment_FragmentAccount_loadImage(JNIEnv *env,
                                                                         jobject instance,
                                                                         jstring imageFileName,
                                                                         jlong addrImage,
                                                                            jint resizing) {
        Mat &img_input = *(Mat *) addrImage;

        const char *nativeFileNameString = env->GetStringUTFChars(imageFileName, JNI_FALSE);

        string filePath(nativeFileNameString);
        const char *pathDir = filePath.c_str();

        img_input = imread(pathDir, IMREAD_UNCHANGED);
        cvtColor( img_input, img_input, CV_BGRA2RGBA);

        resize(img_input, img_input, resizing); //리사이징
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_fragment_FragmentAccount_saveImage(JNIEnv *env, jobject instance,
                                                                         jstring saveFilepath,
                                                                         jstring saveFileName,
                                                                         jlong saveImg) {
        Mat &img_file = *(Mat *) saveImg;
        const char *nativeMaskFilePathString = env->GetStringUTFChars(saveFilepath, JNI_FALSE); //파일 저장 경로
        const char *nativeMaskFileNameString = env->GetStringUTFChars(saveFileName, JNI_FALSE); //파일 저장 이름

        string fileFullName(nativeMaskFilePathString); //파일 이름 (경로 포함)
        fileFullName.append("/");
        fileFullName.append(nativeMaskFileNameString);
        const char *fileFullNameChar = fileFullName.c_str();

        cvtColor(img_file, img_file, CV_BGRA2RGBA);

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                        "이미지 저장 경로:  %s", fileFullNameChar);

        imwrite(fileFullNameChar, img_file);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_ImageFilterActivity_saveImage(JNIEnv *env, jobject instance,
                                                                       jstring saveFilepath,
                                                                       jstring saveFileName,
                                                                       jlong saveImg) {
        Mat &img_file = *(Mat *) saveImg;
        const char *nativeMaskFilePathString = env->GetStringUTFChars(saveFilepath,
                                                                      JNI_FALSE); //파일 저장 경로
        const char *nativeMaskFileNameString = env->GetStringUTFChars(saveFileName,
                                                                      JNI_FALSE); //파일 저장 이름

        string fileFullName(nativeMaskFilePathString); //파일 이름 (경로 포함)
        fileFullName.append("/");
        fileFullName.append(nativeMaskFileNameString);
        const char *fileFullNameChar = fileFullName.c_str();

        cvtColor(img_file, img_file, CV_BGRA2RGBA);

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "이미지 저장 경로:  %s", fileFullNameChar);

        imwrite(fileFullNameChar, img_file);
    }

    JNIEXPORT void JNICALL
    Java_com_example_kimdongun_paintchat_activity_CanvasActivity_loadImage(JNIEnv *env,
                                                                         jobject instance,
                                                                         jstring imageFileName,
                                                                           jlong addrImage,
                                                                           jint rot) {
        Mat &img_input = *(Mat *) addrImage;
//        Mat img_temp;

        const char *nativeFileNameString = env->GetStringUTFChars(imageFileName, JNI_FALSE);

        string filePath(nativeFileNameString);
        const char *pathDir = filePath.c_str();

        img_input = imread(pathDir, IMREAD_UNCHANGED);

        cvtColor( img_input, img_input, CV_BGRA2RGBA);

//        cv::Point2f ptCp(img_temp.cols*0.5, img_temp.rows*0.5);
//        cv::Mat M = cv::getRotationMatrix2D(ptCp, rot * -1, 1.0);
//        cv::warpAffine(img_temp, img_input, M, img_temp.size(), cv::INTER_CUBIC); //Nearest is too rough,
    }
}
