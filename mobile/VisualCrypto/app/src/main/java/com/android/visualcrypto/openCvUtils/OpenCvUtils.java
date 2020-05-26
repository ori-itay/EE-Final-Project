package com.android.visualcrypto.openCvUtils;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.util.Pair;

import com.pc.configuration.Constants;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.HashMap;
import java.util.Map;

import georegression.struct.point.Point2D_F64;

import static com.pc.configuration.Parameters.encodingColorLevels;


public class OpenCvUtils {

    private static final int BLACK_THRESHOLD = 127;
    private static final int WHITE_THRESHOLD = 128;
    private static final int BLACK_PASSAGE = 1;
    private static final int WHITE_PASSAGE = 2;

    public static double calcDistance(Point a, Point b) {
        double distance;

        double temp = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2) * 1.0;
        distance = Math.sqrt(temp);
        return distance;
    }

    public static double getMaxDistance(Point leftUpper, Point rightUpper, Point rightLower, Point leftLower) {
        double max1 = Math.max(calcDistance(leftUpper, rightUpper), calcDistance(leftUpper, leftLower));
        double max2 = Math.max(calcDistance(rightLower, rightUpper), calcDistance(rightLower, leftLower));

        return Math.max(max1, max2);
    }

    static int[] levelsArr = new int[encodingColorLevels];
    static {
        final int MAX_VALUE = 255;
        double levelDiff = MAX_VALUE / (encodingColorLevels - 1);
        for(int i = 0; i < encodingColorLevels; i++){
            levelsArr[i] = (int) Math.round(levelDiff * i);
        }
    }

    public static int[] thresholdAndNormalizeChannels(double[] channels, double[][][] minPixelVal,
                                                         double[][][] maxPixelVal, int indexRow, int indexCol) {

        int subMatIndRow = indexRow / DistortedImageSampler.tileHeight;
        int subMatIndCol = indexCol / DistortedImageSampler.tileWidth;

        double normalizedChannel;
        int closestLevel = 0;
        int processedChannels[] = new int[Constants.CHANNELS];
        for (int i = 0; i < processedChannels.length; i++) {
            if (channels[i] < minPixelVal[subMatIndRow][subMatIndCol][i]) normalizedChannel = 0;
            else if (channels[i] > maxPixelVal[subMatIndRow][subMatIndCol][i]) normalizedChannel = 255;
            else normalizedChannel =  ((channels[i] - minPixelVal[subMatIndRow][subMatIndCol][i]) * 255.0 /
                        (maxPixelVal[subMatIndRow][subMatIndCol][i] - minPixelVal[subMatIndRow][subMatIndCol][i]));
            double diff = 255;
            for(int level :levelsArr){
                if(Math.abs(normalizedChannel - level) < diff){
                    diff = Math.abs(normalizedChannel - level);
                    closestLevel = level;
                }
            }
            processedChannels[i] = closestLevel;
        }
        return processedChannels;
    }

    public static Point undistortedToDistortedIndexes(Mat unDistortedImageMatCord, Mat inverseH) {
        Mat distortedImageMatCord = new Mat();
        Core.gemm(inverseH, unDistortedImageMatCord.t(), 1.0, new Mat(), 0, distortedImageMatCord, 0); // res = inverseH.t() * undistortedImageMatCord
        double x = distortedImageMatCord.get(0, 0)[0];
        double y = distortedImageMatCord.get(1, 0)[0];
        double z = distortedImageMatCord.get(2, 0)[0];
        x = x / z;
        y = y / z;
        z = z / z;

        int indexRow = (int) (Math.round(x));
        int indexCol = (int) (Math.round(y));
        return new Point(indexRow, indexCol);
    }


