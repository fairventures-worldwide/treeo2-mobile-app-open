//treeo project class structure
#include <iostream>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>

using namespace std;

class ObjectDetector {    
private:
    cv::Mat TreeInputImage;
    //cv::Mat CardInputImage;
    std::vector<cv::KeyPoint> card_keypoints;
    cv::Mat card_descriptors;
    cv::Rect roi;


    //pair<int, int>* card_polygon; //nebo std::vector<cv::Point2f>
    std::vector<cv::Point2f> card_polygon;
    double card_confidence = 0;//0 to 1
    //pair<int, int>* tree_polygon; //nebo std::vector<cv::Point2f>
    std::vector<cv::Point2f> tree_lines;
    double tree_confidence = 0; //0 to 1
    double diameter_value;
    double diameter_cofidence = 0; //0 to 1

    //the following functions only return error codes
    int setImage(string);
    int detectCard();
    int refineCardPosition();
    int detectTree();
    int computeDiameter();


    int checkXYInImage(int xy, int max_val);
    std::vector<cv::Point> treeToPolygon(std::vector<cv::Point2f> tree_lines);


    std::vector<cv::Point> floatToIntCard(std::vector<cv::Point2f>);

    //void reset();//internal structures/data
  public:
    /*ObjectDetector ();
    ObjectDetector (cv::Mat);
    ObjectDetector (string);*/
    ObjectDetector (cv::Mat card_image);

    //Getters
    double getDiameterValue(){return diameter_value;}
    vector<cv::Point2f> getCardPolygon(){return card_polygon;}
    vector<cv::Point2f> getTreeLines(){return tree_lines;}

    //int measureTree(cv::Mat input_image, double &diameter); //&confidence
    int measureTree(cv::Mat input_image, cv::Rect roi, std::vector<cv::Point> &card, std::vector<cv::Point> &tree, double &diameter); //&confidence
    /*return value is error type: 
    0 OK
    1 card failed
    2 tree failed 
    3 diameter failed
    */
};
