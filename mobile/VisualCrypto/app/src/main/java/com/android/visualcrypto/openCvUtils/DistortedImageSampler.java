package com.android.visualcrypto.openCvUtils;

import android.content.Context;
import android.util.Log;

import com.android.visualcrypto.MainActivity;
import com.pc.encoderDecoder.StdImageSampler;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.objdetect.QRCodeDetector;

import static com.android.visualcrypto.openCvUtils.Utils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.Utils.getModuleStride;
import static com.android.visualcrypto.openCvUtils.Utils.getPixelChannels;

public class DistortedImageSampler extends StdImageSampler {
    private static Mat distortedImage;
    private static Mat inverseH;

    private Context context;


    public DistortedImageSampler(Mat distortedImage, Context context) {
        DistortedImageSampler.distortedImage = distortedImage;
        this.context = context;
    }

    public int initParameters() {
        this.setModulesInMargin(0);

        QRCodeDetector detector = new QRCodeDetector();
        MatOfPoint2f corners1 = new MatOfPoint2f();
        boolean foundCorners = detector.detect(distortedImage, corners1);
        if (!foundCorners) {
            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
            ((MainActivity) this.context).showAlert("Couldn't detect QR position detectors");
            return 1;
        }
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0,0), new Point(1,0), new Point(1,1), new Point(0,1));
        Mat H;
        H = Calib3d.findHomography(corners1, corners2);
        Mat inverseH = H.inv();

        DistortedImageSampler.inverseH = inverseH;
        Point[] pts = corners1.toArray();
        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

        this.setModuleSize(getModuleStride(minPixelStride, inverseH, DistortedImageSampler.distortedImage));
        int effectiveModulesInDim = (int) Math.round(1.0 / this.getModuleSize()); //TODO: double test = this.getModuleSize() / minPixelStride;
        this.setModulesInDim(effectiveModulesInDim);

        return 0;
    }

    @Override
    public int getPixel(double rowLoc, double colLoc){
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0,0, rowLoc);
        unDistortedImageMatCord.put(0,1, colLoc);
        unDistortedImageMatCord.put(0,2, 1);
        double[] channels = getPixelChannels(unDistortedImageMatCord, DistortedImageSampler.inverseH, DistortedImageSampler.distortedImage);
        int pixelValue = (int) (Math.round(channels[0])) | (int) (Math.round(channels[1]) << 8) | (int) (Math.round(channels[2]) << 16);
        return pixelValue;
    }
}
