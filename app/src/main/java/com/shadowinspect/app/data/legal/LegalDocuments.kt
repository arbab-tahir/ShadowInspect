package com.shadowinspect.app.data.legal

import com.shadowinspect.app.domain.settings.LegalDocument
import com.shadowinspect.app.domain.settings.LegalDocumentType

object LegalDocuments {
    
    val termsOfService = LegalDocument(
        type = LegalDocumentType.TERMS_OF_SERVICE,
        title = "Terms of Service",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # ShadowInspect Terms of Service
            
            **Last Updated: March 16, 2026**
            
            ## 1. Acceptance of Terms
            
            By downloading, installing, or using ShadowInspect ("the App"), you agree to be bound by these Terms of Service. If you do not agree to these terms, do not use the App.
            
            ## 2. Description of Service
            
            ShadowInspect is a mobile security intelligence application designed to:
            - Analyze suspicious digital content (URLs, APK files, documents, images)
            - Provide risk assessments based on the MITRE ATT&CK® framework
            - Educate users about cybersecurity threats
            - Generate forensic-style reports for personal use
            
            The App performs all analysis **locally on your device** and does not upload your files to external servers.
            
            ## 3. User Responsibilities
            
            As a user of ShadowInspect, you agree to:
            
            a. **Lawful Use**: Use the App only for lawful purposes and in accordance with these Terms.
            
            b. **No Hacking**: NOT to use the App for:
                - Unauthorized access to others' devices
                - Developing malicious software
                - Any illegal activities
                - Bypassing security measures on devices you don't own
            
            c. **Accurate Information**: Provide accurate information when using the App's features.
            
            d. **Device Security**: Maintain appropriate security on your device (screen lock, updates).
            
            ## 4. Intellectual Property Rights
            
            a. **App Ownership**: ShadowInspect and its original content, features, and functionality are owned by Malik Muhammad Arbab Tahir and protected by international copyright, trademark, and other intellectual property laws.
            
            b. **MITRE ATT&CK®**: This application uses the MITRE ATT&CK® framework. MITRE ATT&CK® is a registered trademark of The MITRE Corporation. Our use is in accordance with MITRE's terms of use.
            
            c. **Open Source Components**: The App uses various open source libraries. Full attributions and licenses are available in the "Open Source Licenses" section.
            
            d. **User Content**: You retain all rights to any files you scan with the App. We do not claim ownership over your content.
            
            ## 5. Privacy and Data Handling
            
            Your privacy is critically important to us. Please read our [Privacy Policy] for detailed information about how we handle your data.
            
            **Key Privacy Points:**
            - ✅ All scanning is performed locally on your device
            - ✅ No files are uploaded to external servers
            - ✅ No personal information is collected
            - ✅ You control all your data
            - ❌ We do not sell your information
            
            ## 6. Disclaimer of Warranties
            
            THE APP IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT ANY WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED.
            
            a. **No Guarantee of Detection**: While we strive for high accuracy (97%+ in testing), no security tool can guarantee 100% detection of all threats.
            
            b. **Educational Purpose**: Some content is for educational purposes and should not be your sole basis for security decisions.
            
            c. **Not Professional Advice**: The App does not constitute professional security advice. For critical security needs, consult qualified professionals.
            
            ## 7. Limitation of Liability
            
            TO THE MAXIMUM EXTENT PERMITTED BY LAW, MALIK MUHAMMAD ARBAB TAHIR SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES ARISING FROM YOUR USE OF THE APP.
            
            This includes, without limitation:
            - Data loss
            - Device damage
            - Financial loss from security breaches
            - Missed threats or false positives
            
            ## 8. Indemnification
            
            You agree to defend, indemnify, and hold harmless Malik Muhammad Arbab Tahir from any claims, damages, liabilities, costs, or expenses arising from your violation of these Terms or your misuse of the App.
            
            ## 9. Third-Party Services
            
            The App may integrate with third-party APIs (VirusTotal, etc.) for enhanced detection. When you use these features:
            - Your data may be subject to their terms and privacy policies
            - We do not control these third-party services
            - Use of these features is at your own risk
            
            ## 10. Updates and Changes
            
            We may update these Terms from time to time. We will notify you of any material changes through the App. Your continued use after changes constitutes acceptance.
            
            ## 11. Termination
            
            We may terminate or suspend your access immediately, without prior notice, for conduct that violates these Terms.
            
            ## 12. Governing Law
            
            These Terms shall be governed by the laws of Pakistan/Punjab, without regard to its conflict of law provisions.
            
            ## 13. Contact Information
            
            For questions about these Terms, contact:
            
            **Email:** shadowinspect.app@gmail.com
            **GitHub:** https://github.com/arbabtahir1/shadowinspect
            
            ---
            *By using ShadowInspect, you acknowledge that you have read, understood, and agree to be bound by these Terms of Service.*
        """.trimIndent()
    )
    
    val privacyPolicy = LegalDocument(
        type = LegalDocumentType.PRIVACY_POLICY,
        title = "Privacy Policy",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # ShadowInspect Privacy Policy
            
            **Last Updated: March 15, 2026**
            
            Your privacy is critically important to us. This policy explains what information we collect, how we use it, and your rights.
            
            ## Our Privacy Promise
            
            ✅ **NO data collection** - We do not collect personal information
            ✅ **ON-DEVICE processing** - All analysis stays on your phone
            ✅ **NO tracking** - No analytics, no trackers, no fingerprints
            ✅ **NO selling** - We never sell any information
            ✅ **TRANSPARENT** - Everything is explained here
            
            ## Information We DO NOT Collect
            
            ShadowInspect is designed to respect your privacy. We do NOT collect:
            
            - ❌ Your name, email, or contact information
            - ❌ Your location (unless you explicitly scan location data)
            - ❌ Your contacts or messages
            - ❌ Your browsing history
            - ❌ Device identifiers (IMEI, serial numbers)
            - ❌ Advertising IDs
            - ❌ Usage statistics or analytics
            
            ## Information We Process (Locally)
            
            The following information is processed **entirely on your device** and never leaves your phone:
            
            ### Files You Choose to Scan
            - APK files (analyzed for permissions, malware)
            - Documents (PDF, Office files - metadata extracted)
            - Images (EXIF data, steganography detection)
            - URLs (checked against threat databases)
            - Phone numbers (validated for spam)
            
            ### Scan Results
            - Risk scores and classifications
            - MITRE ATT&CK technique mappings
            - Detection reports
            
            ### App Preferences
            - Your settings (theme, default scan options)
            - Widget configurations
            - Lesson progress and earned badges
            
            All this data stays in a local database on your device. You can delete it anytime through Settings.
            
            ## When You Use Third-Party APIs
            
            For enhanced detection, you may choose to use optional third-party services:
            
            ### VirusTotal
            - When you scan a URL, you can optionally check it against VirusTotal's database
            - The URL is sent to VirusTotal's servers
            - Subject to VirusTotal's [Terms](https://support.virustotal.com/hc/en-us/articles/115002168385-Terms-of-Service) and [Privacy Policy](https://support.virustotal.com/hc/en-us/articles/115002149369-Privacy-Policy)
            
            ### Phone Validation APIs
            - Optional phone number validation uses free tier APIs
            - Phone numbers are sent to these services
            - Each service has its own privacy policy
            
            These features are **OPTIONAL** and clearly marked. You control when to use them.
            
            ## Permissions Explained
            
            The App requests certain permissions to function:
            
            | Permission | Purpose | Required? |
            |------------|---------|-----------|
            | Internet | Checking URLs against threat databases | Yes |
            | Read Storage | Select files to scan | Yes |
            | Camera | Scanning QR codes (future) | Optional |
            | Notifications | Alert you about scan results | Optional |
            
            You can revoke any permission in your device settings.
            
            ## Children's Privacy
            
            The App is not directed at children under 13. We do not knowingly collect information from children.
            
            ## Data Retention
            
            All data is stored locally on your device. You can:
            - Delete individual scan results
            - Clear all history (Settings → Storage → Clear All)
            - Uninstall the app to remove all data
            
            ## Security
            
            We implement reasonable security measures:
            - Local database encryption
            - No network transmission of your files
            - Regular security updates
            
            However, no method of electronic storage is 100% secure.
            
            ## Your Rights
            
            You have the right to:
            - Access all your data (it's on your device)
            - Delete your data (clear app data)
            - Export your data (Settings → Export)
            - Opt out of optional features
            
            ## Changes to This Policy
            
            We may update this policy. We will notify you of significant changes through the App.
            
            ## Contact Us
            
            For privacy questions:
            
            **Email:** shadowinspect.app@gmail.com
            
            ## Compliance
            
            This app complies with:
            - GDPR (General Data Protection Regulation)
            - CCPA (California Consumer Privacy Act)
            - COPPA (Children's Online Privacy Protection Act)
            
            ---
            *Your trust matters. We built ShadowInspect to protect your privacy, not exploit it.*
        """.trimIndent()
    )
    
