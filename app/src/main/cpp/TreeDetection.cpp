#include "TreeDetection.h"

/**
 * Constructor. Resize original image to defined width. Resize and order card points.
 * @param source_img original input image with tree and card
 * @param card_pts vector of card points, corresponds to the original image
 */
TreeDetection::TreeDetection(cv::Mat source_img, std::vector<cv::Point2f> card_pts) {
    // resize image
    ratio = float(resize_to_width / source_img.cols);
    int new_height = round(ratio * source_img.rows);
    cv::resize(source_img, image, cv::Size(resize_to_width, new_height), cv::INTER_LINEAR);

    cv::GaussianBlur(image, image, cv::Size(3, 3), 0);

    // resize card points to image
    std::vector<cv::Point2f> pts;
    for (int i = 0; i < int(card_pts.size()); i++) {
        pts.push_back(cv::Point2f(card_pts[i].x * ratio, card_pts[i].y * ratio));
    }
    // order card points
    this->card_points = orderCardPoints(pts);

}


/**
 * Run the tree detection in chosen region
 * @param position set 1 to search above card, 2 under card
 * @return 0 if detection is successful, -1 otherwise
 */
int TreeDetection::findTree(int position) {
    // crop image above or under card
    float roi_top, roi_bottom; 
    //above card
    if(position == 1){
        //find top point of the card = bottom line of region
        if (card_points["tl"].y > card_points["tr"].y){
            roi_bottom = card_points["tl"].y;
        }else{
            roi_bottom = card_points["tr"].y;
        }
        //height of the region
        roi_top = roi_bottom - roi_height;
        if (roi_top < 0) roi_top = 0;
        roi = cv::Rect2f(cv::Point2f(0,roi_top), cv::Point2f(resize_to_width, roi_bottom));

    //under card
    }else{
        //find bottom point of the card = top line of region
        if (card_points["bl"].y < card_points["br"].y){
            roi_top = card_points["bl"].y;
        }else{
            roi_top = card_points["br"].y;
        }
        //height of the region
        roi_bottom = roi_top + roi_height;
        if (roi_bottom > image.rows-1) roi_bottom = image.rows-1;
        roi = cv::Rect2f(cv::Point2f(0,roi_top), cv::Point2f(resize_to_width, roi_bottom));
    }

    // set roi and init tree mask
    image_roi = image(roi);
    tree_mask_roi = cv::Mat::zeros(image_roi.rows, image_roi.cols, CV_8U);

    // card center
    cv::Point2f center = cv::Point2f(
        (this->card_points["tl"].x + this->card_points["br"].x) / 2,
        (this->card_points["tl"].y + this->card_points["br"].y) / 2);
    // tree segmentation with GrabCut algorithm
    doGrabcut(center);

    // fit lines to segmentation
    int ret = findLines();

    return ret;
}


/**
 * Create input mask for GrabCut algorithm (green background, foreground behind card).
 * Run GrabCut, save output binary tree mask.
 * @param card_center center of the detected card
 */
