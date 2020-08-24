package com.android.visualcrypto.openCvUtils;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.util.Pair;

import com.android.visualcrypto.MainActivity;
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
import java.nio.file.Path;
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

/**
 * A class that handles the image processing
 */
public class DistortedImageSampler extends StdImageSampler {
    private static final int gridSplitSize = 1;

    public final List<List<Map<Integer,Integer>>> percentileValuesMapTilesRed = new ArrayList<>(gridSplitSize);
    public final List<List<Map<Integer,Integer>>> percentileValuesMapTilesGreen = new ArrayList<>(gridSplitSize);
    public final List<List<Map<Integer,Integer>>> percentileValuesMapTilesBlue = new ArrayList<>(gridSplitSize);

    int tileHeight;
    int tileWidth;
    private Mat distortedImage;
    private Mat inverseH;
    private Bitmap distortedBitmap;
    private int d, row;
    public int errCounterTotal = 0;
    public int errCounterRed = 0;
    public int errCounterGreen = 0;
    public int errCounterBlue = 0;

    public Mat debugBW;
    public Mat debugPathtaken;
    public Path DEBUG_FOLDER;

    public static final Mat itaysCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat orisCamConfigMtx = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat itaysCamConfigDst = new Mat(1,5 ,CvType.CV_64F);
    public static final Mat orisCamConfigDst = new Mat(1,5 ,CvType.CV_64F);

    public static final Mat itaysCamConfigMtxVideo = new Mat(3,3 ,CvType.CV_64F);
    public static final Mat itaysCamConfigDstVideo = new Mat(1,5 ,CvType.CV_64F);

    static {
        orisCamConfigDst.put(0,0,    1.88976465e-01, -8.27046848e-01, -6.61431273e-04,  4.78026017e-04, 1.01532664e+00);
        orisCamConfigMtx.put(0,0, 3.57877704e+03, 0D,1.73734294e+03,   0D, 3.58348580e+03, 2.30337522e+03,    0D, 0D, 1.0D);
        itaysCamConfigDst.put(0,0, 1.12401533e-01, -5.59376588e-01, -4.51872138e-04, -5.01959503e-04, 8.00293967e-01);
        itaysCamConfigMtx.put(0,0, 3.17223621e+03, 0, 2.08430378e+03,  0, 3.16876503e+03, 1.46472194e+03, 0, 0, 1D);
        itaysCamConfigDstVideo.put(0,0, 2.97261556e+03, 0, 1.85805949e+03, 0, 2.97191363e+03, 1.10037754e+03, 0, 0, 1);
        itaysCamConfigMtxVideo.put(0,0,   3.75570453e-01, -1.45061390e+00, -9.40469522e-04, 7.57586055e-04 , 1.59684301e+00);
    }


    public DistortedImageSampler(Mat distortedImage, Bitmap distortedBitmap) {
        this.distortedImage = distortedImage;
        this.distortedBitmap = distortedBitmap;
    }

