package com.android.visualcrypto.openCvUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.core.util.Pair;

import com.android.visualcrypto.MainActivity;
import com.pc.encoderDecoder.StdImageSampler;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import boofcv.abst.fiducial.QrCodePreciseDetector;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import static boofcv.android.ConvertBitmap.bitmapToGray;
import static com.android.visualcrypto.openCvUtils.Utils.getMaxDistance;
import static com.android.visualcrypto.openCvUtils.Utils.getPixelChannels;
import static com.android.visualcrypto.openCvUtils.Utils.thresholdAndNormalizeChannels;
import static java.lang.Thread.yield;


public class DistortedImageSampler extends StdImageSampler {
    private static Mat distortedImage;
    private static Mat inverseH;
    private static Bitmap distortedBitmap;
    public static double[] minPixelVal = new double[3];
    public static double[] maxPixelVal = new double[3];

    private Context context;

    MatOfPoint2f possibleCenters = new MatOfPoint2f();
    List<Double> estimatedModuleSize = new ArrayList<>();


    public DistortedImageSampler(Mat distortedImage, Bitmap distortedBitmap, Context context) {
        DistortedImageSampler.distortedImage = distortedImage;
        DistortedImageSampler.distortedBitmap = distortedBitmap;
        this.context = context;
    }