void TreeDetection::doGrabcut(cv::Point2f card_center) {

    cv::Mat mask(image_roi.rows, image_roi.cols, CV_8U, cv::Scalar::all(cv::GC_PR_BGD));
    cv::Mat bgd_model = cv::Mat();
    cv::Mat fgd_model = cv::Mat();

    ////////////////////////////////////////////////////////
    // *Prepare GrabCut input mask*
    // draw a rectangle GC_PR_FGD with the center of the card (as a probable area of ​​tree)
    // the rectangle width is the shorter side of the card
    // we dont know how the card is rotated - find shorter side
    //int half_rect_width = round(0.11 * image_roi.cols);
    double dist1 = pointsDistance(card_points["tl"], card_points["tr"]);
    double dist2 = pointsDistance(card_points["tl"], card_points["bl"]);
    int half_rect_width;
    if (dist1 < dist2){
        half_rect_width = dist1/2 -1;
    }else{
        half_rect_width = dist2/2 -1;
    }
    // Draw the rectangle (size = roi.height x card shorter side)
    //check image boundaries
    int x1 = card_center.x - half_rect_width;
    int x2 = card_center.x + half_rect_width;
    if (x1 < 0)  x1 = 0;
    if (x2 >= image_roi.cols-1)  x2 = image_roi.cols-1;
    cv::rectangle(mask, cv::Point(x1, 0), cv::Point(x2, roi.height-1), cv::GC_PR_FGD, cv::FILLED);

    // Set green color as background GC_BGD in input mask
    cv::Mat hsv;
    cv::Mat green, white;
    cv::Mat kernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3));
    cv::cvtColor(image_roi, hsv, cv::COLOR_BGR2HSV);
    //77°, 31%, 46%
    cv::inRange(hsv, cv::Scalar(40, 120, 100), cv::Scalar(95, 255, 255), green);
    //cv::inRange(hsv, cv::Scalar(0, 0, 250), cv::Scalar(179, 5, 255), white);
    cv::morphologyEx(green, green, cv::MORPH_OPEN, kernel);
    mask.setTo(cv::Scalar::all(cv::GC_BGD), green);
    //mask.setTo(cv::Scalar::all(cv::GC_BGD), white);
    if (TREE_SHOW_IMAGES) cv::imshow("Grabcut input mask", mask*20);

    ////////////////////////////////////////////////////////
    // *Run GrabCut*
    grabCut(image_roi, mask, cv::Rect(0, 0, image_roi.cols-1, image_roi.rows-1), bgd_model, fgd_model, 5, cv::GC_INIT_WITH_MASK );

    // create a binary mask from the segmentation
    tree_mask_roi = (mask == cv::GC_FGD) | (mask == cv::GC_PR_FGD);
    kernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 7));
    cv::morphologyEx(tree_mask_roi, tree_mask_roi, cv::MORPH_OPEN, kernel, cv::Point(-1, -1), 1);
    cv::morphologyEx(tree_mask_roi, tree_mask_roi, cv::MORPH_CLOSE, kernel, cv::Point(-1, -1), 1);


    if (TREE_SHOW_IMAGES) {
        // mask foreground pixels of the original image
        cv::Mat masked_foreground_image = cv::Mat();
        cv::bitwise_and(image_roi, image_roi, masked_foreground_image, tree_mask_roi);

        cv::imshow("Grabcut output mask", mask*20);
        cv::imshow("masked_foreground_image", masked_foreground_image);
        cv::imshow("tree_mask", tree_mask_roi);
        cv::imshow("green", green);
        cv::waitKey(0);
    }

}


/**
 * Check if two lines intersect in the ROI area (where the tree is searched).
 * @param p1 first point of first line
 * @param p2 second point of first line
 * @param p3 first point of second line
 * @param p4 second point of second line
 * @return 1 if intersect, 0 otherwise
 */
int TreeDetection::linesIntersect(cv::Point2f p1, cv::Point2f p2, cv::Point2f p3, cv::Point2f p4){

    // compute intersection
    cv::Point2f r = intersection( std::tuple<cv::Point2f, cv::Point2f>(p1, p2), std::tuple<cv::Point2f, cv::Point2f>(p3, p4));

    // check if intersection is in the image roi
    if ((r.x > roi.x) && (r.x < roi.width) && (r.y > roi.y) && (r.y < roi.y+roi.height)){
        return 1;
    }

    return 0;
}



/**
 * Compute point position to line (left/right)
 * @param p1 first point of line
 * @param p2 second point of line
 * @param p point
 * @return <0 right, >0 left
 */
double TreeDetection::pointPositionToLine(cv::Point2f p1, cv::Point2f p2, cv::Point2f p){
    // subtracting co-ordinates of point 1 from 2 and P, to make p1 as origin
    p2 = p2-p1;
    p = p-p1;
 
    return p2.cross(p);
}


/**
 * Compute the position of the center of the card relative to the lines
 * @param p1 first point of first line
 * @param p2 second point of first line
 * @param p3 first point of second line
 * @param p4 second point of second line
 * @return card position: 0-left, 1-between the lines, 2-right
 */
int TreeDetection::cardPositionToLines(cv::Point2f p1, cv::Point2f p2, cv::Point2f p3, cv::Point2f p4){
    
    cv::Point2f card_center = intersection( std::tuple<cv::Point2f, cv::Point2f>(card_points["tl"], card_points["br"]), std::tuple<cv::Point2f, cv::Point2f>(card_points["tr"], card_points["bl"]));
    // left line first
    if (p1.x > p3.x){
        std::swap(p1, p3);
        std::swap(p2, p4);
    }   

    int left_l = pointPositionToLine(p1, p2, card_center);
    int right_l = pointPositionToLine(p3, p4, card_center);
    //std::cout << "left: " << left_l << "  right: " << right_l << std::endl;

    //middle
    if (left_l < 0 && right_l > 0){
        return 1;
    }
    //right
    if (left_l < 0 && right_l < 0){
        return 2;
    }
    //left
    return 0;
}


/**
 * Compute the distances from lines to the center of the card
 * @return sum of distances
 */
