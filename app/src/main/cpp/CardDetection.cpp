#include "CardDetection.h"
#include "SaveBinarySIFTmodel.h"
#include "TreeDiameter.h"

bool edge_ref_opticalFlow = false;  //if true card edges will be refined by optical flow method
string CARD_FILE_PATH = "../../images/karta39.png";            // set path to card image if you want to run optical flow method!
bool edge_ref_hough = false;         //if true card edges will be refined by hough method

/**
 * Constructor. Localize card and compute confidence score
 * @param sourceImg original input image with tree and card
 * @param descriptors_card card descriptors loaded from .txt file which contains model of card (500*316px)
 * @param keypoints_card card keypoints loaded from .txt file which contains model of card (500*316px)
 * @param roi region of interest where card will be searched, if card is not in ROI it wont be found (ROI is cv::Rect(x,y,w,h))
 */
CardDetection::CardDetection(cv::Mat sourceImg, cv::Mat descriptors_card, std::vector<cv::KeyPoint> keypoints_card, cv::Rect roi ){
    this->sourceImg = sourceImg.clone();
    this->good_matches = 0;
    this->inliers = 0;
    this->roi = roi;
    this->keypoints_card = keypoints_card;
    this->descriptors_card = descriptors_card;
    this->points = findCard();
    this->card_confidence = confidence();
}


/**
 * Getter. 
 * @return vector of card points (upper left, upper right, bottom right, bottom left)
 */
std::vector<cv::Point2f> CardDetection::getPoints() {
    return this->points;
}

float CardDetection::getConfidenceScore() {
    return this->card_confidence;
}

int CardDetection::getGoodMatches() {
    return this->good_matches;
}

int CardDetection::getInliers() {
    return this->inliers;
}

int CardDetection::getNumCardDescriptors() {
    return this->num_card_descriptors;
}

int CardDetection::getNumImageDescriptors() {
    return this->num_image_descriptors;
}

Mat CardDetection::getinliersDrawn() {
    return this->inliersDrawn;
}


/**
 * Compute card confidence score.
 * How much inner angles of rectangle differs from 90degrees. Each of 4 angles can differ up to 90degrees. Sum all four differences together. 0 is minimum error, 360 is maximum error. Then map it to interval <0,1>.
 * @return confidence score, interval <0,1> where 1 is perfect rectangle
 */
float CardDetection::confidence() {

    if (points.empty()) {     //card was not found
        return 0.0;
    }
    else {
        float angle1 = float(abs(abs(fmod(angleBetween3Points(points[0], points[1], points[2]), 180)) - 90));
        float angle2 = float(abs(abs(fmod(angleBetween3Points(points[1], points[2], points[3]), 180)) - 90));
        float angle3 = float(abs(abs(fmod(angleBetween3Points(points[2], points[3], points[0]), 180)) - 90));
        float angle4 = float(abs(abs(fmod(angleBetween3Points(points[3], points[0], points[1]), 180)) - 90));

        return ((360 - (angle1 + angle2 + angle3 + angle4)) / 360);
    }

}

/**
 * Compute angle between 3 points
 * @return angle between 3 points
 */
float angleBetween3Points(cv::Point2f pointA, cv::Point2f pointB, cv::Point2f pointC) {
    float a = pointB.x - pointA.x;
    float b = pointB.y - pointA.y;
    float c = pointB.x - pointC.x;
    float d = pointB.y - pointC.y;

    float atanA = atan2(a, b);
    float atanB = atan2(c, d);

    return float(((atanB - atanA) * 180.0) / CV_PI);
}



/**
 * Find card in the image with SIFT.
 * SIFT tutorial: https://docs.opencv.org/3.4/d7/dff/tutorial_feature_homography.html
 * @return 4 points of card or empty vector if card was not found
 */
