//treeo project class structure
#include "ObjectDetector.h"
#include "CardDetection.h"
#include "TreeDetection.h"
#include "TreeDiameter.h"
#include "SaveBinarySIFTmodel.h"

using namespace std;


/*
int ObjectDetector::measureTree (cv::Mat input_image, double &diameter) {
    this->TreeInputImage = input_image;
    int ret_value = 0;

    // detect all
    ret_value = detectCard();
    if (ret_value > 0) return ret_value;

    ret_value = detectTree();
    if (ret_value > 0) return ret_value;

    //measure
    computeDiameter();
    //return vals
    diameter = this->diameter_value;

    return 0;
}*/

int ObjectDetector::measureTree (cv::Mat input_image, cv::Rect roi, std::vector<cv::Point> &card, std::vector<cv::Point> &tree, double &diameter) {
    this->TreeInputImage = input_image;
    this->roi = roi;
    int ret_value = 0;

    // detect all
    ret_value = detectCard();
    if (ret_value > 0) return ret_value;

    ret_value = detectTree();
    if (ret_value > 0) return ret_value;

    //measure
    computeDiameter();
    //return vals
    card = floatToIntCard(this->card_polygon);
    tree = treeToPolygon(this->tree_lines);
    diameter = this->diameter_value;

    return 0;
}



/**
 * Transform tree lines to polygon (change points order), round to integer and check image borders.
 * @param tree_lines
 * @return 4 tree points as polygon (int)
 */
std::vector<cv::Point> ObjectDetector::treeToPolygon(std::vector<cv::Point2f> tree_lines) {
    std::vector<cv::Point> out;
    out.push_back( cv::Point(checkXYInImage(round(tree_lines.at(0).x),this->TreeInputImage.cols), checkXYInImage(round(tree_lines.at(0).y),this->TreeInputImage.rows) ));
    out.push_back( cv::Point(checkXYInImage(round(tree_lines.at(2).x),this->TreeInputImage.cols), checkXYInImage(round(tree_lines.at(2).y),this->TreeInputImage.rows) ));
    out.push_back( cv::Point(checkXYInImage(round(tree_lines.at(3).x),this->TreeInputImage.cols), checkXYInImage(round(tree_lines.at(3).y),this->TreeInputImage.rows) ));
    out.push_back( cv::Point(checkXYInImage(round(tree_lines.at(1).x),this->TreeInputImage.cols), checkXYInImage(round(tree_lines.at(1).y),this->TreeInputImage.rows) ));
    
    return out;
}



/**
 * Check if the coordinate is within the range of the image. If not set it as the image border.
 * @param xy x or y coordinate
 * @param max_val the size of the image in the given axis 
 * @return coordinate within the range of image
 */
int ObjectDetector::checkXYInImage(int xy, int max_val) {

    if (xy <= 0) 
        return 0;

    if (xy >= max_val)
        return max_val-1;

    return xy;
}

/**
 * Convert card points from floats to integers and check if the integer coordinate is within the range of the image
 * @param pointsF floats card points
 */
std::vector<cv::Point> ObjectDetector::floatToIntCard(std::vector<cv::Point2f> pointsF) {
    std::vector<cv::Point> pointsI;
    pointsI.push_back(Point(checkXYInImage(round(pointsF[0].x),TreeInputImage.cols),checkXYInImage(round(pointsF[0].y),TreeInputImage.rows)));
    pointsI.push_back(Point(checkXYInImage(round(pointsF[1].x),TreeInputImage.cols),checkXYInImage(round(pointsF[1].y),TreeInputImage.rows)));
    pointsI.push_back(Point(checkXYInImage(round(pointsF[2].x),TreeInputImage.cols),checkXYInImage(round(pointsF[2].y),TreeInputImage.rows)));
    pointsI.push_back(Point(checkXYInImage(round(pointsF[3].x),TreeInputImage.cols),checkXYInImage(round(pointsF[3].y),TreeInputImage.rows)));

    return pointsI;
}


/* 
ObjectDetector::ObjectDetector () {//prázdný kontrsuktor
}

ObjectDetector::ObjectDetector (cv::Mat chosen_card_image) {//constructor with card file
    this->CardInputImage = chosen_card_image;
}

ObjectDetector::ObjectDetector (string path_to_card) {//constructor with card file
    CardInputImage = cv::imread(path_to_card);
    if (!CardInputImage.data) {
        std::cerr << "Error: Unable to read card image file" << std::endl;
    }
}*/