    val gdprCompliance = LegalDocument(
        type = LegalDocumentType.GDPR_COMPLIANCE,
        title = "GDPR Compliance",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # GDPR Compliance Statement
            
            **For users in the European Economic Area (EEA)**
            
            This document explains how ShadowInspect complies with the General Data Protection Regulation (GDPR).
            
            ## Our GDPR Commitment
            
            ShadowInspect is designed with privacy by default and by design. We process minimal personal data, and all processing is done locally on your device.
            
            ## Legal Basis for Processing
            
            Under GDPR, we process data on the following bases:
            
            ### 1. Consent (Article 6(1)(a))
            - When you choose to scan a file, you consent to that specific analysis
            - When you use optional third-party APIs, you consent to that data transfer
            - You can withdraw consent anytime by not using those features
            
            ### 2. Legitimate Interests (Article 6(1)(f))
            - Providing security analysis functionality
            - Improving the app through crash reporting (if you enable it)
            - Ensuring app security and stability
            
            ### 3. Legal Obligations (Article 6(1)(c))
            - Compliance with applicable laws
            
            ## Data Subject Rights
            
            Under GDPR, you have the following rights:
            
            ### Right to Access (Article 15)
            All your data is already on your device. You can view it directly in the app.
            
            ### Right to Rectification (Article 16)
            You can correct information by rescanning or editing notes.
            
            ### Right to Erasure (Article 17)
            You can delete individual scans or clear all data in Settings.
            
            ### Right to Restrict Processing (Article 18)
            Simply don't use the app for files you want to exclude.
            
            ### Right to Data Portability (Article 20)
            Export your scan history as CSV/JSON (Settings → Export).
            
            ### Right to Object (Article 21)
            Opt out of optional features at any time.
            
            ### Rights Related to Automated Decision-Making (Article 22)
            Our ML detection involves automated decisions, but:
            - You always see explanations
            - You can review and override
            - You can use rule-based detection instead
            
            ## Data Protection Officer
            
            For GDPR inquiries:
            
            **Email:** shadowinspect.app@gmail.com
            
            ## Supervisory Authority
            
            You have the right to lodge a complaint with your local data protection authority.
            
            ## International Transfers
            
            If you use optional third-party APIs, data may be transferred outside the EEA. These transfers are based on:
            - Standard Contractual Clauses
            - Adequacy decisions
            - Your explicit consent
            
            ## Data Minimization
            
            We collect only what's necessary:
            - No tracking
            - No analytics
            - No personal identifiers
            
            ## Storage Limitation
            
            Data is stored only as long as you keep it. You control retention.
            
            ## Integrity and Confidentiality
            
            We protect your data through:
            - On-device storage
            - No unnecessary transmission
            - Regular security updates
            
            ---
            *This statement was last updated March 15, 2026. It may be updated to reflect regulatory changes.*
        """.trimIndent()
    )
    
    val disclaimer = LegalDocument(
        type = LegalDocumentType.DISCLAIMER,
        title = "Disclaimer",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # ShadowInspect Disclaimer
            
            **PLEASE READ CAREFULLY**
            
            ## 1. No Warranty
            
            SHADOWINSPECT IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
            
            ## 2. Not a Substitute for Professional Security
            
            ShadowInspect is a tool to assist with security awareness, NOT a substitute for:
            - Professional security audits
            - Enterprise-grade antivirus solutions
            - Legal advice
            - Compliance certifications
            
            ## 3. Detection Limitations
            
            While we strive for high accuracy (97%+ in testing):
            
            ⚠️ **False Positives**: The app may occasionally flag safe files as suspicious
            ⚠️ **False Negatives**: The app may miss some threats
            ⚠️ **Zero-Day Attacks**: New, unknown attacks may not be detected
            ⚠️ **Advanced Malware**: Sophisticated malware may evade detection
            
            ## 4. Educational Purpose
            
            Some content is for educational purposes. The techniques described should only be used:
            - On your own devices
            - In authorized security testing
            - For learning and awareness
            
            ## 5. Third-Party Content
            
            The app may link to or reference third-party content. We do not endorse or control:
            - External websites
            - Third-party tools
            - User-contributed content
            
            ## 6. No Liability
            
            TO THE MAXIMUM EXTENT PERMITTED BY LAW, MALIK MUHAMMAD ARBAB TAHIR SHALL NOT BE LIABLE FOR ANY:
            - Direct or indirect damages
            - Loss of data
            - Financial loss
            - Device damage
            - Security breaches
            - Missed threats
            
            arising from your use of the App.
            
            ## 7. Use at Your Own Risk
            
            YOU USE SHADOWINSPECT AT YOUR OWN RISK. Always maintain:
            - Regular backups
            - Multiple security layers
            - Professional advice for critical systems
            
            ## 8. No Professional Relationship
            
            Use of the App does not create a professional-client relationship. For critical security needs, consult qualified professionals.
            
            ## 9. Export Controls
            
            You are responsible for complying with all applicable export control laws.
            
            ## 10. Governing Law
            
            This disclaimer is governed by the laws of Pakistan/Punjab.
            
            ---
            *By using ShadowInspect, you acknowledge and accept these disclaimers.*
        """.trimIndent()
    )
    
