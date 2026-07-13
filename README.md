# DevTimer – Developer Productivity Tracker

DevTimer is a Kotlin desktop application for tracking focused coding sessions and visualizing project activity. It monitors Kotlin source files in a selected project directory, stores completed sessions locally, and presents monthly code-change statistics.

## Features

- Select a local development project
- Choose a 25, 45, or 60 minute focus session
- Start, pause, and reset the timer
- Count non-empty Kotlin `.kt` and `.kts` code lines
- Ignore blank lines, comments, and generated/build directories
- Store projects and completed sessions in a local SQLite database
- Show information about the latest completed session
- Filter statistics by month and year
- Visualize daily project activity with a column chart

## Tech Stack

- **Kotlin**
- **Compose Multiplatform for Desktop**
- **SQLDelight**
- **SQLite**
- **Kotlin Coroutines**
- **Compose Charts**
- **Gradle Kotlin DSL**
- **Kotlin Test and JUnit**

## How It Works

1. Select a directory containing a Kotlin project.
2. Choose the desired focus-session duration.
3. DevTimer records the current Kotlin code-line count when the session starts.
4. When the timer finishes, the project is scanned again.
5. Added, removed, and changed line totals are stored in SQLite.
6. Stored activity can be viewed by month and year in the statistics screen.

DevTimer only reads project files to calculate line counts. Source code is not uploaded or sent to an external service.

## Project Structure

```text
composeApp/src/
├── commonMain/sqldelight/     # SQLDelight database schema and queries
├── jvmMain/kotlin/            # Desktop UI, repository, and application logic
└── jvmTest/kotlin/            # Unit and database integration tests
```

Important classes and files:

```text
Main.kt                 Desktop application entry point
DevTimerApp.kt          Compose user interface and timer state
TimerRepository.kt      Project and session persistence
DatabaseManager.kt      SQLite database initialization
CodeLineCounter.kt      Kotlin source-line analysis
SessionMetrics.kt       Added and removed line calculations
TimeFormatter.kt        Timer display formatting
```

## Getting Started

### Requirements

- JDK 17 or newer
- Git

### Clone the Repository

```bash
git clone https://github.com/RokKog/devtimer-desktop.git
cd devtimer-desktop
```

### Run the Application

Linux and macOS:

```bash
./gradlew :composeApp:run
```

Windows:

```powershell
gradlew.bat :composeApp:run
```

## Run Tests

```bash
./gradlew :composeApp:jvmTest
```

The test suite covers:

- Timer formatting
- Added and removed line calculations
- Recursive Kotlin line counting
- Comment handling
- Project deduplication
- Session persistence
- Daily statistics aggregation
- Latest-session retrieval

## Build a Native Package

Create a package for the current operating system:

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

Supported package formats are configured for:

- Linux: `.deb`
- Windows: `.msi`
- macOS: `.dmg`

## Local Data

The SQLite database is stored in the current user's home directory:

```text
~/devtimer.db
```

Delete this file to reset all stored projects and sessions.

## Current Limitations

- Source analysis currently supports Kotlin `.kt` and `.kts` files only.
- Line changes are calculated from total line-count differences rather than a Git diff.
- A session is saved only after the timer reaches zero.

## Possible Future Improvements

- Git-based added, modified, and deleted line statistics
- Support for more programming languages
- Custom session durations
- Session history and deletion
- CSV or JSON report export
- Application icons and signed installers

## Author

**Rok Kogovsek**

- GitHub: [RokKog](https://github.com/RokKog)

## License

This project is available under the [MIT License](LICENSE).
