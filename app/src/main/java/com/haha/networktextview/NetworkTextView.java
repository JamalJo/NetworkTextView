package com.haha.networktextview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.Spanned;
import android.util.AttributeSet;

/**
 * Created by zhoumao on 2019/4/8.
 * Description: 通用图文TextView
 */
public class NetworkTextView extends AppCompatTextView {

    private NetworkImageSpan[] mNetworkImageSpans;
    private boolean mHasDraweeInText;

    public NetworkTextView(Context context) {
        this(context, null);
    }

    public NetworkTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setIncludeFontPadding(false);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        mHasDraweeInText = false;
        if (text instanceof Spannable) {
            mNetworkImageSpans = ((Spanned) text).getSpans(0, text.length(),
                    NetworkImageSpan.class);
            mHasDraweeInText = mNetworkImageSpans.length > 0;
        }
        super.setText(text, type);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mNetworkImageSpans == null) {
            return;
        }
        for (NetworkImageSpan image : mNetworkImageSpans) {
            image.onDetach();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mNetworkImageSpans == null) {
            return;
        }
        for (NetworkImageSpan image : mNetworkImageSpans) {
            image.onAttach();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (mHasDraweeInText) {
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }
}
