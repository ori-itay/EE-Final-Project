package com.android.visualcrypto.openCvUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.core.util.Pair;

import com.android.visualcrypto.flow.Flow;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.StdImageSampler;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import boofcv.abst.fiducial.QrCodePreciseDetector;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import static boofcv.android.ConvertBitmap.bitmapToGray;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.calcDistance;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.classifyPixelChannelsLevels;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.switchCoordinates;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class DistortedImageSampler extends StdImageSampler {
    private static final int gridSplitSize = 1;
//    private static final double[][][] minPixelVal = new double[gridSplitSize][gridSplitSize][Constants.CHANNELS];
//    private static final double[][][] maxPixelVal = new double[gridSplitSize][gridSplitSize][Constants.CHANNELS];
    public static final List<List<Map<Integer,Integer>>> percentileValuesMapTilesRed = new ArrayList<>(gridSplitSize);
    public static final List<List<Map<Integer,Integer>>> percentileValuesMapTilesGreen = new ArrayList<>(gridSplitSize);
    public static final List<List<Map<Integer,Integer>>> percentileValuesMapTilesBlue = new ArrayList<>(gridSplitSize);

//    public static final Map<Integer, Integer> percentilesMapRed = new TreeMap<>();
//    public static final Map<Integer, Integer> percentilesMapGreen = new TreeMap<>();
//    public static final Map<Integer, Integer> percentilesMapBlue = new TreeMap<>();
    static int tileHeight;
    static int tileWidth;
    private static Mat distortedImage;
    private static Mat inverseH;
    private static Bitmap distortedBitmap;
    private static int d, row;
    public static int errCounterTotal = 0;
    public static int errCounterRed = 0;
    public static int errCounterGreen = 0;
    public static int errCounterBlue = 0;


    public static Mat debugBW;

    public static final Mat itaysCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat orisCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat itaysCamConfigDst = new Mat(1,5 ,CvType.CV_64F);
    public static final Mat orisCamConfigDst = new Mat(1,5 ,CvType.CV_64F);

    public static final Mat itaysCamConfigMtxVideo = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat itaysCamConfigDstVideo = new Mat(1,5 ,CvType.CV_64F);
    private static Mat colorBalancingMat;


    static {
        orisCamConfigDst.put(0,0,    2.37788419e-01, -1.05060973, -1.24226900e-03, -1.06977518e-03, 1.36988634);
        orisCamConfigMtx.put(0,0, 3.54906380e+03, 0D, 2.28702762e+03,   0D, 3.55130312e+03, 1.69588285e+03,    0D, 0D, 1.0D);

        //itaysCamConfigDst.put(0,0,   0.51539854, -0.00120593, 0.00206427, -0.69356642 ,0.24467169);
        itaysCamConfigDst.put(0,0,   3.90420233e-01, -1.54381552e+00, -9.02378490e-04, 1.83526192e-03 ,1.79797055e+00);
        itaysCamConfigMtx.put(0,0, 3.14574313e+03, 0, 1.49381470e+03, 0, 3.14831362e+03, 1.95266442e+03, 0, 0, 1);
        itaysCamConfigDstVideo.put(0,0, 2.97261556e+03, 0, 1.85805949e+03, 0, 2.97191363e+03, 1.10037754e+03, 0, 0, 1);
        itaysCamConfigMtxVideo.put(0,0,   3.75570453e-01, -1.45061390e+00, -9.40469522e-04, 7.57586055e-04 , 1.59684301e+00);
    }



    //MatOfPoint2f possibleCenters = new MatOfPoint2f();
    //List<Double> estimatedModuleSize = new ArrayList<>();
    private Context context;

    public DistortedImageSampler(Mat distortedImage, Bitmap distortedBitmap, Context context) {
        DistortedImageSampler.distortedImage = distortedImage;
        DistortedImageSampler.distortedBitmap = distortedBitmap;
        this.context = context;
    }

    public int initParameters() {
        this.setModulesInMargin(0);

        GrayU8 gray = bitmapToGray(DistortedImageSampler.distortedBitmap, (GrayU8) null, null);

        ConfigQrCode config = new ConfigQrCode();
        QrCodePreciseDetector<GrayU8> detector = FactoryFiducial.qrcode(config, GrayU8.class);
        detector.process(gray);

        QrCode boofCorners = null;
        Point[] pts = new Point[4]; //opencv Points

        List<QrCode> failures = detector.getFailures();
        Log.d("fail", String.valueOf(failures.size()));//TODO Delete
        if (failures.size() >= 1) {
            boofCorners = failures.get(0);
            convertBoofToOpenPoints(boofCorners, pts);
        }

        List<QrCode> detections = detector.getDetections();
        Log.d("fail", String.valueOf(detections.size()));//TODO Delete
        if (detections.size() >= 1) {
            boofCorners = detections.get(0);
            convertBoofToOpenPoints(boofCorners, pts);
        }

        List<PositionPatternNode> pointsQueue = detector.getDetectPositionPatterns().getPositionPatterns().toList();
        if (failures.size() == 0 && detections.size() == 0 && pointsQueue.size() == 3) {
            //pointsQueue.get(0).
            Point2D_F64 boofPt0 = pointsQueue.get(0).square.get(3); // PT0         PT1
            //Point2D_F64 boofPt0 = pointsQueue.get(0).square.get(2);
            Point2D_F64 boofPt1 = pointsQueue.get(1).square.get(0); //PT3          PT2
            //Point2D_F64 boofPt1 = pointsQueue.get(1).square.get(3);
            Point2D_F64 boofPt3 = pointsQueue.get(2).square.get(2);
            //Point2D_F64 boofPt3 = pointsQueue.get(2).square.get(1);

            pts[0] = new Point(boofPt0.x, boofPt0.y);
            pts[1] = new Point(boofPt1.x, boofPt1.y);
            pts[3] = new Point(boofPt3.x, boofPt3.y);
            double x3 = pts[3].x + (pts[1].x - pts[0].x);
            double y3 = pts[3].y + (pts[1].y - pts[0].y);
            pts[2] = new Point(x3, y3);

        } else if (failures.size() == 0 && detections.size() == 0) {
            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
            //showAlert(context, "Couldn't detect QR position detectors");
            return 1;
        }

        //PERFORMANCE START
        Log.d("performance", "start: " + System.currentTimeMillis());

        double[] xValues = new double[] {pts[0].x, pts[1].x, pts[2].x, pts[3].x};
        Arrays.sort(xValues);
        int xMin = (int) Math.floor(xValues[0]);
        int xMax = (int) Math.ceil(xValues[3]);

        double[] yValues = new double[] {pts[0].y, pts[1].y, pts[2].y, pts[3].y};
        Arrays.sort(yValues);
        int yMin = (int) Math.floor(yValues[0]);
        int yMax = (int) Math.ceil(yValues[3]);

        long start = System.currentTimeMillis();// performance
        /********************ROI****************************/
        try {
            Rect roi = new Rect(new Point(xMin-10, yMin-10), new Point(xMax+10, yMax+10));
            DistortedImageSampler.distortedImage = new Mat(DistortedImageSampler.distortedImage, roi);
            Log.d("performance", "roi took: " + (System.currentTimeMillis() - start));//performance
        } catch (Exception e) {
            Log.d("roi", "roi threw exception");
            return 1;
        }
        /***************************************************/

        start = System.currentTimeMillis(); //performance
        Mat bw = new Mat();
        cvtColor(DistortedImageSampler.distortedImage, bw, Imgproc.COLOR_BGR2GRAY);
        //adaptiveThreshold(bw, bw, 255, ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 25);
        adaptiveThreshold(bw, bw, 255, ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 45);
        cvtColor(bw, bw, Imgproc.THRESH_BINARY);


        pts[0].x -= (xMin - 10); pts[1].x -= (xMin - 10); pts[2].x -= (xMin - 10); pts[3].x -= (xMin - 10);
        pts[0].y -= (yMin - 10); pts[1].y -= (yMin - 10); pts[2].y -= (yMin - 10); pts[3].y -= (yMin - 10);
        DistortedImageSampler.debugBW = bw.clone();
        pts[2] = findCorner(bw, 0, false, false); //TODO: dynamically find what corner to find: pts[2].y < pts[1].y && pts[2].x < pts[3].x ?
        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/debugBW.jpg", DistortedImageSampler.debugBW);
        Log.d("performance", "bw cvtColors and findCorner took: " + (System.currentTimeMillis() - start));//performance
        MatOfPoint2f corners1 = new MatOfPoint2f(pts[0], pts[1], pts[2], pts[3]);
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(0, 1));

        Mat H = Calib3d.findHomography(corners1, corners2);
        Mat inverseH = H.inv();

        DistortedImageSampler.inverseH = inverseH;

        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

//        /******DEBUGGING***************/
        String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + Instant.now().toString();
        Path path = Paths.get(folderPath);
        try {
            Files.createDirectory(path);
            FileWriter fw = new FileWriter(folderPath + "/parameters.txt");
            Imgcodecs.imwrite(folderPath + "/bw.jpg", bw);
            Imgcodecs.imwrite(folderPath + "/afterRoi.jpg", DistortedImageSampler.distortedImage);
            String upperRowPts = String.format("leftUpperPoint: %f,%f\t\trightUpperPoint: %f,%f\n", pts[0].x,pts[0].y, pts[1].x, pts[1].y);
            String lowerRowPts = String.format("leftLowerrPoint: %f,%f\t\trightLowerPoint: %f,%f", pts[3].x,pts[3].y, pts[2].x, pts[2].y);
            fw.write(upperRowPts);
            fw.write(lowerRowPts);
            fw.close();
        }catch(Exception e){
            Log.d("writing_file", e.getMessage());
        }
        /*****************************/

        /***********INSERT HERE THE ROTATION + CALCULATION OF THE MATRIX "A" (COLOR BALANCING)**********/
        List<double[]> potentialsCenters = new ArrayList<>();
        for (PositionPatternNode center : pointsQueue) {
            Point2D_F64 centerPoint = center.center;
            if (!inQrCenter(boofCorners, centerPoint)) {
                continue;
            }
            centerPoint.x -= (xMin - 10);
            centerPoint.y -= (yMin - 10);
            double[] centerChannels = OpenCvUtils.getAvgQrCornerColor(centerPoint, minPixelStride, H, inverseH, distortedImage, 7);// למה u need to do a video?
            if (centerChannels != null)
                potentialsCenters.add(centerChannels);
        }
        if (potentialsCenters.size() != 3) {
            Log.d("potentialsCenters", "Error: Found " + potentialsCenters.size() + "centers");
            return 1;
        }
        double[] topLeft; double[] topRight; double[] bottomLeft;

        if (!OpenCvUtils.getCentersOrder(potentialsCenters)) {  // indexes[0] == R, indexes[1] = G, indexes[2] = B
            Log.d("getCentersOrder", "Error in getCentersOrder()");
            return 1;
        } else {
            topLeft = potentialsCenters.get(0);
            topRight = potentialsCenters.get(1);
            bottomLeft = potentialsCenters.get(2);
            Mat colorBalancingMat = OpenCvUtils.getColorBalancingMatrix(topLeft, topRight, bottomLeft); // TODO: integrate colorbalancingMat with getpixel?
            DistortedImageSampler.colorBalancingMat = colorBalancingMat;
        }

        //rotate(topLeft, topRight, bottomLeft);
        /***********************************************************************************************/

        start = System.currentTimeMillis(); // performance
        //findMinMaxPixelVals(DistortedImageSampler.distortedImage);

        Rect roi = new Rect(new Point(pts[0].x+10+150, pts[0].y-10+150), new Point(pts[2].x+10-100, pts[2].y+10-100));
        Mat croppedMatForHisto = new Mat(DistortedImageSampler.distortedImage, roi);
//        Imgcodecs.imwrite(folderPath + "/for_histo.jpg", croppedMatForHisto);

        //findPercentilesValues(DistortedImageSampler.distortedImage);
        DistortedImageSampler.tileHeight = distortedImage.height() / gridSplitSize;
        DistortedImageSampler.tileWidth = distortedImage.width() / gridSplitSize;
        findPercentilesValues(croppedMatForHisto);
        Log.d("performance", "findMinMaxPixelVals took: " + (System.currentTimeMillis() - start));
        Point rightLowerOfPts0 = new Point(failures.get(0).ppCorner.vertexes.data[2].x - xMin + 10, failures.get(0).ppCorner.vertexes.data[2].y - yMin + 10);
        //Point leftLowerOfPts0 = new Point(pointsQueue.get(0).square.vertexes.get(0).x, pointsQueue.get(0).square.vertexes.get(0).y);
        start = System.currentTimeMillis(); // performance
        double estimatedModuleSize = computeModuleSize(pts[0], rightLowerOfPts0, H, Math.sqrt(2 * 49));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        double normalizedEstimatedModuleSize = 1 / (Math.floor(1.0 / estimatedModuleSize));
        start = System.currentTimeMillis();



        Flow.delete = distortedImage.clone();
        Point alignmentBottomRight = OpenCvUtils.findAlignmentBottomRight(this, normalizedEstimatedModuleSize, minPixelStride, inverseH, DistortedImageSampler.distortedImage);
        Imgcodecs.imwrite(folderPath + "/pathTaken.jpg", Flow.delete);
        if (alignmentBottomRight == null) {
//            String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + Instant.now().toString();
//            Path path = Paths.get(folderPath);
//            try {
//                Files.createDirectory(path);
//                Imgcodecs.imwrite(folderPath + "/bw.jpg", bw);
//                Imgcodecs.imwrite(folderPath + "/afterRoi.jpg", DistortedImageSampler.distortedImage);
//                FileWriter fw = new FileWriter(folderPath + "/parameters.txt");
//                String upperRowPts = String.format("leftUpperPoint: %f,%f\t\trightUpperPoint: %f,%f\n", pts[0].x,pts[0].y, pts[1].x, pts[1].y);
//                String lowerRowPts = String.format("leftLowerrPoint: %f,%f\t\trightLowerPoint: %f,%f", pts[3].x,pts[3].y, pts[2].x, pts[2].y);
//                fw.write(upperRowPts);
//                fw.write(lowerRowPts);
//                fw.close();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            Log.d("DistortedImageSampler", "findAlignmentBottomRight returned null");
            return 1;
        }
        Log.d("performance", "FindAlignmentBottomRight took: " + (System.currentTimeMillis() - start));
        Mat alignmentBottomRightMat = new Mat(1, 3, CvType.CV_64F);
        alignmentBottomRightMat.put(0, 0, alignmentBottomRight.x, alignmentBottomRight.y, 1);
        Point distortedPoint = switchCoordinates(alignmentBottomRightMat, inverseH);

        start = System.currentTimeMillis();
        this.setModuleSize(computeModuleSize(pts[0], distortedPoint, H, Math.sqrt(2 * 99 * 99)));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        int effectiveModulesInDim = (int) Math.round(1.0 / this.getModuleSize());
//
//        try (FileWriter fw = new FileWriter(folderPath + "/information.txt")) {
//            fw.write("alignment: " + distortedPoint.x +"," + distortedPoint.y);
//            fw.write("\nModules in dim: " + (1.0/this.getModuleSize()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        this.setModulesInDim(effectiveModulesInDim);

        Log.d("ModulesInDim", "modules in dim: " + Float.toString(this.getModulesInDim()));
        return 0;
    }

    private boolean inQrCenter(QrCode qrCode, Point2D_F64 centerPoint) {
        if (qrCode == null) {
            return false;
        }

        Polygon2D_F64 qr1 = qrCode.ppCorner;
        Polygon2D_F64 qr2 = qrCode.ppRight;
        Polygon2D_F64 qr3 = qrCode.ppDown;

        return qr1.isInside(centerPoint) || qr2.isInside(centerPoint) || qr3.isInside(centerPoint);
    }

    private Point findCorner(Mat img, int val, boolean top, boolean left) {
        Pair p = scanDiagonalFromCorner(img.size(), top, left);
        while (img.get((int) p.first, (int) p.second)[0] != val) {
            Point debugBWPoint = new Point((int) p.second, (int) p.first);
            Imgproc.rectangle(DistortedImageSampler.debugBW, debugBWPoint,debugBWPoint, new Scalar(155,155,155));
            p = scanDiagonalFromCorner(img.size(), top, left);
        }
        return new Point((int) p.second, (int) p.first);
    }

    private Pair scan(int height, int width) {
        int startRow = Math.max(0, d - (width - 1));
        int endRow = Math.min(d, height - 1);
        if (row >= endRow + 1) {
            row = startRow;
            d++;
        }
        int col = d - row;

        if (d >= height + width - 1) {
            return null;
        }

        return new Pair(row++, col);
    }

    private Pair scanDiagonalFromCorner(Size size, boolean top, boolean left) {
        int height = (int) size.height;
        int width = (int) size.width;
        Pair scan = scan(height, width);
        int row, col;
        row = (int) scan.first;
        col = (int) scan.second;
        if (!top) {
            row = height - (int) scan.first - 1;
        }
        if (!left) {
            col = width - (int) scan.second - 1;
        }
        return new Pair(row, col);
    }


    private double computeModuleSize(Point upperLeft, Point lowerRight, Mat H, double expectedModulesDistance) {
        Mat mat = new Mat(1, 3, CvType.CV_64F);
        mat.put(0, 0, upperLeft.x,  upperLeft.y, 1);
        Point upperLeftPt = switchCoordinates(mat, H);

        mat.put(0, 0, lowerRight.x, lowerRight.y);

        Point lowerPt = switchCoordinates(mat, H);
        return calcDistance(upperLeftPt, lowerPt) / expectedModulesDistance;
    }

