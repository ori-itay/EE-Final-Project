package com.android.visualcrypto.openCvUtils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

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
    public static double getModuleStride(double maxPixelStride, Mat inverseH, Mat capturedImg) {
        double startingPoint = maxPixelStride / 2.0;
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


        for (int i = 0; i < Math.max(capturedImg.height(), capturedImg.width()); i++){
            Mat distoredImageMatCord = new Mat();
            Core.gemm(unDistortedImageMatCord,  inverseH, 1.0, new Mat(), 0, distoredImageMatCord, 0); // res = inverseH.t() * undistortedImageMatCord
            double x = distoredImageMatCord.get(0,0)[0];
            double y = distoredImageMatCord.get(0,1)[0];
            double z = distoredImageMatCord.get(0,2)[0];
            x = (double) x / z;
            y = (double) y / z;
            z = (double) z / z;

            int indexRow = (int) (Math.round(x));
            int indexCol = (int) (Math.round(y));
            double pixel = capturedImg.get(indexRow, indexCol)[0];
            undistortedLocX += maxPixelStride;
            undistortedLocY += maxPixelStride;
            unDistortedImageMatCord.put(0,0, undistortedLocX);
            unDistortedImageMatCord.put(0,1, undistortedLocY);
        }
        return 0.0;
    }
}
