# Hyprlang Kotlin Parser

A Kotlin/Android wrapper for the official [hyprlang](https://github.com/hyprwm/hyprlang) C++ library. This library allows you to parse Hyprland configuration files directly in your Android applications using JNI.

## Features

- Parse Hyprland configuration strings.
- Register configuration keys dynamically (INT, FLOAT, STRING, VEC2).
- Retrieve parsed values safely.
- Bundle as an Android Archive (AAR).

## Installation

### Maven Local (for testing)

1. Build and publish to your local Maven repository:
   ```bash
   ./gradlew :parser:publishReleasePublicationToMavenLocal
   ```

2. Add `mavenLocal()` to your project's repositories.

3. Add the dependency:
   ```kotlin
   dependencies {
       implementation("dev.cphiri.hyprlang:parser:0.0.1")
   }
   ```

### GitHub Packages

1. Add the repository to your `build.gradle.kts`:
   ```kotlin
   repositories {
       maven {
           name = "GitHubPackages"
           url = uri("https://maven.pkg.github.com/spacedao/hyprlang")
           credentials {
               username = System.getenv("GITHUB_ACTOR")
               password = System.getenv("GITHUB_TOKEN")
           }
       }
   }
   ```

2. Add the dependency:
   ```kotlin
   dependencies {
       implementation("dev.cphiri.hyprlang:parser:0.0.1")
   }
   ```

## Usage

```kotlin
import dev.cphiri.hyprlang.parser.HyprlangParser

// Use try-with-resources (use) to automatically close the parser
HyprlangParser().use { parser ->
```
    // 1. Register Configuration Keys
    parser.addConfigValue("general:border_size", "INT")
    parser.addConfigValue("general:gaps_in", "INT")
    parser.addConfigValue("opacity", "FLOAT")
    parser.addConfigValue("terminal", "STRING")

    // 2. Define Input String
    val configContent = """
        general {
            border_size = 2
            gaps_in = 5
        }
        opacity = 0.95
        terminal = kitty
    """.trimIndent()

    // 3. Parse and Check for Errors
    val error = parser.parse(configContent)

    if (error.isEmpty()) {
        println("Configuration parsed successfully!")
        
        // 4. Retrieve Values
        val borderSize = parser.getInt("general:border_size")
        val opacity = parser.getFloat("opacity")
        val terminal = parser.getString("terminal")

        println("Border Size: $borderSize") // 2
        println("Opacity: $opacity")       // 0.95
        println("Terminal: $terminal")     // kitty

    } else {
        println("Parsing error: $error")
    }
}
```

## Development

### Prerequisites

- Android Studio / IntelliJ IDEA
- Android NDK (Side-by-side)
- CMake
- Git

### Build Instructions

1. Clone the repository with submodules:
   ```bash
   git clone --recursive https://github.com/spacedao/hyprlang-android.git
   cd hyprlang-android
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

3. Run tests:
   ```bash
   ./gradlew :parser:connectedAndroidTest
   ```

### Publishing

To publish to GitHub Packages, create a `local.properties` file in the root directory with your credentials:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

Then run:
```bash
./gradlew :parser:publishReleasePublicationToGitHubPackagesRepository
```

## License

This project wraps `hyprlang` which is licensed under the BSD 3-Clause License.
The wrapper code is available under the MIT License.