double TreeDetection::linesToCardDistance(tree tree){
    
    cv::Point2f card_center = intersection( std::tuple<cv::Point2f, cv::Point2f>(card_points["tl"], card_points["br"]), std::tuple<cv::Point2f, cv::Point2f>(card_points["tr"], card_points["bl"]));

    double dist1 = pointDistanceToLine(tree.left_line.first, tree.left_line.second, card_center);
    double dist2 = pointDistanceToLine(tree.right_line.first, tree.right_line.second, card_center);
    //std::cout << dist1 << "  " << dist2 << "   " << abs(dist1-dist2)  << std::endl;

    return abs(dist1)+abs(dist2);
}



/**
 * Find lines that respresent tree edges.
 * Uses Canny edge detecor and Hough line transformation on tree mask to find lines.
 * @return 0 if lines were found, -1 otherwise
 */
int TreeDetection::findLines(){
    cv::Mat canny_out = cv::Mat::zeros(this->image_roi.size(), CV_8UC1);

    ////////////////////////////////////////////////////////
    // *Find lines*
    // use canny edge detector on tree mask
    cv::Canny(tree_mask_roi, canny_out, 50, 200, 3);

    // use hough transform on Canny edges to find lines
    std::vector<cv::Vec2f> hough_lines;
    cv::HoughLines(canny_out, hough_lines, 2, CV_PI/140, 50, 0, 0);
    if (hough_lines.size() <= 1) {
        std::cerr << TAG << ": Couldn't detect tree lines with Hough" << std::endl;
        return -1;
    }
    
    ////////////////////////////////////////////////////////
    // *Check lines and find only those that dont intersect in ROI*
    // make copy of all vertical lines
    std::vector<cv::Vec4f> lines_that_dont_intersect;
    for (int i=0; i<hough_lines.size(); i++){
        //only vertical lines
        if (hough_lines[i][1] < 0.5  ||  hough_lines[i][1] > 2.6 ){
            //trensform to cartesian coordinates
            cv::Point2f pt1, pt2;
            linePoints(hough_lines[i], pt1, pt2);
            lines_that_dont_intersect.push_back(cv::Vec4f(pt1.x, pt1.y, pt2.x, pt2.y));
        }
    }

    // find lines that intersect with selected line to delete
    std::vector<int> lines_to_delete;
    cv::Point2f pt1, pt2, pt3, pt4;
    int intersect = 0;
    for (size_t i = 0; i < lines_that_dont_intersect.size(); i++) {
        pt1 = cv::Point2f(lines_that_dont_intersect[i][0], lines_that_dont_intersect[i][1]);
        pt2 = cv::Point2f(lines_that_dont_intersect[i][2], lines_that_dont_intersect[i][3]);

        for (size_t j = i+1; j < lines_that_dont_intersect.size(); j++) {
            pt3 = cv::Point2f(lines_that_dont_intersect[j][0], lines_that_dont_intersect[j][1]);
            pt4 = cv::Point2f(lines_that_dont_intersect[j][2], lines_that_dont_intersect[j][3]);

            intersect = linesIntersect(pt1, pt2, pt3, pt4);
            //std::cout << "intersect: " << intersect << std::endl;
            if(intersect){
                lines_to_delete.push_back(j);
            }
        }
    }

    // delete lines that intersect from original vector
    sort( lines_to_delete.rbegin(), lines_to_delete.rend() );
    lines_to_delete.erase( unique( lines_to_delete.begin(), lines_to_delete.end() ), lines_to_delete.end() );
    for (size_t i = 0; i < lines_to_delete.size(); i++) {
        lines_that_dont_intersect.erase(lines_that_dont_intersect.begin() + lines_to_delete.at(i));
    }


    if (TREE_SHOW_IMAGES){
        cv::Mat im_dont_intersect = image.clone();
        for (size_t i = 0; i < lines_that_dont_intersect.size(); i++) {
            cv::Point2f pt1 = cv::Point2f(lines_that_dont_intersect[i][0], lines_that_dont_intersect[i][1]);
            cv::Point2f pt2 = cv::Point2f(lines_that_dont_intersect[i][2], lines_that_dont_intersect[i][3]);
            cv::line(im_dont_intersect, pt1, pt2, cv::Scalar(0, 0, 255), 1, cv::LINE_AA);
        }
        cv::imshow("lines_that_dont_intersect", im_dont_intersect);
        cv::waitKey(0);
    }



    ////////////////////////////////////////////////////////
    // *Find lines that have card center between them*
    std::vector<tree> tree_candidates;
    for (size_t i = 0; i < lines_that_dont_intersect.size(); i++) {
        pt1 = cv::Point2f(lines_that_dont_intersect[i][0], lines_that_dont_intersect[i][1]);
        pt2 = cv::Point2f(lines_that_dont_intersect[i][2], lines_that_dont_intersect[i][3]);

        for (size_t j = i+1; j < lines_that_dont_intersect.size(); j++) {
            pt3 = cv::Point2f(lines_that_dont_intersect[j][0], lines_that_dont_intersect[j][1]);
            pt4 = cv::Point2f(lines_that_dont_intersect[j][2], lines_that_dont_intersect[j][3]);

            int card_position = cardPositionToLines(pt1, pt2, pt3, pt4);

            //if card is in the middle, create tree candidate from lines
            if (card_position == 1){
                tree t;
                t.left_line = std::make_pair(pt1, pt2);
                t.right_line = std::make_pair(pt3, pt4);

                if (t.left_line.first.x > t.right_line.first.x)
                    std::swap(t.left_line, t.right_line);

                tree_candidates.push_back(t);
            }
        }
    }

    if (tree_candidates.size() == 0){
        std::cerr << TAG << ": Couldn't detect suitable tree lines" << std::endl;
        return -1;
    }



    ////////////////////////////////////////////////////////
    // *Find best tree according to distance between lines and card center*
    // select tree which lines are closest to the card centre
    double min_distance = image.cols;
    tree the_tree = tree_candidates.at(0);
    for (size_t i = 0; i < tree_candidates.size(); i++) {
        tree tree = tree_candidates.at(i);
        double distance = linesToCardDistance(tree);
        //std::cout << distance << std::endl;
        if (distance < min_distance){
            min_distance = distance;
            the_tree = tree;
        }
    }


    ////////////////////////////////////////////////////////
    // *Check if selected lines are good (angle, edge strength, color around line)*
    // un/comment one of the following lines:
    //int ret = 0;
    int ret = checkEdges(the_tree);



    ////////////////////////////////////////////////////////
    // * Recalculate the points of the tree so that it is across the entire ROI*
    pt1 = the_tree.left_line.first;
    pt2 = the_tree.left_line.second;
    pt3 = the_tree.right_line.first;
    pt4 = the_tree.right_line.second;
    //top and bottom ROI lines
    std::tuple<cv::Point2f, cv::Point2f> top_roi( cv::Point2f(roi.x, roi.y), cv::Point2f(roi.width, roi.y));
    std::tuple<cv::Point2f, cv::Point2f> bottom_roi( cv::Point2f(roi.x, roi.y+roi.height), cv::Point2f(roi.width, roi.y+roi.height));
    //compute intersections of tree lines with ROI lines
    cv::Point2f top_intersect_left = intersection(top_roi, std::tuple<cv::Point2f, cv::Point2f>(pt1, pt2));
    cv::Point2f bot_intersect_left = intersection(bottom_roi, std::tuple<cv::Point2f, cv::Point2f>(pt1, pt2));
    pt1 = top_intersect_left;
    pt2 = bot_intersect_left;
    left_tree_line = std::make_tuple(pt1, pt2);

    cv::Point2f top_intersect_right = intersection(top_roi, std::tuple<cv::Point2f, cv::Point2f>(pt3, pt4));
    cv::Point2f bot_intersect_right = intersection(bottom_roi, std::tuple<cv::Point2f, cv::Point2f>(pt3, pt4));
    pt3 = top_intersect_right;
    pt4 = bot_intersect_right;
    right_tree_line = std::make_tuple(pt3, pt4);


    if (TREE_SHOW_IMAGES) {
        cv::imshow("canny_out", canny_out);

        cv::Mat im_hough_lines = image.clone();
        for (size_t i = 0; i < hough_lines.size(); i++) {
            cv::Point2f pt1, pt2;
            linePoints(hough_lines[i], pt1, pt2);
            cv::line(im_hough_lines, pt1, pt2, cv::Scalar(0, 0, 255), 1, cv::LINE_AA);
        }
        cv::imshow("hough_lines", im_hough_lines);

        cv::Mat line_output = image.clone();
        cv::line(line_output, std::get<0>(left_tree_line), std::get<1>(left_tree_line), cv::Scalar(0, 0, 255), 1, cv::LINE_AA);
        cv::line(line_output, std::get<0>(right_tree_line), std::get<1>(right_tree_line), cv::Scalar(0, 0, 255), 1, cv::LINE_AA);
        cv::imshow("line_output", line_output);

        cv::waitKey(0);
    }


    return ret;

}




