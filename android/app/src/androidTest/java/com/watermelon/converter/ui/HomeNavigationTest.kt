// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

package com.watermelon.converter.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watermelon.converter.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun home_shows_primary_actions() {
        rule.onNodeWithText("Convert a single SVG").assertIsDisplayed()
        rule.onNodeWithText("Batch convert a ZIP").assertIsDisplayed()
    }

    @Test fun navigate_to_settings_and_back() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Preview resolution").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("Convert a single SVG").assertIsDisplayed()
    }

    @Test fun navigate_to_history_empty_state() {
        rule.onNodeWithText("History").performClick()
        rule.onNodeWithText("No conversions yet this session.").assertIsDisplayed()
    }
}
