package com.android.visualcrypto.openCvUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.core.util.Pair;

import com.android.visualcrypto.MainActivity;
import com.pc.configuration.Constants;
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
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import boofcv.abst.fiducial.QrCodePreciseDetector;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import static boofcv.android.ConvertBitmap.bitmapToGray;
import static com.android.visualcrypto.MainActivity.showAlert;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.calcDistance;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.thresholdAndNormalizeChannels;
import static com.android.visualcrypto.openCvUtils.OpenCvUtils.undistortedToDistortedIndexes;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class DistortedImageSampler extends StdImageSampler {
    static final int gridSplitSize = 1;
    private static final double[][][] minPixelVal = new double[gridSplitSize][gridSplitSize][Constants.CHANNELS];
    private static final double[][][] maxPixelVal = new double[gridSplitSize][gridSplitSize][Constants.CHANNELS];
    static int tileHeight;
    static int tileWidth;
    private static Mat distortedImage;
    private static Mat inverseH;
    private static Bitmap distortedBitmap;
    private static int d, row;
    public static int errCounter = 0;

    public static final Mat itaysCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat orisCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat itaysCamConfigDst = new Mat(1,5 ,CvType.CV_64F);
    public static final Mat orisCamConfigDst = new Mat(1,5 ,CvType.CV_64F);


    static {
        orisCamConfigDst.put(0,0,    2.37788419e-01, -1.05060973, -1.24226900e-03, -1.06977518e-03, 1.36988634);
        orisCamConfigMtx.put(0,0, 3.54906380e+03, 0D, 2.28702762e+03,   0D, 3.55130312e+03, 1.69588285e+03,    0D, 0D, 1.0D);

        //itaysCamConfigDst.put(0,0,   0.51539854, -0.00120593, 0.00206427, -0.69356642 ,0.24467169);
        itaysCamConfigDst.put(0,0,   0.24467169, -0.69356642, 0.00206427, -0.00120593 ,0.51539854);
        itaysCamConfigMtx.put(0,0, 3.14068354e+03, 0, 1.91008252e+03,              0, 3.14439719e+03, 1.59031581e+03,                0, 0, 1);
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

        Point[] pts = new Point[4]; //opencv Points

        List<QrCode> failures = detector.getFailures();
        if (failures.size() == 1) {
            QrCode boofCorners = failures.get(0);
            Polygon2D_F64 polygon = boofCorners.bounds;
            convertBoofToOpenPoints(polygon, pts);
        }

        List<QrCode> detections = detector.getDetections();
        if (detections.size() == 1) {
            QrCode boofCorners = detections.get(0);
            Polygon2D_F64 polygon = boofCorners.bounds;
            convertBoofToOpenPoints(polygon, pts);
        }

        List<PositionPatternNode> pointsQueue = detector.getDetectPositionPatterns().getPositionPatterns().toList();
        if (failures.size() != 1 && detections.size() != 1 && pointsQueue.size() == 3) {
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
            showAlert(context, "Couldn't detect QR position detectors");
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

        Rect roi = new Rect(new Point(xMin-10, yMin-10), new Point(xMax+10, yMax+10));
        DistortedImageSampler.distortedImage = new Mat(DistortedImageSampler.distortedImage ,roi);
        Log.d("performance", "roi took: " + (System.currentTimeMillis() - start));//performance
 //       Bitmap DELETE = MainActivity.convertMatToBitmap(DistortedImageSampler.distortedImage);


//        GrayU8 grayDELETE = bitmapToGray(DELETE, (GrayU8) null, null);
//        detector.process(grayDELETE);
//        List<QrCode> DELETEDETECTOR = detector.getFailures();
        start = System.currentTimeMillis(); //performance
        Mat bw = new Mat();
        cvtColor(DistortedImageSampler.distortedImage, bw, Imgproc.COLOR_BGR2GRAY);
        adaptiveThreshold(bw, bw, 255, ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 65);
        cvtColor(bw, bw, Imgproc.THRESH_BINARY);
       // Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/bw.jpg", bw);

        pts[0].x -= (xMin - 10); pts[1].x -= (xMin - 10); pts[2].x -= (xMin - 10); pts[3].x -= (xMin - 10);
        pts[0].y -= (yMin - 10); pts[1].y -= (yMin - 10); pts[2].y -= (yMin - 10); pts[3].y -= (yMin - 10);
        pts[2] = findCorner(bw, 0, false, false); //TODO: dynamically find what corner to find: pts[2].y < pts[1].y && pts[2].x < pts[3].x ?
        Log.d("performance", "bw cvtColors and findCorner took: " + (System.currentTimeMillis() - start));//performance
        MatOfPoint2f corners1 = new MatOfPoint2f(pts[0], pts[1], pts[2], pts[3]);
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(0, 1));

        Mat H = Calib3d.findHomography(corners1, corners2);
        Mat inverseH = H.inv();

        DistortedImageSampler.inverseH = inverseH;

        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

        Imgcodecs.imwrite(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/for_debug.jpg", DistortedImageSampler.distortedImage);

        start = System.currentTimeMillis(); // performance
        findMinMaxPixelVals(DistortedImageSampler.distortedImage);
        Log.d("performance", "findMinMaxPixelVals took: " + (System.currentTimeMillis() - start));
        Point rightLowerOfPts0 = new Point(failures.get(0).ppCorner.vertexes.data[2].x - xMin + 10, failures.get(0).ppCorner.vertexes.data[2].y - yMin + 10);
        //Point leftLowerOfPts0 = new Point(pointsQueue.get(0).square.vertexes.get(0).x, pointsQueue.get(0).square.vertexes.get(0).y);
        start = System.currentTimeMillis(); // performance
        double estimatedModuleSize = computeModuleSize(pts[0], rightLowerOfPts0, H, Math.sqrt(2 * 49));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        double normalizedEstimatedModuleSize = 1 / (Math.floor(1.0 / estimatedModuleSize));
        start = System.currentTimeMillis();
        Point alignmentBottomRight = OpenCvUtils.findAlignmentBottomRight(normalizedEstimatedModuleSize, minPixelStride, inverseH, DistortedImageSampler.distortedImage);
        //alignmentBottomRight = new Point(2362.5, 1691.5);
        Log.d("performance", "FindAlignmentBottomRight took: " + (System.currentTimeMillis() - start));
        Mat alignmentBottomRightMat = new Mat(1, 3, CvType.CV_64F);
        alignmentBottomRightMat.put(0, 0, alignmentBottomRight.x, alignmentBottomRight.y, 1);
        Point distortedPoint = undistortedToDistortedIndexes(alignmentBottomRightMat, inverseH);
        start = System.currentTimeMillis();
        this.setModuleSize(computeModuleSize(pts[0], distortedPoint, H, Math.sqrt(2 * 99 * 99)));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        int effectiveModulesInDim = (int) Math.round(1.0 / this.getModuleSize());
        this.setModulesInDim(effectiveModulesInDim);

        Log.d("ModulesInDim", "modules in dim: " + Float.toString(this.getModulesInDim()));
        Log.d("ModulesInDim", "left lower: " + rightLowerOfPts0.x + "," + rightLowerOfPts0.y);
        return 0;
    }

    private Point findCorner(Mat img, int val, boolean top, boolean left) {
        Pair p = scanDiagonalFromCorner(img.size(), top, left);
        while (img.get((int) p.first, (int) p.second)[0] != val)
            p = scanDiagonalFromCorner(img.size(), top, left);
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
        Mat upperPoint = new Mat(1, 3, CvType.CV_64F);

        upperPoint.put(0, 0, upperLeft.x);
        upperPoint.put(0, 1, upperLeft.y);
        upperPoint.put(0, 2, 1);


        Mat undistortedUpperPoint = new Mat();
        Core.gemm(H, upperPoint.t(), 1.0, new Mat(), 0, undistortedUpperPoint, 0);

        double zUpper = undistortedUpperPoint.get(2, 0)[0];
        double xUpper = undistortedUpperPoint.get(0, 0)[0] / zUpper;
        double yUpper = undistortedUpperPoint.get(1, 0)[0] / zUpper;


        upperPoint.put(0, 0, lowerRight.x);
        upperPoint.put(0, 1, lowerRight.y);
        undistortedUpperPoint = new Mat();
        Core.gemm(H, upperPoint.t(), 1.0, new Mat(), 0, undistortedUpperPoint, 0);
        double zLower = undistortedUpperPoint.get(2, 0)[0];
        double xLower = undistortedUpperPoint.get(0, 0)[0] / zLower;
        double yLower = undistortedUpperPoint.get(1, 0)[0] / zLower;

        return calcDistance(new Point(xLower, yLower), new Point(xUpper, yUpper)) / expectedModulesDistance;
    }

    private void findMinMaxPixelVals(Mat capturedImage) {
        this.tileHeight = capturedImage.height() / gridSplitSize;
        this.tileWidth = capturedImage.width() / gridSplitSize;
        Mat subMat;
        int histSize = 256;
        float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);
        boolean accumulate = false;
        int countR = 0, countG = 0, countB = 0;

        final int lowPercentileRed = (int) Math.floor(0.0*(tileWidth*tileHeight));
        final int highPercentileRed = (int) Math.floor(1*(tileWidth*tileHeight));

        final int lowPercentileGreen = (int) Math.floor(0.0*(tileWidth*tileHeight));
        final int highPercentileGreen = (int) Math.floor(1*(tileWidth*tileHeight));

        final int lowPercentileBlue = (int) Math.floor(0.0*(tileWidth*tileHeight));
        final int highPercentileBlue = (int) Math.floor(1*(tileWidth*tileHeight));

        int high, low, left, right;
        for(int i = 0; i < gridSplitSize; i++){
            for(int j = 0; j < gridSplitSize; j++){
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

                for (int pixelValue = 0; pixelValue < 256; pixelValue++){
                    countR += rHist.get(pixelValue, 0)[0];
                    countG += gHist.get(pixelValue, 0)[0];
                    countB += bHist.get(pixelValue, 0)[0];
                    if(minPixelVal[i][j][0] == 0 && countR >= lowPercentileRed){
                        minPixelVal[i][j][0] = pixelValue;
                    }
                    if(maxPixelVal[i][j][0] == 0 && countR >= highPercentileRed){
                        maxPixelVal[i][j][0] = pixelValue;
                    }
                    if(minPixelVal[i][j][1] == 0 && countG >= lowPercentileGreen){
                        minPixelVal[i][j][1] = pixelValue;
                    }
                    if(maxPixelVal[i][j][1] == 0 && countG >= highPercentileGreen){
                        maxPixelVal[i][j][1] = pixelValue;
                    }
                    if(minPixelVal[i][j][2] == 0 && countB >= lowPercentileBlue){
                        minPixelVal[i][j][2] = pixelValue;
                    }
                    if(maxPixelVal[i][j][2] == 0 && countB >= highPercentileBlue){
                        maxPixelVal[i][j][2] = pixelValue;
                    }
                }
            }
        }


//        int lowPercentileRed = (int) Math.floor(0.05*(capturedImage.width()*capturedImage.height()));
//        int highPercentileRed = (int) Math.floor(0.5*(capturedImage.width()*capturedImage.height()));
//
//        int lowPercentileGreen = (int) Math.floor(0.03*(capturedImage.width()*capturedImage.height()));
//        int highPercentileGreen = (int) Math.floor(0.5*(capturedImage.width()*capturedImage.height()));
//
//        int lowPercentileBlue = (int) Math.floor(0.05*(capturedImage.width()*capturedImage.height()));
//        int highPercentileBlue = (int) Math.floor(0.5*(capturedImage.width()*capturedImage.height()));




    }

    private void convertBoofToOpenPoints(Polygon2D_F64 polygon, Point[] pts) {
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
    public int getPixel(double rowLoc, double colLoc, boolean duplicateChannels) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, rowLoc);
        unDistortedImageMatCord.put(0, 1, colLoc);
        unDistortedImageMatCord.put(0, 2, 1);
        Point distortedIndex = undistortedToDistortedIndexes(unDistortedImageMatCord, inverseH);
        int indexCol = (int) distortedIndex.x; int indexRow = (int) distortedIndex.y;
        double[] channels = DistortedImageSampler.distortedImage.get(indexRow, indexCol);
        int[] processedChannels = thresholdAndNormalizeChannels(channels, minPixelVal, maxPixelVal, indexRow, indexCol);

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

/*
        // debugging code for comparison to original image
        int rowPixel = (int) Math.round((Parameters.modulesInMargin + rowLoc/this.getModuleSize()) * Parameters.pixelsInModule);
        int colPixel = (int) Math.round((Parameters.modulesInMargin + colLoc/this.getModuleSize()) * Parameters.pixelsInModule);
        int encodedPixelValue = super.getPixel(colPixel, rowPixel);
        int GBR = Integer.reverseBytes(encodedPixelValue) >>> 8;
        if(GBR != pixelValue){
            errCounter++;
//            Mat alignmentBottomRightMat = new Mat(1, 3, CvType.CV_64F);
//            alignmentBottomRightMat.put(0, 0, alignmentBottomRight.x, alignmentBottomRight.y, 1);
            Point distortedPoint = OpenCvUtils.undistortedToDistortedIndexes(unDistortedImageMatCord, inverseH);
            Log.d("DistortedImageSampler", "Module pixel value different than expected");
        }

*/

        return pixelValue;
    }