/**
 * Compute angle between tree lines
 * @param tree tree lines
 * @return angle between tree lines
 */
double TreeDetection::linesAngle(tree tree){
    //Geometric interpretation of the angle between two vectors defined using an inner product
    cv::Point2f line1 = tree.left_line.first - tree.left_line.second;
    cv::Point2f line2 = tree.right_line.first - tree.right_line.second;

    double m1 = sqrt( line1.x*line1.x + line1.y*line1.y );
    double m2 = sqrt( line2.x*line2.x + line2.y*line2.y );
    double tmp = (line1.x*line2.x + line1.y*line2.y) / (m1 * m2);
    if (std::fabs(tmp - 1.0) < 0.000001){
        return 0;
    }
    return acos(tmp) *180/CV_PI; 
}


/**
 * Compute color distance
 * @param c1 first color
 * @param c2 second color
 * @return color distance
 */
double TreeDetection::colorDistance(cv::Scalar c1, cv::Scalar c2){
    //https://www.compuphase.com/cmetric.htm
    //https://en.wikipedia.org/wiki/Color_difference
    long rmean = ( (long)c1[2] + (long)c2[2] ) / 2;
    long r = (long)c1[2] - (long)c2[2];
    long g = (long)c1[1] - (long)c2[1];
    long b = (long)c1[0] - (long)c2[0];
    return sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
}


