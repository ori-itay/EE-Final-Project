//package com.pc.encoderDecoder;
//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;
//import java.awt.Point;
//import java.awt.geom.Point2D;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Set;
//
//public class CapturedImageSampler extends StdImageSampler {
//
//    boolean foundPositionDetectors = false;
//    Set<Point2D.Float> possibleCenters = new HashSet<>();
//    HashMap<Integer, Float> estimatedModuleSize = new HashMap<>();
//
//    void locatePositionDetectors(String encodedImagePath){
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        Mat source = Imgcodecs.imread(encodedImagePath);
//        Mat imgBW = new Mat();
//        // Converting the image to gray scale and
//        // saving it in the dst matrix
//        Imgproc.cvtColor(source, imgBW, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.adaptiveThreshold(source, imgBW, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//                Imgproc.THRESH_BINARY, 51, 0);
//
//        //foundPositionDetectors = find(imgBW);
//        // Writing the image
//        Imgcodecs.imwrite("c:\\users\\user\\downloads\\BWImage.png", imgBW);
//    }
///*
//    private boolean find(Mat imgBW) {
//        possibleCenters.clear();
//        estimatedModuleSize.clear();
//        int skipRows = 3;
//        int stateCount[] = new int[5];
//        int currentState = 0;
//        for(int row=skipRows-1; row<imgBW.rows(); row+=skipRows) {
//            stateCount[0] = 0;
//            stateCount[1] = 0;
//            stateCount[2] = 0;
//            stateCount[3] = 0;
//            stateCount[4] = 0;
//            currentState = 0;
//
//            for (int col = 0; col < imgBW.cols(); col++) {
//                if (imgBW.get(row,col)[0] < 128) {
//                    // We're at a black pixel
//                    if ((currentState & 0x1) == 1) {
//                        // We were counting white pixels
//                        // So change the state now
//
//                        // W->B transition
//                        currentState++;
//                    }
//
//                    // Works for boths W->B and B->B
//                    stateCount[currentState]++;
//                } else {
//                    // We got to a white pixel...
//                    if ((currentState & 0x1) == 1) {
//                        // W->W change
//                        stateCount[currentState]++;
//                    } else {
//                        // ...but, we were counting black pixels
//                        if (currentState == 4) {
//                            // We found the 'white' area AFTER the finder patter
//                            // Do processing for it here
//                            if (checkRatio(stateCount)) {
//                                // This is where we do some more checks
//                                boolean confirmed = handlePossibleCenter(imgBW, stateCount, row, col);
//                            }
//                            else {
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
//                        else {
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
//        return (possibleCenters.size()>0);
//    }*/
///*
//    private boolean handlePossibleCenter(Mat imgBW, int[] stateCount, int row, int col) {
//        int stateCountTotal = 0;
//        for(int i=0;i<5;i++) {
//            stateCountTotal += stateCount[i];
//        }
//
//        // Cross check along the vertical axis
//        float centerCol = centerFromEnd(stateCount, col);
//        float centerRow = crossCheckVertical(imgBW, row, (int)centerCol, stateCount[2], stateCountTotal);
//        if(isnan(centerRow)) {
//            return false;
//        }
//
//        // Cross check along the horizontal axis with the new center-row
//        centerCol = crossCheckHorizontal(imgBW, centerRow, centerCol, stateCount[2], stateCountTotal);
//        if(isnan(centerCol)) {
//            return false;
//        }
//
//        // Cross check along the diagonal with the new center row and col
//        boolean validPattern = crossCheckDiagonal(imgBW, centerRow, centerCol, stateCount[2], stateCountTotal);
//        if(!validPattern) {
//            return false;
//        }
//        Point2D.Float newPt = new Point.Float(centerCol, centerRow);
//        float newEstimatedModuleSize = stateCountTotal / 7.0f;
//        boolean found = false;
//        int idx = 0;
//
//        // Definitely a finder pattern - but have we seen it before?
//        for(Point2D.Float pt : possibleCenters) {
//            Point2D.Float diff = new Point2D.Float((float)(pt.getX()-newPt.getX()), (float)(pt.getY()-newPt.getY()));
//            float dist = (float)Math.sqrt(diff.getX()*diff.getX() + diff.getY()*diff.getY());
//
//            // If the distance between two centers is less than 10px, they're the same.
//            if(dist < 10) {
//                pt = new Point2D.Float((float)(pt.getX()+newPt.getX()), (float)(pt.getY()+newPt.getY()));
//                pt.x /= 2.0f; pt.y /= 2.0f;
//                estimatedModuleSize.put(idx, estimatedModuleSize.get(idx) + newEstimatedModuleSize/2.0f);
//                found = true;
//                break;
//            }
//            idx++;
//        }
//        if(!found) {
//            possibleCenters.put(ptNew);
//            estimatedModuleSize.put(newEstimatedModuleSize);
//        }
//
//        return false;
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
//    }*/
//
//}