    val openSourceLicenses = LegalDocument(
        type = LegalDocumentType.LICENSES,
        title = "Open Source Licenses",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # Open Source Licenses & Attributions
            
            ShadowInspect is built using these amazing open source libraries. We are grateful to their creators.
            
            ## Core Libraries
            
            ### Jetpack Compose
            **License:** Apache 2.0
            **Copyright:** The Android Open Source Project
            **Used for:** Modern UI toolkit
            ```
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            
                http://www.apache.org/licenses/LICENSE-2.0
            
            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
            ```
            
            ### Dagger Hilt
            **License:** Apache 2.0
            **Copyright:** The Dagger Authors
            **Used for:** Dependency injection
            
            ### Room Database
            **License:** Apache 2.0
            **Copyright:** The Android Open Source Project
            **Used for:** Local data storage
            
            ### Retrofit
            **License:** Apache 2.0
            **Copyright:** Square, Inc.
            **Used for:** HTTP API client
            
            ## Security & Analysis Libraries
            
            ### TensorFlow Lite
            **License:** Apache 2.0
            **Copyright:** Google LLC
            **Used for:** On-device ML inference
            
            ### Apache PDFBox
            **License:** Apache 2.0
            **Copyright:** The Apache Software Foundation
            **Used for:** PDF parsing
            
            ### Apache POI
            **License:** Apache 2.0
            **Copyright:** The Apache Software Foundation
            **Used for:** Office document parsing
            
            ## UI & Charts
            
            ### Vico Compose
            **License:** Apache 2.0
            **Copyright:** Patryk Andrzejewski
            **Used for:** Charts and visualizations
            
            ### Accompose
            **License:** Apache 2.0
            **Copyright:** Google LLC
            **Used for:** Compose utilities
            
            ## Full License Texts
            
            For complete license texts, visit:
            https://github.com/arbabtahir1/shadowinspect/blob/main/licenses/
            
            ## Compliance with MITRE ATT&CK®
            
            MITRE ATT&CK® is a registered trademark of The MITRE Corporation.
            
            This application uses the MITRE ATT&CK framework under the terms of use specified by MITRE. Our use includes:
            - Attribution to MITRE
            - No claim of endorsement
            - Compliance with their usage guidelines
            
            ## Icons and Assets
            
            Some icons are from:
            - Material Icons (Apache 2.0)
            - Custom assets created for this project
            
            ---
            *We stand on the shoulders of giants. Thank you to all open source contributors!*
        """.trimIndent()
    )
    
    val copyright = LegalDocument(
        type = LegalDocumentType.COPYRIGHT,
        title = "Copyright & Credits",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # Copyright & Credits
            
            ## © 2026 Malik Muhammad Arbab Tahir. All Rights Reserved.
            
            ## Copyright Notice
            
            Copyright © 2026 Malik Muhammad Arbab Tahir. All rights reserved.
            
            This software, including its source code, documentation, and associated files, is protected by copyright law and international treaties. Unauthorized reproduction or distribution of this software, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible under law.
            
            ## Limited License for Users
            
            Users are granted a non-exclusive, non-transferable license to:
            - Install and use the software on their personal devices
            - Use the software for lawful purposes
            - Generate reports for personal use
            
            Users may NOT:
            - Reverse engineer, decompile, or disassemble the software
            - Distribute, sublicense, or rent the software
            - Remove or alter any copyright notices
            - Use the software for commercial purposes without permission
            
            ## MITRE ATT&CK® Attribution
            
            This product uses the MITRE ATT&CK® framework. 
            
            MITRE ATT&CK® and ATT&CK® are registered trademarks of The MITRE Corporation.
            
            **Attribution Statement:**
            "ShadowInspect uses the MITRE ATT&CK framework to map detected threats to known adversary techniques. MITRE ATT&CK is a globally-accessible knowledge base of adversary tactics and techniques based on real-world observations."
            
            For more information: https://attack.mitre.org
            
            ## Third-Party Trademarks
            
            All other trademarks, service marks, and company names are the property of their respective owners.
            
            ## Project Credits
            
            ### Created By
            **Malik Muhammad Arbab Tahir**
            - Lead Developer
            - Security Researcher
            - UI/UX Design
            
            ### Special Thanks
            
            **Supervisor:** Syed Atta ur Rehman
            For guidance and mentorship
            
            **Beta Testers:**
            - [Tester 1]
            - [Tester 2]
            - [Tester 3]
            
            **Dataset Providers:**
            - Droidware Dataset (IEEE Dataport)
            - AndroZoo Project
            - MITRE Corporation
            
            **Open Source Community**
            All contributors to the libraries we use
            
            ## Contact
            
            For general inquiries: shadowinspect.app@gmail.com
            For licensing inquiries: shadowinspect.app@gmail.com
            For vulnerability reports: https://github.com/arbabtahir1/shadowinspect/issues
            
            ## Source Code
            
            This project is available on GitHub:
            https://github.com/arbabtahir1/shadowinspect
            
            ---
            *Made with ❤️ for the security community*
        """.trimIndent()
    )
    
    val mitreAttribution = LegalDocument(
        type = LegalDocumentType.ATTRIBUTIONS,
        title = "MITRE ATT&CK® Attributions",
        lastUpdated = "March 15, 2026",
        version = "3.0.1",
        content = """
            # MITRE ATT&CK® Attributions
            
            This product uses the MITRE ATT&CK® framework.
            
            ## Trademark Notice
            
            MITRE ATT&CK® and ATT&CK® are registered trademarks of The MITRE Corporation.
            
            ## Official Attribution
            
            "ShadowInspect uses the MITRE ATT&CK framework to map detected threats to known adversary techniques. MITRE ATT&CK is a globally-accessible knowledge base of adversary tactics and techniques based on real-world observations."
            
            ## About MITRE ATT&CK®
            
            MITRE ATT&CK® is a globally-accessible knowledge base of adversary tactics and techniques based on real-world observations. The ATT&CK knowledge base is used as a foundation for the development of specific threat models and methodologies in the private sector, in government, and in the cybersecurity product and service community.
            
            For more information: https://attack.mitre.org
            
            ---
            *ShadowInspect is not endorsed by or affiliated with The MITRE Corporation.*
        """.trimIndent()
    )
}