//    private Point getCornerFromCenter(Point pt, Mat inverseH, double stride, boolean left, boolean upper) {
//        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
//        unDistortedImageMatCord.put(0, 2, 1);
//        if (left) {
//            unDistortedImageMatCord.put(0, 0, -3.5 * stride);
//        } else {
//            unDistortedImageMatCord.put(0, 0, 3.5 * stride);
//        }
//
//        if (upper) {
//            unDistortedImageMatCord.put(0, 1, -3.5 * stride);
//        } else {
//            unDistortedImageMatCord.put(0, 1, 3.5 * stride);
//        }
//
//        Mat distortedImageMatCord = new Mat();
//        Core.gemm(inverseH, unDistortedImageMatCord.t(), 1.0, new Mat(), 0, distortedImageMatCord, 0);
//        double x = distortedImageMatCord.get(0, 0)[0];
//        double y = distortedImageMatCord.get(1, 0)[0];
//        double z = distortedImageMatCord.get(2, 0)[0];
//        x = x / z;
//        y = y / z;
//        z = z / z;
//
//        int indexRow = (int) (Math.round(x));
//        int indexCol = (int) (Math.round(y));
//
//        return new Point(indexRow, indexCol);
//    }

//    public boolean detect(Mat img){
//        Mat imgBW = new Mat();
//        cvtColor(img, imgBW, Imgproc.COLOR_BGR2GRAY);
//        adaptiveThreshold(imgBW, imgBW, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C , Imgproc.THRESH_BINARY, 51, 0);
//        Bitmap bp = MainActivity.convertMatToBitmap(imgBW);
//
//        boolean found = find(imgBW);
//        double x3 = this.possibleCenters.toArray()[2].x +
//                (this.possibleCenters.toArray()[1].x - this.possibleCenters.toArray()[0].x);
//        double y3 = this.possibleCenters.toArray()[2].y +
//                (this.possibleCenters.toArray()[1].y - this.possibleCenters.toArray()[0].y);
//        List<Point> temp = possibleCenters.toList();
//        List<Point> centers = new ArrayList<>();
//
//        centers.add(temp.get(0));centers.add(temp.get(1)); centers.add(new Point(x3, y3)); centers.add(temp.get(2));
//        //possibleCenters.push_back(new MatOfPoint2f(new Point(x3, y3)));
//        possibleCenters.fromList(centers);
//        return found;
//    }
//
//    private boolean find(Mat img) {
//
//        int skipRows = 3;
//
//        int stateCount[] = new int[5];
//        int currentState = 0;
//        for(int row=skipRows-1; row<img.height(); row+=skipRows){
//            stateCount[0] = 0;
//            stateCount[1] = 0;
//            stateCount[2] = 0;
//            stateCount[3] = 0;
//            stateCount[4] = 0;
//            currentState = 0;
////            Mat a = img.row(row);
////            List<Integer> b = new ArrayList<>();
////            int[] c = b.stream().mapToInt(i->i).toArray();
////            Converters.Mat_to_vector_int(a,b);
//            //const uchar* ptr = img.ptr<uchar>(row);
//            for(int col=0; col<img.width(); col++) {
//                double a[] = img.get(row,col);
//                if(a[0]<128) // ptr[col]
//                {
//                    // We're at a black pixel
//                    if((currentState & 0x1)==1)
//                    {
//                        // We were counting white pixels
//                        // So change the state now
//
//                        // W->B transition
//                        currentState++;
//                    }
//
//                    // Works for boths W->B and B->B
//                    stateCount[currentState]++;
//                }
//                else
//                {
//                    // We got to a white pixel...
//                    if((currentState & 0x1)==1)
//                    {
//                        // W->W change
//                        stateCount[currentState]++;
//                    }
//                    else {
//                        // ...but, we were counting black pixels
//                        if (currentState == 4) {
//                            // We found the 'white' area AFTER the finder patter
//                            // Do processing for it here
//                            if(checkRatio(stateCount))
//                            {
//                                boolean confirmed = handlePossibleCenter(img, stateCount, row, col);
//                            }
//                            else
//                            {
//                                currentState = 3;
//                                stateCount[0] = stateCount[2];
//                                stateCount[1] = stateCount[3];
//                                stateCount[2] = stateCount[4];
//                                stateCount[3] = 1;
//                                stateCount[4] = 0;
//                                continue;
//                            }
//                            currentState = 0;
//                            stateCount[0] = 0;
//                            stateCount[1] = 0;
//                            stateCount[2] = 0;
//                            stateCount[3] = 0;
//                            stateCount[4] = 0;
//                        }
//                        else
//                        {
//                            // We still haven't go 'out' of the finder pattern yet
//                            // So increment the state
//                            // B->W transition
//                            currentState++;
//                            stateCount[currentState]++;
//                        }
//                    }
//                }
//            }
//        }
//        return (possibleCenters.width()>0);//was possibleCenters.size()
//    }
//
//    private boolean handlePossibleCenter(Mat img, int[] stateCount, int row, int col) {
//        int stateCountTotal = 0;
//        for(int i=0;i<5;i++) {
//            stateCountTotal += stateCount[i];
//        }
//
//        // Cross check along the vertical axis
//        float centerCol = centerFromEnd(stateCount, col);
//        float centerRow = crossCheckVertical(img, row, (int)centerCol, stateCount[2], stateCountTotal);
//        if(Float.isNaN(centerRow)) {
//            return false;
//        }
//
//        // Cross check along the horizontal axis with the new center-row
//        centerCol = crossCheckHorizontal(img, (int) centerRow, (int) centerCol, stateCount[2], stateCountTotal);
//        if(Float.isNaN(centerCol)) {
//            return false;
//        }
//
//        // Cross check along the diagonal with the new center row and col
//        boolean validPattern = crossCheckDiagonal(img, centerRow, centerCol, stateCount[2], stateCountTotal);
//        if(!validPattern) {
//            return false;
//        }
//
//
//        Point ptNew = new Point(centerCol, centerRow); // was: Point2f ptNew(centerCol, centerRow);
//        double newEstimatedModuleSize = stateCountTotal / 7.0D;
//        boolean found = false;
//        int idx = 0;
//
//        // Definitely a finder pattern - but have we seen it before?
//        for (int i = 0 ; i < possibleCenters.toArray().length; i++){
//            Point pt = possibleCenters.toArray()[i];
////            MatOfPoint2f diff = pt - ptNew;
////            float dist = (float)Math.sqrt((diff.dot(diff));
//            double dist = calcDistance(pt, ptNew);
//
//            // If the distance between two centers is less than 10px, they're the same.
//            if(dist < 10) {
//                //pt = pt + ptNew;
//                pt.x = (pt.x + ptNew.x) / 2.0f;
//                pt.y = (pt.y + ptNew.y) / 2.0f;
//                estimatedModuleSize.set(idx, (estimatedModuleSize.get(idx) + newEstimatedModuleSize)/2.0f);
//                found = true;
//                break;
//            }
//            idx++;
//        }
//        if(!found) {
//            MatOfPoint2f a = new MatOfPoint2f(ptNew);
//            possibleCenters.push_back(a);
//            estimatedModuleSize.add(newEstimatedModuleSize);
//            //estimatedModuleSize.push_back(newEstimatedModuleSize);
//        }
//
//        return false;
//    }
//
//    private boolean crossCheckDiagonal(Mat img, float centerRow, float centerCol, int maxCount, int stateCountTotal) {
//
//        int stateCount[] = new int[5];
//
//        int i=0;
//        while(centerRow>=i && centerCol>=i && img.get((int)centerRow-i, (int)centerCol-i)[0]<128) {
//            stateCount[2]++;
//            i++;
//        }
//        if(centerRow<i || centerCol<i) {
//            return false;
//        }
//
//        while(centerRow>=i && centerCol>=i && img.get((int)centerRow-i, (int)centerCol-i)[0]>=128 && stateCount[1]<=maxCount) {
//            stateCount[1]++;
//            i++;
//        }
//        if(centerRow<i || centerCol<i || stateCount[1]>maxCount) {
//            return false;
//        }
//
//        while(centerRow>=i && centerCol>=i && img.get((int)centerRow-i, (int)centerCol-i)[0]<128 && stateCount[0]<=maxCount) {
//            stateCount[0]++;
//            i++;
//        }
//        if(stateCount[0]>maxCount) {
//            return false;
//        }
//
//        int maxCols = img.width();
//        int maxRows = img.height();
//        i=1;
//        while((centerRow+i)<maxRows && (centerCol+i)<maxCols && img.get((int)centerRow+i, (int)centerCol+i)[0]<128) {
//            stateCount[2]++;
//            i++;
//        }
//        if((centerRow+i)>=maxRows || (centerCol+i)>=maxCols) {
//            return false;
//        }
//
//        while((centerRow+i)<maxRows && (centerCol+i)<maxCols && img.get((int)centerRow+i, (int)centerCol+i)[0]>=128 && stateCount[3]<maxCount) {
//            stateCount[3]++;
//            i++;
//        }
//        if((centerRow+i)>=maxRows || (centerCol+i)>=maxCols || stateCount[3]>maxCount) {
//            return false;
//        }
//
//        while((centerRow+i)<maxRows && (centerCol+i)<maxCols && img.get((int)centerRow+i, (int)centerCol+i)[0]<128 && stateCount[4]<maxCount) {
//            stateCount[4]++;
//            i++;
//        }
//        if((centerRow+i)>=maxRows || (centerCol+i)>=maxCols || stateCount[4]>maxCount) {
//            return false;
//        }
//
//        int newStateCountTotal = 0;
//        for(int j=0;j<5;j++) {
//            newStateCountTotal += stateCount[j];
//        }
//
//        return (Math.abs(stateCountTotal - newStateCountTotal) < 2*stateCountTotal) && checkRatio(stateCount);
//    }
//
//    private float crossCheckHorizontal(Mat img, int centerRow, int startCol, int centerCount, int stateCountTotal) {
//        int maxCols = img.width();
//        int stateCount[] = new int[5];
//
//        int col = startCol;
//        //const uchar* ptr = img.ptr<uchar>(centerRow);
//        while(col>=0 && img.get(centerRow ,col)[0]<128) {
//        //while(col>=0 && ptr[col]<128) {
//            stateCount[2]++;
//            col--;
//        }
//        if(col<0) {
//            return Float.NaN;
//        }
//
//        while(col>=0 && img.get(centerRow ,col)[0]>=128 && stateCount[1]<centerCount) {
//            stateCount[1]++;
//            col--;
//        }
//        if(col<0 || stateCount[1]==centerCount) {
//            return Float.NaN;
//        }
//
//        while(col>=0 && img.get(centerRow ,col)[0]<128 && stateCount[0]<centerCount) {
//            stateCount[0]++;
//            col--;
//        }
//        if(col<0 || stateCount[0]==centerCount) {
//            return Float.NaN;
//        }
//
//        col = startCol + 1;
//        while(col<maxCols && img.get(centerRow ,col)[0]<128) {
//            stateCount[2]++;
//            col++;
//        }
//        if(col==maxCols) {
//            return Float.NaN;
//        }
//
//        while(col<maxCols && img.get(centerRow ,col)[0]>=128 && stateCount[3]<centerCount) {
//            stateCount[3]++;
//            col++;
//        }
//        if(col==maxCols || stateCount[3]==centerCount) {
//            return Float.NaN;
//        }
//
//        while(col<maxCols && img.get(centerRow ,col)[0]<128 && stateCount[4]<centerCount) {
//            stateCount[4]++;
//            col++;
//        }
//        if(col==maxCols || stateCount[4]==centerCount) {
//            return Float.NaN;
//        }
//
//        int newStateCountTotal = 0;
//        for(int i=0;i<5;i++) {
//            newStateCountTotal += stateCount[i];
//        }
//
//        if(5*Math.abs(stateCountTotal-newStateCountTotal) >= stateCountTotal) {
//            return Float.NaN;
//        }
//
//        return checkRatio(stateCount)?centerFromEnd(stateCount, col):Float.NaN;
//    }
//
//
//    private float crossCheckVertical(Mat img, int startRow, int centerCol, int centralCount, int stateCountTotal) {
//        int maxRows = img.height();
//        int crossCheckStateCount[] = new int[5];
//        int row = startRow;
//        while(row>=0 && img.get(row, centerCol)[0]<128) {
//            crossCheckStateCount[2]++;
//            row--;
//        }
//        if(row<0) {
//            return Float.NaN;// #define nan std::numeric_limits<float>::quiet_NaN();
//        }
//
//
//        while(row>=0 && img.get(row, centerCol)[0]>=128 && crossCheckStateCount[1]<centralCount) {
//            crossCheckStateCount[1]++;
//            row--;
//        }
//        if(row<0 || crossCheckStateCount[1]>=centralCount) {
//            return Float.NaN;
//        }
//
//        while(row>=0 && img.get(row, centerCol)[0]<128 && crossCheckStateCount[0]<centralCount) {
//            crossCheckStateCount[0]++;
//            row--;
//        }
//        if(row<0 || crossCheckStateCount[0]>=centralCount) {
//            return Float.NaN;
//        }
//
//        // Now we traverse down the center
//        row = startRow+1;
//        while(row<maxRows && img.get(row, centerCol)[0]<128) {
//            crossCheckStateCount[2]++;
//            row++;
//        }
//        if(row==maxRows) {
//            return Float.NaN;
//        }
//
//        while(row<maxRows && img.get(row, centerCol)[0]>=128 && crossCheckStateCount[3]<centralCount) {
//            crossCheckStateCount[3]++;
//            row++;
//        }
//        if(row==maxRows || crossCheckStateCount[3]>=stateCountTotal) {
//            return Float.NaN;
//        }
//
//        while(row<maxRows && img.get(row, centerCol)[0]<128 && crossCheckStateCount[4]<centralCount) {
//            crossCheckStateCount[4]++;
//            row++;
//        }
//        if(row==maxRows || crossCheckStateCount[4]>=centralCount) {
//            return Float.NaN;
//        }
//
//        int crossCheckStateCountTotal = 0;
//        for(int i=0;i<5;i++) {
//            crossCheckStateCountTotal += crossCheckStateCount[i];
//        }
//
//        if(5*Math.abs(crossCheckStateCountTotal-stateCountTotal) >= 2*stateCountTotal) {
//            return Float.NaN;
//        }
//
//        float center = centerFromEnd(crossCheckStateCount, row);
//        return checkRatio(crossCheckStateCount)?center:Float.NaN;
//    }
//
//    private float centerFromEnd(int[] stateCount, int end) {
//        return (float)(end-stateCount[4]-stateCount[3])-(float)stateCount[2]/2.0f;
//    }
//
//    private boolean checkRatio(int[] stateCount) {
//        int totalFinderSize = 0;
//        for(int i=0; i<5; i++)
//        {
//            int count = stateCount[i];
//            totalFinderSize += count;
//            if(count==0)
//                return false;
//        }
//
//        if(totalFinderSize<7)
//            return false;
//
//        // Calculate the size of one module
//        int moduleSize = (int) Math.ceil(totalFinderSize / 7.0);
//        int maxVariance = moduleSize/2;
//
//        boolean retVal= ((Math.abs(moduleSize - (stateCount[0])) < maxVariance) &&
//                (Math.abs(moduleSize - (stateCount[1])) < maxVariance) &&
//                (Math.abs(3*moduleSize - (stateCount[2])) < 3*maxVariance) &&
//                (Math.abs(moduleSize - (stateCount[3])) < maxVariance) &&
//                (Math.abs(moduleSize - (stateCount[4])) < maxVariance));
//
//        return retVal;
//    }
}