ObjectDetector::ObjectDetector (cv::Mat card) {// konstruktor with card image

    // convert to grey
    cvtColor(card, card, cv::COLOR_BGR2GRAY);

    // resize card
    int resizeCardToWidth = 500;
    float ratioCard = float(resizeCardToWidth) / float(card.cols);
    int newCardHeight = int(round(ratioCard * card.rows));
    cv::resize(card, card, cv::Size(resizeCardToWidth, newCardHeight), cv::INTER_LINEAR);

    //SIFT
    // initialize SIFT detector 
    cv::Ptr<cv::SiftFeatureDetector> detectorS = cv::SiftFeatureDetector::create();
    detectorS->detectAndCompute(card, noArray(), this->card_keypoints, this->card_descriptors);

}

int ObjectDetector::detectCard(){
    
    CardDetection cardDet = CardDetection(this->TreeInputImage, this->card_descriptors, this->card_keypoints, this->roi);
    card_polygon = cardDet.getPoints();
    float confidence = cardDet.getConfidenceScore();

    // if card is empty or confidence is too low then card wasnt found
    if (card_polygon.empty() || confidence<0.8) {
        std::clog << "Card was not found." << std::endl;
        return 1;
    }

    // show detected card
    /*Mat res = cardDet.getMarkedImage();
    imshow("res", res);
    waitKey(0);
    destroyAllWindows();*/

    return 0;
}


int ObjectDetector::detectTree(){

    TreeDetection tree = TreeDetection(TreeInputImage, card_polygon);
    int ret = tree.findTree(1);
    if (ret < 0) {
        std::clog << "Tree was not found." << std::endl;
        return 2;
        /*std::clog << "Tree was not found. Another try" << std::endl;

        ret = tree.findTree(2);
        if (ret < 0) {
            std::clog << "Tree was not found." << std::endl;
            return 2;
        }*/
    }
    tree_lines = tree.getTreeLines();

    return 0;
}


int ObjectDetector::computeDiameter(){
    if (this->card_polygon.empty() || this->tree_lines.empty()){
        std::cerr << "Error: Empty card or tree points" << std::endl;
        return (-1);
    }
    //measure
    float tree_width_in_pixels = getTreeWidth(this->tree_lines, this->card_polygon);
    float card_width_in_pixels = distBetweenPoints(this->card_polygon[0], this->card_polygon[1]);
    float tree_width_in_cm = float((tree_width_in_pixels / card_width_in_pixels * 85.6));

    // use tree width in cm and card width in pixels to perform perspective adjustment
    this->diameter_value = float(-tree_width_in_cm/(sqrt(pow(tree_width_in_cm,2)/(4*pow(219185/card_width_in_pixels,2)) + 1)*(tree_width_in_cm/(2*(219185/card_width_in_pixels)*sqrt(pow(tree_width_in_cm,2)/(4*pow(219185/card_width_in_pixels,2)) + 1)) - 1)));

    return 0;
}


// ./treeProject path/to/image

int main(int argc, char const* argv[]){

    std::string path_to_tree;

    // parse args
    if (argc == 2){
        path_to_tree = argv[1];
    }else{
        std::cerr << "Set path to image (./treeProject path/to/image)" << std::endl;
        return -1;
    }

    // Load Image of tree 
    //std::clog << "Filename: " + path_to_tree << std::endl;
    cv::Mat input_image = cv::imread(path_to_tree);
    if (!input_image.data) {
        std::cerr << "Error: Unable to read tree image file" << std::endl;
        return -1;
    }

    // Load Image of card
    //std::clog << "Filename: " + path_to_tree << std::endl;
    cv::Mat card_image = cv::imread("../images/karta39.png");
    if (!card_image.data) {
        std::cerr << "Error: Unable to read card image file" << std::endl;
        return -1;
    }

    // roi - region of interest where to look for a card
    // if card is not in ROI it wont be found (ROI is cv::Rect(x,y,w,h), where origin is top left corner), smaller roi->more precise and quick search
    // for now is roi set as whole image
    cv::Rect roi = Rect(0,0,0,0); // if roi is set as Rect(0,0,0,0), whole image will be searched for card


    //set Mat with card that we are looking for
    ObjectDetector detector(card_image);

    std::vector<cv::Point> card_polygon;
    std::vector<cv::Point> tree_polygon;
    double diameter_value = 0.0;
    cout << "ObjectDetector::measureTree() returned code: " << detector.measureTree(input_image, roi, card_polygon, tree_polygon, diameter_value) << endl;
    cout << "Diameter: " << diameter_value << endl;

    return 0;
}
