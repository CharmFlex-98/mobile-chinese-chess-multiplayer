import com.charmflex.app.mobile_chinese_chess_multiplayer.di.NativeDependencyProvider
import org.koin.dsl.KoinConfiguration

interface KoinInitializer {
    fun initialize(nativeDependencyProvider: NativeDependencyProvider? = null)
    fun initAsync(): KoinConfiguration

    companion object {
        var instance: KoinInitializer? = null
    }
}