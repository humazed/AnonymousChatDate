package humazed.github.com.anonymouschatanddate

import android.app.Application
import android.support.multidex.MultiDex
import com.facebook.stetho.Stetho

/**
 * User: YourPc
 * Date: 12/10/2017
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)

        Stetho.initializeWithDefaults(this)

//        saveLanguage(ARABIC)
    }
}