package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoorUiCujTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `home screen and bottom navigation tabs render and switch cleanly`() {
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_quran").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_listen").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_memorize").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_more").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_quran").performClick()
        composeRule.onNodeWithTag("quran_index_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_listen").performClick()
        composeRule.onNodeWithTag("listen_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_memorize").performClick()
        composeRule.onNodeWithTag("memorization_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_more").performClick()
        composeRule.onNodeWithTag("more_hub_screen").assertIsDisplayed()
    }
}
