package com.android.visualcrypto;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.photo.Photo;

import java.util.Iterator;

import static com.android.visualcrypto.VideoProcessing.THREADPOOL_NUM_THREADS;


/**
 * This class is ran by a single thread that is responsible for fetching Bitmaps from finishedQueue
 * at a dynamic calculated pace and displaying it to the user.
 */
public class ConsumerSideQueue implements Runnable {

    private VideoProcessing videoProcessing;
    private ImageView processedImgView;
    private int rounds = 1;
    private int sleepingTime = 800;

    public static final CircularFifoQueue<Integer> lastTimes = new CircularFifoQueue<>(6);

    public ConsumerSideQueue(VideoProcessing videoProcessing, ImageView processedImgView) {
        this.videoProcessing = videoProcessing;
        this.processedImgView = processedImgView;
    }

    @Override
    public void run() {
        while (true){
            try {
                if (rounds % 6 == 0) {
                    int sum = 0;
                    Iterator<Integer> elements = lastTimes.iterator();
                    int size = lastTimes.size();
                    while (elements.hasNext()) {
                        sum += elements.next();
                    }
                    sleepingTime = sum/(size*THREADPOOL_NUM_THREADS);
                    rounds = 1;
                }

                rounds++;
                Log.d("queue", "Sleeping for  " + sleepingTime + " milli before trying to fetch finalBitmap");
                Thread.sleep(sleepingTime);
                Bitmap finalBitmap = VideoProcessing.finishedQueue.take();

                long s = System.nanoTime();
                Mat src = new Mat(); Mat dst = new Mat();
                Utils.bitmapToMat(finalBitmap,src);
                Photo.fastNlMeansDenoisingColored(src, dst, 3, 3, 7, 21);
                Utils.matToBitmap(dst, finalBitmap);
                Log.d("fastNlMeansDenoisingColored", "took: (milli) " +  (System.nanoTime() - s) / 1e6);

                videoProcessing.runOnUiThread(() -> processedImgView.setImageBitmap(finalBitmap));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