/**
 * Compute and compare the mean color in area on the left and right side of the lines. 
 * In good image they should be very different (grey/brown tree and green background).
 * Selects small area to the left of the line and compares its color with the area 
 * to the right of the line.
 * @param tree tree lines
 * @return differences in color values
 */
std::vector<float> TreeDetection::inVsOutTreeColorDifference(tree tree){
    // *Prepare masks from tree lines to compute mean colors*
    // We want to compare the mean color on the left and right side of the line
    cv::Mat tree_line_mask = cv::Mat::zeros(this->image_roi.size(), CV_8UC1);
    cv::Point2f shift_p(0, roi.y);
    std::vector<cv::Point> pts;
    pts.push_back(cv::Point(tree.left_line.first - shift_p));
    pts.push_back(cv::Point(tree.left_line.second - shift_p));
    pts.push_back(cv::Point(tree.right_line.second - shift_p));
    pts.push_back(cv::Point(tree.right_line.first - shift_p));
    cv::fillPoly(tree_line_mask, std::vector<cv::Point>{pts}, cv::Scalar(255));

    //remove card area
    float bottom = card_points["br"].y, top=card_points["tr"].y;
    if (card_points["tl"].y < card_points["tr"].y){
        top = card_points["tl"].y;
    }
    if (card_points["bl"].y > card_points["br"].y){
        bottom = card_points["bl"].y;
    }
    if (TREE_SHOW_IMAGES) cv::imshow("tree_line_mask", tree_line_mask);


    cv::Mat tree_line_mask_dilated, tree_line_mask_dilated2, tree_line_mask_outers;
    cv::Mat tree_line_mask_eroded, tree_line_mask_eroded2, tree_line_mask_inners;
    cv::Mat left_outer_mask, left_inner_mask, right_outer_mask, right_inner_mask;

    //dilate mask for outer area
    cv::Mat kernel20 = cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(20, 20));
    cv::Mat kernel5 = cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(5, 5));
    cv::morphologyEx(tree_line_mask, tree_line_mask_dilated, cv::MORPH_DILATE, kernel20);
    cv::morphologyEx(tree_line_mask, tree_line_mask_dilated2, cv::MORPH_DILATE, kernel5);
    //outer area
    cv::bitwise_xor(tree_line_mask_dilated2, tree_line_mask_dilated, tree_line_mask_outers);
    cv::rectangle(tree_line_mask_outers, cv::Point2f(0,top - roi.y), cv::Point2f( image_roi.cols-1, bottom  - roi.y), cv::Scalar(0, 0, 0), -1); //card remove
    if (TREE_SHOW_IMAGES) cv::imshow("tree_line_mask_outers", tree_line_mask_outers);


    // split left and right part
    cv::Mat half = cv::Mat::zeros(this->image_roi.size(), CV_8UC1);
    std::tuple<cv::Point2f, cv::Point2f> top_roi_line( cv::Point2f(0,roi.y), cv::Point2f(image.cols-1,roi.y));
    cv::Point2f left_top_intersect = intersection(top_roi_line, tree.left_line);
    cv::Point2f right_top_intersect = intersection(top_roi_line, tree.right_line);
    cv::rectangle(half, cv::Point2f(0,0), cv::Point2f((left_top_intersect.x + right_top_intersect.x)/2., image_roi.rows-1), cv::Scalar(255, 255, 255), -1);

    // outer masks
    cv::bitwise_and(tree_line_mask_outers, half, left_outer_mask);
    cv::bitwise_and(tree_line_mask_outers, (255-half), right_outer_mask);


    //erode mask for inner area
    cv::morphologyEx(tree_line_mask, tree_line_mask_eroded, cv::MORPH_ERODE, kernel20);
    cv::morphologyEx(tree_line_mask, tree_line_mask_eroded2, cv::MORPH_ERODE, kernel5);
    cv::bitwise_xor(tree_line_mask_eroded2, tree_line_mask_eroded, tree_line_mask_inners);
    cv::rectangle(tree_line_mask_inners, cv::Point2f(0,top  - roi.y), cv::Point2f( image_roi.cols-1, bottom  - roi.y), cv::Scalar(0, 0, 0), -1); //card area
    if (TREE_SHOW_IMAGES) cv::imshow("tree_line_mask_inners", tree_line_mask_inners);

    //inner masks
    cv::bitwise_and(tree_line_mask_inners, half, left_inner_mask);
    cv::bitwise_and(tree_line_mask_inners, (255-half), right_inner_mask);

    //masked image for imshow
    if (TREE_SHOW_IMAGES){
        cv::Mat left_in, left_out, right_out, right_in;
        cv::bitwise_and(image_roi, image_roi, left_out, left_outer_mask);
        cv::bitwise_and(image_roi, image_roi, left_in, left_inner_mask);
        cv::bitwise_and(image_roi, image_roi, right_out, right_outer_mask);
        cv::bitwise_and(image_roi, image_roi, right_in, right_inner_mask);
        cv::imshow("left_out", left_out);
        cv::imshow("left_in", left_in);
        cv::imshow("right_out", right_out);
        cv::imshow("right_in", right_in);
    }


    //////////////////////////////////////////////
    // compute mean values in rgb in inner/outer area of each line
    cv::Scalar mean_left_outer, mean_left_inner,  mean_right_outer, mean_right_inner;
    cv::Scalar stdDev_left_outer, stdDev_left_inner,  stdDev_right_outer, stdDev_right_inner;
    cv::meanStdDev(image_roi, mean_left_outer, stdDev_left_outer, left_outer_mask);
    cv::meanStdDev(image_roi, mean_left_inner, stdDev_left_inner, left_inner_mask);
    cv::meanStdDev(image_roi, mean_right_outer, stdDev_right_outer, right_outer_mask);
    cv::meanStdDev(image_roi, mean_right_inner, stdDev_right_inner, right_inner_mask);

    //color difference of inner and outer area
    double diff_l = colorDistance(mean_left_outer, mean_left_inner);
    double diff_r = colorDistance(mean_right_outer, mean_right_inner);
    //max diff 765?

    //https://en.wikipedia.org/wiki/Coefficient_of_variation
    float std_dev_l_out = (stdDev_left_outer[0]/mean_left_outer[0] + stdDev_left_outer[1]/mean_left_outer[1] + stdDev_left_outer[2]/mean_left_outer[2]) / 3;
    float std_dev_l_in = (stdDev_left_inner[0]/mean_left_inner[0] + stdDev_left_inner[1]/mean_left_inner[1] + stdDev_left_inner[2]/mean_left_inner[2]) / 3;
    float std_dev_r_out = (stdDev_right_outer[0]/mean_right_outer[0] + stdDev_right_outer[1]/mean_right_outer[1] + stdDev_right_outer[2]/mean_right_outer[2]) / 3;
    float std_dev_r_in = (stdDev_right_inner[0]/mean_right_inner[0] + stdDev_right_inner[1]/mean_right_inner[1] + stdDev_right_inner[2]/mean_right_inner[2]) / 3;
    //std::cerr <<  "std_dev_l   "  << std_dev_l_out-std_dev_l_in   <<  std::endl;
    //std::cerr <<  "std_dev_r   "  << std_dev_r_out-std_dev_r_in   <<  std::endl;

    std::vector<float> return_values;
    return_values.push_back(diff_l/765); //color difference left line
    return_values.push_back(diff_r/765); //color difference right line
    return_values.push_back(std_dev_l_out-std_dev_l_in);    //variation difference left line
    return_values.push_back(std_dev_r_out-std_dev_r_in);    //variation difference right line
    return return_values;

}


