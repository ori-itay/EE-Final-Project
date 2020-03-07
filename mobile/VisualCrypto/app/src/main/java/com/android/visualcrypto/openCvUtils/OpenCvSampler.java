package com.android.visualcrypto.openCvUtils;

import android.graphics.Bitmap;

import com.pc.encoderDecoder.StdImageSampler;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

public class OpenCvSampler extends StdImageSampler {

    MatOfPoint2f corners = null;
    private Point[] pointsArr = null;
    private final Bitmap b;
    private boolean searchedPositionDetectors;

    public OpenCvSampler(Bitmap b) {
        this.b = b;
        this.searchedPositionDetectors = false;
    }

    public MatOfPoint2f getPositionDetectorsLocation() {
        if (this.corners == null && !searchedPositionDetectors) {
            searchedPositionDetectors = true;
            Mat mat = new Mat (b.getWidth(), b.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(b, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
            Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 51, 0);

            QRCodeDetector detector = new QRCodeDetector();
            //detector.setEpsX(3);
            //detector.setEpsY(3);
            Mat points = new Mat();
            if (detector.detect(mat, points)){
                //this.pointsArr = new MatOfPoint2f(points).toArray();
                this.corners = new MatOfPoint2f(points);

            }
        }
        return this.corners;
        /*
        // Writing the image
        Utils.matToBitmap(mat, b);
        FileOutputStream fileOutputStream = new FileOutputStream("/sdcard/Download/out.png");
        b.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
         */
    }
}