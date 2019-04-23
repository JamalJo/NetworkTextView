package com.haha.networktextview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class MainActivity extends AppCompatActivity {

    private NetworkTextView mNetworkTextView;
    private final static String mImgUrl1 =
            "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1556021891228&di"
                    + "=1a429bba7d719abfe71f52ec0db12b75&imgtype=0&src=http%3A%2F%2Fwww.xwcms"
                    + ".net%2FwebAnnexImages%2FfileAnnex%2F201506%2F14305%2F12-travel"
                    + "-transportation.png";

    private final static String mImgUrl2 =
            "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1556021891414&di"
                    + "=0853cdda76804778ce0049c7ad944e59&imgtype=0&src=http%3A%2F%2Fimg.zcool"
                    + ".cn%2Fcommunity%2F019bf75641489b6ac7259e0fa3367c.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initData();
    }

    private void initViews() {
        mNetworkTextView = findViewById(R.id.network_txt);
    }

    private void initData() {
        SpannableStringBuilder mSpannableStringBuilder = new SpannableStringBuilder();
        mSpannableStringBuilder.clear();
        mSpannableStringBuilder.append(" ");
        NetworkImageSpan imageSpan1 = createImageSpan(50, mImgUrl1);
        mSpannableStringBuilder.setSpan(imageSpan1, 0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mSpannableStringBuilder.append(
                "左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边"
                        + "左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边是网络上加载的图片左边");
        mSpannableStringBuilder.append(" ");
        NetworkImageSpan imageSpan2 = createImageSpan(100, mImgUrl2);
        mSpannableStringBuilder.setSpan(imageSpan2, mSpannableStringBuilder.length() - 1,
                mSpannableStringBuilder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mNetworkTextView.setText(mSpannableStringBuilder);
    }

    private NetworkImageSpan createImageSpan(int size, String imgUrl) {
        NetworkImageSpan mNetworkImageSpan = new NetworkImageSpan.Builder()
                .setMarginRight(8)
                .setHeight(size)
                .setWidth(size)
                .setAttachedView(mNetworkTextView)
                .build();
        mNetworkImageSpan.setImgUrl(imgUrl);
        return mNetworkImageSpan;
    }
}
