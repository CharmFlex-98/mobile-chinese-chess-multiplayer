package com.charmflex.app.mobile_chinese_chess_multiplayer
import android.app.Application
import android.os.StrictMode
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependencies
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AndroidKoinInitializer

class MCCApplication : Application(), AppDependenciesProvider {
    private lateinit var appDependencies: AppDependencies

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen() // Visual indicator
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        val koinInstance = AndroidKoinInitializer(this)
        KoinInitializer.instance = koinInstance
        koinInstance.initialize()

        appDependencies = AppDependencies()
        AppDependenciesProvider.instance = this
    }
    override fun provideAppDependencies(): AppDependencies {
        return appDependencies
    }

}