//    private void findMinMaxPixelVals(Mat capturedImage) {
//        DistortedImageSampler.tileHeight = capturedImage.height() / gridSplitSize;
//        DistortedImageSampler.tileWidth = capturedImage.width() / gridSplitSize;
//        Mat subMat;
//        int histSize = 256;
//        float[] range = {0, 256}; //the upper boundary is exclusive
//        MatOfFloat histRange = new MatOfFloat(range);
//        boolean accumulate = false;
//        int countR = 0, countG = 0, countB = 0;
//
//        final int lowPercentileRed = (int) Math.floor(0.1*(tileWidth*tileHeight));
//        final int highPercentileRed = (int) Math.floor(0.9*(tileWidth*tileHeight));
//
//        final int lowPercentileGreen = (int) Math.floor(0.1*(tileWidth*tileHeight));
//        final int highPercentileGreen = (int) Math.floor(0.9*(tileWidth*tileHeight));
//
//        final int lowPercentileBlue = (int) Math.floor(0.1*(tileWidth*tileHeight));
//        final int highPercentileBlue = (int) Math.floor(0.9*(tileWidth*tileHeight));
//
//        int high, low, left, right;
//        for(int i = 0; i < gridSplitSize; i++){
//            for(int j = 0; j < gridSplitSize; j++){
//                countR = 0; countG = 0; countB = 0;
//                List<Mat> bgrPlanes = new ArrayList<>();
//                high = i*tileHeight; low = Math.min((i+1)*tileHeight, capturedImage.rows());
//                left = i*tileWidth; right = Math.min((i+1)*tileWidth, capturedImage.cols());
//                subMat = capturedImage.submat(high, low, left, right);
//                Core.split(subMat, bgrPlanes);
//                Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();
//                Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new MatOfInt(histSize), histRange, accumulate);
//                Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new MatOfInt(histSize), histRange, accumulate);
//                Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new MatOfInt(histSize), histRange, accumulate);
//
//                for (int pixelValue = 0; pixelValue < 256; pixelValue++){
//                    countR += rHist.get(pixelValue, 0)[0];
//                    countG += gHist.get(pixelValue, 0)[0];
//                    countB += bHist.get(pixelValue, 0)[0];
//                    if(minPixelVal[i][j][0] == 0 && countR >= lowPercentileRed){
//                        minPixelVal[i][j][0] = pixelValue;
//                    }
//                    if(maxPixelVal[i][j][0] == 0 && countR >= highPercentileRed){
//                        maxPixelVal[i][j][0] = pixelValue;
//                    }
//                    if(minPixelVal[i][j][1] == 0 && countG >= lowPercentileGreen){
//                        minPixelVal[i][j][1] = pixelValue;
//                    }
//                    if(maxPixelVal[i][j][1] == 0 && countG >= highPercentileGreen){
//                        maxPixelVal[i][j][1] = pixelValue;
//                    }
//                    if(minPixelVal[i][j][2] == 0 && countB >= lowPercentileBlue){
//                        minPixelVal[i][j][2] = pixelValue;
//                    }
//                    if(maxPixelVal[i][j][2] == 0 && countB >= highPercentileBlue){
//                        maxPixelVal[i][j][2] = pixelValue;
//                    }
//                }
//            }
//        }
//    }

    private void findPercentilesValues(Mat capturedImage) {
//        DistortedImageSampler.tileHeight = capturedImage.height() / gridSplitSize;
//        DistortedImageSampler.tileWidth = capturedImage.width() / gridSplitSize;
        int tileWidth = capturedImage.width();
        int tileHeight = capturedImage.height();
        final int BLOCK_SIZE = (tileWidth*tileHeight);
        Mat subMat;
        int histSize = 256;
        float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);
        boolean accumulate = false;
        int countR = 0, countG = 0, countB = 0;

        double diffBetweenLevels = 255.0 / (Parameters.encodingColorLevels - 1);
        double diffBetweenPercentiles = 1.0 / Parameters.encodingColorLevels;
        double lowestPercentile = diffBetweenPercentiles / 2.0;
        final int[] percentilesPixelCounts = new int[Parameters.encodingColorLevels];

        for(int i = 0; i < Parameters.encodingColorLevels; i++){
            percentilesPixelCounts[i] = (int) Math.round((lowestPercentile + diffBetweenPercentiles*i) * BLOCK_SIZE);//how many pixels for each percentile
        }



        int high, low, left, right;
        for(int i = 0; i < gridSplitSize; i++){
            percentileValuesMapTilesRed.add(new ArrayList<>(gridSplitSize));
            percentileValuesMapTilesGreen.add(new ArrayList<>(gridSplitSize));
            percentileValuesMapTilesBlue.add(new ArrayList<>(gridSplitSize));

            for(int j = 0; j < gridSplitSize; j++){
                Map<Integer, Integer> currMapRed = new TreeMap<>();
                Map<Integer, Integer> currMapGreen = new TreeMap<>();
                Map<Integer, Integer> currMapBlue = new TreeMap<>();
                percentileValuesMapTilesRed.get(i).add(j, currMapRed);
                percentileValuesMapTilesGreen.get(i).add(j, currMapGreen);
                percentileValuesMapTilesBlue.get(i).add(j, currMapBlue);

                countR = 0; countG = 0; countB = 0;
                List<Mat> bgrPlanes = new ArrayList<>();
                high = i*tileHeight; low = Math.min((i+1)*tileHeight, capturedImage.rows());
                left = i*tileWidth; right = Math.min((i+1)*tileWidth, capturedImage.cols());
                subMat = capturedImage.submat(high, low, left, right);
                Core.split(subMat, bgrPlanes);
                Mat bHist = new Mat(), gHist = new Mat(), rHist = new Mat();
                Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), bHist, new MatOfInt(histSize), histRange, accumulate);
                Imgproc.calcHist(bgrPlanes, new MatOfInt(1), new Mat(), gHist, new MatOfInt(histSize), histRange, accumulate);
                Imgproc.calcHist(bgrPlanes, new MatOfInt(2), new Mat(), rHist, new MatOfInt(histSize), histRange, accumulate);

                int currPercentileIndexRed = 0, currPercentileIndexGreen = 0, currPercentileIndexBlue = 0;
                for (int pixelValue = 0; pixelValue < 256; pixelValue++){
                    countR += rHist.get(pixelValue, 0)[0];
                    countG += gHist.get(pixelValue, 0)[0];
                    countB += bHist.get(pixelValue, 0)[0];

                    if (currPercentileIndexRed < Parameters.encodingColorLevels && percentilesPixelCounts[currPercentileIndexRed] <= countR){
                        //listOfMapsRed[i][j].put...
                        currMapRed.put((int) Math.round(diffBetweenLevels*currPercentileIndexRed), pixelValue);
                        //percentilesMapRed.put((int) Math.round(diffBetweenLevels*currPercentileIndexRed), pixelValue);
                        currPercentileIndexRed++;
                    }
                    if (currPercentileIndexGreen < Parameters.encodingColorLevels && percentilesPixelCounts[currPercentileIndexGreen] <= countG){
                        //percentilesMapGreen.put((int) Math.round(diffBetweenLevels*currPercentileIndexGreen), pixelValue);
                        currMapGreen.put((int) Math.round(diffBetweenLevels*currPercentileIndexGreen), pixelValue);
                        currPercentileIndexGreen++;
                    }
                    if (currPercentileIndexBlue < Parameters.encodingColorLevels && percentilesPixelCounts[currPercentileIndexBlue] <= countB){
                        //percentilesMapBlue.put((int) Math.round(diffBetweenLevels*currPercentileIndexBlue), pixelValue);
                        currMapBlue.put((int) Math.round(diffBetweenLevels*currPercentileIndexBlue), pixelValue);
                        currPercentileIndexBlue++;
                    }
                }
            }
        }
    }

    private void convertBoofToOpenPoints(QrCode qrCode, Point[] pts) {
        Polygon2D_F64 polygon = qrCode.bounds;
        Point2D_F64 boofPoint0 = polygon.get(0);
        Point2D_F64 boofPoint1 = polygon.get(1);
        Point2D_F64 boofPoint2 = polygon.get(2);
        Point2D_F64 boofPoint3 = polygon.get(3);
        pts[0] = new Point(boofPoint0.x, boofPoint0.y);
        pts[1] = new Point(boofPoint1.x, boofPoint1.y);
        pts[2] = new Point(boofPoint2.x, boofPoint2.y);
        pts[3] = new Point(boofPoint3.x, boofPoint3.y);
    }

    @Override
    public int getPixel(double rowLoc, double colLoc, boolean duplicateChannels, boolean radiusSample) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, rowLoc);
        unDistortedImageMatCord.put(0, 1, colLoc);
        unDistortedImageMatCord.put(0, 2, 1);
        Point distortedIndex = switchCoordinates(unDistortedImageMatCord, inverseH);
        Imgproc.circle(Flow.delete, distortedIndex, 1, new Scalar(0,0,255), 1);
        int indexCol = (int) distortedIndex.x; int indexRow = (int) distortedIndex.y;
