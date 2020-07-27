package com.android.visualcrypto;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class ConsumerSideQueue implements Runnable {

    private  VideoProcessing videoProcessing;
    private  ImageView processedImgView;

    private static final int SLEEPING_TIME_MILLI = 800;

    public ConsumerSideQueue(VideoProcessing videoProcessing, ImageView processedImgView) {
        this.videoProcessing = videoProcessing;
        this.processedImgView = processedImgView;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(SLEEPING_TIME_MILLI);
            Bitmap finalBitmap = VideoProcessing.finishedQueue.take();
            videoProcessing.runOnUiThread(() -> processedImgView.setImageBitmap(finalBitmap));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
