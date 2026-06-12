package com.davrukin.watchlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.davrukin.watchlist.ui.theme.WatchlistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent(
            content = {
                WatchlistTheme(
                    content = {
                        WatchlistApp()
                    },
                )
            },
        )
    }
}
