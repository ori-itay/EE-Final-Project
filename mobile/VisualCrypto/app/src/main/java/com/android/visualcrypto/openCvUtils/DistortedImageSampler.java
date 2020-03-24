package com.android.visualcrypto.openCvUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.visualcrypto.MainActivity;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.StdImageSampler;

import org.ddogleg.struct.FastQueueList;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.QRCodeDetector;

import java.util.ArrayList;
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
import static com.android.visualcrypto.openCvUtils.Utils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.Utils.getModuleStride;
import static com.android.visualcrypto.openCvUtils.Utils.getPixelChannels;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class DistortedImageSampler extends StdImageSampler {
    private static Mat distortedImage;
    private static Mat inverseH;
    private static Bitmap distortedBitmap;

    private Context context;

    MatOfPoint2f possibleCenters = new MatOfPoint2f();
    List<Double> estimatedModuleSize = new ArrayList<>();


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
        if (pointsQueue.size() == 3) {
            //pointsQueue.get(0).
            Point2D_F64 boofPt0 = pointsQueue.get(0).square.get(3);
            Point2D_F64 boofPt1 = pointsQueue.get(1).square.get(0);
            Point2D_F64 boofPt3 = pointsQueue.get(2).square.get(2);

            pts[0] = new Point(boofPt0.x, boofPt0.y);
            pts[1] = new Point(boofPt1.x, boofPt1.y);

            pts[2] = new Point(, ); // TODO: fill here the measurement to find the forth corner
            pts[3] = new Point(boofPt3.x, boofPt3.y);

        } else if (failures.size() == 0 && detections.size() == 0) {
            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
            ((MainActivity) this.context).showAlert("Couldn't detect QR position detectors");
            return 1;
        }


//        QRCodeDetector detector = new QRCodeDetector();
//        MatOfPoint2f corners1 = new MatOfPoint2f();
        //boolean foundCorners = detector.detect(distortedImage, corners1);
        //boolean foundCorners = this.detect(distortedImage);
//        if (!foundCorners) {
//            Log.d("DistortedImageSampler", "Couldn't detect QR position detectors");
//            ((MainActivity) this.context).showAlert("Couldn't detect QR position detectors");
//            return 1;
//        }

//        MatOfPoint2f test = new MatOfPoint2f();
//
//        MatOfPoint2f corners1 = new MatOfPoint2f(new Point(1170,10), new Point(1170,1170), new Point(10,1170), new Point(10,10));
        MatOfPoint2f corners1 = new MatOfPoint2f(pts[0], pts[1], pts[2], pts[3]);
        MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(0, 1));
        Mat H;
        H = Calib3d.findHomography(corners1, corners2); // possibleCenters
        Mat inverseH = H.inv();

        DistortedImageSampler.inverseH = inverseH;

//        Point[] pts = possibleCenters.toArray();
//        Point leftUpperCorner = getCornerFromCenter(pts[0], inverseH, SOMESTRIDE,true,true); //TODO: implement getCornerFromCenter
//        Point rightUpperCorner = getCornerFromCenter(pts[1], inverseH, SOMESTRIDE, false, true);
//        Point leftLowerCorner = getCornerFromCenter(pts[2], inverseH, SOMESTRIDE,true, false);
//        find fourth corner ;
//
//        double minPixelStride = 1 / getMaxDistance(leftUpperCorner, rightUpperCorner, leftLowerCorner, fourth corner);

        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);

        //double maxDistanceBetweenCenters = getMaxDistance(pts[0], pts[1], pts[2], pts[3]);
        //int modulesBetweenCenters = (int) Math.round(maxDistanceBetweenCenters /  estimatedModuleSize.get(0));
        // this.setModuleSize((double) 1 / modulesBetweenCenters );

        this.setModuleSize(getModuleStride(minPixelStride, inverseH, DistortedImageSampler.distortedImage));
        int effectiveModulesInDim = (int) Math.round(1.0 / this.getModuleSize());
        this.setModulesInDim(effectiveModulesInDim);
        //this.setModulesInDim(modulesBetweenCenters );

        return 0;
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

    private Point getCornerFromCenter(Point pt, Mat inverseH, double stride, boolean left, boolean upper) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 2, 1);
        if (left) {
            unDistortedImageMatCord.put(0, 0, -3.5 * stride);
        } else {
            unDistortedImageMatCord.put(0, 0, 3.5 * stride);
        }

        if (upper) {
            unDistortedImageMatCord.put(0, 1, -3.5 * stride);
        } else {
            unDistortedImageMatCord.put(0, 1, 3.5 * stride);
        }

        Mat distortedImageMatCord = new Mat();
        Core.gemm(inverseH, unDistortedImageMatCord.t(), 1.0, new Mat(), 0, distortedImageMatCord, 0);
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

    @Override
    public int getPixel(double rowLoc, double colLoc) {
        Mat unDistortedImageMatCord = new Mat(1, 3, CvType.CV_64F);
        unDistortedImageMatCord.put(0, 0, rowLoc);
        unDistortedImageMatCord.put(0, 1, colLoc);
        unDistortedImageMatCord.put(0, 2, 1);
        double[] channels = getPixelChannels(unDistortedImageMatCord, DistortedImageSampler.inverseH, DistortedImageSampler.distortedImage);
        double[] threshholdedChannels = thresholdChannels(channels);
        int pixelValue = (int) (Math.round(threshholdedChannels[0])) | (int) (Math.round(threshholdedChannels[1]) << 8) | (int) (Math.round(threshholdedChannels[2]) << 16);
        return pixelValue;
    }

    private double[] thresholdChannels(double[] channels) {
        double threshholdedChannels[] = new double[3];
        int q;
        int thLevel = 255 / Parameters.encodingColorLevels;
        int colorStride = 255 / (Parameters.encodingColorLevels - 1);
        for (int i = 0; i < threshholdedChannels.length; i++) {
            q = Math.floorDiv((int) Math.round(channels[i]), thLevel);
            threshholdedChannels[i] = q * colorStride;
        }
        return threshholdedChannels;
    }


//    public boolean detect(Mat img){
//        Mat imgBW = new Mat();
//        cvtColor(img, imgBW, Imgproc.COLOR_BGR2GRAY);
//        adaptiveThreshold(imgBW, imgBW, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C , Imgproc.THRESH_BINARY, 51, 0);
//        Bitmap bp = MainActivity.convertMatToBitmap(imgBW);
//        /*File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "out.png");
//        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
//        os.close();*/
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
//        //possibleCenters.clear();
//        //estimatedModuleSize.clear();
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
//            double dist = Utils.calcDistance(pt, ptNew);
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
//}
}