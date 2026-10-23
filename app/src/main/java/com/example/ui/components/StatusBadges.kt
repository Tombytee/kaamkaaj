package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusExpired
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusPurple
import com.example.ui.theme.StatusWarning

@Composable
fun ExpiryStatusBadge(expiryDateMillis: Long?, modifier: Modifier = Modifier) {
    if (expiryDateMillis == null) {
        BadgeTag(
            text = "No Expiry",
            backgroundColor = GlassSurfaceDark,
            borderColor = GlassBorderDark,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val daysLeft = DateUtils.getDaysDifference(expiryDateMillis)

    val (text, color) = when {
        daysLeft < 0 -> Pair("Expired (${-daysLeft}d ago)", StatusExpired)
        daysLeft == 0L -> Pair("Expires Today", StatusExpired)
        daysLeft <= 7 -> Pair("Expires in ${daysLeft}d", StatusWarning)
        daysLeft <= 30 -> Pair("Expires in ${daysLeft}d", StatusWarning)
        else -> Pair("Active (${daysLeft}d)", StatusActive)
    }

    BadgeTag(
        text = text,
        backgroundColor = color.copy(alpha = 0.12f),
        borderColor = color.copy(alpha = 0.25f),
        textColor = color,
        modifier = modifier
    )
}

@Composable
fun PaymentStatusBadge(isPaid: Boolean, dueDateMillis: Long, modifier: Modifier = Modifier) {
    if (isPaid) {
        BadgeTag(
            text = "PAID",
            backgroundColor = StatusActive.copy(alpha = 0.12f),
            borderColor = StatusActive.copy(alpha = 0.25f),
            textColor = StatusActive,
            modifier = modifier
        )
    } else {
        val daysLeft = DateUtils.getDaysDifference(dueDateMillis)
        val (text, color) = when {
            daysLeft < 0 -> Pair("OVERDUE (${-daysLeft}d)", StatusExpired)
            daysLeft == 0L -> Pair("DUE TODAY", StatusWarning)
            daysLeft <= 3 -> Pair("Due in ${daysLeft}d", StatusWarning)
            else -> Pair("Due in ${daysLeft}d", StatusInfo)
        }

        BadgeTag(
            text = text,
            backgroundColor = color.copy(alpha = 0.12f),
            borderColor = color.copy(alpha = 0.25f),
            textColor = color,
            modifier = modifier
        )
    }
}

@Composable
fun WarrantyStatusBadge(expiryDateMillis: Long, modifier: Modifier = Modifier) {
    val daysLeft = DateUtils.getDaysDifference(expiryDateMillis)
    val (text, color) = when {
        daysLeft < 0 -> Pair("Warranty Expired", StatusExpired)
        daysLeft == 0L -> Pair("Expires Today", StatusWarning)
        daysLeft <= 30 -> Pair("${daysLeft}d left", StatusWarning)
        else -> Pair("${daysLeft / 30}m left", StatusPurple)
    }

    BadgeTag(
        text = text,
        backgroundColor = color.copy(alpha = 0.12f),
        borderColor = color.copy(alpha = 0.25f),
        textColor = color,
        modifier = modifier
    )
}

@Composable
fun ReminderStatusBadge(isCompleted: Boolean, dueDateTimeMillis: Long, modifier: Modifier = Modifier) {
    if (isCompleted) {
        BadgeTag(
            text = "DONE",
            backgroundColor = StatusActive.copy(alpha = 0.12f),
            borderColor = StatusActive.copy(alpha = 0.25f),
            textColor = StatusActive,
            modifier = modifier
        )
    } else {
        val daysLeft = DateUtils.getDaysDifference(dueDateTimeMillis)
        val (text, color) = when {
            daysLeft < 0 -> Pair("OVERDUE", StatusExpired)
            daysLeft == 0L -> Pair("TODAY", StatusWarning)
            else -> Pair("PENDING", StatusInfo)
        }
        BadgeTag(
            text = text,
            backgroundColor = color.copy(alpha = 0.12f),
            borderColor = color.copy(alpha = 0.25f),
            textColor = color,
            modifier = modifier
        )
    }
}

@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    BadgeTag(
        text = category.uppercase(),
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
        textColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun BadgeTag(
    text: String,
    backgroundColor: Color,
    borderColor: Color = Color.Transparent,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            ),
            color = textColor
        )
    }
}