/**
 * Check the area around the lines to see if they are well detected. 
 * There should be a large color difference between the areas to the left and right of the line. 
 * Compares average colors and variations.
 * Also the angle between lines should be small.
 * @param tree tree lines
 * @return 0 - ok, <0 some of the conditions are not met
 */
int TreeDetection::checkEdges(tree tree){
    //compute angle between lines
    double angle = linesAngle(tree)/90;
    
    //color differencces and variations
    std::vector<float> edge_colors = inVsOutTreeColorDifference(tree);
    if (TREE_SHOW_IMAGES){
        std::cerr << "angle   " <<  angle << std::endl;
        std::cerr << "diff_l  " <<  edge_colors.at(0) << std::endl;
        std::cerr << "diff_r  " <<  edge_colors.at(1) << std::endl;
        std::cerr << "stddev_l  " <<  edge_colors.at(2) << std::endl;
        std::cerr << "stddev_r  " <<  edge_colors.at(3) << std::endl;
    }

    //std::vector<float> edge_strength = outer_sobel_edge(tree);

    detection_score = (-angle) + 
        edge_colors.at(0) + edge_colors.at(1) +
        edge_colors.at(2) + edge_colors.at(3);

    //lines arent verticall
    if (angle > 0.15) {
        return -2;
    }  
    
    //small color difference and variance (left or right line)
    if (edge_colors.at(0)<0.08 && edge_colors.at(2)<0.08  ){
        return -3;
    }
    if (edge_colors.at(1)<0.08 && edge_colors.at(3)<0.08  ){
        return -3;
    }

    return 0;
}



