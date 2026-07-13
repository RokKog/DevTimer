package com.rokkogovsek.devtimer

import java.io.File

private val supportedKotlinExtensions = setOf("kt", "kts")
private val ignoredDirectoryNames = setOf(
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    "build",
    "node_modules",
    "out",
    "target"
)

fun countKotlinCodeLines(directory: File): Int {
    if (!directory.isDirectory) {
        return 0
    }

    return directory
        .walkTopDown()
        .onEnter { currentDirectory ->
            currentDirectory == directory || currentDirectory.name !in ignoredDirectoryNames
        }
        .filter { file ->
            file.isFile && file.extension.lowercase() in supportedKotlinExtensions
        }
        .sumOf(::countCodeLinesInFile)
}

private fun countCodeLinesInFile(file: File): Int {
    val commentState = CommentState()

    return file.useLines { lines ->
        lines.count { line -> containsCode(line, commentState) }
    }
}

private data class CommentState(
    var insideBlockComment: Boolean = false
)

private fun containsCode(line: String, state: CommentState): Boolean {
    var index = 0
    var insideString = false
    var insideCharacter = false
    var escaped = false
    var containsCode = false

    while (index < line.length) {
        val current = line[index]
        val next = line.getOrNull(index + 1)

        if (state.insideBlockComment) {
            if (current == '*' && next == '/') {
                state.insideBlockComment = false
                index += 2
            } else {
                index++
            }
            continue
        }

        if (insideString) {
            containsCode = true
            when {
                escaped -> escaped = false
                current == '\\' -> escaped = true
                current == '"' -> insideString = false
            }
            index++
            continue
        }

        if (insideCharacter) {
            containsCode = true
            when {
                escaped -> escaped = false
                current == '\\' -> escaped = true
                current == '\'' -> insideCharacter = false
            }
            index++
            continue
        }

        when {
            current == '/' && next == '/' -> break
            current == '/' && next == '*' -> {
                state.insideBlockComment = true
                index += 2
            }
            current == '"' -> {
                containsCode = true
                insideString = true
                index++
            }
            current == '\'' -> {
                containsCode = true
                insideCharacter = true
                index++
            }
            else -> {
                if (!current.isWhitespace()) {
                    containsCode = true
                }
                index++
            }
        }
    }

    return containsCode
}
