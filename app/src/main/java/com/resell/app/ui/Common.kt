package com.resell.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.resell.app.data.AppScreen
import com.resell.app.data.formatDateForDisplay
import com.resell.app.data.parseDateOrNull
import java.time.LocalDate

@Composable
fun ScreenSelector(
    current: AppScreen,
    onSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = BrandPurple,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            Text(current.label)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppScreen.entries.filterNot { it == AppScreen.DETAILS }.forEach { screen ->
                DropdownMenuItem(
                    text = { Text(screen.label) },
                    onClick = {
                        expanded = false
                        onSelected(screen)
                    }
                )
            }
        }
    }
}

@Composable
fun DateField(
    label: String,
    value: String,
    enabled: Boolean = true,
    showBorder: Boolean = false,
    containerColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    val current = parseDateOrNull(value) ?: LocalDate.now()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        label = { Text(label) },
        placeholder = { Text("DD-MM-YYYY") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedBorderColor = if (showBorder) CardBorder else Color.Transparent,
            unfocusedBorderColor = if (showBorder) CardBorder else Color.Transparent,
            disabledBorderColor = if (showBorder) CardBorder else Color.Transparent
        ),
        trailingIcon = {
            IconButton(
                enabled = enabled,
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onValueChange(formatDateForDisplay(LocalDate.of(year, month + 1, dayOfMonth)))
                        },
                        current.year,
                        current.monthValue - 1,
                        current.dayOfMonth
                    ).show()
                }
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = "Choose date")
            }
        }
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(22.dp))
    ) {
        content()
    }
}
