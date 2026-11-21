package me.rerere.rikkahub.data.db

import android.content.Context
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import io.requery.android.database.sqlite.SQLiteCustomExtension
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import java.io.File

/**
 * Use the same SQLite extensions for the live database and backup validation.
 *
 * `libsimple` 是 SQLite FTS5 的中文分词扩展（wangfenjin/simple）。它只在部分 ABI 上随包提供：
 * 缺少该 so 的 ABI（例如没有补 v7a 产物时）直接跳过扩展，FTS 表会退化为内置 unicode61 分词，
 * 消息搜索仍可用，不会因为 dlopen 失败导致数据库打不开。
 */
internal object SQLiteConfiguration {
    const val DATABASE_NAME = "rikka_hub"

    fun configure(context: Context, configuration: SQLiteDatabaseConfiguration): SQLiteDatabaseConfiguration {
        val libraryDir = File(context.applicationInfo.nativeLibraryDir)
        val extension = File(libraryDir, "libsimple.so")
        if (extension.isFile) {
            // requery 的扩展路径不带 .so 后缀，由 SQLite 自己补全
            configuration.customExtensions.add(
                SQLiteCustomExtension(File(libraryDir, "libsimple").absolutePath, null)
            )
        }
        return configuration
    }

    fun openHelperFactory(context: Context) = RequerySQLiteOpenHelperFactory(
        listOf(RequerySQLiteOpenHelperFactory.ConfigurationOptions { configure(context, it) })
    )

    /** 当前设备是否带 simple 分词扩展，供 FTS 层选择分词器 / 查询函数。 */
    fun hasSimpleExtension(context: Context): Boolean =
        File(context.applicationInfo.nativeLibraryDir, "libsimple.so").isFile
}
