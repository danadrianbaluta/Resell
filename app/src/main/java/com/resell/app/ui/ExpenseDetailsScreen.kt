package com.resell.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resell.app.data.AppScreen
import com.resell.app.data.Expense
import com.resell.app.data.ProductRepository
import com.resell.app.data.formatDateForDisplay
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun ExpenseDetailsScreen(
    existingExpense: Expense?,
    repository: ProductRepository,
    onAfterSave: (Expense) -> Unit,
    onAfterDelete: () -> Unit,
    onSelectScreen: (AppScreen) -> Unit
) {
    val original = remember(existingExpense) {
        existingExpense ?: Expense(date = formatDateForDisplay(LocalDate.now()))
    }
    var draft by remember(existingExpense?.id) { mutableStateOf(original) }
    var pendingScreen by remember { mutableStateOf<AppScreen?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hasChanges = draft != original
    val scope = rememberCoroutineScope()

    fun saveAndThen(after: () -> Unit) {
        scope.launch {
            repository.saveExpense(draft)
            after()
        }
    }

    BackHandler(enabled = hasChanges) {
        pendingScreen = AppScreen.EXPENSES
        showUnsavedDialog = true
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Expense details", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (draft.comment.isBlank()) "Create or refine an expense" else draft.comment,
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScreenSelector(current = AppScreen.EXPENSE_DETAILS, onSelected = { screen ->
                        if (screen == AppScreen.EXPENSE_DETAILS) return@ScreenSelector
                        if (hasChanges) {
                            pendingScreen = screen
                            showUnsavedDialog = true
                        } else {
                            onSelectScreen(screen)
                        }
                    }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { saveAndThen { onAfterSave(draft) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = draft.amount,
                            onValueChange = { draft = draft.copy(amount = it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Amount") },
                            prefix = {
                                if (draft.amount.isNotBlank()) Text("\u00A3")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        DateField(
                            label = "Date",
                            value = draft.date,
                            showBorder = true,
                            modifier = Modifier.fillMaxWidth()
                        ) { draft = draft.copy(date = it) }
                        OutlinedTextField(
                            value = draft.comment,
                            onValueChange = { draft = draft.copy(comment = it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Comment") }
                        )
                    }
                }

                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Permanent delete", style = MaterialTheme.typography.titleMedium, color = BrandOrange)
                            Text("Remove this expense forever. This cannot be undone.", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save changes?") },
            text = { Text("You have unsaved changes. Do you want to save before leaving this page?") },
            confirmButton = {
                TextButton(onClick = {
                    saveAndThen {
                        val next = pendingScreen
                        pendingScreen = null
                        showUnsavedDialog = false
                        if (next == null || next == AppScreen.EXPENSES) onAfterSave(draft) else onSelectScreen(next)
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val next = pendingScreen
                    pendingScreen = null
                    showUnsavedDialog = false
                    if (next != null) onSelectScreen(next)
                }) { Text("No") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete permanently?") },
            text = { Text("Are you sure? This action is irreversible.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteExpense(draft.id)
                        showDeleteDialog = false
                        onAfterDelete()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
