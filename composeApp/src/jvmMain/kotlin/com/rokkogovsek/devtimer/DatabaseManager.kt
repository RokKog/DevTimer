package com.rokkogovsek.devtimer

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.rokkogovsek.devtimer.database.DevTimerDatabase
import java.io.File
import java.util.Properties

object DatabaseManager {
    val database: DevTimerDatabase by lazy {
        val databaseFile = File(System.getProperty("user.home"), "devtimer.db")
        val shouldCreateSchema = !databaseFile.exists() || databaseFile.length() == 0L

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${databaseFile.absolutePath}",
            properties = Properties()
        )

        if (shouldCreateSchema) {
            DevTimerDatabase.Schema.create(driver)
        }

        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        DevTimerDatabase(driver)
    }
}
