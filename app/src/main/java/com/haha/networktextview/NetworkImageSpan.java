package com.haha.networktextview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Created by zhoumao on 2019/4/8.
 * Description: 网络图片Span
 * 图片回收策略：
 * 1. 请求成功，将之前的image关闭；
 * 2. detach后，将当前image关闭；
 * 3. attach后，重新提交请求，获取image；
 * 4. 给textView设置新imageUrl后，释放之前的image，然后重新提交请求，获取image；
 */
public class NetworkImageSpan extends DynamicDrawableSpan implements DeferredReleaser.Releasable {

    private final ForwardingDrawable mActiveDrawable;
    private final DeferredReleaser mDeferredReleaser;

    private NetworkTextView mAttachedView;

    private String mImgUrl;
    private int mWidth;
    private int mHeight;
    private Rect mMargin;

    private Drawable mDrawable;
    private Drawable mPlaceHolder;

    private CloseableReference<CloseableImage> mFetchedImage;
    private DataSource<CloseableReference<CloseableImage>> mDataSource;
    private boolean mIsRequestSubmitted;

    private Context mContext;


    private NetworkImageSpan(NetworkTextView attachedView, int width, int height,
            Drawable placeHolder) {
        mWidth = width;
        mHeight = height;
        mPlaceHolder = placeHolder;
        if (mPlaceHolder == null) {
            mPlaceHolder = new GradientDrawable();
        }
        mActiveDrawable = new ForwardingDrawable(mPlaceHolder);
        mAttachedView = attachedView;
        mContext = mAttachedView.getContext();
        mMargin = new Rect();
        mDeferredReleaser = DeferredReleaser.getInstance();
        mActiveDrawable.setBounds(0, 0, mWidth, mHeight);
    }

    @Override
    public Drawable getDrawable() {
        return mActiveDrawable;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start,
            int end, @Nullable Paint.FontMetricsInt fm) {
        Rect rect = getDrawable().getBounds();
        if (fm != null) {
            fm.ascent = -rect.bottom - mMargin.top;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = rect.bottom ;
        }
        return rect.right + mMargin.left + mMargin.right;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start,
            int end, float x, int top, int y, int bottom,
            @NonNull Paint paint) {
        super.draw(canvas, text, start, end, x + mMargin.left, top, y, bottom, paint);
    }

    public void onAttach() {
        mActiveDrawable.setCallback(mAttachedView);   // span刷新回调TextView的刷新
        mDeferredReleaser.cancelDeferredRelease(this);
        if (!mIsRequestSubmitted) {                  // mFetchedImage close后，需要重新提交请求，获取bitmap；
            submitRequest();
        }
    }

    public void onDetach() {
        mImgUrl = null;
        if (mDataSource != null && !mDataSource.isClosed()) {
            mDataSource.close();
        }
        mDeferredReleaser.scheduleDeferredRelease(this);
        mActiveDrawable.setCallback(null);      // 取消span对TextView的刷新回调
    }

    public void setImgUrl(String imgUrl) {
        if (imgUrl == mImgUrl) { // 重复imgUrl的情况下，不需要重新请求
            return;
        }
        reset();
        release();            // 释放之前的img
        mImgUrl = imgUrl;
        onAttach();           // 重新请求
    }

    public int getHeight() {
        return mHeight;
    }

    private void reset() {
        setImage(mPlaceHolder);
    }

    private void setImage(Drawable drawable) {
        if (mDrawable != drawable) {
            setActiveDrawable(drawable);
            mDrawable = drawable;
        }
    }

