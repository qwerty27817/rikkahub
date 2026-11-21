package me.rerere.rikkahub.wear.pages.mediacreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.data.model.MediaCreationSession
import me.rerere.rikkahub.ui.pages.mediacreation.MediaCreationSessionsVM
import me.rerere.rikkahub.utils.toLocalDateTime
import me.rerere.rikkahub.wear.WearIcons
import me.rerere.rikkahub.wear.components.WearScreen
import me.rerere.rikkahub.wear.components.buttonItem
import me.rerere.rikkahub.wear.components.section
import me.rerere.rikkahub.wear.components.textItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearMediaCreationSessionsPage(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val vm: MediaCreationSessionsVM = koinViewModel()
    val sessions by vm.sessions.collectAsStateWithLifecycle()

    WearScreen(title = "Media sessions", onBack = onBack) {
        buttonItem(label = "New session", icon = WearIcons.Add) {
            vm.newSession { id -> onOpenSession(id.toString()) }
        }
        when {
            sessions == null -> textItem("Loading sessions…", small = true, muted = true)
            sessions?.isEmpty() == true -> textItem(
                "No media creation sessions yet. Create one to generate images or videos.",
                small = true,
                muted = true,
            )

            else -> {
                section("Sessions")
                sessions.orEmpty().forEach { session ->
                    item(key = "session:${session.id}") {
                        SessionRow(
                            session = session,
                            onClick = { onOpenSession(session.id.toString()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: MediaCreationSession, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = session.title.ifBlank { "Untitled session" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(if (session.nodeCount == 1) "1 item" else "${session.nodeCount} items")
                    if (session.activeCount > 0) append(" · ${session.activeCount} running")
                    append(" · ${session.updateAt.toLocalDateTime()}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
