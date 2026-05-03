# Onboarding Mentor - IntelliJ IDEA Plugin

**Onboarding Mentor** is an IntelliJ IDEA plugin that uses agentic AI (Anthropic's Claude) to help engineers quickly understand unfamiliar Java codebases (that may get increasingly large). By leveraging a custom tool loop, the agent actively explores the class, its callers, and its internal method calls to give you a comprehensive breakdown of what the class does and how data flows through it.

## Features

- **Agentic Analysis**: Uses `claude-haiku` to analyze classes, allowing the AI to automatically run IntelliJ PSI searches (`get_class_source`, `get_callers`) to trace relationships.
- **Rich Explanations**: Generates proper markdown-formatted explanations directly in a dedicated tool window.
- **Contextual Awareness**: Automatically extracts class fields, annotations, methods, callers, and callees.
- **Seamless Integration**: Triggers easily via a right-click editor action.

## Prerequisites

- **Java Development Kit (JDK)**: JDK 21 is required to build this plugin.
- **Anthropic API Key**: You will need an active Anthropic API key to interact with the Claude models.

## Installation and Setup

### 1. Build and Run the Plugin
To run the plugin locally in a sandboxed IntelliJ IDEA instance, use the included Gradle wrapper:

```bash
./gradlew runIde
```
*(Note: If you run into Java version issues, you can explicitly point Gradle to your JDK 21 installation using `./gradlew runIde -Dorg.gradle.java.home=/path/to/jdk-21`)*

### 2. Configure Your API Key
Once the sandboxed IDE opens:
1. Navigate to **Preferences / Settings** (`Cmd` + `,` on macOS or `Ctrl` + `Alt` + `S` on Windows/Linux).
2. Go to **Tools** > **Onboarding Mentor**.
3. Paste your **Anthropic API Key** into the secure password field and click **Apply**.

## Usage

1. Open any Java project inside the IDE.
2. Open a `.java` file.
3. **Right-click** anywhere inside the editor.
4. Select **Explain with Onboarding Mentor**.
5. The "Onboarding Mentor" tool window will open on the right side of your IDE, displaying an "Analyzing..." message while the agent gathers data.
6. Once complete, you will see a structured, markdown-formatted report covering:
   - What the class does
   - Data flow (inputs and outputs)
   - Key collaborators
   - Things to watch out for

## Architecture & Tech Stack

- **IntelliJ Platform SDK**: Core integration (Actions, ToolWindows, Settings, PSI extraction).
- **OkHttp & Gson**: Network and JSON handling for the Anthropic API.
- **CommonMark**: Renders the AI's markdown responses into rich HTML in the tool window.
- **Language Support**: Designed explicitly for Java (`com.intellij.java` module dependencies).

## Contributing

To modify the agent's behavior, check out `MentorAgentClient.java`. The agent utilizes an autonomous loop allowing up to 3 tool calls per analysis.