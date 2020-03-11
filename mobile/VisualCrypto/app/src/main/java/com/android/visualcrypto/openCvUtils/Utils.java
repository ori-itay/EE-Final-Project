package com.android.visualcrypto.openCvUtils;

import org.opencv.core.Core;
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
        List<Double> unDistortedImage = new ArrayList<>();
        unDistortedImage.add(0, startingPoint);
        unDistortedImage.add(1, startingPoint);
        unDistortedImage.add(2, 1.0);

        for (int i = 0; i < Math.max(capturedImg.height(), capturedImg.width()); i++){
            Mat unDistortedImageMatCord = Converters.vector_double_to_Mat(unDistortedImage);
            //Mat res = inverseH.mul(unDistortedImageMatCord);
            //Mat DistortedImageMatCord = unDistortedImageMatCord.mul(inverseH);
            //Mat ads = unDistortedImageMatCord * inverseH.t();
            //capturedImg.get(DistortedImageMatCord.
            Mat res = new Mat();
           Core.gemm(inverseH,  unDistortedImageMatCord, 1.0, new Mat(), 0, res, 0); //TODO: continue from here..put size in res etc
            int b =4;

        }



        return 0.0;
    }
}
