package com.haha.networktextview;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

/**
 * Created by zhoumao on 2019/4/23.
 * Description:
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // initialize fresco with enabled webp
        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this)
                .experiment()
                .setWebpSupportEnabled(true)
                .build());
    }
}
