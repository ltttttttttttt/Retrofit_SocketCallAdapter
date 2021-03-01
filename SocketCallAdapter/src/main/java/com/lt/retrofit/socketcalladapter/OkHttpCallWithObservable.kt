package com.lt.retrofit.socketcalladapter

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okio.Timeout
import java.io.IOException

/**
 * creator: lt  2021/2/27  lt.dygzs@qq.com
 * effect : 用于包装Observable为OkHttpCall
 * warning:
 */
internal class OkHttpCallWithObservable(
        private val observable: Observable<Any?>,
        private val adapter: SocketCallAdapter,
) : Call {
    private var isCancelled = false
    private var isExecuted = false//是否运行过,一个call对象只允许运行一次
    private var d: Disposable? = null

    override fun clone(): Call = OkHttpCallWithObservable(observable, adapter)

    override fun request(): Request = Request.Builder().url("").build()

    override fun execute(): Response {

    }

    override fun enqueue(responseCallback: Callback) {
        observable.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(object : Observer<Any?> {
                    override fun onSubscribe(d: Disposable) {
                        this@OkHttpCallWithObservable.d = d
                    }

                    override fun onNext(t: Any) {
                        adapter.handlerCallbackRunnable {
                            responseCallback.onResponse(this@OkHttpCallWithObservable,
                                    Response.Builder()
                                            .code(200)
                                            .request(Request.Builder().url("").build())
                                            .protocol(Protocol.HTTP_2)
                                            .message("")
                                            .body(ResponseBody.create(MediaType.get("text/plain"),))
                                            .build()
                            )
                        }
                    }

                    override fun onError(e: Throwable) {
                        adapter.handlerCallbackRunnable {
                            responseCallback.onFailure(this@OkHttpCallWithObservable, IOException(e))
                        }
                    }

                    override fun onComplete() {
                    }
                })
    }

    override fun cancel() {
        isCancelled = true

    }

    override fun isExecuted(): Boolean = isExecuted

    override fun isCanceled(): Boolean = isCancelled

    override fun timeout(): Timeout = Timeout.NONE
}