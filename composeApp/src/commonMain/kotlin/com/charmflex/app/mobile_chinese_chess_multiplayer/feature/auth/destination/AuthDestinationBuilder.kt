package com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.destination

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.navigation.DestinationBuilder
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.ui.getViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependencies
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login.LoginViewModel
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.route.AuthRoute
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.ui.login.LoginScreen

internal class AuthDestinationBuilder : DestinationBuilder {
    private val appDependencies: AppDependencies by lazy { AppDependenciesProvider.instance.provideAppDependencies() }

    override fun NavGraphBuilder.buildGraph() {
        composable<AuthRoute.Login> {
            val loginViewModel: LoginViewModel = getViewModel { appDependencies.getLoginViewModel() }
            LoginScreen(loginViewModel)
        }
    }
}