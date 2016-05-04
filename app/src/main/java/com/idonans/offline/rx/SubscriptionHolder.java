package com.idonans.offline.rx;

import android.support.annotation.Nullable;

import rx.Subscription;

/**
 * 辅助控制 Subscription 的释放
 * Created by idonans on 16-5-4.
 */
public class SubscriptionHolder {

    private Subscription mSubscription;

    /**
     * unsubscribe last subscription if need.
     */
    public void setSubscription(@Nullable Subscription subscription) {
        if (mSubscription == subscription) {
            return;
        }
        if (mSubscription != null) {
            if (!mSubscription.isUnsubscribed()) {
                mSubscription.unsubscribe();
            }
        }
        mSubscription = subscription;
    }

}