//        double[] avgChannels = new double[Constants.CHANNELS];
        double[] medianChannels = new double[Constants.CHANNELS];
        final int NUM_OF_SAMPLES_FOR_RADIUS_SMAPLING = 9;
        if(radiusSample) {
            double[] channels1 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow + .51), (int) Math.round(indexCol + .51));
            Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol + .51), (int) Math.round(indexRow + .51)),new Point((int) Math.round(indexCol + .51), (int) Math.round(indexRow + .51)),  new Scalar(255-avgChannels[2],255-avgChannels[2],255-avgChannels[0]));
            double[] channels2 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow - .51), (int) Math.round(indexCol - .51));
            Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol - .51), (int) Math.round(indexRow - .51)),new Point((int) Math.round(indexCol - .51), (int) Math.round(indexRow - .51)),  new Scalar(255-avgChannels[2],255-avgChannels[2],255-avgChannels[0]));
            double[] channels3 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow + .51), (int) Math.round(indexCol - .51));
            Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol + .51), (int) Math.round(indexRow - .51)),new Point((int) Math.round(indexCol + .51), (int) Math.round(indexRow - .51)),  new Scalar(255-avgChannels[2],255-avgChannels[2],255-avgChannels[0]));
            double[] channels4 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow - .51), (int) Math.round(indexCol + .51));
            double[] channels5 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol + .51));
            double[] channels6 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol - .51));
            double[] channels7 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow - .51), (int) Math.round(indexCol));
            double[] channels8 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow + .51), (int) Math.round(indexCol));
            double[] channels9 = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol));

            for (int i = 0; i < medianChannels.length; i++) {
                double[] radiusValues = {channels1[i], channels2[i], channels3[i], channels4[i],
                        channels5[i], channels6[i], channels7[i], channels8[i], channels9[i]};
                Arrays.sort(radiusValues);
            Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol - .51), (int) Math.round(indexRow + .51)),new Point((int) Math.round(indexCol - .51), (int) Math.round(indexRow + .51)),  new Scalar(255-avgChannels[2],255-avgChannels[2],255-avgChannels[0]));
            for (int i = 0; i < medianChannels.length; i++) {
                if (channels1 == null || channels2 == null || channels3 == null || channels4 == null) {
                    Log.d("null", distortedIndex.x + "," + distortedIndex.y);
                }
                medianChannels[i] = radiusValues[4];
//                avgChannels[i] = (channels1[i] + channels2[i] + channels3[i] + channels4[i] + channels5[i] + channels6[i] +
//                channels7[i] + channels8[i] + channels9[i]) / NUM_OF_SAMPLES_FOR_RADIUS_SMAPLING;
            }
        }
        else{
            medianChannels = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol));
