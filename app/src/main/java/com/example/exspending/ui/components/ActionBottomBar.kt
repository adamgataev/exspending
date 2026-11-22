package com.example.exspending.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 NavigationBar default container height (NavigationBarTokens). */
private val NavigationBarContainerHeight: Dp = 80.dp

/**
 * Shared action row used as Scaffold bottomBar.
 *
 * When the outer tab NavigationBar is hidden (edit / detail routes), pass
 * [reserveNavigationBarSpace] = true so buttons sit at the same height as on
 * Transactions (where the outer scaffold already pads for the tab bar).
 */
@Composable
fun ActionBottomBar(
    reserveNavigationBarSpace: Boolean,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.End,
    content: @Composable RowScope.() -> Unit
) {
    val bottomExtra =
        if (reserveNavigationBarSpace) NavigationBarContainerHeight else 0.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp + bottomExtra),
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