    /**
     * This is where the image processing takes place - finding ROI and alignment, modules in dim etc
     * @return error number
     */
    public int initParameters() {
        Log.d("performance", "start of initParameters");
        this.setModulesInMargin(0);
        long start = System.currentTimeMillis();

        GrayU8 gray = bitmapToGray(this.distortedBitmap, (GrayU8) null, null);
        Log.d("performance", "bitmapToGray(milli): " + (System.currentTimeMillis() - start));

        ConfigQrCode config = new ConfigQrCode();
        QrCodePreciseDetector<GrayU8> detector = FactoryFiducial.qrcode(config, GrayU8.class);
        start = System.currentTimeMillis();
        detector.process(gray);
        Log.d("performance", "Boof searching for corners(milli): " + (System.currentTimeMillis() - start));

        QrCode boofCorners = null;
        Point[] pts = new Point[4]; //opencv Points
        Point2D_F64[] boofPts;
        Point2D_F64 rightLowerPos0 = null;

        List<QrCode> failures = detector.getFailures();
        if (failures.size() >= 1) {
            boofCorners = failures.get(0);
            convertBoofToOpenPoints(boofCorners, pts);
            rightLowerPos0 = failures.get(0).ppCorner.vertexes.data[2];
        }

        List<QrCode> detections = detector.getDetections();
        if (detections.size() >= 1) {
            boofCorners = detections.get(0);
            convertBoofToOpenPoints(boofCorners, pts);
        }

        List<PositionPatternNode> pointsQueue = detector.getDetectPositionPatterns().getPositionPatterns().toList();
        if (failures.size() == 0 && detections.size() == 0) {
            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
            MainActivity.lastDetectedRoi = null;
            return 1;
        } else if (pointsQueue.size() == 3 && failures.size() == 0) {
            Point2D_F64 boofPt0 = pointsQueue.get(0).square.get(3); // PT0         PT1
            //Point2D_F64 boofPt0 = pointsQueue.get(0).square.get(2);
            Point2D_F64 boofPt1 = pointsQueue.get(1).square.get(0); //PT3          PT2
            //Point2D_F64 boofPt1 = pointsQueue.get(1).square.get(3);
            Point2D_F64 boofPt3 = pointsQueue.get(2).square.get(2);
            //Point2D_F64 boofPt3 = pointsQueue.get(2).square.get(1);

            boofPts = new Point2D_F64[] {boofPt0,boofPt1,boofPt3};
            Arrays.sort(boofPts, (b1,b2)-> Integer.compare((int)(b1.x + b1.y), (int)(b2.x + b2.y)));
            if (boofPts[0] == boofPt0) {
                Point2D_F64[] qr0Pos =  {pointsQueue.get(0).square.get(0),pointsQueue.get(0).square.get(1), pointsQueue.get(0).square.get(2)};
                Arrays.sort(qr0Pos, (b1,b2)-> Integer.compare((int)(b1.x + b1.y), (int)(b2.x + b2.y)));
                rightLowerPos0 = qr0Pos[0];
            } else if (boofPts[0] == boofPt1) {
                Point2D_F64[] qr0Pos =  {pointsQueue.get(1).square.get(3),pointsQueue.get(1).square.get(1), pointsQueue.get(1).square.get(2)};
                Arrays.sort(qr0Pos, (b1,b2)-> Integer.compare((int)(b1.x + b1.y), (int)(b2.x + b2.y)));
                rightLowerPos0 = qr0Pos[0];
            } else {
                Point2D_F64[] qr0Pos =  {pointsQueue.get(2).square.get(0),pointsQueue.get(2).square.get(1), pointsQueue.get(2).square.get(3)};
                Arrays.sort(qr0Pos, (b1,b2)-> Integer.compare((int)(b1.x + b1.y), (int)(b2.x + b2.y)));
                rightLowerPos0 = qr0Pos[0];
            }
            
            pts[0] = new Point(boofPts[0].x, boofPts[0].y);
            if (boofPts[1].y > boofPts[2].y) {
                pts[1] = new Point(boofPts[2].x, boofPts[2].y);
                pts[3] = new Point(boofPts[1].x, boofPts[1].y);
            } else {
                pts[1] = new Point(boofPts[1].x, boofPts[1].y);
                pts[3] = new Point(boofPts[2].x, boofPts[2].y);
            }

            double x2 = pts[3].x + Math.abs(pts[1].x - pts[0].x);
            double y2 = pts[3].y + Math.abs(pts[1].y - pts[0].y);
            pts[2] = new Point(x2, y2);
        }

        double[] xValues = new double[] {pts[0].x, pts[1].x, pts[2].x, pts[3].x};
        Arrays.sort(xValues);
        int xMin = (int) Math.floor(xValues[0]);
        int xMax = (int) Math.ceil(xValues[3]);

        double[] yValues = new double[] {pts[0].y, pts[1].y, pts[2].y, pts[3].y};
        Arrays.sort(yValues);
        int yMin = (int) Math.floor(yValues[0]);
        int yMax = (int) Math.ceil(yValues[3]);

        start = System.currentTimeMillis();// performance
        /********************ROI****************************/
        int cutValue = 20;
        try {
            Rect roi = new Rect(new Point(Math.max(0,xMin-cutValue), Math.max(0,yMin-cutValue)), new Point(xMax+cutValue, yMax+cutValue));
            this.distortedImage = new Mat(this.distortedImage, roi);
            Log.d("performance", "roi took: " + (System.currentTimeMillis() - start));//performance
        } catch (Exception e) {
            Log.d("roi", "roi threw exception");
            MainActivity.lastDetectedRoi = null;
            return 2;
        }
        /***************************************************/

        start = System.currentTimeMillis(); //performance
        Mat bw = new Mat(this.distortedImage.size(), CvType.CV_8UC4);
        cvtColor(this.distortedImage, bw, Imgproc.COLOR_BGR2GRAY);
        adaptiveThreshold(bw, bw, 255, ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 45);
        cvtColor(bw, bw, Imgproc.THRESH_BINARY);

        pts[0].x -= (xMin - cutValue); pts[1].x -= (xMin - cutValue); pts[2].x -= (xMin - cutValue); pts[3].x -= (xMin - cutValue);
        pts[0].y -= (yMin - cutValue); pts[1].y -= (yMin - cutValue); pts[2].y -= (yMin - cutValue); pts[3].y -= (yMin - cutValue);
        if (MainActivity.DEBUG) {
            this.debugBW = bw.clone();
        }

        pts[2] = findCorner(bw, 0, false, false);
        if (MainActivity.DEBUG) {
            Imgcodecs.imwrite(this.DEBUG_FOLDER + "/debugBW.jpg", this.debugBW);
        }

        Log.d("performance", "bw cvtColors and findCorner took: " + (System.currentTimeMillis() - start));//performance
        MatOfPoint2f corners1 = new MatOfPoint2f(pts[0], pts[1], pts[2], pts[3]);
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(0, 1));

