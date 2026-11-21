package me.rerere.rikkahub.wear.pages.mediacreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.rerere.rikkahub.R
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

    // WearScreen 的 content 是普通 DSL builder（非 @Composable），先算好所有字符串。
    val title = stringResource(R.string.wear_misc_media_sessions_title)
    val newSession = stringResource(R.string.wear_misc_new_session)
    val loadingSessions = stringResource(R.string.wear_misc_media_sessions_loading)
    val emptySessions = stringResource(R.string.wear_misc_media_sessions_empty)
    val sessionsSection = stringResource(R.string.wear_misc_sessions)

    WearScreen(title = title, onBack = onBack) {
        buttonItem(label = newSession, icon = WearIcons.Add) {
            vm.newSession { id -> onOpenSession(id.toString()) }
        }
        when {
            sessions == null -> textItem(loadingSessions, small = true, muted = true)
            sessions?.isEmpty() == true -> textItem(
                emptySessions,
                small = true,
                muted = true,
            )

            else -> {
                section(sessionsSection)
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
    val untitled = stringResource(R.string.wear_misc_untitled_session)
    val itemText = if (session.nodeCount == 1) {
        stringResource(R.string.wear_misc_media_session_items_one, session.nodeCount)
    } else {
        stringResource(R.string.wear_misc_media_session_items_other, session.nodeCount)
    }
    val runningText = stringResource(R.string.wear_misc_media_session_running, session.activeCount)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = session.title.ifBlank { untitled },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(itemText)
                    if (session.activeCount > 0) append(" · $runningText")
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
