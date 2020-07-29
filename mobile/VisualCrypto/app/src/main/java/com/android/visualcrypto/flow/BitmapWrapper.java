package com.android.visualcrypto.flow;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.TextView;

import com.android.visualcrypto.VideoProcessing;


public class BitmapWrapper {

    private boolean errorOccured;
    private Error errorType;
    private Bitmap bp;

    public enum Error {
        IV_OR_DIMS_CHECKSUM,
        QR_POS_NOT_DETECTED,
        INVALID_ROI,
        ALIGNMENT_PATTERN_NOT_FOUND,
    }

    public boolean error() { return this.errorOccured;}
    public Error getErrorType() { return this.errorType;}

    public BitmapWrapper(Bitmap bp, boolean errorOccured, Error errorType) {
        this.bp = bp;
        this.errorOccured = errorOccured;
        this.errorType = errorType;
    }

    public Bitmap getBitmap() {  return this.bp;}

    public static void notifyUser(TextView errorMsgView, BitmapWrapper.Error errorType, long delayMillis, VideoProcessing videoProcessing) {
        String msg = "";
        if (errorType == BitmapWrapper.Error.ALIGNMENT_PATTERN_NOT_FOUND) {
            msg = "Alignment pattern undetected";
        } else if (errorType == BitmapWrapper.Error.INVALID_ROI) {
            msg = "Invalid ROI";
        } else if (errorType == BitmapWrapper.Error.QR_POS_NOT_DETECTED) {
            msg = "QR-POS not found!";
        } else if (errorType == BitmapWrapper.Error.IV_OR_DIMS_CHECKSUM) {
            msg = "Wrong IV or DIMS checksum";
        }

        errorMsgView.setText(msg);
//        errorMsgView.setVisibility(View.VISIBLE);
        errorMsgView.postDelayed(() -> {
            if (videoProcessing != null) {
                videoProcessing.runOnUiThread(()->errorMsgView.setText(""));
            } else {
                errorMsgView.setText("");
            }
        }, delayMillis);
    }

}