//            avgChannels = DistortedImageSampler.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol));
        }

        //Imgproc.circle(Flow.delete, distortedIndex, 1, new Scalar(0,0,255), 1);

          //  Imgproc.circle(Flow.delete, distortedIndex, 1, new Scalar(255-avgChannels[2],255-avgChannels[2],255-avgChannels[0]) , 1);

//        Mat balancedColors = new Mat();
//        Mat unbalancedColors = new Mat(1,3, CvType.CV_64F); unbalancedColors.put(0,0, avgChannels[0], avgChannels[1], avgChannels[2]);
//        Core.gemm(DistortedImageSampler.colorBalancingMat, unbalancedColors.t(), 1.0, new Mat(), 0, balancedColors, 0);
//        avgChannels[0] = balancedColors.get(0,0)[0]; avgChannels[1] = balancedColors.get(1,0)[0]; avgChannels[2] = balancedColors.get(2,0)[0];
//        int[] processedChannels = thresholdAndNormalizeChannels(avgChannels, minPixelVal, maxPixelVal, indexRow, indexCol);
        int[] processedChannels = classifyPixelChannelsLevels(medianChannels, indexRow, indexCol);

        // set all values to the majority
        if (duplicateChannels) {
            if (processedChannels[0] == processedChannels[1]) {
                processedChannels[2] = processedChannels[0];
            } else if (processedChannels[1] == processedChannels[2]) {
                processedChannels[0] = processedChannels[1];
            } else if (processedChannels[0] == processedChannels[2]) {
                processedChannels[1] = processedChannels[0];
            }
        }

        int pixelValue = (processedChannels[0]) |(processedChannels[1] << 8) | (processedChannels[2] << 16);

        // debugging code for comparison to original image
        if(radiusSample) {
            int rowPixel = (int) Math.round((Parameters.modulesInMargin + rowLoc / this.getModuleSize()) * Parameters.pixelsInModule);
            int colPixel = (int) Math.round((Parameters.modulesInMargin + colLoc / this.getModuleSize()) * Parameters.pixelsInModule);
            if(rowPixel >= 0 && rowPixel < this.tempOrigPixelMatrix.length && colPixel >=0 && colPixel < this.tempOrigPixelMatrix[0].length) {
                int encodedPixelValue = super.getPixel(colPixel, rowPixel);
                int RGB = Integer.reverseBytes(encodedPixelValue) >>> 8;
                int[] originalChannelVals = new int[Constants.CHANNELS];
                originalChannelVals[0] = RGB & 0xff;
                originalChannelVals[1] = (RGB >> 8) & 0xff;
                originalChannelVals[2] = (RGB >> 16) & 0xff;
                if (RGB != pixelValue) {
                    errCounterTotal++;
                    if(originalChannelVals[0] != processedChannels[0]){
                        errCounterRed++;
                    }
                    if(originalChannelVals[1] != processedChannels[1]){
                        errCounterGreen++;
                    }
                    if(originalChannelVals[2] != processedChannels[2]){
                        errCounterBlue++;
                    }

                    //            Mat alignmentBottomRightMat = new Mat(1, 3, CvType.CV_64F);
                    //            alignmentBottomRightMat.put(0, 0, alignmentBottomRight.x, alignmentBottomRight.y, 1);

                   //double[] middleVals = DistortedImageSampler.distortedImage.get((int) Math.round(indexCol), (int) Math.round(indexRow));
                   // Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Scalar(255-middleVals[0],255-middleVals[1],255-middleVals[2]));
                    Imgproc.rectangle(Flow.delete, new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Scalar(0,0,0));
                    //Log.d("DistortedImageSampler", "Module pixel value different than expected");
                }
            }
        }


        return pixelValue;
    }

}