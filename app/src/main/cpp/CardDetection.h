#ifndef CARDDETECTION_H
#define CARDDETECTION_H


#include <stdio.h>
#include <string>
#include <iostream>
#include <math.h>
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>

#include <opencv2/features2d/features2d.hpp>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

class CardDetection {
    private: 
        std::string TAG = "CardDetection";
        cv::Mat sourceImg;                  //image of tree and card
        std::vector<cv::Point2f> points;    //vector of card points (upper left, upper right, bottom right, bottom left)
        float card_confidence;              //confidence that card was found
        cv::Rect roi;                       // region of interest where card will be searched, if card is not in ROI it wont be found (ROI is cv::Rect(x,y,w,h), where origin is top left corner)
        std::vector<cv::KeyPoint> keypoints_card; //card keypoints loaded from .txt file which contains model of card (500*316px)
        Mat descriptors_card;               //card descriptors loaded from .txt file which contains model of card (500*316px)

        int good_matches;                   //just for development..
        int num_card_descriptors;
        int num_image_descriptors;
        int inliers; 

        cv::Mat inliersDrawn;

        std::vector<cv::Point2f> findCard();
        float confidence();
        

    public:
        CardDetection(cv::Mat sourceImg, cv::Mat descriptors_card, std::vector<cv::KeyPoint> keypoints_card, cv::Rect roi);
        cv::Mat getMarkedImage();
        std::vector<cv::Point2f> getPoints();
        float getConfidenceScore();
        int getGoodMatches();
        int getNumCardDescriptors();
        int getNumImageDescriptors();
        int getInliers();
        Mat getinliersDrawn();

};

float angleBetween3Points(cv::Point2f a, cv::Point2f b, cv::Point2f c);

#endif //CARDDETECTION_H