        Mat H = Calib3d.findHomography(corners1, corners2);
        Mat inverseH = H.inv();
        this.inverseH = inverseH;

        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

        if (MainActivity.DEBUG) {
            try {
                FileWriter fw = new FileWriter(this.DEBUG_FOLDER + "/parameters.txt");
                Imgcodecs.imwrite(this.DEBUG_FOLDER + "/bw.jpg", bw);
                Imgcodecs.imwrite(this.DEBUG_FOLDER + "/afterRoi.jpg", this.distortedImage);
                String upperRowPts = String.format("leftUpperPoint: %f,%f\t\trightUpperPoint: %f,%f\n", pts[0].x,pts[0].y, pts[1].x, pts[1].y);
                String lowerRowPts = String.format("leftLowerrPoint: %f,%f\t\trightLowerPoint: %f,%f", pts[3].x,pts[3].y, pts[2].x, pts[2].y);
                fw.write(upperRowPts);
                fw.write(lowerRowPts);
                fw.close();
            }catch(Exception e){
                Log.d("writing_file", e.getMessage());
            }
        }

        start = System.currentTimeMillis(); // performance
        Rect roi = new Rect(new Point(pts[0].x+150, pts[0].y+150), new Point(pts[2].x-100, pts[2].y-100));
        Mat croppedMatForHisto;
        try {
            croppedMatForHisto = new Mat(this.distortedImage, roi);
        } catch (Exception e) {
            Log.d("croppedMatForHisot", "threw an exception: " + e.getCause());
            return 2;
        }

        Log.d("performance", "cropped for histo(milli): " + (System.currentTimeMillis() - start));

        if (MainActivity.DEBUG) {
            Imgcodecs.imwrite(this.DEBUG_FOLDER + "/for_histo.jpg", croppedMatForHisto);
        }


        start = System.currentTimeMillis();
        this.tileHeight = distortedImage.height() / gridSplitSize;
        this.tileWidth = distortedImage.width() / gridSplitSize;
        findPercentilesValues(croppedMatForHisto);
        Log.d("performance", "findPercentilesValues(milli): " + (System.currentTimeMillis() - start));
        Point rightLowerOfPts0 = new Point(rightLowerPos0.x - xMin + cutValue,
                rightLowerPos0.y - yMin + cutValue);

        start = System.currentTimeMillis(); // performance
        double estimatedModuleSize = computeModuleSize(pts[0], rightLowerOfPts0, H, Math.sqrt(2 * 49));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        double normalizedEstimatedModuleSize = 1 / (Math.floor(1.0 / estimatedModuleSize));

        start = System.currentTimeMillis();
        if (MainActivity.DEBUG) {
            this.debugPathtaken = distortedImage.clone();
        }

