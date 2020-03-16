package com.android.visualcrypto.openCvUtils;

import android.util.Log;

import com.pc.encoderDecoder.StdImageSampler;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.objdetect.QRCodeDetector;

import static com.android.visualcrypto.openCvUtils.Utils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.Utils.getModuleStride;

public class DistortedImageSampler extends StdImageSampler {
    private Mat distortedImage;
    private Mat inverseH;
    private double normalizedModuleSize;


    public DistortedImageSampler(Mat distortedImage) {
        this.distortedImage = distortedImage;
        initializeParameters();
    }



    private void initializeParameters() {
        QRCodeDetector detector = new QRCodeDetector();
        MatOfPoint2f corners1 = new MatOfPoint2f();
        boolean foundCorners = detector.detect(this.distortedImage, corners1);
        if (!foundCorners) {
            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
            //return; TODO: handle case
        }
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0,0), new Point(1,0), new Point(1,1), new Point(0,1)); // TODO: verify it contains appropriate white margin
        Mat H = new Mat();
        H = Calib3d.findHomography(corners1, corners2);
        Mat inverseH = H.inv();
        Point[] pts = corners1.toArray();
        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

        this.normalizedModuleSize = getModuleStride(minPixelStride, inverseH, this.distortedImage);
    }

    @Override
    public int getPixel(int rowPixel, int colPixel) {

        return 0;
    }

    private double getPixelStride(Point[] pts) {
        double minPixelStride = 1/getMaxDistance(pts[0], pts[1], pts[2], pts[3]);
        return minPixelStride;
    }
}
