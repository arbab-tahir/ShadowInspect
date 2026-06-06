# 🛡️ ShadowInspect: A Machine Learning-Driven On-Device Android Forensic Auditing and Threat Intelligence Framework Integrated with MITRE ATT&CK®

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

## 🏗️ System Architecture

ShadowInspect is built on a **five-layer MVVM architecture** following modern Android architecture guidelines. The Model-View-ViewModel pattern enforces strict separation between UI rendering, business logic, and data management — enabling independent testability, clean maintainability, and modular scalability.

<p align="center">
  <img src="docs/architecture_diagram.png" alt="ShadowInspect System Architecture Diagram" width="850"/>
</p>

<details>
<summary><b>📐 Click to expand: Detailed Layer Descriptions</b></summary>
<br/>

| Layer | Responsibility | Key Components |
|:------|:--------------|:---------------|
| **Presentation Layer** | Renders the UI and captures user input. Built entirely with Jetpack Compose — zero XML dependency. | `Activities`, `Fragments`, `Jetpack Compose UI`, `Navigation Component` |
| **ViewModel Layer** | Mediates between business logic and UI. Exposes reactive `StateFlow` streams observed by the UI and processes user actions. | `UrlScanViewModel`, `FileScanViewModel`, `PhoneScanViewModel`, `DocumentScanViewModel`, `ImageScanViewModel`, `DashboardViewModel`, `EducationViewModel`, `SettingsViewModel` |
| **Domain Layer** | Core business logic — framework-agnostic, independently unit-testable. Defines repository contracts and use cases. | `Use Cases / Interactors`, `Repository Interfaces`, `Domain Models`, `RiskScoreEngine`, `MitreAnalyzer` |
| **Data Layer** | Implements repository interfaces. Manages all data persistence and remote API communication. | `Repository Implementations`, `Room Database`, `Retrofit API Clients`, `SharedPreferences`, `File System` |
| **Security & ML Layer** | Specialized security services powering the core intelligence engine. | `TensorFlow Lite Models`, `MITRE ATT&CK Parser`, `Permission Analyzer`, `Steganography Detector`, `Encryption Manager` |

**External Systems:** `VirusTotal API` · `Phone Validation APIs` · `MITRE ATT&CK Framework`

> **Data Flow Pattern:** Unidirectional — UI dispatches actions → ViewModel processes logic → State updates flow back to UI via `StateFlow` / `Flow` observers. Dependency injection via **Dagger-Hilt** provides all dependencies without manual instantiation, minimizing boilerplate and maximizing testability.

</details>

---

## 🛠️ Tech Stack

| Category | Technology |
|:---------|:-----------|
| **Language** | Kotlin 1.9.x (Coroutines, Flow, StateFlow) |
| **UI Framework** | Jetpack Compose, Material Design 3, Lottie Animations |
| **Dependency Injection** | Dagger-Hilt (`@HiltAndroidApp`, `@Inject`, `@Provides`) |
| **Navigation** | Compose Navigation (Type-safe arguments, single-activity) |
| **Database & Storage** | Room SQLite, Proto DataStore, SharedPreferences |
| **Networking** | Retrofit 2, OkHttp 4, Gson Serialization |
| **Machine Learning** | TensorFlow Lite v2.14.0 (On-device inference) |
| **Security** | Android Keystore, AES Encryption, BuildConfig key abstraction |
| **Version Control** | Git, Git LFS (Large File Storage for ML models & MITRE datasets) |

---

## 📊 ML Model Performance & Accuracy Metrics

The on-device TensorFlow Lite binary classification model was evaluated against a curated dataset of benign and malicious APK samples. Below are the key performance benchmarks:

| Metric | Score | Description |
|:-------|:-----:|:------------|
| **Overall Accuracy** | **94.2%** | Correct classification rate across all test samples |
| **Precision** | **93.8%** | Ratio of true positives among predicted positives |
| **Recall (Sensitivity)** | **95.1%** | Ratio of true positives among actual positives |
| **F1-Score** | **94.4%** | Harmonic mean of precision and recall |
| **False Positive Rate** | **4.7%** | Benign samples incorrectly flagged as malicious |
| **Inference Latency** | **~120ms** | Average per-APK classification time on-device |
| **Model Size** | **~7.8 MB** | Optimized TFLite model footprint |

> **Note:** The model uses permission vectors, intent filters, API call patterns, and manifest metadata as input features for binary risk classification. All inference runs locally — no data leaves the device.

---

## 🗺️ MITRE ATT&CK® Mobile Technique Mapping

ShadowInspect maps detected APK behaviors and device vulnerabilities to the industry-standard [MITRE ATT&CK® Mobile Matrix](https://attack.mitre.org/matrices/mobile/). Below is a summary of the key tactic-technique mappings implemented:

| Tactic | Technique ID | Technique Name | ShadowInspect Detection Method |
|:-------|:-------------|:---------------|:-------------------------------|
| **Initial Access** | T1474 | Supply Chain Compromise | APK signature verification & certificate chain analysis |
| **Initial Access** | T1476 | Deliver Malicious App via Other Means | Unknown source installation flag detection |
| **Execution** | T1575 | Native Code Execution | Native library (`.so`) presence scanning in APK bundles |
| **Persistence** | T1398 | Boot or Logon Initialization Scripts | `BOOT_COMPLETED` broadcast receiver detection in manifest |
| **Persistence** | T1402 | Broadcast Receivers | Implicit broadcast registration analysis |
| **Privilege Escalation** | T1626 | Abuse Elevation Control Mechanism | Device admin permission request detection |
| **Defense Evasion** | T1406 | Obfuscated Files or Information | Code obfuscation indicator analysis (ProGuard/R8 patterns) |
| **Defense Evasion** | T1628 | Hide Artifacts | Hidden activity/service component detection |
| **Credential Access** | T1409 | Access Stored Application Data | `READ_EXTERNAL_STORAGE` and data directory access patterns |
| **Discovery** | T1418 | Software Discovery | `QUERY_ALL_PACKAGES` permission detection |
| **Discovery** | T1426 | System Information Discovery | Device fingerprinting API call pattern analysis |
| **Collection** | T1429 | Capture Audio | `RECORD_AUDIO` permission without user-facing justification |
| **Collection** | T1512 | Capture Camera | `CAMERA` permission analysis in non-camera apps |
| **Collection** | T1636 | Contact & Call Log Access | Contact/call log permission cross-referencing |
| **Command & Control** | T1437 | Application Layer Protocol | Suspicious outbound HTTP/HTTPS endpoint analysis |
| **Exfiltration** | T1646 | Exfiltration Over C2 Channel | Network permission + background service correlation |
| **Impact** | T1447 | Delete Device Data | `WRITE_EXTERNAL_STORAGE` + bulk file operation detection |

> **How It Works:** When a user scans an APK, ShadowInspect's `MitreAnalyzer` engine parses the app's manifest permissions, broadcast receivers, services, and intent filters. Each flagged behavior is cross-referenced against the locally cached MITRE ATT&CK Mobile JSON dataset (~49 MB) to produce technique-level mappings with severity scores and professional mitigation recommendations.

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
