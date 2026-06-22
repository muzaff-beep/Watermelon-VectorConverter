// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.ui.screens.FilesScreen
import com.watermelon.converter.ui.screens.HomeScreen
import com.watermelon.converter.ui.screens.SettingsScreen
import com.watermelon.converter.ui.theme.DeepNavy
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.PureWhite
import com.watermelon.converter.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Home", Icons.Filled.Home),
    Tab("Files", Icons.Filled.Search),
    Tab("Settings", Icons.Filled.Settings),
)

@Composable
fun MainPager(nav: NavController, settingsVm: SettingsViewModel) {
    val settings by settingsVm.settings.collectAsState()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DeepNavy,
            ) {
                TABS.forEachIndexed { index, tab ->
                    val selected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            scope.launch {
                                if (settings.slideAnimation) pagerState.animateScrollToPage(index)
                                else pagerState.scrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FreshTeal,
                            selectedTextColor = FreshTeal,
                            unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                            unselectedTextColor = PureWhite.copy(alpha = 0.6f),
                            indicatorColor = DeepNavy,  // no pill highlight — active color alone signals state
                        ),
                    )
                }
            }
        },
    ) { pad ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(pad),
            pageSize = PageSize.Fill,
        ) { page ->
            when (page) {
                0 -> HomeScreen(nav)
                1 -> FilesScreen(nav)
                2 -> SettingsScreen(nav)
                else -> {}
            }
        }
    }
}