std::vector<cv::Point2f> CardDetection::findCard()
{

    cv::Mat image;

    // convert to grey
    cvtColor(this->sourceImg, image, cv::COLOR_BGR2GRAY);

    // resize image
    int resizeToWidth = 1000;
    float ratio = float(resizeToWidth) / float(image.cols);
    int newHeight = int(round(ratio * image.rows));
    cv::resize(image, image, cv::Size(resizeToWidth, newHeight), cv::INTER_LINEAR);


    Mat original_image = image.clone();
    Rect resized_roi;
    if (this->roi.width!=0 && this->roi.height!=0){ // if roi is defined, crop image else search in whole image
        resized_roi.x = int(round(roi.x * ratio));
        resized_roi.y = int(round(roi.y* ratio));
        resized_roi.width = int(round(roi.width * ratio));
        resized_roi.height = int(round(roi.height * ratio));
        image = original_image(resized_roi);
    
    }
    
    
    // Variables to store keypoints and descriptors
    std::vector<cv::KeyPoint> keypoints_image;
    Mat descriptors_image;

    //SIFT
    // initialize SIFT detector 
    cv::Ptr<cv::SiftFeatureDetector> detectorS = cv::SiftFeatureDetector::create();

    // detect keypoints and compute descriptors
    detectorS->detectAndCompute(image, noArray(), keypoints_image, descriptors_image);


    //show keypoints in tree image 
    /*Mat outimg;
    drawKeypoints(image, keypoints_image, outimg, Scalar::all(-1), DrawMatchesFlags::DEFAULT);
    imshow("SIFT", outimg);
    Mat outimg22;
    drawKeypoints(card, keypoints_card, outimg22, Scalar::all(-1), DrawMatchesFlags::DEFAULT);
    imshow("SIFTcard", outimg22);
    waitKey(0);
    destroyAllWindows();*/
 
    //Flann needs the descriptors to be of type CV_32F. 
    descriptors_image.convertTo(descriptors_image, CV_32F);
    this->descriptors_card.convertTo(this->descriptors_card, CV_32F);

    //check if decriptors are empty
    if ( descriptors_image.empty() ){
        std::cerr << "Error: Image descriptors are empty." << std::endl;
        std::vector<Point2f> empty_corners;
        return empty_corners;   
    }
    if ( this->descriptors_card.empty() ){
        std::cerr << "Error: Card descriptors are empty." << std::endl;
        std::vector<Point2f> empty_corners;
        return empty_corners;
    }

    //Matching descriptor vectors with a FLANN based matcher
    //Since SURF is a floating-point descriptor NORM_L2 must be used
    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create(DescriptorMatcher::FLANNBASED);
    std::vector< std::vector<DMatch> > knn_matches;
    matcher->knnMatch(this->descriptors_card, descriptors_image, knn_matches, 2);
 

    //-- Filter matches using the Lowe's ratio test - pre SIFT 0.5f
    // higher ratio -> more points
    const float ratio_thresh = 0.5f;
    std::vector<DMatch> good_matches;
    for (size_t i = 0; i < knn_matches.size(); i++)
    {
        if (knn_matches[i][0].distance < ratio_thresh * knn_matches[i][1].distance)
        {
            good_matches.push_back(knn_matches[i][0]);
        }
    }

    this->good_matches = good_matches.size();
    this->num_card_descriptors = keypoints_card.size();
    this->num_image_descriptors = keypoints_image.size();

    //at least 5 good matches to find a card
    if (good_matches.size()<5) {
        std::vector<Point2f> empty_corners;
        return empty_corners;
    }
    
    //-- Localize  card
    std::vector<Point2f> obj;
    std::vector<Point2f> scene;
    for (size_t i = 0; i < good_matches.size(); i++)
    {
        //-- Get the keypoints from the good matches
        obj.push_back(keypoints_card[good_matches[i].queryIdx].pt);
        scene.push_back(keypoints_image[good_matches[i].trainIdx].pt);
    }

    cv::Mat mask;
    Mat H = findHomography(obj, scene, RANSAC, 3, mask);


    this->inliers = countNonZero(mask); //number of inliers, used in refinement

    // cannot find homography
    if (H.empty())
    { 
        std::vector<Point2f> empty_corners;
        return empty_corners;
    }

    //-- Get the corners from the image
    std::vector<Point2f> obj_corners(4);
    obj_corners[0] = Point2f(0, 0);
    obj_corners[1] = Point2f(500.0, 0); // (float)card.cols //if card has size other than 500x316, change this
    obj_corners[2] = Point2f(500.0, 316.0); //(float)card.cols, (float)card.rows
    obj_corners[3] = Point2f(0, 316.0);  //(float)card.rows

    // transform objects corners to the scene
    std::vector<Point2f> scene_corners(4);
    perspectiveTransform(obj_corners, scene_corners, H);

    if (this->roi.width!=0 && this->roi.height!=0){ // if roi is defined, adapt points back to original image
        for (int i = 0; i < scene_corners.size(); i++){
        scene_corners[i].x += resized_roi.x;
        scene_corners[i].y += resized_roi.y;
        }
    }

    /////////////////////////////////////////////////////////////
    /////////////// REFINE CARD-OPTICAL FLOW ////////////////////
    /////////////////////////////////////////////////////////////
    if (edge_ref_opticalFlow){
        Mat card_image = cv::imread(CARD_FILE_PATH);
        if (!card_image.data) {
            std::cerr << "Error: Unable to read card image file" << std::endl;
            std::vector<Point2f> empty_corners;
            return empty_corners;
        }

        // resize card
        int resizeCardToWidth = 500;
        float ratioCard = float(resizeCardToWidth) / float(card_image.cols);
        int newCardHeight = int(round(ratioCard * card_image.rows));
        cv::resize(card_image, card_image, cv::Size(resizeCardToWidth, newCardHeight), cv::INTER_LINEAR);

        //make copy from card image
        Mat card_copy;
        cvtColor(card_image, card_copy, cv::COLOR_BGR2GRAY);

        
        int max_corners = 50;   //Lower limit of control
        vector<Point2f> corners;           // Store the corner coordinates found inside
        double qualityLevel = 0.01;
        double minDistance = 10;
        int blockSize = 3;
        double k = 0.04;

        // Call goodFeaturesToTrack for shi-Tomasi corner detection
        goodFeaturesToTrack(card_copy, corners, max_corners, qualityLevel, minDistance, Mat(), blockSize, false, k);
        
        //show detected features
        //Mat result_img = card_copy.clone();
        //Mat result_img1 = card_copy.clone();
        //for (auto t = 0; t < corners.size(); ++t)
        //{
        //    circle(result_img, corners[t], 2, Scalar(0,0,255), 2, 8, 0);    
        //}
        

        // parameter settings
        Size winSize = Size(5, 5);
        Size zerozone = Size(-1, -1);
        TermCriteria criteria = TermCriteria(TermCriteria::EPS + TermCriteria::MAX_ITER, 40, 0.001);
        cornerSubPix(card_copy, corners, winSize, zerozone, criteria);   	// Call the cornerSubPix function to calculate the position of the sub-pixel corner point

        // show sub-pixel corner information
        //for (auto t = 0; t < corners.size(); ++t)
        //{
        //    circle(result_img, corners[t], 2, Scalar(255, 0, 0), 2, 8, 0);
        //}
        //imshow("corners and subpix", result_img);

        //get just corners in some distance from SIFT keypoints
        Mat card_mask(card_copy.rows, card_copy.cols, CV_8UC3, Scalar(0,0,0));
        
        
        //get positions of inliers and draw white circle to black mask
        int circle_radius = int(card_mask.rows/15);
        for(int i=0;i<this->inliers;i++){
            if((unsigned int)mask.at<uchar>(i)){
                circle(card_mask, obj.at(i), circle_radius, Scalar(255, 255, 255), -1);
            }
        }
        //imshow("mask", card_mask);

        // get just corners which are masked by white circles
        vector<Point2f> masked_corners;
        for(int i=0;i<corners.size();i++){
            Vec3b color = card_mask.at<Vec3b>(Point(int(corners[i].x), int(corners[i].y)));
            if(color.val[0]==255){
                masked_corners.insert(masked_corners.begin(),corners[i]);
                //circle(result_img1, corners[i], 4, Scalar(0, 255, 255), -1);
            }
        }

        //imshow("cmasked corners", result_img1);

        //transform card by M matrix to image with tree
        Mat card_copy2 = card_copy.clone();
        Mat warped, warped_mask_inv;
        warpPerspective(card_copy2, warped, H, image.size());
        
        //get binary mask for transformed card in image
        threshold( warped, warped_mask_inv, 10, 255, 1 );
        
        // get original image with transformed card (for optical flow)
        Mat image_with_warped_card, image_masked2;
        image_with_warped_card = image.clone();
        bitwise_and(image_with_warped_card,image_with_warped_card, image_masked2, mask = warped_mask_inv);
        add(warped, image_masked2, image_with_warped_card);
        

        //transform corners to the image
        vector<Point2f> corners_refined_transformed, p1, good_old, good_new; 
        if(masked_corners.size()==0){
            std::vector<Point2f> empty_corners;
            return empty_corners;
        }
        perspectiveTransform(masked_corners, corners_refined_transformed, H);

        // calculate optical flow
        vector<uchar> status;
        vector<float> err;
        TermCriteria criteria2 = TermCriteria((TermCriteria::COUNT) + (TermCriteria::EPS), 10, 0.03);
        calcOpticalFlowPyrLK(image_with_warped_card, image, corners_refined_transformed, p1, status, err, Size(60,60), 2, criteria2);

        // draw old corners (red) and new corner computed with optical flow (green)
        Mat result_img2 = image.clone();
        cvtColor(result_img2, result_img2, COLOR_GRAY2BGR);
        for(int i=0;i<p1.size();i++){
            if(status[0]==1){
                //circle(result_img2, p1[i], 4, Scalar(0, 255, 0), -1);
                //circle(result_img2, corners_refined_transformed[i], 4, Scalar(255, 0, 0), -1);
                good_new.insert(good_new.begin(),p1[i]);
                good_old.insert(good_old.begin(),corners_refined_transformed[i]);
            }
            
        }
        
        //if tehre are less than 5 points, homography cant be found
        if (good_old.size()<5) {
            std::vector<Point2f> empty_corners;
            return empty_corners;
        }

        // find homography between good_old and good_new
        Mat H_refined = findHomography(good_old, good_new, RANSAC, 5);

        // transform objects corners to the scene
        std::vector<Point2f> scene_corners_old(4);
        scene_corners_old = scene_corners;
        perspectiveTransform(scene_corners_old, scene_corners, H_refined);
   
    }
    //////////////// END REFINE CARD-OPTICAL FLOW /////////////////////////
    
    /////////////////////////////////////////////////////////////
    //////////////////////REFINE CARD-HOUGH//////////////////////
    /////////////////////////////////////////////////////////////
    if (edge_ref_hough){
        Mat image_copy, detected_edges, dst;
        image_copy = original_image.clone();

        // image_draw - just to see results
        Mat image_draw = original_image.clone();
        cvtColor(image_draw, image_draw, cv::COLOR_GRAY2BGR);

        std::vector<Point2f> scene_lines;  //8 points (first 2 are first line, second 2 are second line..)

        // loop through all 4 card edges
        for(int i=0; i<4; i++){
            //black image in size of original one
            Mat card_mask(image_copy.rows, image_copy.cols, CV_8UC1, Scalar(0,0,0));

            //find second point of line that we want to refine
            int second_linepoint = i+1;
            if( i+1 == 4) 
                second_linepoint = 0;

            //draw thick white line
            line(card_mask, scene_corners[i], scene_corners[second_linepoint], Scalar(255, 255, 255), 20); 

            // create bounding rect around those white points and cut out this rectangle
            Rect bb = boundingRect(card_mask);
            Mat card_edge = image_copy(bb).clone();

            // blur region and make Canny edge detection
            blur( card_edge, card_edge, Size(3,3) );
            int lowThreshold = 50;
            Canny( card_edge, card_edge, lowThreshold, lowThreshold*3, 3 );
            
            //cv::imshow("found rect", image_copy(bb));
            //cv::imshow("card_edge1", card_edge);

            // Standard Hough Line Transform
            vector<Vec2f> lines; // will hold the results of the detection
            int thresh_hough = 200;

            while(lines.size()==0 && thresh_hough>=60){
                HoughLines(card_edge, lines, 1, CV_PI/180, thresh_hough, 0, 0 ); // runs the detection of line
                thresh_hough = thresh_hough - 10;
            }
            if (thresh_hough<=60){   // new refined line wasnt find, image is too messy, so use the old one (not refined from SIFT)
                scene_lines.push_back(scene_corners[i]);
                scene_lines.push_back(scene_corners[second_linepoint]);
                continue;
            }
            
            //compute and draw first line 
            float rho = lines[0][0], theta = lines[0][1];
            Point2f pt1, pt2;
            double a = cos(theta), b = sin(theta);
            double x0 = a*rho, y0 = b*rho;
            pt1.x = x0 + 1000*(-b);
            pt1.y = y0 + 1000*(a);
            pt2.x = x0 - 1000*(-b);
            pt2.y = y0 - 1000*(a);
            //line( card_edge, pt1, pt2, Scalar(255,0,0), 1, LINE_AA);          //show line

            //adapt found line back to whole image
            pt1.x = pt1.x + bb.x;
            pt1.y = pt1.y + bb.y;
            pt2.x = pt2.x + bb.x;
            pt2.y = pt2.y + bb.y;

            scene_lines.push_back(pt1);
            scene_lines.push_back(pt2);

            //line(image_draw, scene_corners[i], scene_corners[second_linepoint], Scalar(0, 0, 255), 1);  //show line
            //line(image_draw, pt1, pt2, Scalar(0, 255, 0), 1);  //show line
        }
        //show results
        //resize(image_draw, image_draw, Size(image_draw.cols/1.5, image_draw.rows/1.5));
        //imshow("image_draw", image_draw);

        vector<Point2f> refined_scene_corners;
        //intersection of all 4 lines
        refined_scene_corners.push_back(line_intersection(scene_lines[6], scene_lines[7], scene_lines[0], scene_lines[1]));
        refined_scene_corners.push_back(line_intersection(scene_lines[0], scene_lines[1], scene_lines[2], scene_lines[3]));
        refined_scene_corners.push_back(line_intersection(scene_lines[2], scene_lines[3], scene_lines[4], scene_lines[5]));
        refined_scene_corners.push_back(line_intersection(scene_lines[4], scene_lines[5], scene_lines[6], scene_lines[7]));

        scene_corners = refined_scene_corners;
    }
    /////////////////////////END - REFINE CARD-HOUGH/////////////////////////////


    // adapt points to original size
    for (int i = 0; i < scene_corners.size(); i++)
        scene_corners[i] /= ratio;



    return scene_corners;
}



