package com.rokkogovsek.devtimer

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeLineCounterTest {

    @Test
    fun `counts Kotlin files recursively and ignores non-code lines`() {
        withTemporaryDirectory { directory ->
            File(directory, "Main.kt").writeText(
                """
                package sample

                // This line is ignored.
                fun main() {
                    val url = "https://example.com" // Inline comments keep the code line.
                    /*
                     * This block is ignored.
                     */
                    println(url)
                }
                """.trimIndent()
            )

            val nestedDirectory = File(directory, "nested").apply { mkdirs() }
            File(nestedDirectory, "BuildScript.kts").writeText(
                """
                /* A comment-only line. */
                plugins {
                    kotlin("jvm")
                }
                """.trimIndent()
            )
            File(directory, "notes.txt").writeText("This file must be ignored.")

            val buildDirectory = File(directory, "build/generated").apply { mkdirs() }
            File(buildDirectory, "Generated.kt").writeText("val generated = true")

            assertEquals(8, countKotlinCodeLines(directory))
        }
    }

    @Test
    fun `handles comment markers inside string and character literals`() {
        withTemporaryDirectory { directory ->
            File(directory, "Strings.kt").writeText(
                """
                val url = "https://example.com/path"
                val slash = '/'
                // ignored
                """.trimIndent()
            )

            assertEquals(2, countKotlinCodeLines(directory))
        }
    }

    @Test
    fun `returns zero for a missing or non-directory path`() {
        val missingDirectory = File("does-not-exist-${System.nanoTime()}")
        assertEquals(0, countKotlinCodeLines(missingDirectory))

        val temporaryFile = Files.createTempFile("devtimer", ".kt").toFile()
        try {
            assertEquals(0, countKotlinCodeLines(temporaryFile))
        } finally {
            temporaryFile.delete()
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("devtimer-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
