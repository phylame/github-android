package pw.phylame.github

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import io.realm.Realm
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future

private typealias CleanupAction = () -> Unit

class App : Application() {
    companion object {
        lateinit var sharedApp: App
            private set

        private val cleanups = LinkedHashSet<CleanupAction>()

        private val executor by lazy {
            val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            registerClean(pool::shutdown)
            pool
        }

        val githubPreferences: SharedPreferences by lazy {
            sharedApp.getSharedPreferences("github", Context.MODE_PRIVATE)
        }

        fun getString(resId: Int): String = sharedApp.getString(resId)

        fun getString(resId: Int, vararg args: Any): String = sharedApp.getString(resId, *args)

        fun openAssets(name: String): InputStream = sharedApp.assets.open(name)

        fun <R> schedule(action: () -> R, consume: (R) -> Unit): Subscription = Observable.create<R> {
            it.onNext(action())
            it.onCompleted()
        }.subscribeOn(Schedulers.from(executor))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consume)

        fun schedule(action: () -> Unit): Future<*> = executor.submit(action)

        fun registerClean(action: CleanupAction) {
            cleanups += action
        }

        fun cleanup() {
            for (cleanup in cleanups) {
                cleanup()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedApp = this
        Realm.init(this)
    }
}