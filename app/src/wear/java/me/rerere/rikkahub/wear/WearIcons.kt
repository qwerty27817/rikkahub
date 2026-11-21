package me.rerere.rikkahub.wear

import androidx.compose.ui.graphics.vector.ImageVector
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.AiMagic
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowReloadHorizontal
import me.rerere.hugeicons.stroke.Backpack01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.Bug01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.ChartLine
import me.rerere.hugeicons.stroke.Chat01
import me.rerere.hugeicons.stroke.CheckmarkCircle02
import me.rerere.hugeicons.stroke.ChevronLeft
import me.rerere.hugeicons.stroke.ChevronRight
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Cpu
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Download01
import me.rerere.hugeicons.stroke.File01
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.GlobalSearch
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.InformationCircle
import me.rerere.hugeicons.stroke.McpServer
import me.rerere.hugeicons.stroke.MessageMultiple01
import me.rerere.hugeicons.stroke.Mic01
import me.rerere.hugeicons.stroke.MoreHorizontal
import me.rerere.hugeicons.stroke.PaintBrush01
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.QrCode
import me.rerere.hugeicons.stroke.Robot01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.ServerStack01
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Star
import me.rerere.hugeicons.stroke.Stop
import me.rerere.hugeicons.stroke.Task01
import me.rerere.hugeicons.stroke.Time04
import me.rerere.hugeicons.stroke.Translate
import me.rerere.hugeicons.stroke.Upload01
import me.rerere.hugeicons.stroke.UserMultiple
import me.rerere.hugeicons.stroke.VolumeHigh

/** 手表 UI 用到的图标集中在这里，页面里直接用 [WearIcons.xxx]，避免每个文件写一长串 import。 */
object WearIcons {
    val Back: ImageVector = HugeIcons.ChevronLeft
    val Forward: ImageVector = HugeIcons.ChevronRight
    val Add: ImageVector = HugeIcons.Add01
    val Chat: ImageVector = HugeIcons.Chat01
    val Settings: ImageVector = HugeIcons.Settings03
    val Star: ImageVector = HugeIcons.Star
    val History: ImageVector = HugeIcons.Time04
    val Cpu: ImageVector = HugeIcons.Cpu
    val Assistant: ImageVector = HugeIcons.UserMultiple
    val Search: ImageVector = HugeIcons.Search01
    val Mcp: ImageVector = HugeIcons.McpServer
    val Server: ImageVector = HugeIcons.ServerStack01
    val Mic: ImageVector = HugeIcons.Mic01
    val Image: ImageVector = HugeIcons.Image02
    val Folder: ImageVector = HugeIcons.Folder01
    val Stats: ImageVector = HugeIcons.ChartLine
    val File: ImageVector = HugeIcons.File01
    val Debug: ImageVector = HugeIcons.Bug01
    val Info: ImageVector = HugeIcons.InformationCircle
    val Theme: ImageVector = HugeIcons.PaintBrush01
    val Retry: ImageVector = HugeIcons.ArrowReloadHorizontal
    val Stop: ImageVector = HugeIcons.Stop
    val Copy: ImageVector = HugeIcons.Copy01
    val Delete: ImageVector = HugeIcons.Delete02
    val More: ImageVector = HugeIcons.MoreHorizontal
    val Expand: ImageVector = HugeIcons.ArrowDown01
    val Skills: ImageVector = HugeIcons.Task01
    val Messages: ImageVector = HugeIcons.MessageMultiple01
    val Ai: ImageVector = HugeIcons.AiMagic
    val Brain: ImageVector = HugeIcons.Brain02
    val Database: ImageVector = HugeIcons.Database02
    val Web: ImageVector = HugeIcons.GlobalSearch
    val Robot: ImageVector = HugeIcons.Robot01
    val Volume: ImageVector = HugeIcons.VolumeHigh
    val Translate: ImageVector = HugeIcons.Translate
    val Qr: ImageVector = HugeIcons.QrCode
    val Download: ImageVector = HugeIcons.Download01
    val Upload: ImageVector = HugeIcons.Upload01
    val Check: ImageVector = HugeIcons.CheckmarkCircle02
    val Cancel: ImageVector = HugeIcons.Cancel01
    val Play: ImageVector = HugeIcons.Play
    val Backup: ImageVector = HugeIcons.Backpack01
}
