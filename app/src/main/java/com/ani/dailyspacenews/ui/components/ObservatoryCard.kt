package com.ani.dailyspacenews.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ani.dailyspacenews.ui.theme.BgElevated
import com.ani.dailyspacenews.ui.theme.BorderHairline

@Composable
fun ObservatoryCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BgElevated,
    borderColor: Color = BorderHairline,
    cornerRadius: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardPressAlpha"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(clickModifier)
            .defaultMinSize(minHeight = 48.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
