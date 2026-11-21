package me.rerere.rikkahub.wear.pages.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.pages.favorite.FavoriteVM
import me.rerere.rikkahub.ui.pages.favorite.NodeFavoriteListItem
import me.rerere.rikkahub.utils.toLocalDateTime
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearConfirmDialog
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.infoItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun WearFavoritesPage(
    onBack: () -> Unit,
    onOpenChat: (kotlin.uuid.Uuid) -> Unit,
) {
    val vm: FavoriteVM = koinViewModel()
    val favorites by vm.nodeFavorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<NodeFavoriteListItem?>(null) }

    WearScreen(title = "Favorites", onBack = onBack) {
        infoItem("Saved messages", favorites.size.toString())
        if (favorites.isEmpty()) {
            textItem("No favorites yet.")
            textItem(
                "Favorite a message from the phone app to find it here.",
                small = true,
                muted = true,
            )
        } else {
            section("Saved messages")
            favorites.forEach { favorite ->
                item(key = "favorite:${favorite.id}") {
                    FavoriteRow(
                        item = favorite,
                        onClick = { onOpenChat(favorite.conversationId) },
                        onDelete = { pendingDelete = favorite },
                    )
                }
            }
        }
    }

    pendingDelete?.let { favorite ->
        WearConfirmDialog(
            visible = true,
            title = "Delete favorite?",
            message = "The message itself stays in the conversation.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                pendingDelete = null
                scope.launch { vm.removeFavorite(favorite.refKey) }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun FavoriteRow(
    item: NodeFavoriteListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.conversationTitle.ifBlank { "Untitled conversation" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.preview.isNotBlank()) {
                    Text(
                        text = item.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = Instant.ofEpochMilli(item.createdAt).toLocalDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = WearIcons.Delete,
                    contentDescription = "Delete favorite",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
