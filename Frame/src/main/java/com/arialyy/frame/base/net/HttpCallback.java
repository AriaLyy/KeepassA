/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.arialyy.frame.base.net;
//
//import com.arialyy.frame.util.show.FL;
//import com.arialyy.frame.util.show.L;
//
//import io.reactivex.Observable;
//import io.reactivex.schedulers.Schedulers;
//
///**
// * Created by “Aria.Lao” on 2016/10/26.
// * HTTP数据回调
// */
//public abstract class HttpCallback<T> implements INetResponse<T>, Observable.Transformer<T, T> {
//
//  @Override public void onFailure(Throwable e) {
//    L.e("HttpCallback", FL.getExceptionString(e));
//  }
//
//  @Override public Observable<T> call(Observable<T> observable) {
//    Observable<T> tObservable = observable.subscribeOn(Schedulers.io())
//        .unsubscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .map(new Func1<T, T>() {
//          @Override public T call(T t) {
//            onResponse(t);
//            return t;
//          }
//        })
//        .onErrorReturn(new Func1<Throwable, T>() {
//          @Override public T call(Throwable throwable) {
//            onFailure(throwable);
//            return null;
//          }
//        });
//    tObservable.subscribe();
//    return tObservable;
//  }
//}