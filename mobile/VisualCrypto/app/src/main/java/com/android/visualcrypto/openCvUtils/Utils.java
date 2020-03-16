package com.android.visualcrypto.openCvUtils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class Utils {

    private static double calcDistance(Point a, Point b) {
        double distance;

        double temp = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2)*1.0;
        distance = Math.sqrt(temp);
        return distance;
    }

    public static double getMaxDistance(Point leftUpper, Point rightUpper, Point rightLower, Point leftLower) {
        double max1 = Math.max(calcDistance(leftUpper, rightUpper), calcDistance(leftUpper, leftLower));
        double max2 = Math.max(calcDistance(rightLower, rightUpper), calcDistance(rightLower, leftLower));

        return Math.max(max1, max2);
    }

    // DistortedImage * H = UnDistortedImage
    public static double getModuleStride(double minPixelStride, Mat inverseH, Mat capturedImg) {
        double startingPoint = minPixelStride / 2.0;
       // List<Double> unDistortedImage = new ArrayList<>();
        double undistortedLocX = startingPoint;
        double undistortedLocY = startingPoint;

//        unDistortedImage.add(0, undistortedLocX);
//        unDistortedImage.add(1, undistortedLocY);
//        unDistortedImage.add(2, 1.0);
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0,0, undistortedLocX);
        unDistortedImageMatCord.put(0,1,undistortedLocY);
        unDistortedImageMatCord.put(0,2, 1);
        boolean firstBlackFound = false;
        double undistortedModuleDimension = 0.0;

        //for (int i = 0; i < Math.max(capturedImg.height(), capturedImg.width()); i++){
        while(undistortedLocX < 1 && undistortedLocY < 1){
            Mat distoredImageMatCord = new Mat();
            Core.gemm(unDistortedImageMatCord,  inverseH, 1.0, new Mat(), 0, distoredImageMatCord, 0); // res = inverseH.t() * undistortedImageMatCord
            double x = distoredImageMatCord.get(0,0)[0];
            double y = distoredImageMatCord.get(0,1)[0];
            double z = distoredImageMatCord.get(0,2)[0];
            //x = (double) x / z;
            //y = (double) y / z;
            //z = (double) z / z;
            int BLACK_THRESHOLD = 40;

            int indexRow = (int) (Math.round(x));
            int indexCol = (int) (Math.round(y));
            double[] channels = capturedImg.get(indexRow, indexCol);
            double pixel = (int) (channels[0] + channels[1] + channels[2]) / 3.0;
            if (pixel < BLACK_THRESHOLD && !firstBlackFound){ // first black after whites!
                firstBlackFound = true;
            }
            else if(pixel < BLACK_THRESHOLD && firstBlackFound){ // still black after first black was found
                undistortedModuleDimension+= minPixelStride;
            }
            else if(pixel >= BLACK_THRESHOLD && firstBlackFound){ // first white after first black was found
                undistortedModuleDimension+= minPixelStride;
                return undistortedModuleDimension;
            }
            undistortedLocX += minPixelStride;
            undistortedLocY += minPixelStride;
            unDistortedImageMatCord.put(0,0, undistortedLocX);
            unDistortedImageMatCord.put(0,1, undistortedLocY);
        }
        return 0.0;
    }
}
