package smart.study.planner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Menu hành động cho task (Sửa, Xóa, Xem chi tiết, Đánh dấu)
 */
@Composable
fun TaskActionMenu(
    isCompleted: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: () -> Unit,
    onToggleComplete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expandedMenu = true },
        modifier = Modifier
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Menu",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    DropdownMenu(
        expanded = expandedMenu,
        onDismissRequest = { expandedMenu = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        // Edit option
        DropdownMenuItem(
            text = { Text("Sửa") },
            onClick = {
                expandedMenu = false
                onEdit()
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        // View detail option
        DropdownMenuItem(
            text = { Text("Xem chi tiết") },
            onClick = {
                expandedMenu = false
                onViewDetail()
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        // Toggle complete option
        DropdownMenuItem(
            text = {
                if (isCompleted) {
                    Text("Đánh dấu chưa hoàn thành")
                } else {
                    Text("Đánh dấu hoàn thành")
                }
            },
            onClick = {
                expandedMenu = false
                onToggleComplete()
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = if (isCompleted) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = "Toggle Complete",
                    tint = if (isCompleted) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }
        )

        // Delete option
        DropdownMenuItem(
            text = { Text("Xóa") },
            onClick = {
                expandedMenu = false
                onDelete()
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

/**
 * Dialog xác nhận xóa task
 */
@Composable
fun DeleteConfirmationDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa task?") },
        text = {
            Text(
                "Bạn có chắc muốn xóa task '$taskTitle'? Hành động này không thể hoàn tác."
            )
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa")
            }
        }
    )
}