    public int initParameters() throws IOException {
        this.setModulesInMargin(0);

        Pair<Point, Mat> bla = findPatch(blackAndWhite, scanFromCorner(blackAndWhite.size()), false, false);





        //findMinMaxPixelVals();

/*
        Bitmap normalized = Bitmap.createBitmap(distortedBitmap.getWidth(), distortedBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        int channels, normalizedChannel;
        final byte ALPHA_VALUE = (byte) 0xff;
        for(int row = 0; row< distortedBitmap.getHeight(); row++){
            for(int col = 0; col<distortedBitmap.getWidth(); col++){
                channels = distortedBitmap.getPixel(col, row);
                int a = (channels & 0xFF000000) >>> 24;
                int normA = a;
                int r = (channels & 0x00FF0000) >>> 16;
                int normR = (int) ((r - minPixelVal[0]) * 255.0 / (maxPixelVal[0] - minPixelVal[0]));
                int g = (channels & 0x0000FF00) >>> 8;
                int normG = (int) ((g - minPixelVal[1]) * 255.0 / (maxPixelVal[1] - minPixelVal[1]));;
                int b = channels & 0x000000FF;
                int normB = (int) ((b - minPixelVal[2]) * 255.0 / (maxPixelVal[2] - minPixelVal[2]));;
                normalizedChannel = normA<<24 | normR<<16 | normG<<8 | normB;
                normalized.setPixel(col, row, normalizedChannel);
            }
        }
        DistortedImageSampler.distortedBitmap = normalized;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +"/normalized_captured50_50_2levels_10pixInModule.jpg");
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        distortedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();
    */
//        Imgproc.threshold(DistortedImageSampler.distortedImage, DistortedImageSampler.distortedImage, 75, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
//        Bitmap shit  = MainActivity.convertMatToBitmap(DistortedImageSampler.distortedImage);
        // GrayU8 gray = bitmapToGray(shit, (GrayU8) null, null);
        GrayU8 gray = bitmapToGray(DistortedImageSampler.distortedBitmap, (GrayU8) null, null);

        ConfigQrCode config = new ConfigQrCode();
        config.threshold.fixedThreshold =75D;
        config.threshold.thresholdFromLocalBlocks = true;
        config.polygon.detector.canTouchBorder=true;
        config.polygon.refineContour=true;

//        ConfigThreshold thre = new ConfigThreshold();
//        thre.thresholdFromLocalBlocks = true;
//        config.threshold.fixedThreshold(75D);
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
        pts[2] = new Point(3323,2915);
        MatOfPoint2f corners1 = new MatOfPoint2f(pts[0], pts[1], pts[2], pts[3]); // pts[2] = new Point(3802, 3308) , for captured_179modulesindum.kpg (actually 175)
        //MatOfPoint2f corners2 = new MatOfPoint2f(new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(0, 1));
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
        //double maxDistanceBetweenCenters = getMaxDistance(pts[0], pts[1], pts[2], pts[3]);
        //int modulesBetweenCenters = (int) Math.round(maxDistanceBetweenCenters /  estimatedModuleSize.get(0));
        // this.setModuleSize((double) 1 / modulesBetweenCenters );
        //this.setModulesInDim(modulesBetweenCenters );

        double minPixelStride = 1 / getMaxDistance(pts[0], pts[1], pts[2], pts[3]);
        Mat grayscale = new Mat();
        //Imgproc.cvtColor(DistortedImageSampler.distortedImage, grayscale, Imgproc.COLOR_RGB2GRAY);
        //this.setModuleSize(getModuleStride(minPixelStride, inverseH, DistortedImageSampler.distortedImage));
        //Point leftLowerOfPts0 = new Point(pointsQueue.get(0).square.vertexes.get(0).x, pointsQueue.get(0).square.vertexes.get(0).y);
        Point leftLowerOfPts0 = new Point(2124,1691);
        this.setModuleSize(testItaySize(pts[0], leftLowerOfPts0, H, grayscale));
        int effectiveModulesInDim = (int) Math.floor(1.0 / this.getModuleSize());
        this.setModulesInDim(effectiveModulesInDim);
        Log.d("ModulesInDim", "modules in dim: "+Float.toString(this.getModulesInDim()));
        Log.d("ModulesInDim", "left lower: "+leftLowerOfPts0.x+","+leftLowerOfPts0.y);
        return 0;
    }


//    private List<Point> scanInDiagonal(int size) {
//        List<Point> pts = new ArrayList<>();
//        for (int start_row = 0; start_row < size - 1; start_row++) {
//            for (int col = 0; col < start_row +1; col++) {
//                pts.add(new Point(start_row-col, col));
//            }
//        }
//        for (int col = 0; col <size; col++) {
//            for (int row = size-1; row >=0; row--) {
//                pts.add(new Point(row-col, col));
//            }
//        }
//        return pts;
//    }
//
//    private List<Point> scanFromCorner(int size, boolean top, boolean left) {
//        List<Point> pts = new ArrayList<>();
//        for (Point pt : scanInDiagonal(size)) {
//            int y = (int) pt.x;
//            int x = (int) pt.y;
//            if (!top) {
//                y = size - y - 1;
//            }
//            if (!left) {
//                x = size - x - 1;
//            }
//            pts.add(new Point(y, x));
//        }
//        return pts;
//    }
//
//    private Mat findSame(Mat img, int x0, int y0) {
//        Mat result = new Mat(new Size(10, 3), CvType.CV_32F); // TODO: potential problematic line
//        List<Pair> pendingCheck = new ArrayList<>();
//        pendingCheck.add(new Pair<>(x0, y0));
//        Set<Pair> checked = new HashSet<>();
//        while (!pendingCheck.isEmpty()) {
//            Pair pt = pendingCheck.get(0); pendingCheck.remove(0);
//            int x = (int) pt.first; int y = (int) pt.second;
//            if (x < 0 || y < 0 || x>=img.cols() || y>= img.width()) { // TODO: potential problematic line
//                continue;
//            }
//            if (checked.contains(pt)) {
//                continue;
//            }
//
//            checked.add(pt);
//
//            if (img.get(y,x)[0] == img.get(y0, x0)[0]) {
//                result.put(y, x, 1);
//                pendingCheck.add(new Pair<>(x+1, y));
//                pendingCheck.add(new Pair<>(x-1, y));
//                pendingCheck.add(new Pair<>(x, y+1));
//                pendingCheck.add(new Pair<>(x, y-1));
//            }
//        }
//        return result;
//    }
//
//    private Pair findPatch(Mat img, List<Point> indices) {
//        for (Point pt : indices) {
//            int y = (int) pt.x;
//            int x = (int) pt.y;
//            if (img.get(y,x)[0] == 0) { // TODO: img needs to be black and white...  0 is black
//                return new Pair<>(pt, findSame(img, y, x));
//            }
//        }
//        return null;
//    }

