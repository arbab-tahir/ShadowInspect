# 🛡️ ShadowInspect: Advanced Mobile Security & MITRE ATT&CK® Intelligence Suite

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2024.02-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger_Hilt-2.50-orange.svg?style=flat)](https://dagger.dev/hilt/)
[![Security](https://img.shields.io/badge/Cybersecurity-Mobile_Defense-red.svg?style=flat-square)](#)

**ShadowInspect** is an enterprise-grade, highly interactive Android Cybersecurity and Mobile Auditing application. Designed for security professionals, developers, and students, the platform combines real-time threat intelligence with gamified cyber-education, bridging the gap between passive mobile scanning and proactive security awareness.

---

## 🚀 Key Features

### 🔍 1. Threat & Vulnerability Scanning Engines
*   **APK Binary Analysis**: Analyze compiled Android packages (`.apk`) for suspicious permissions, hidden endpoints, and embedded malicious URLs.
*   **On-Device Security Audits**: Conduct automated configuration checks on the host device (Developer Mode status, Root detection indicators, and permission vulnerabilities).
*   **Web Threat Scan**: Inspect uniform resource locators (URLs) for malicious flags, redirect loops, and phishing signatures.
*   **Deep Document & Image Scanning**: Audit files (PDF, DOCX) and metadata (EXIF tags) to detect embedded exploits, macro-based malware, and tracking markers.

### 📊 2. MITRE ATT&CK® Intelligence Browser
*   **Technique Navigator**: Explore the mobile version of the industry-standard MITRE ATT&CK Matrix natively.
*   **Tactical Mapping**: Directly link detected APK vulnerabilities to specific ATT&CK tactics (e.g., *Initial Access, Command and Control, Persistence*).
*   **Exploit Visualizations**: Deep-dive screens displaying technique details, typical threat vectors, and professional mitigation strategies.

### 🎓 3. Gamified Cybersecurity Education Platform
*   **Structured Learning Paths**: Progress through *Beginner*, *Intermediate*, and *Advanced* paths covering key concepts of mobile security.
*   **Interactive Quizzes & Badges**: Validate your understanding of security principles to unlock distinct, cryptographically-inspired digital achievements.
*   **MITRE Interactive Playground**: Explore simulated attack chains to practice real-world detection workflows in a safe sandbox.

### 🔐 4. Cryptographic Analytics & Advanced Administration
*   **Encrypted Exports**: Safely export security inspection reports with local database encryption.
*   **Diagnostics Hub**: Monitor live application health, memory allocation diagnostics, and background threads.
*   **Granular Permission Audits**: Interactive explanations detailing why every permission is requested and how it is used.

---

## 🛠️ Architecture & Tech Stack

This project is built using modern Android architecture guidelines to ensure scalability, testability, and separation of concerns.

```text
       ┌────────────────────────────────────────────────────────┐
       │                   Presentation Layer                   │
       │  (Jetpack Compose UI, Compose Navigation, ViewModels)  │
       └───────────────────────────┬────────────────────────────┘
                                   │
                                   ▼
       ┌────────────────────────────────────────────────────────┐
       │                      Domain Layer                      │
       │    (Use Cases, Business Models, Repository Contracts)  │
       └───────────────────────────┬────────────────────────────┘
                                   │
                                   ▼
       ┌────────────────────────────────────────────────────────┐
       │                       Data Layer                       │
       │ (Room Database, Retrofit API Client, SharedPreferences)│
       └────────────────────────────────────────────────────────┘
```

*   **Language**: Kotlin (Modern functional paradigms, Coroutines, Flow)
*   **UI Framework**: Jetpack Compose (Declarative UI, State Hoisting, Neon-cyber theme)
*   **Dependency Injection**: Dagger Hilt (Modular constructor-injection, scoped components)
*   **Navigation**: Compose Navigation (Type-safe arguments, single-activity layout)
*   **Database & Storage**: Room database (Structured threat signatures, learning progress)
*   **Local Security**: Android Cryptographic Keystore (Encrypted SharedPrefs, report encryption)

---

## 📦 Installation & Setup

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/arbab-tahir/ShadowInspect.git
    cd ShadowInspect
    ```

2.  **LFS (Large File Storage) Requirement**:
    This project uses Git LFS to track TensorFlow Lite models and large MITRE ATT&CK JSON datasets. Make sure you have [Git LFS installed](https://git-lfs.com/) before pulling.
    ```bash
    git lfs install
    git lfs pull
    ```

3.  **Environment Variables (API Keys)**:
    Rename the provided `local.properties.example` file to `local.properties` and add your own API keys. **Never commit real keys.**
    ```properties
    # local.properties
    VIRUSTOTAL_API_KEY=your_key_here
    GEMINI_API_KEY=your_key_here
    URLSCAN_API_KEY=your_key_here
    ABSTRACT_API_KEY=your_key_here
    NUMVERIFY_API_KEY=your_key_here
    IPQUALITY_KEY=your_key_here
    VERIPHONE_KEY=your_key_here
    ```

4.  **Import in Android Studio**:
    *   Open Android Studio (Koala/Ladybug or newer recommended).
    *   Select **File > Open** and select the `ShadowInspect` directory.
    *   Ensure you have JDK 17 selected in Android Studio settings.
    *   Allow Gradle to sync dependencies automatically.

5.  **Run the Application**:
    *   Connect an Android device (via USB/Wi-Fi debugging) or start an Emulator (API 31+ recommended).
    *   Click **Run (Shift + F10)**.

---

## 🎬 Application Demo
*(Demo video link will be provided soon, content me via email if you're interested in this project.)*

---

## 📈 Recruiter Highlight: Engineering Excellence

Here are some highlights that demonstrate industry-standard practices implemented in this codebase:
*   **Clean Architecture**: Complete decoupling of data ingestion, business logic, and UI screens.
*   **Performance Optimization**: Avoidance of unnecessary recompositions using state preservation, lightweight flow maps, and custom thread management.
*   **Strict Security Standards**: High-priority safety parameters including local encryption, strict exception handling, API key protection via `BuildConfig`, and runtime warning policies.
*   **Scalable Theme System**: Fully customizable "Cyber-Neon" theme powered by Jetpack Compose Material 3 color tokens.

---

## 🤝 Contributing
Contributions are welcome. Please ensure that all pull requests follow the existing `Clean Architecture` conventions and that no sensitive information or API keys are committed in any PR.

## 📄 License & Terms

This project is licensed under the Apache License 2.0. Version: **3.0.1**.
For more details, see the settings panel in the app or read the legal documents inside the project files.
