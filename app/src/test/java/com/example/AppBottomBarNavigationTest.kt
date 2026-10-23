package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.example.ui.components.AppBottomBar
import com.example.ui.components.BottomNavDestination
import com.example.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppBottomBarNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyBottomNavDestinationsMapCorrectly() {
        val destinations = BottomNavDestination.entries
        assertEquals(5, destinations.size)
        assertEquals(Routes.HOME, destinations[0].route)
        assertEquals(Routes.DOCUMENTS, destinations[1].route)
        assertEquals(Routes.PAYMENTS, destinations[2].route)
        assertEquals(Routes.REMINDERS, destinations[3].route)
        assertEquals(Routes.MORE, destinations[4].route)
    }

    @Test
    fun testTabSwitchingAcrossAllTabs() {
        val navigatedRoutes = mutableListOf<String>()
        var currentRoute by mutableStateOf(Routes.HOME)

        composeTestRule.setContent {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    navigatedRoutes.add(route)
                    currentRoute = route
                }
            )
        }

        // Tap Documents Tab
        composeTestRule.onNodeWithTag("tab_${Routes.DOCUMENTS}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Routes.DOCUMENTS), navigatedRoutes)

        // Tap Payments Tab
        composeTestRule.onNodeWithTag("tab_${Routes.PAYMENTS}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Routes.DOCUMENTS, Routes.PAYMENTS), navigatedRoutes)

        // Tap Reminders Tab
        composeTestRule.onNodeWithTag("tab_${Routes.REMINDERS}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Routes.DOCUMENTS, Routes.PAYMENTS, Routes.REMINDERS), navigatedRoutes)

        // Tap More Tab
        composeTestRule.onNodeWithTag("tab_${Routes.MORE}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Routes.DOCUMENTS, Routes.PAYMENTS, Routes.REMINDERS, Routes.MORE), navigatedRoutes)

        // Tap Home Tab back
        composeTestRule.onNodeWithTag("tab_${Routes.HOME}").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(Routes.DOCUMENTS, Routes.PAYMENTS, Routes.REMINDERS, Routes.MORE, Routes.HOME), navigatedRoutes)
    }

    @Test
    fun testDragSwitchingNavigation() {
        val navigatedRoutes = mutableListOf<String>()
        var currentRoute by mutableStateOf(Routes.HOME)

        composeTestRule.setContent {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    navigatedRoutes.add(route)
                    currentRoute = route
                }
            )
        }

        // Drag from Home across to Payments/Reminders
        composeTestRule.onNodeWithTag("tab_${Routes.HOME}").performTouchInput {
            down(center)
            moveTo(center.copy(x = center.x + 300f))
            up()
        }
        composeTestRule.waitForIdle()
        // Ensure that touch input triggers navigation
        assert(navigatedRoutes.isNotEmpty())
    }
}
