package com.android.visualcrypto.openCvSampler;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Environment;
import android.widget.ImageView;
import com.android.visualcrypto.R;
import com.pc.encoderDecoder.StdImageSampler;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;
import java.io.FileNotFoundException;

public class OpenCvSampler extends StdImageSampler {

    public void locatePositionDetectors(Bitmap b) throws FileNotFoundException {

        Mat mat = new Mat (b.getWidth(), b.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(b, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 51, 0);

        QRCodeDetector detector = new QRCodeDetector();
        //detector.setEpsX(3);
        //detector.setEpsY(3);
        Mat points = new Mat();
        if(detector.detect(mat, points)){
            PointF[] corners = new PointF[4];
            PointF p;
            for(int i=0; i<4; i++){
                p = new PointF((float)points.get(0,0)[0],
                        (float) points.get(0,0)[1]);
                corners[i] = p;
            }
        }
        /*
        // Writing the image
        Utils.matToBitmap(mat, b);
        FileOutputStream fileOutputStream = new FileOutputStream("/sdcard/Download/out.png");
        b.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
         */
    }
}