/**
 * Draw points and lines into output image (resized)
 * @return output image
 */
cv::Mat TreeDetection::getOutputImage() {
    cv::Mat output_image = image.clone();
    // draw card points
    cv::circle(output_image, card_points["tl"], 2, cv::Scalar(100, 0, 255), -1);
    cv::circle(output_image, card_points["tr"], 2, cv::Scalar(100, 0, 255), -1);
    cv::circle(output_image, card_points["bl"], 2, cv::Scalar(0, 255, 0), -1);
    cv::circle(output_image, card_points["br"], 2, cv::Scalar(0, 255, 0), -1);
    // draw tree lines
    cv::line(output_image, std::get<0>(this->left_tree_line), std::get<1>(this->left_tree_line), cv::Scalar(0, 0, 255), 1, cv::LINE_AA);
    cv::line(output_image, std::get<0>(this->right_tree_line), std::get<1>(this->right_tree_line), cv::Scalar(0, 0, 255), 1, cv::LINE_AA);

    return output_image;
}


/**
 * Creates vector of 4 points representing two tree lines, that correspond to the original image size.
 * Left line first. Top points first.
 * @return tree lines (4 points)
 */
std::vector<cv::Point2f> TreeDetection::getTreeLines() {
    std::vector<cv::Point2f> out;
    out.push_back(std::get<0>(left_tree_line) / ratio);
    out.push_back(std::get<1>(left_tree_line) / ratio);
    out.push_back(std::get<0>(right_tree_line) / ratio);
    out.push_back(std::get<1>(right_tree_line) / ratio);

    return out;
};


/**
 * Compute line points in image (Cartesian coordinate system) from Hough transformation. Mostly for drawing.
 * @param line line in polar coordinate system (Hough transform output)
 * @param pt1 first point of the line (output)
 * @param pt2 secont point of the line (output)
 */
void TreeDetection::linePoints(cv::Vec2f line, cv::Point2f& pt1, cv::Point2f& pt2) {

    float rho = line[0], theta = line[1];
    double a = cos(theta), b = sin(theta);
    double x0 = a * rho, y0 = b * rho;
    pt1.x = cvRound(x0 + 1000 * (-b));
    pt1.y = cvRound(y0 + 1000 * (a));
    pt2.x = cvRound(x0 - 1000 * (-b));
    pt2.y = cvRound(y0 - 1000 * (a));

    if (pt1.y > pt2.y){
        std::swap(pt1,pt2);
    }
    
    // move points to original uncropped image position
    cv::Point2f shift_p(0, roi.y);
    pt1 += shift_p;
    pt2 += shift_p;

    std::tuple<cv::Point2f, cv::Point2f> tree_line(pt1, pt2);

    std::tuple<cv::Point2f, cv::Point2f> top_image_line( cv::Point2f(0,0), cv::Point2f(image.cols-1,0));
    std::tuple<cv::Point2f, cv::Point2f> bottom_image_line( cv::Point2f(0,image.rows-1), cv::Point2f(image.cols-1,image.rows-1));

    cv::Point2f top_intersect = intersection(top_image_line, tree_line);
    cv::Point2f bot_intersect = intersection(bottom_image_line, tree_line);

    pt1 = top_intersect;
    pt2 = bot_intersect;

}

