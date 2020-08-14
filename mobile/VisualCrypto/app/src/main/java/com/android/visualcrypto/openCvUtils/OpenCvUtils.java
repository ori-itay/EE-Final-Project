package com.android.visualcrypto.openCvUtils;

import android.util.Log;

import androidx.core.util.Pair;

import com.pc.configuration.Constants;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import georegression.struct.point.Point2D_F64;

import static com.pc.configuration.Parameters.encodingColorLevels;

/**
 * A utility class for image processing
 */
public class OpenCvUtils {

    private static final int BLACK_THRESHOLD = 127;
    private static final int WHITE_THRESHOLD = 128;
    private static final int BLACK_PASSAGE = 1;
    private static final int WHITE_PASSAGE = 2;
    private static final int TOTAL_PIXELS_CHECKED = 11;
    private static final int IS_COLOR_THRESHOLD = 3;

    static int[] levelsArr = new int[encodingColorLevels];

    static {
        final int MAX_VALUE = 255;
        double levelDiff = MAX_VALUE / (encodingColorLevels - 1);
        for(int i = 0; i < encodingColorLevels; i++){
            levelsArr[i] = (int) Math.round(levelDiff * i);
        }
    }

    /**
     * Calculates euclidean distance between two points
     * @param a - Point a
     * @param b  Point b
     * @return the distance between them
     */
    public static double calcDistance(Point a, Point b) {
        double distance;

        double temp = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2) * 1.0;
        distance = Math.sqrt(temp);
        return distance;
    }

    /**
     * Get max distance between 4 points
     * @param leftUpper - The left upper point
     * @param rightUpper - The right upper point
     * @param rightLower - The right lower point
     * @param leftLower - The left lower point
     * @return the max distance between them
     */
    public static double getMaxDistance(Point leftUpper, Point rightUpper, Point rightLower, Point leftLower) {
        double max1 = Math.max(calcDistance(leftUpper, rightUpper), calcDistance(leftUpper, leftLower));
        double max2 = Math.max(calcDistance(rightLower, rightUpper), calcDistance(rightLower, leftLower));

        return Math.max(max1, max2);
    }

    /**
     * Classify the pixel channel
     * @param sampler - The image sampler
     * @param channels - The RGB values
     * @param indexRow - Row index
     * @param indexCol - Column index
     * @return the pixel value after classification
     */
    public static int[] classifyPixelChannelsLevels(DistortedImageSampler sampler, int[] channels, int indexRow, int indexCol){
        int subMatIndRow = indexRow / sampler.tileHeight;
        int subMatIndCol = indexCol / sampler.tileWidth;
        int processedChannels[] = new int[Constants.CHANNELS];

        double diffRed = 255, diffBlue = 255, diffGreen = 255;
        int currDiff;
        for(int level : sampler.percentileValuesMapTilesRed.get(subMatIndRow).get(subMatIndCol).keySet()){
            currDiff = (int) Math.abs(channels[0] - sampler.percentileValuesMapTilesRed.get(subMatIndRow).get(subMatIndCol).get(level));
            if(currDiff < diffRed){
                diffRed = currDiff;
                processedChannels[0] = level;
            }
            currDiff = (int) Math.abs(channels[1] - sampler.percentileValuesMapTilesGreen.get(subMatIndRow).get(subMatIndCol).get(level));
            if(currDiff < diffGreen){
                diffGreen = currDiff;
                processedChannels[1] = level;
            }
            currDiff = (int) Math.abs(channels[2] - sampler.percentileValuesMapTilesBlue.get(subMatIndRow).get(subMatIndCol).get(level));
            if(currDiff < diffBlue){
                diffBlue = currDiff;
                processedChannels[2] = level;
            }
        }
        return processedChannels;
    }

    /**
     * Switches between distorted and undistorted coordinates
     * @param oneSideCord - Coordinates of original side
     * @param homographyWay - The homography or inverse homography
     * @return the new coordinates
     */
    public static Point switchCoordinates(Mat oneSideCord, Mat homographyWay) {
        Mat secondSideCord = new Mat();
        Core.gemm(homographyWay, oneSideCord.t(), 1.0, new Mat(), 0, secondSideCord, 0);
        double x = secondSideCord.get(0, 0)[0];
        double y = secondSideCord.get(1, 0)[0];
        double z = secondSideCord.get(2, 0)[0];
        x = x / z;
        y = y / z;
        z = z / z;
        return new Point(x, y);
    }

    /* for pixel stride */

    /**
     * Finds the alignment pattern
     * @param distortedImageSampler - The image sampler
     * @param estimatedModuleSize - The estimated module size
     * @param pixelStride - The pixel stride
     * @param inverseH - The inverse homography matrix
     * @param capturedImg - The captured image matrix
     * @return the alignment pattern bottom right coordinate
     */
    public static Point findAlignmentBottomRight(DistortedImageSampler distortedImageSampler, double estimatedModuleSize, double pixelStride, Mat inverseH, Mat capturedImg) {
        double startingPoint = (Constants.ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - 10) * estimatedModuleSize;
        double undistortedLoc = startingPoint;

        int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
        double[] channels = pixelToChannels(pixelValue);

        while (undistortedLoc < 1) {
            while (!isBlack(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }


            Point endOfPattern = isStartOfPattern(distortedImageSampler, undistortedLoc, pixelStride);
            if (endOfPattern != null) {
                return endOfPattern;
            } else {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }
        }
        return null;
    }

    /**
     * Convert pixel to channels
     * @param pixelValue - The pixel value as integer
     * @return the channels as array
     */
    private static double[] pixelToChannels(int pixelValue) {
        double[] channels = new double[Constants.CHANNELS];

        channels[0] = pixelValue & 0xff;
        channels[1] = (pixelValue & 0xff00) >> 8;
        channels[2] = (pixelValue & 0xff0000) >> 16;

        return channels;
    }

    /**
     * A utility function to findAlignmentBottomRight
     * @param distortedImageSampler - The image sampler
     * @param undistortedLoc - The undistorted image location
     * @param pixelStride - The pixel stride
     * @return a point if it's the start of a pattern, otherwise null
     */
    private static Point isStartOfPattern(DistortedImageSampler distortedImageSampler, double undistortedLoc, double pixelStride) {
        int pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
        double[] channels = pixelToChannels(pixelValue);

        int blackAndWhitePassageCounter = 0;
        while (undistortedLoc < 1) {

            while (isBlack(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }

            Pair pair = isPassage(distortedImageSampler, WHITE_PASSAGE, undistortedLoc, pixelStride);
            assert pair != null;
            if (!((boolean) pair.first)) {
                return null;
            }
            undistortedLoc = (double) pair.second - pixelStride*7;
            blackAndWhitePassageCounter++;
            pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
            channels = pixelToChannels(pixelValue);
            while (isWhite(channels)) {
                undistortedLoc += pixelStride;
                if (undistortedLoc > 1) return null;
                pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
                channels = pixelToChannels(pixelValue);
            }

            pair = isPassage(distortedImageSampler, BLACK_PASSAGE, undistortedLoc, pixelStride);
            assert pair != null;
            if (!((boolean) pair.first)) {
                return null;
            }

            if (blackAndWhitePassageCounter == 3) {
                return new Point(undistortedLoc, undistortedLoc);
            }

            undistortedLoc = (double) pair.second - 7*pixelStride;
            pixelValue = distortedImageSampler.getPixel(undistortedLoc, undistortedLoc, false, false);
            channels = pixelToChannels(pixelValue);
            blackAndWhitePassageCounter++;
        }
        return null;
    }

    /**
     * Checks whether this is a passage between black and white in alignment pattern
     * @param distortedImageSampler - The image sampler
     * @param passage - Whether it's a transition from black to white or from white to bloack
     * @param undistortedLoc - The undistorted image location
     * @param pixelStride - The pixel stride
     * @return Whether it is a passage and the coordinate
     */
    private static Pair<Boolean, Number> isPassage(DistortedImageSampler distortedImageSampler,
                                                   int passage,
                                                   double undistortedLoc, double pixelStride) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, undistortedLoc, undistortedLoc, 1);

        if (passage == WHITE_PASSAGE) {
            int isWhiteCounter = 0;
            for (int i = 0; i < TOTAL_PIXELS_CHECKED; i++) {
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

    /**
     * @param channels - The channel values
     * @return whether the values correspond to a black pixel
     */
    private static boolean isBlack(double[] channels) {
        if (channels.length < 3) {
            Log.d("isBlack", "isBlack empty channels...Out of bounds?");
            return false;
        }
        return channels[0] < BLACK_THRESHOLD && channels[1] < BLACK_THRESHOLD && channels[2] < BLACK_THRESHOLD;
    }

    /**
     * @param channels - The channel values
     * @return whether the values correspond to a white pixel
     */
    private static boolean isWhite(double[] channels) {
        if (channels.length < 3) {
            Log.d("isWhite", "isWhite empty channels...Out of bounds?");
            return false;
        }
        return channels[0] > WHITE_THRESHOLD && channels[1] > WHITE_THRESHOLD && channels[2] > WHITE_THRESHOLD;
    }

    @Deprecated
    public static Mat calibrateImage(Mat capturedImage, boolean videoMode) {
        Mat undistorted = new Mat();
        if(videoMode){
            Calib3d.undistort(capturedImage, undistorted, DistortedImageSampler.itaysCamConfigMtx, DistortedImageSampler.itaysCamConfigDst);
        }
        else {
            Calib3d.undistort(capturedImage, undistorted, DistortedImageSampler.itaysCamConfigMtx, DistortedImageSampler.itaysCamConfigDst);
            //Calib3d.undistort(capturedImage, undistorted, DistortedImageSampler.orisCamConfigMtx, DistortedImageSampler.orisCamConfigDst);
        }
        return undistorted;
    }

    @Deprecated
    public static Mat getColorBalancingMatrix(double[] topLeft, double[] topRight, double[] bottomLeft) {
        Mat M = new Mat(3,3, CvType.CV_64F);

        M.put(0,0, topLeft[0], topRight[0], bottomLeft[0]);
        M.put(1,0, topLeft[1], topRight[1], bottomLeft[1]);
        M.put(2,0, topLeft[2], topRight[2], bottomLeft[2]);

        Mat A = new Mat();
        Core.multiply(M.inv(), new Scalar(255.0), A); // 1/

        return A;
    }

    /**
     * Gets the average value for each channel in the colorful center of the QR position detector
     */
    @Deprecated
    public static double[] getAvgQrCornerColor(Point2D_F64 centerOfQr, double pixelStride, Mat H,
                                        Mat inverseH, Mat distortedImage, int sampledPixelsDimension) {
        double[] pixelChannels = new double[4];
        Mat distortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        distortedImageMatCord.put(0, 0, centerOfQr.x); //TODO verify that it's not the opposite
        distortedImageMatCord.put(0, 1, centerOfQr.y);
        distortedImageMatCord.put(0, 2, 1);

        Point undistortedIndex = switchCoordinates(distortedImageMatCord, H);
        Point startingPoint = new Point(undistortedIndex.x - (sampledPixelsDimension/2)*pixelStride,
                undistortedIndex.y - (sampledPixelsDimension/2)*pixelStride);

        int totalSampledPixels = sampledPixelsDimension * sampledPixelsDimension;

        double sumR = 0, sumG = 0, sumB = 0;

        for (double row = 0; row < (sampledPixelsDimension*pixelStride); row += pixelStride) {
            for (double col = 0; col < (sampledPixelsDimension*pixelStride); col += pixelStride) {
                distortedImageMatCord.put(0,0, startingPoint.x + row);
                distortedImageMatCord.put(0, 1, startingPoint.y + col);

                Point distortedIndex = switchCoordinates(distortedImageMatCord, inverseH);
                pixelChannels = distortedImage.get((int) Math.round(distortedIndex.y), (int) Math.round(distortedIndex.x));
                sumR += pixelChannels[0]; sumG += pixelChannels[1]; sumB += pixelChannels[2];
                if ((pixelChannels[0] > 160 && pixelChannels[1] > 160) || (pixelChannels[0] > 160 && pixelChannels[2] > 160) ||
                        (pixelChannels[1] > 160 && pixelChannels[2] > 160)) {
                    return null;
                }
            }
        }

        pixelChannels[0] = sumR / totalSampledPixels; pixelChannels[1] = sumG / totalSampledPixels;
        pixelChannels[2] = sumB / totalSampledPixels;
        return pixelChannels;
    }

    @Deprecated
    static boolean getCentersOrder(List<double[]> centers) {
        double[] maxR = new double[1], maxG = new double[1], maxB = new double[1]; // arrays are used to allow modification in function
        int[] indexOfR = new int[1], indexOfG = new int[1], indexOfB = new int[1]; // arrays are used to allow modification in function

        Map<Integer, double[]> mapping = new HashMap<>();

        if (centers.get(0) != null) {
            mapping.put(0, centers.get(0));
            processChannel(centers.get(0), 0, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }
        if (centers.get(1) != null) {
            mapping.put(1, centers.get(1));
            processChannel(centers.get(1), 1, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }
        if (centers.get(2) != null) {
            mapping.put(2, centers.get(2));
            processChannel(centers.get(2), 2, maxR, maxG, maxB, indexOfR, indexOfG, indexOfB);
        }

        centers.set(0, mapping.get(indexOfR[0]));
        centers.set(1, mapping.get(indexOfG[0]));
        centers.set(2, mapping.get(indexOfB[0]));

        if (indexOfR[0] == indexOfG[0] || indexOfR[0] == indexOfB[0] || indexOfB[0] == indexOfG[0]) {
            Log.d("getCentersOrder", "Found two candidates for same color!");
            return false; // failure
        }
        else {
            return true; // success
        }
    }

    @Deprecated
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
