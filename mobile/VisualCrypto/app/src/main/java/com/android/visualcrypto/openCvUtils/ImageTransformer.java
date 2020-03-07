package com.android.visualcrypto.openCvUtils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.visualcrypto.R;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import static android.app.PendingIntent.getActivity;

public class ImageTransformer {

/*    public static void homographicTransform(MatOfPoint2f coordinates) {

        MatOfPoint2f dstCoordinates = new MatOfPoint2f(new Point(0,0),new Point(200,0),
                new Point(200,200), new Point(0,200));
        Mat H = new Mat();
        H = Calib3d.findHomography(coordinates,dstCoordinates);
        Mat img1_warp = new Mat();
        Mat img1 = Imgcodecs.imread(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/encodedImage.png");
        Imgproc.warpPerspective(img1, img1_warp, H, img1.size());
        Bitmap bp = Bitmap.createBitmap(img1_warp.cols(), img1_warp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img1_warp, bp);

        View rootView = inflater.inflate(R.layout);
        ImageView iView = findViewById(R.id.decodedImgId);
        iView.setImageBitmap(Bitmap.createScaledBitmap(bp, iView.getWidth(), iView.getHeight(), false));

    }*/
}
