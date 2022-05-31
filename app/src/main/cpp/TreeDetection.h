#ifndef TREEDETECTION_H
#define TREEDETECTION_H

#include <stdio.h>
#include <string>
#include <iostream>
#include <map>
#include <iterator>

#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>

#define TREE_SHOW_IMAGES 0

struct tree {
    std::pair<cv::Point2f, cv::Point2f> left_line;
    std::pair<cv::Point2f, cv::Point2f> right_line;
} ;

class TreeDetection {
private:
    std::string TAG = "TreeDetection";

    cv::Mat image;      /**< Input image resized to 'resize_to_width' */
    cv::Mat image_roi;  /**< Cropped resized image */   
    cv::Rect2f roi;     /**< Region of interest above or under card */  
    float resize_to_width = 600;    /**< The width to which the input image is resized */
    float roi_height = resize_to_width/3;   /**< The height of the area where the tree is searched */ 
    float ratio;        /**< Ratio of resized width and original width */
    float detection_score = 0.0;

    cv::Mat tree_mask_roi;  /**< Binary mask of the tree */
    std::map<std::string, cv::Point2f> card_points; /**< Ordered card points in map. Top left point = 'tl', bottom right = 'br' */
    std::tuple<cv::Point2f, cv::Point2f> left_tree_line, right_tree_line;   /**< The edge of tree represented by a line. Tuple points, top point first */
    //std::tuple<cv::Point2f, cv::Point2f> left_tree_line2, right_tree_line2;


    void doGrabcut(cv::Point2f card_middle);
    int findLines();

    void linePoints(cv::Vec2f line, cv::Point2f& pt1, cv::Point2f& pt2);
    cv::Point2f intersection(std::tuple<cv::Point2f, cv::Point2f> image_line, std::tuple<cv::Point2f, cv::Point2f> line);
    int linesIntersect(cv::Point2f p1, cv::Point2f p2, cv::Point2f p3, cv::Point2f p4);
    std::map<std::string, cv::Point2f> orderCardPoints(std::vector<cv::Point2f> points);
    int cardPositionToLines(cv::Point2f p1, cv::Point2f p2, cv::Point2f p3, cv::Point2f p4);
    double linesToCardDistance(tree tree);
    double pointPositionToLine(cv::Point2f p1, cv::Point2f p2, cv::Point2f p);
    int checkEdges(tree);
    double linesAngle(tree);
    std::vector<float> inVsOutTreeColorDifference(tree);
    std::vector<float> outer_sobel_edge(tree);
    double colorDistance(cv::Scalar c1, cv::Scalar c2);


public:
    TreeDetection(cv::Mat source_img, std::vector<cv::Point2f> card_points);
    ~TreeDetection(){};

    int findTree(int position);

    cv::Mat getOutputImage();
    cv::Mat getTreeMask(){return tree_mask_roi;}
    std::vector<cv::Point2f> getTreeLines();
    float getROIarea(){ return image_roi.cols * image_roi.rows; };
    float getDetectionScore(){ return detection_score;}
    //std::tuple<cv::Point2f, cv::Point2f> getLeftTreeLine(){return this->left_tree_line;};
    //std::tuple<cv::Point2f, cv::Point2f> getRightTreeLine(){return this->right_tree_line;};
};


double pointsDistance(cv::Point2f p1, cv::Point2f p2);
double pointDistanceToLine(cv::Point2f line_start, cv::Point2f line_end, cv::Point2f point);
cv::Mat maskCard(std::map<std::string, cv::Point2f> points, cv::Mat input, int margin = 0);
void printTree (tree tree);


#endif //TREEDETECTION_H