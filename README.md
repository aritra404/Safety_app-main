# 🚨 SafetyApp - Voice Activated Safety App

SafetyApp is a smart Android safety app designed to provide fast, voice-triggered emergency assistance. With just a custom spoken trigger word, it records a short audio clip, sends an alert SMS with the location and audio link to guardians, initiates a call, and now also allows **real-time chat** with guardians using CometChat.

---

## 📱 Project Overview

SafeGuard ensures user safety through voice-activated emergency detection. When the user speaks a pre-set "safe word", the app performs the following actions:

- Records a 20-second audio clip
- Uploads the clip securely to Firebase Storage
- Fetches real-time GPS location
- Sends the audio link and location to pre-added guardians via SMS
- Initiates a phone call for emergency support
- Enables live **chat with guardians** via CometChat

> Powered by **Kotlin**, **Firebase**, **CometChat**, and **Android Foreground Services**, SafeGuard is always listening in the background—offering both alerts and real-time communication.

---

## 🚀 Key Features

✅ Voice-activated emergency detection  
✅ Customizable detection word  
✅ Sends alerts via SMS with location & audio link  
✅ Automatically places an emergency call  
✅ Real-time location tracking  
✅ Secure audio recording + Firebase Storage upload  
✅ Foreground service for continuous background listening  
✅ **Real-time chat with guardians using CometChat**  
✅ App runs even when the screen is off

---

## 💬 Chat Functionality (CometChat Integration)

SafeGuard uses the **CometChat UI Kit** to enable real-time communication with guardians. This allows users to:

- Text guardians directly from the app
- Send messages immediately after an alert
- Keep conversations logged within the app
- Maintain communication even after the SOS alert is triggered

### 🔧 CometChat Integration Highlights:
- UI Kit used for seamless chat UI
- Authentication and user setup handled through CometChat dashboard
- Chats are protected and stored in real-time

> Note: Make sure your CometChat API keys and App ID are correctly configured in your project.

---

## 🛠️ Technical Implementation

- Built with **Kotlin** using an **XML-based UI**
- Uses **Speech Recognition API** to detect a user-defined safe word
- Runs as a **foreground service** to ensure continuous listening
- On detection:
  - Records a **20-second audio clip**
  - Uploads it to **Firebase Storage**
  - Gets **real-time GPS location** using Fused Location Provider
  - Sends alert via **SMS** using `SmsManager`
  - **Initiates a call** to a guardian
  - Opens **chat interface with the guardian** using CometChat

---

## 💻 Technology Stack

| Technology         | Role                                |
|-------------------|-------------------------------------|
| Kotlin             | Core programming language           |
| XML                | UI Design                           |
| Firebase Storage   | Cloud storage for audio files       |
| Speech Recognition | Detecting emergency trigger word    |
| Fused Location API | Real-time GPS tracking              |
| SmsManager         | Sending alerts via SMS              |
| Foreground Service | Continuous background operation     |
| CometChat          | Real-time chat with guardians       |

---

## 📷 Screenshots!

<p align="center">
  <img src="https://github.com/user-attachments/assets/31e1a10c-c3ac-4950-93a9-76df46e930ff" width="250" />
  <img src="https://github.com/user-attachments/assets/ee25e5e1-1f77-42d5-b94f-da8bf45dc684" width="250" />
  <img src="https://github.com/user-attachments/assets/57baa237-09b7-4c68-848b-cc81a7b92937" width="250" />
  <img src="https://github.com/user-attachments/assets/2d44bfa5-03e4-4425-ac3b-1be88b4d40cc" width="250" />
<img src="https://github.com/user-attachments/assets/ea2e8d32-a3fa-4231-8f39-4da3b45fa554" width="250" />
</p>