cv::Point2f TreeDetection::intersection(std::tuple<cv::Point2f, cv::Point2f> image_line, std::tuple<cv::Point2f, cv::Point2f> line){
    // Line AB represented as a1x + b1y = c1
    double a1 = std::get<1>(image_line).y - std::get<0>(image_line).y;
    double b1 = std::get<0>(image_line).x - std::get<1>(image_line).x;
    double c1 = a1*(std::get<0>(image_line).x) + b1*(std::get<0>(image_line).y);
  
    // Line CD represented as a2x + b2y = c2
    double a2 = std::get<1>(line).y - std::get<0>(line).y;
    double b2 = std::get<0>(line).x - std::get<1>(line).x;
    double c2 = a2*(std::get<0>(line).x)+ b2*(std::get<0>(line).y);
  
    double determinant = a1*b2 - a2*b1;

    double x = (b2*c1 - b1*c2)/determinant;
    double y = (a1*c2 - a2*c1)/determinant;

    return cv::Point2f(x, y);
}


/*************************************************************/



/**
 * Order card points.
 * @param c_points unordered card points
 * @return dictionary with ordered points, top-left = 'tl', bottom-right = 'br', tr, bl
 */
std::map<std::string, cv::Point2f> TreeDetection::orderCardPoints(std::vector<cv::Point2f> c_points) {

    std::map<std::string, cv::Point2f> card_pts;
    cv::Point2f top1(image.cols, image.rows);
    cv::Point2f bottom1(0, 0);
    for (cv::Point2f p : c_points) {
        if(p.y < top1.y){
            top1 = p;
        }
        if(p.y > bottom1.y){
            bottom1 = p;
        }
    }
    cv::Point2f top2(image.cols, image.rows);
    cv::Point2f bottom2(0, 0);
    for (cv::Point2f p : c_points) {
        if(p.y < top2.y && p!=top1){
            top2 = p;
        }
        if(p.y > bottom2.y && p!=bottom1){
            bottom2 = p;
        }
    }
    if(top1.x < top2.x){
        card_pts["tl"] = top1;
        card_pts["tr"] = top2;
    }else{
        card_pts["tl"] = top2;
        card_pts["tr"] = top1;
    }

    if(bottom1.x < bottom2.x){
        card_pts["bl"] = bottom1;
        card_pts["br"] = bottom2;
    }else{
        card_pts["bl"] = bottom2;
        card_pts["br"] = bottom1;
    }
    return card_pts;
}


/**
 * Distance between two points.
 * @param p1 first point
 * @param p2 second point
 * @return distance
 */
double pointsDistance(cv::Point2f p1, cv::Point2f p2) {
    float dx = p1.x - p2.x;
    float dy = p1.y - p2.y;
    return sqrt(dx * dx + dy * dy);
}


/**
 * Calculate the distance between the point and the line.
 * @param line_start first point of the line
 * @param line_end second point of the line
 * @param point point
 * @return computed distance
 */
double pointDistanceToLine(cv::Point2f line_start, cv::Point2f line_end, cv::Point2f point) {

    cv::Point2f line = line_end - line_start;

    float t = (point - line_start).dot(line_end - line_start) /
        (line.x * line.x + line.y * line.y);

    cv::Point2f closest = cv::Point2f(line_start.x + t * line.x, line_start.y + t * line.y);
    line.x = point.x - closest.x;
    line.y = point.y - closest.y;

    return sqrt(line.x * line.x + line.y * line.y);

}


/**
 * Mask card area in input image. You can set margin to enlarge the area.
 * @param points ordered card points
 * @param margin the number of pixels by which the area on each side increases
 * @param input input image to mask
 * @return image with white mask of card
 */
cv::Mat maskCard(std::map<std::string, cv::Point2f> points, cv::Mat input, int margin) {

    std::vector<cv::Point> pts;
    pts.push_back(cv::Point(points["tl"].x - margin, points["tl"].y - margin));
    pts.push_back(cv::Point(points["tr"].x + margin, points["tr"].y - margin));
    pts.push_back(cv::Point(points["br"].x + margin, points["br"].y + margin));
    pts.push_back(cv::Point(points["bl"].x - margin, points["bl"].y + margin));
    //pts.push_back(cv::Point(points["tl"].x - margin, points["tl"].y - margin));

    cv::fillPoly(input, std::vector<cv::Point>{pts}, cv::Scalar(255));

    return input;
}

void printTree (tree tree){
    std::cout << "L: " << tree.left_line.first << " " << tree.left_line.second;
    std::cout << "    R: " << tree.right_line.first << " " << tree.right_line.second << std::endl;
}