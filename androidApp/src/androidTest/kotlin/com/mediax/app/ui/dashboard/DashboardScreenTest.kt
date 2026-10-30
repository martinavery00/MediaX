package com.mediax.app.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mediax.app.ui.theme.MediaXTheme
import com.mediax.shared.model.DashboardSnapshot
import com.mediax.shared.model.DashboardSummary
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsDashboardSummary() {
        composeRule.setContent {
            MediaXTheme {
                DashboardScreen(
                    state =
                        DashboardUiState.Content(
                            DashboardSnapshot(DashboardSummary(12, 8, 2_400), emptyList()),
                        ),
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Media dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("No recent media").assertIsDisplayed()
    }

    @Test
    fun errorShowsRetryAction() {
        composeRule.setContent {
            MediaXTheme {
                DashboardScreen(
                    state = DashboardUiState.Error("Unavailable"),
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