//    public static double[] getPixelChannels(Mat unDistortedImageMatCord, Mat inverseH, Mat capturedImg) {
//        Point distortedIndex = undistortedToDistortedIndexes(unDistortedImageMatCord, inverseH);
//        int indexRow = (int) distortedIndex.x; int indexCol = (int) distortedIndex.y;
//        double[] channels = capturedImg.get(indexCol, indexRow);
//        return channels;
//    }

    /* for pixel stride */
    public static Point findAlignmentBottomRight(DistortedImageSampler distortedImageSampler, double estimatedModuleSize, double pixelStride, Mat inverseH, Mat capturedImg) {
        double startingPoint = 90 * estimatedModuleSize;
        double undistortedLoc = startingPoint;

        Mat unDistortedImageMatCord1 = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord1.put(0, 0, undistortedLoc);
        unDistortedImageMatCord1.put(0, 1, undistortedLoc);
        unDistortedImageMatCord1.put(0, 2, 1);
        //double[] channels = getPixelChannels(unDistortedImageMatCord1, inverseH, capturedImg);
        int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
        double[] channels = pixelToChannels(pixelValue);

        while (undistortedLoc < 1) {
            while (!isBlack(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                //channels = getNewPixel(unDistortedImageMatCord1, undistortedLoc, inverseH, capturedImg);
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }

            Mat m = new Mat(1, 3, CvType.CV_64F); m.put(0,0,undistortedLoc,undistortedLoc,1);
            Point p = undistortedToDistortedIndexes(m,inverseH);
            Point endOfPattern = isStartOfPattern(distortedImageSampler, undistortedLoc, pixelStride, inverseH, capturedImg);
            if (endOfPattern != null) {
                return endOfPattern;
            } else {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                //channels = getNewPixel(unDistortedImageMatCord1, undistortedLoc, inverseH, capturedImg);
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }
        }
        return null;
    }

    private static double[] pixelToChannels(int pixelValue) {
        double[] channels = new double[Constants.CHANNELS];

        channels[0] = pixelValue & 0xff;
        channels[1] = (pixelValue & 0xff00) >> 8;
        channels[2] = (pixelValue & 0xff0000) >> 16;

        return channels;
    }

    private static Point isStartOfPattern(DistortedImageSampler distortedImageSampler, double undistortedLoc, double pixelStride, Mat inverseH, Mat capturedImg) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 2, 1);
        //double[] channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
        int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
        double[] channels = pixelToChannels(pixelValue);

        int blackAndWhitePassageCounter = 0;
        while (undistortedLoc < 1) {

            while (isBlack(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                //channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }

            Pair pair = isPassage(distortedImageSampler, WHITE_PASSAGE, undistortedLoc, inverseH, capturedImg, pixelStride);
            assert pair != null;
            if (!((boolean) pair.first)) {
                return null;
            }
            undistortedLoc = (double) pair.second - pixelStride*7;// ORIs MODIFCATION FROM ITAYS TEAMVIEWER
            unDistortedImageMatCord.put(0, 0, undistortedLoc, undistortedLoc); // advance the pixels
            blackAndWhitePassageCounter++;
            //channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
            pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
            channels = pixelToChannels(pixelValue);
            while (isWhite(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                //channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }

            pair = isPassage(distortedImageSampler, BLACK_PASSAGE, undistortedLoc, inverseH, capturedImg, pixelStride);
            assert pair != null;
            if (!((boolean) pair.first)) {
                return null;
            }

            if (blackAndWhitePassageCounter == 3) {
                return new Point(undistortedLoc, undistortedLoc);
            }

            undistortedLoc = (double) pair.second - 7*pixelStride; // ORIs MODIFCATION FROM ITAYS TEAMVIEWER
            unDistortedImageMatCord.put(0, 0, undistortedLoc, undistortedLoc); // advance the pixels
            //channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
            pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
            channels = pixelToChannels(pixelValue);
            blackAndWhitePassageCounter++;
        }
        return null;
    }

    private static final int TOTAL_PIXELS_CHECKED = 11;
    private static final int IS_COLOR_THRESHOLD = 3;

    private static Pair<Boolean, Number> isPassage(DistortedImageSampler distortedImageSampler,
                                                   int passage,
                                                   double undistortedLoc, Mat inverseH, Mat capturedImg, double pixelStride) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, undistortedLoc, undistortedLoc, 1);

        if (passage == WHITE_PASSAGE) {
            int isWhiteCounter = 0;
            for (int i = 0; i < TOTAL_PIXELS_CHECKED; i++) {
                //double[] channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
                int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                double[] channels = pixelToChannels(pixelValue);
                if (isWhite(channels)) {
                    isWhiteCounter++;
                }
                undistortedLoc += pixelStride;
            }
            return new Pair<>(isWhiteCounter >= IS_COLOR_THRESHOLD, undistortedLoc);

        } else if (passage == BLACK_PASSAGE) {
            int isBlackCounter = 0;
            for (int i = 0; i < TOTAL_PIXELS_CHECKED; i++) {
                //double[] channels = getNewPixel(unDistortedImageMatCord, undistortedLoc, inverseH, capturedImg);
                int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                double[] channels = pixelToChannels(pixelValue);
                if (isBlack(channels)) {
                    isBlackCounter++;
                }
                undistortedLoc += pixelStride;
            }
            return new Pair<>(isBlackCounter >= IS_COLOR_THRESHOLD, undistortedLoc);
        }

        return null;
    }

//    private static double[] getNewPixel(Mat unDistortedImageMatCord, double undistortedLoc, Mat inverseH, Mat capturedImg) {
//        unDistortedImageMatCord.put(0, 0, undistortedLoc, undistortedLoc);
//        return getPixelChannels(unDistortedImageMatCord, inverseH, capturedImg);
//    }


    private static boolean isBlack(double[] channels) {
        //double avg = (channels[0]+channels[1]+channels[2])/3;
        //return avg < BLACK_THRESHOLD;
        if (channels.length < 3) {
            Log.d("isBlack", "isBlack empty channels...Out of bounds?");
            return false;
        }
        return channels[0] < BLACK_THRESHOLD && channels[1] < BLACK_THRESHOLD && channels[2] < BLACK_THRESHOLD;
    }

    private static boolean isWhite(double[] channels) {
//        double avg = (channels[0]+channels[1]+channels[2])/3;
//        return avg >= WHITE_THRESHOLD;
        if (channels.length < 3) {
            Log.d("isWhite", "isWhite empty channels...Out of bounds?");
            return false;
        }
        return channels[0] > WHITE_THRESHOLD && channels[1] > WHITE_THRESHOLD && channels[2] > WHITE_THRESHOLD;
    }

    public static Mat calibrateImage(Mat capturedImage) {
        Mat undistored = new Mat();
        Calib3d.undistort(capturedImage, undistored, DistortedImageSampler.itaysCamConfigMtx, DistortedImageSampler.itaysCamConfigDst);
        //Calib3d.undistort(capturedImage, undistored, DistortedImageSampler.orisCamConfigMtx, DistortedImageSampler.orisCamConfigDst);
        return undistored;
    }

    public static Mat getColorBalancingMatrix(double[] topLeft, double[] topRight, double[] bottomLeft) {
        Mat M = new Mat(3,3, CvType.CV_32F);

        M.put(0,0, topLeft[0], topRight[0], bottomLeft[0]);
        M.put(1,0, topLeft[1], topRight[1], bottomLeft[1]);
        M.put(2,0, topLeft[2], topRight[2], bottomLeft[2]);

        Mat A = new Mat();
        Core.multiply(M.inv(), new Scalar(1/255.0), A);

        return A;
    }

    /**
     * Gets the average value for each channel in the colorful center of the QR position detector
     * @param centerOfQr
     * @return
     */
    public static double[] getAvgQrCornerColor(Point2D_F64 centerOfQr) {

        return null;
    }

    /**
     * Decides which center accounts for which color
     * @param center0Channels
     * @param center1Channels
     * @param center2Channels
     * @param center3Channels
     * @return
     */
    static double[][] getCentersOrder(double[] center0Channels, double[] center1Channels, double[] center2Channels, double[] center3Channels) {
        double[][] indexes = new double[3][]; // one of the array suppose to be null (=not a qr pattern)
        double[] maxR = new double[1], maxG = new double[1], maxB = new double[1]; // arrays are used to allow modification in function
        int[] indexOfR = new int[1], indexOfG = new int[1], indexOfB = new int[1]; // arrays are used to allow modification in function

        Map<Integer, double[]> mapping = new HashMap<>();

        if (center0Channels != null) {
            mapping.put(0, center0Channels);
            processChannel(center0Channels, 0, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }
        if (center1Channels != null) {
            mapping.put(1, center1Channels);
            processChannel(center1Channels, 1, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }
        if (center2Channels != null) {
            mapping.put(2, center2Channels);
            processChannel(center2Channels, 2, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }
        if (center3Channels != null) {
            mapping.put(3, center3Channels);
            processChannel(center3Channels, 3, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }

        indexes[0] = mapping.get(indexOfR[0]); // represents red qr
        indexes[1] = mapping.get(indexOfG[0]); // represents green qr
        indexes[2] = mapping.get(indexOfB[0]); // represents blue qr

        if (indexOfR[0] == indexOfG[0] || indexOfR[0] == indexOfB[0] || indexOfB[0] == indexOfG[0]) {
            return null;
        }
        else {
            return indexes;
        }
    }

    private static void processChannel(double[] center, int centerNum, double[] maxR, double[] maxG,
                                       double[] maxB, int[] indexOfR, int[] indexOfG, int[] indexOfB) {
        if (center[0] > maxR[0]) {
            maxR[0] = center[0];
            indexOfR[0] = centerNum;
        }
        if (center[1] > maxG[0]) {
            maxG[0] = center[1];
            indexOfG[0] = centerNum;
        }
        if (center[2] > maxB[0]) {
            maxB[0] = center[2];
            indexOfB[0] = centerNum;
        }
    }
}


//    public static double getModuleStride(double minPixelStride, Mat inverseH, Mat capturedImg) {
//        double startingPoint = 0;
//        double undistortedLocX = startingPoint;
//        double undistortedLocY = startingPoint;
//
//        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
//        unDistortedImageMatCord.put(0, 0, undistortedLocX);
//        unDistortedImageMatCord.put(0, 1, undistortedLocY);
//        unDistortedImageMatCord.put(0, 2, 1);
//        boolean firstBlackFound = false;
//        double undistortedModuleDimension = 0.0;
//
//        int BLACK_THRESHOLD = 80;
//
//        int remainingColorSwitches = 5;
//        boolean inBlack = false;
//        boolean inWhite = false;
//        boolean firstRun = true;
//        boolean startCounting = false;
//
//        int pixelsAmountInDiagonal = 0;
//        double MODULES_IN_QR_DIAGONAL = 7D;
//        while (undistortedLocX < 1 && undistortedLocY < 1) {
//            double[] channels = getPixelChannels(unDistortedImageMatCord, inverseH, capturedImg);
//            double[] processedChannels = thresholdAndNormalizeChannels(channels);
//
//            //double pixel = (int) (channels[0] + channels[1] + channels[2]) / 3.0;
//            double pixel = channels[0]; //TODO: remove me if not using grayscale
//            if (firstRun && pixel > BLACK_THRESHOLD) { // includes
//                //undistortedModuleDimension += minPixelStride; // a posteriori
//                firstRun = false;
//                inWhite = true;
//            } else if (firstRun && pixel < BLACK_THRESHOLD) {
//                firstRun = false;
//                inBlack = true;
//                startCounting = true;
//            }
//
//            if (startCounting && inBlack && pixel > BLACK_THRESHOLD) {
//                inBlack = false;
//                inWhite = true;
//                remainingColorSwitches--;
//            } else if (inWhite && pixel < BLACK_THRESHOLD) {
//                inWhite = false;
//                inBlack = true;
//                if (!startCounting) {
//                    startCounting = true;
//                } else {
//                    remainingColorSwitches--;
//                }
//            }
//            pixelsAmountInDiagonal++;
//            if (remainingColorSwitches == 0) {
//                //return undistortedModuleDimension / MODULES_IN_QR_DIAGONAL;
//                return (pixelsAmountInDiagonal / MODULES_IN_QR_DIAGONAL) * minPixelStride;
//            }
//
//            undistortedLocX += minPixelStride;
//            undistortedLocY += minPixelStride;
//            unDistortedImageMatCord.put(0, 0, undistortedLocX);
//            unDistortedImageMatCord.put(0, 1, undistortedLocY);
//            if (startCounting) {
//                undistortedModuleDimension += minPixelStride;
//            }
//        }
//        return 0.0;
//    }


/* FOR MODULE STRIDE */
//    public static Point findAlignmentBottomRight(double estimatedModuleSize, double pixelStride, Mat inverseH, Mat capturedImg) {
//        double startingPoint = 90*estimatedModuleSize;
//        double undistortedLocX = startingPoint;
//        double undistortedLocY = startingPoint;
//
//        Mat unDistortedImageMatCord1 = new Mat(1, 3, CvType.CV_64F);
//        unDistortedImageMatCord1.put(0, 0, undistortedLocX);
//        unDistortedImageMatCord1.put(0, 1, undistortedLocY);
//        unDistortedImageMatCord1.put(0, 2, 1);
//        Mat unDistortedImageMatCord2 = unDistortedImageMatCord1.clone();
//        Mat unDistortedImageMatCord3 = unDistortedImageMatCord1.clone();
//        Mat unDistortedImageMatCord4 = unDistortedImageMatCord1.clone();
//        Mat unDistortedImageMatCord5 = unDistortedImageMatCord1.clone();
//
//        unDistortedImageMatCord2.put(0, 0, startingPoint + estimatedModuleSize);
//        unDistortedImageMatCord2.put(0, 1, startingPoint + estimatedModuleSize);
//        unDistortedImageMatCord3.put(0, 0, startingPoint + 2*estimatedModuleSize);
//        unDistortedImageMatCord3.put(0, 1, startingPoint + 2*estimatedModuleSize);
//        unDistortedImageMatCord4.put(0, 0, startingPoint + 3*estimatedModuleSize);
//        unDistortedImageMatCord4.put(0, 1, startingPoint + 3*estimatedModuleSize);
//        unDistortedImageMatCord5.put(0, 0, startingPoint + 4*estimatedModuleSize);
//        unDistortedImageMatCord5.put(0, 1, startingPoint + 4*estimatedModuleSize);
//        undistortedLocX = undistortedLocY =  startingPoint + 4*estimatedModuleSize;
//
//        double[] channels1 = getPixelChannels(unDistortedImageMatCord1, inverseH, capturedImg);
//        double[] channels2 = getPixelChannels(unDistortedImageMatCord2, inverseH, capturedImg);
//        double[] channels3 = getPixelChannels(unDistortedImageMatCord3, inverseH, capturedImg);
//        double[] channels4 = getPixelChannels(unDistortedImageMatCord4, inverseH, capturedImg);
//        double[] channels5 = getPixelChannels(unDistortedImageMatCord5, inverseH, capturedImg);
//
//        while (undistortedLocX < 1 && undistortedLocY < 1) {
//
//            if (isBlack(channels1) && isWhite(channels2) && isBlack(channels3) && isWhite(channels4) && isBlack(channels5)) {
//                Mat borderIndexUndistorted = findIndexOfBorder(inverseH, pixelStride, capturedImg,
//                        undistortedLocX-estimatedModuleSize, undistortedLocY-estimatedModuleSize);
//
//                //return undistortedToDistortedIndexes(unDistortedImageMatCord5, inverseH); //getDistortedIndex(undistortedLocX, undistortedLocY);
//                return undistortedToDistortedIndexes(borderIndexUndistorted, inverseH);
//            }
//            channels1 = channels2;
//            channels2 = channels3;
//            channels3 = channels4;
//            channels4 = channels5;
//
//            undistortedLocX += estimatedModuleSize;
//            undistortedLocY += estimatedModuleSize;
//            unDistortedImageMatCord5.put(0, 0, undistortedLocX);
//            unDistortedImageMatCord5.put(0, 1, undistortedLocY);
//
//            channels5 = getPixelChannels(unDistortedImageMatCord5, inverseH, capturedImg);
//        }
//        return null;
//    }

//    private static Mat findIndexOfBorder(Mat inverseH, double pixelStride, Mat capturedImg,
//                                           double undistortedLocX, double undistortedLocY) {
//        Mat borderIndexUndistorted = new Mat(1, 3, CvType.CV_64F);
//        borderIndexUndistorted.put(0, 0, undistortedLocX);
//        borderIndexUndistorted.put(0, 1, undistortedLocY);
//        borderIndexUndistorted.put(0, 2, 1);
//
//        double originalLocX = undistortedLocX;
//
//
//        //go rightmost
//        while (isWhite(getPixelChannels(borderIndexUndistorted, inverseH, capturedImg))) {
//            undistortedLocX += pixelStride;
//            borderIndexUndistorted.put(0, 0, undistortedLocX);
//        }
//
//        borderIndexUndistorted.put(0,0, originalLocX); // return to white zone
//        //go downmost
//        while (isWhite(getPixelChannels(borderIndexUndistorted, inverseH, capturedImg))) {
//            undistortedLocY += pixelStride;
//            borderIndexUndistorted.put(0, 1, undistortedLocY);
//        }
//
//        borderIndexUndistorted.put(0,0, undistortedLocX -pixelStride);
//        borderIndexUndistorted.put(0,1, undistortedLocY -pixelStride);
//        return borderIndexUndistorted;
//    }