        Point alignmentBottomRight = OpenCvUtils.findAlignmentBottomRight(
                this, normalizedEstimatedModuleSize, minPixelStride, inverseH, this.distortedImage);
        if (MainActivity.DEBUG) {
            Imgcodecs.imwrite(this.DEBUG_FOLDER + "/pathTaken.jpg", this.debugPathtaken);
        }

        if (alignmentBottomRight == null) {
            Log.d("DistortedImageSampler", "findAlignmentBottomRight returned null");
            return 3;
        }
        Log.d("performance", "FindAlignmentBottomRight took: " + (System.currentTimeMillis() - start));
        Mat alignmentBottomRightMat = new Mat(1, 3, CvType.CV_64F);
        alignmentBottomRightMat.put(0, 0, alignmentBottomRight.x, alignmentBottomRight.y, 1);
        Point distortedPoint = switchCoordinates(alignmentBottomRightMat, inverseH);

        start = System.currentTimeMillis();
        this.setModuleSize(computeModuleSize(pts[0], distortedPoint, H, Math.sqrt(2 *
                (Constants.MODULES_FROM_UPPER_LEFT_TO_ALIGNMENT_BOTTOM_RIGHT-1) * (Constants.MODULES_FROM_UPPER_LEFT_TO_ALIGNMENT_BOTTOM_RIGHT-1))));
        Log.d("performance", "computeModuleSize took: " + (System.currentTimeMillis() - start));
        int effectiveModulesInDim = (int) Math.round(1.0 / this.getModuleSize());

        this.setModulesInDim(effectiveModulesInDim);