    private void submitRequest() {
        if (TextUtils.isEmpty(mImgUrl)) {
            return;
        }
        if (mDataSource != null && !mDataSource.isClosed()) {
            mDataSource.close();
        }
        mIsRequestSubmitted = true;
        mDataSource = Fresco.getImagePipeline().fetchDecodedImage(getImageRequest(), null);
        final String id = getId();
        DataSubscriber<CloseableReference<CloseableImage>> subscriber =
                new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
                    @Override
                    protected void onNewResultImpl(
                            DataSource<CloseableReference<CloseableImage>> dataSource) {
                        boolean isFinished = dataSource.isFinished();
                        CloseableReference<CloseableImage> result = dataSource.getResult();
                        if (result != null) {
                            onNewResultInternal(id, dataSource, result, isFinished);
                        } else if (isFinished) {
                            onFailureInternal(id, dataSource);
                        }
                    }

                    @Override
                    protected void onFailureImpl(
                            DataSource<CloseableReference<CloseableImage>> dataSource) {
                        onFailureInternal(id, dataSource);
                    }
                };
        mDataSource.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
    }

    private String getId() {
        return String.valueOf(mImgUrl.hashCode());
    }

    private ImageRequest getImageRequest() {
        Uri uri = Uri.parse(mImgUrl);
        ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
                .build();

        return ImageRequestBuilder
                .newBuilderWithSource(uri)
                .setImageDecodeOptions(decodeOptions)
                .setAutoRotateEnabled(true)
                .setLocalThumbnailPreviewsEnabled(true)
                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                .setProgressiveRenderingEnabled(false)
                .setResizeOptions(new ResizeOptions(mWidth, mHeight))
                .build();
    }

    private void onFailureInternal(String id,
            DataSource<CloseableReference<CloseableImage>> dataSource) {
        if (!getId().equals(id)
                || !mIsRequestSubmitted) { // 当前请求还未提交
            dataSource.close();
            return;
        }
        reset();//失败情况下，置空
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void onNewResultInternal(String id,
            DataSource<CloseableReference<CloseableImage>> dataSource,
            CloseableReference<CloseableImage> result,
            boolean isFinished) {

        // 当前url的唯一id和request回来的唯一id不一致，或当前dataSource和请求回来的dataSource不一致
        if (!getId().equals(id)
                || mDataSource != dataSource
                || !mIsRequestSubmitted) {  // 当前请求还未提交
            CloseableReference.closeSafely(result);
            dataSource.close();
            return;
        }
        if (!isFinished) {
            return;
        }
        Drawable drawable;
        try {
            drawable = createDrawable(result);
        } catch (Exception exception) {
            CloseableReference.closeSafely(result);
            onFailureInternal(id, dataSource);
            return;
        }

        CloseableReference previousImage = mFetchedImage;
        mFetchedImage = result;
        try {
            setImage(drawable);
            if (mDataSource != null) {
                mDataSource.close();
            }
            mDataSource = null;
        } finally {
            if (previousImage != null && previousImage != result) {  // 前一个图片资源释放
                CloseableReference.closeSafely(previousImage);
            }
        }
    }

    private void setActiveDrawable(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        mActiveDrawable.setDrawable(drawable);
    }

    private Drawable createDrawable(CloseableReference<CloseableImage> result) {
        CloseableImage closeableImage = result.get();
        if (closeableImage instanceof CloseableStaticBitmap) {
            CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
            return new BitmapDrawable(mContext.getResources(),
                    closeableStaticBitmap.getUnderlyingBitmap());
        }
        throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
    }


    @Override
    public void release() {
        mIsRequestSubmitted = false;
        if (mFetchedImage != null) {
            CloseableReference.closeSafely(mFetchedImage);
            mFetchedImage = null;
        }
    }

    public static class Builder {
        private int width = 52;   //默认值，不具参考性
        private int height = 30;  //默认值，不具参考性
        private Drawable placeHolder;
        private int marginLeft;
        private int marginRight;
        private NetworkTextView attachedView;

        public Builder() {
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setAttachedView(NetworkTextView attachedView) {
            this.attachedView = attachedView;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setPlaceHolder(Drawable placeHolder) {
            this.placeHolder = placeHolder;
            return this;
        }

        public Builder setMarginLeft(int marginLeft) {
            this.marginLeft = marginLeft;
            return this;
        }

        public Builder setMarginRight(int marginRight) {
            this.marginRight = marginRight;
            return this;
        }

        public NetworkImageSpan build() {
            NetworkImageSpan networkImageSpan = new NetworkImageSpan(attachedView, width, height,
                    placeHolder);
            networkImageSpan.mMargin.set(marginLeft, 0, marginRight, 0);
            return networkImageSpan;
        }
    }
}