    private List<Pair<Integer, Integer>> findCorner(Mat img, int val, boolean top, boolean left) {
        List<Pair<Integer, Integer>> pts = new ArrayList<>();
        for (Pair p : scanDiagonalFromCorner(img.size(), top, left)) {
            if (img.get((int) p.first, (int) p.second)[0] == val) {
                pts.add(p);
            }
        }

        return pts;

    }


    private List<Pair> scan(int height, int width) {
        List<Pair> pts = new ArrayList<>();
        for (int d = 0; d< height+width+1; d++) {
            int startRow = Math.max(0, d- (width-1));
            int endRow = Math.min(d, height -1);
            for (int row = startRow; row < endRow +1; row++) {
                int col = d - row;
                pts.add(new Pair(row, col));
            }
        }
        return pts;
    }

    private List<Pair> scanDiagonalFromCorner(Size size, boolean top, boolean left) {
        List<Pair> pts = new ArrayList<>();
        int height = (int) size.height; int width = (int) size.width;
        List<Pair> scan = scan(height, width);
        for (Pair p : scan) {
            int row, col;
            row = (int) p.first;
            col = (int) p.second;

            if (!top) {
                row = height - (int) p.first - 1;
            }
            if (!left) {
                col = width - (int) p.second - 1;
            }
            Pair pair = new Pair(row, col);
            pts.add(pair);
        }
        return pts;
    }



    private double testItaySize(Point upperLeft, Point lowerLeft, Mat H, Mat grayscale) {
        Mat upperPoint = new Mat(1, 3, CvType.CV_64F);

        upperPoint.put(0, 0, upperLeft.x);
        upperPoint.put(0, 1, upperLeft.y);
        upperPoint.put(0, 2, 1);


        Mat undistortedUpperPoint = new Mat();
        Core.gemm(H, upperPoint.t(), 1.0, new Mat(), 0, undistortedUpperPoint, 0);

        double zUpper = undistortedUpperPoint.get(2,0)[0];
        double xUpper = undistortedUpperPoint.get(0,0)[0] / zUpper;
        double yUpper = undistortedUpperPoint.get(1,0)[0] / zUpper;


        upperPoint.put(0, 0, lowerLeft.x);
        upperPoint.put(0, 1, lowerLeft.y);
        undistortedUpperPoint = new Mat();
        Core.gemm(H, upperPoint.t(), 1.0, new Mat(), 0, undistortedUpperPoint, 0);
        double zLower = undistortedUpperPoint.get(2,0)[0];
        double xLower = undistortedUpperPoint.get(0,0)[0] / zLower;
        double yLower = undistortedUpperPoint.get(1,0)[0] / zLower;

        //return calcDistance(new Point(xLower, yLower), new Point(xUpper, yUpper)) / 7D;
        return Math.sqrt(((xLower-xUpper)*(xLower-xUpper)+(yLower-yUpper)*(yLower-yUpper))/20000.0);
        //return calcDistance(new Point(xUpper, yUpper), new Point(xLower, yLower)) / 7D;

    }

    private void findMinMaxPixelVals() {
        double[] channels;
        for(int row = 0; row<distortedImage.height(); row++){
            for(int col = 0; col<distortedImage.width(); col++){
                channels = distortedImage.get(row,col);
                for(int ch = 0; ch<3; ch++){
                    if(channels[ch]>maxPixelVal[ch]){
                        maxPixelVal[ch] = channels[ch];
                    }
                    if(channels[ch]<minPixelVal[ch]){
                        minPixelVal[ch] = channels[ch];
                    }
                }
            }
        }
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
        double[] processedChannels = thresholdAndNormalizeChannels(channels);
        int pixelValue = (int) (Math.round(processedChannels[0])) |
                (int) (Math.round(processedChannels[1]) << 8) | (int) (Math.round(processedChannels[2]) << 16);
        return pixelValue;
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