        Log.d("ModulesInDim", "modules in dim: " + Float.toString(this.getModulesInDim()));
        Log.d("performance", "end of initParameters");
        return 0;
    }

    /**
     * Finds QR corner, is used for the lower-right corner since BoofCV doesn't find it correctly
     * @param img - The image in black and white
     * @param val - 0 black, 1 white
     * @param top - Whether this is a top corner
     * @param left - Whether this is left or right corner
     * @return the coordinate of the corner
     */
    private Point findCorner(Mat img, int val, boolean top, boolean left) {
        d = 0;
        Pair p = scanDiagonalFromCorner(img.size(), top, left);
        while (img.get((int) p.first, (int) p.second)[0] != val) {
            Point debugBWPoint = new Point((int) p.second, (int) p.first);
            if (MainActivity.DEBUG) {
                Imgproc.rectangle(this.debugBW, debugBWPoint,debugBWPoint, new Scalar(155,155,155));
            }

            p = scanDiagonalFromCorner(img.size(), top, left);
        }
        return new Point((int) p.second, (int) p.first);
    }

    /**
     * Utility for findCorner: scans the image
     */
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

    /**
     * Utility for findCorner: scans the image in a diagonal way
     */
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

    /**
     * Compture the module size by calculating the distance between upper-left and lower-right QR corner
     * @param upperLeft - The upper-left point
     * @param lowerRight - The lower-right point
     * @param H - The homography matrix
     * @param expectedModulesDistance - The expected module distane between @upperLeft and @lowerRight
     * @return the computed module size
     */
    private double computeModuleSize(Point upperLeft, Point lowerRight, Mat H, double expectedModulesDistance) {
        Mat mat = new Mat(1, 3, CvType.CV_64F);
        mat.put(0, 0, upperLeft.x,  upperLeft.y, 1);
        Point upperLeftPt = switchCoordinates(mat, H);

        mat.put(0, 0, lowerRight.x, lowerRight.y);

        Point lowerPt = switchCoordinates(mat, H);
        return calcDistance(upperLeftPt, lowerPt) / expectedModulesDistance;
    }

    /**
     * This function is used for finding the percentiles value for color balancing. It calculates
     * histogram for each RGB channel, and finds the percentiles according to the encoding color levels
     * @param capturedImage - The captured image matrix
     */
    private void findPercentilesValues(Mat capturedImage) {
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
                        currMapRed.put((int) Math.round(diffBetweenLevels*currPercentileIndexRed), pixelValue);
                        currPercentileIndexRed++;
                    }
                    if (currPercentileIndexGreen < Parameters.encodingColorLevels && percentilesPixelCounts[currPercentileIndexGreen] <= countG){
                       currMapGreen.put((int) Math.round(diffBetweenLevels*currPercentileIndexGreen), pixelValue);
                        currPercentileIndexGreen++;
                    }
                    if (currPercentileIndexBlue < Parameters.encodingColorLevels && percentilesPixelCounts[currPercentileIndexBlue] <= countB){
                        currMapBlue.put((int) Math.round(diffBetweenLevels*currPercentileIndexBlue), pixelValue);
                        currPercentileIndexBlue++;
                    }
                }
            }
        }
    }

    /**
     * Convert BoofCV points to OpenCV points
     * @param qrCode - BoofCV points
     * @param pts - Assigned OpenCV points
     */
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

    /**
     * This function sample a pixel (or radius sample, aka 9 samples), color balance it and classify
     * it to the appropriate value
     * @param rowLoc - The row location
     * @param colLoc - The column location
     * @param duplicateChannels - Whether all the data in the RGB channels suppose to be equal
     * @param radiusSample - Whether to radius sample
     * @return the pixel value after all the logic
     */
    @Override
    public int getPixel(double rowLoc, double colLoc, boolean duplicateChannels, boolean radiusSample) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, rowLoc);
        unDistortedImageMatCord.put(0, 1, colLoc);
        unDistortedImageMatCord.put(0, 2, 1);
        Point distortedIndex = switchCoordinates(unDistortedImageMatCord, inverseH);
        if(MainActivity.DEBUG)
            Imgproc.circle(this.debugPathtaken, distortedIndex, 1, new Scalar(0,0,255), 1);
        int indexCol = (int) distortedIndex.x; int indexRow = (int) distortedIndex.y;
        final int NUM_OF_SAMPLES_FOR_RADIUS = 9;
        int[] medianChannels = new int[Constants.CHANNELS];
        if(radiusSample) {

            Point pt; Scalar scalar; int a, b;
            a = (int) Math.round(indexRow + .51); b = (int) Math.round(indexCol + .51);
            double[] channels1 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt =  new Point(b, a); scalar = new Scalar(255,255,255);
                Imgproc.rectangle(this.debugPathtaken, pt, pt,  scalar);
            }

            a = (int) Math.round(indexRow - .51); b = (int) Math.round(indexCol - .51);
            double[] channels2 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow + .51); b = (int) Math.round(indexCol - .51);
            double[] channels3 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow - .51); b = (int) Math.round(indexCol + .51);
            double[] channels4 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow); b = (int) Math.round(indexCol + .51);
            double[] channels5 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow); b = (int) Math.round(indexCol - .51);
            double[] channels6 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow - .51); b = (int) Math.round(indexCol);
            double[] channels7 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow + .51); b = (int) Math.round(indexCol);
            double[] channels8 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            a = (int) Math.round(indexRow); b = (int) Math.round(indexCol);
            double[] channels9 = this.distortedImage.get(a, b);
            if (MainActivity.DEBUG) {
                pt = new Point(b,a);
                Imgproc.rectangle(this.debugPathtaken, pt,pt, scalar);
            }

            for (int i = 0; i < medianChannels.length; i++) {
                int[] radiusValues = {(int)channels1[i], (int)channels2[i], (int)channels3[i], (int)channels4[i],
                        (int)channels5[i], (int)channels6[i], (int)channels7[i], (int)channels8[i], (int)channels9[i]};
                Arrays.sort(radiusValues);
                medianChannels[i] = radiusValues[NUM_OF_SAMPLES_FOR_RADIUS/2];
            }
        }
        else{
            double[] channels = this.distortedImage.get((int) Math.round(indexRow), (int) Math.round(indexCol));
            medianChannels[0] = (int) channels[0];
            medianChannels[1] = (int) channels[1];
            medianChannels[2] = (int) channels[2];
        }

        int[] processedChannels;
        if (MainActivity.DEBUG_READ_IMAGE_FROM_FILE) {
            processedChannels = medianChannels;
        }
        else {
            processedChannels = classifyPixelChannelsLevels(this, medianChannels, indexRow, indexCol);
        }

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

        if(MainActivity.DEBUG){
            //debugging code for comparison to original image
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

                        Imgproc.rectangle(this.debugPathtaken, new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Point((int) Math.round(indexCol), (int) Math.round(indexRow)), new Scalar(0,0,0));
                    }
                }
            }
        }
        return pixelValue;
    }
}