/**
 * Show results on downsized image. Draw borders of card which was found.
 * @return image with drawn card
 */
cv::Mat CardDetection::getMarkedImage()
{
    Mat image = sourceImg.clone();

    // resize image
    int resizeToWidth = 600;
    float ratio = float(resizeToWidth) / float(image.cols);
    int newHeight = int(round(ratio * image.rows));
    cv::resize(sourceImg, image, cv::Size(resizeToWidth, newHeight), cv::INTER_LINEAR);

    // adapt points to new size
    std::vector<cv::Point2f> pts = points;
    for (int i = 0; i < pts.size(); i++)
        pts[i] *= ratio;

    //-- Draw lines between the corners (the mapped object in the scene)
    line(image, pts[0], pts[1], Scalar(255, 0, 0), 1);
    line(image, pts[1], pts[2], Scalar(255, 0, 0), 1);
    line(image, pts[2], pts[3], Scalar(255, 0, 0), 1);
    line(image, pts[3], pts[0], Scalar(255, 0, 0), 1);

    // draw points in corners
    circle(image, pts[0], 3, Scalar(0, 255, 0), FILLED, LINE_8);
    circle(image, pts[1], 3, Scalar(0, 255, 0), FILLED, LINE_8);
    circle(image, pts[2], 3, Scalar(0, 255, 0), FILLED, LINE_8);
    circle(image, pts[3], 3, Scalar(0, 255, 0), FILLED, LINE_8);

    return image;
}