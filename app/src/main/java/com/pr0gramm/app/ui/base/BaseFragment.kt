package com.pr0gramm.app.ui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pr0gramm.app.ui.dialogs.OnComplete
import com.pr0gramm.app.ui.dialogs.OnNext
import com.pr0gramm.app.ui.dialogs.subscribeWithErrorHandling
import com.pr0gramm.app.util.Logger
import com.pr0gramm.app.util.debug
import com.pr0gramm.app.util.di.LazyInjectorAware
import com.pr0gramm.app.util.di.PropertyInjector
import com.pr0gramm.app.util.time
import com.trello.rxlifecycle.android.FragmentEvent
import com.trello.rxlifecycle.components.support.RxFragment
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rx.Observable
import rx.Subscription

/**
 * A fragment that provides lifecycle events as an observable.
 */
abstract class BaseFragment(name: String) : RxFragment(), HasViewCache, LazyInjectorAware, AndroidCoroutineScope {
    protected val logger = Logger(name)

    override val injector: PropertyInjector = PropertyInjector()

    override val viewCache: ViewCache = ViewCache { view?.findViewById(it) }

    override lateinit var job: Job

    override val androidContext: Context
        get() = requireContext()


    private lateinit var onStartScope: AndroidCoroutineScope
    private lateinit var onResumeScope: AndroidCoroutineScope

    init {
        debug {
            lifecycle().subscribe { event ->
                logger.info { "Lifecycle ${System.identityHashCode(this)}: $event" }
            }
        }
    }

    fun <T> bindUntilEventAsync(event: FragmentEvent): Observable.Transformer<T, T> {
        return AsyncLifecycleTransformer(bindUntilEvent<T>(event))
    }

    fun <T> bindToLifecycleAsync(): Observable.Transformer<T, T> {
        return AsyncLifecycleTransformer(bindToLifecycle<T>())
    }

    fun <T> Observable<T>.bindToLifecycle(): Observable<T> = compose(this@BaseFragment.bindToLifecycle())

    fun <T> Observable<T>.bindToLifecycleAsync(): Observable<T> = compose(this@BaseFragment.bindToLifecycleAsync())

    override fun onAttach(context: Context) {
        logger.time("Injecting services") { injector.inject(context) }
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        job = SupervisorJob()
    }

    override fun onStart() {
        onStartScope = newChild()
        super.onStart()
    }

    final override fun onResume() {
        super.onResume()

        onResumeScope = onStartScope.newChild()
        onResumeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            doOnResume()
        }
    }

    protected open suspend fun doOnResume() {}

    override fun onPause() {
        super.onPause()
        onResumeScope.cancelScope()
    }

    override fun onStop() {
        super.onStop()
        onStartScope.cancelScope()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.viewCache.reset()

        cancelScope()
    }

    fun <T> Observable<T>.subscribeWithErrorHandling(
            onComplete: OnComplete = {}, onNext: OnNext<T> = {}): Subscription {

        return subscribeWithErrorHandling(childFragmentManager, onComplete, onNext)
    }

    fun setTitle(title: String) {
        val activity = activity as? AppCompatActivity ?: return
        activity.title = title
    }
}
