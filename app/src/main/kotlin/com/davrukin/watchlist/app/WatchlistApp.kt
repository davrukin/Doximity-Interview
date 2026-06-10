package com.davrukin.watchlist.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.davrukin.watchlist.presentation.search.SearchPresenter
import com.davrukin.watchlist.presentation.search.SearchScreen
import com.davrukin.watchlist.presentation.watchlist.WatchlistPresenter
import com.davrukin.watchlist.presentation.watchlist.WatchlistScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data object WatchlistKey : NavKey

@Serializable
data object SearchKey : NavKey

@Composable
fun WatchlistApp() {
    val backStack = rememberNavBackStack(WatchlistKey)
    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
        },
        entryProvider =
            entryProvider {
                entry<WatchlistKey> {
                    val presenter: WatchlistPresenter = koinInject<WatchlistPresenter>()
                    val params: WatchlistPresenter.Params =
                        remember(key1 = backStack) {
                            WatchlistPresenter.Params(
                                onOpenSearch = {
                                    backStack.add(SearchKey)
                                },
                            )
                        }
                    WatchlistScreen(
                        model = presenter.present(params = params),
                    )
                }
                entry<SearchKey> {
                    val presenter: SearchPresenter = koinInject<SearchPresenter>()
                    val params: SearchPresenter.Params =
                        remember(key1 = backStack) {
                            SearchPresenter.Params(
                                onBack = {
                                    backStack.removeLastOrNull()
                                },
                            )
                        }
                    SearchScreen(
                        model = presenter.present(params = params),
                    )
                }
            },
    )
}
