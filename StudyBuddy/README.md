# StudyBuddy 📚🤖

An AI-powered Android study companion app that helps students prepare for exams through live chat, voice interaction, and intelligent study planning — powered by **Claude AI (Sonnet 4.6)**.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 💬 **Live AI Chat** | Talk with your AI study buddy via text — get explanations, ask questions, discuss any topic |
| 🎤 **Voice Interaction** | Speak your questions and hear answers (Speech-to-Text + Text-to-Speech) |
| 📸 **Chapter Photo Upload** | Take a photo of your textbook page — AI reads and explains it instantly |
| 📚 **Subjects & Syllabus** | Add all subjects, enter syllabus/topics, track chapter completion |
| 📅 **Exam Timetable** | Add all exam dates with countdowns — never miss an exam |
| 🗓 **Study Scheduling** | Ask your buddy to create a personalized study plan |
| 🧠 **Quiz Mode** | Ask your buddy to quiz you on any topic |
| 🔖 **Session History** | All your study chats are saved locally |
| ⚙️ **Personalization** | Set student name, class, buddy name, and extra context for better AI responses |

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room (local, offline)
- **Networking**: Retrofit + OkHttp
- **AI**: Anthropic Claude API (claude-sonnet-4-6)
- **Voice**: Android SpeechRecognizer + TextToSpeech
- **Image**: CameraX + Coil
- **Preferences**: DataStore

---

## 🚀 Setup

### 1. Clone the repo
```bash
git clone https://github.com/gauravkumar25/StudyBuddy.git
cd StudyBuddy
```

### 2. Open in Android Studio
Open the `StudyBuddy/` folder in Android Studio Hedgehog or later.

### 3. Get a Claude API Key
1. Go to [console.anthropic.com](https://console.anthropic.com)
2. Create an account and generate an API key
3. The free tier is sufficient to get started

### 4. Configure the App
- Launch the app on your device
- Go to **Settings** (bottom nav)
- Enter your daughter's name, class, and the Claude API key
- Optionally customize the buddy's name and add extra context

### 5. Build & Run
```bash
./gradlew assembleDebug
```

---

## 📱 How to Use

### Chat Mode
1. Tap **"Start Studying"** on the Home screen
2. Type or tap the 🎤 mic to speak
3. Ask anything: "Explain photosynthesis", "Quiz me on Chapter 3", "Make a study plan for my Math exam in 5 days"

### Upload a Chapter
1. In chat, tap the 📷 camera icon
2. Take a photo of your textbook page
3. The AI will read and explain the content

### Add Subjects
1. Go to **Subjects** tab
2. Add each subject with its full syllabus/topics
3. Add chapters and track their status (Not Started → In Progress → Completed)

### Add Exam Dates
1. Go to **Exams** tab
2. Add each exam with date, time, and venue
3. See countdown timers on the Home screen

### Study Plans
Ask your buddy in chat:
> "I have Math exam in 7 days and Science in 10 days. Create a study schedule for me."

---

## 🔐 Privacy

- All data is stored **locally on the device** (Room database)
- Only your chat messages are sent to the Claude API
- The API key is stored locally in encrypted DataStore
- No user data is stored on any server

---

## 📂 Project Structure

```
app/src/main/java/com/studybuddy/app/
├── data/
│   ├── model/          # Room entities (Message, Subject, Chapter, Exam)
│   ├── local/          # Room DB, DAOs
│   └── repository/     # Data repositories
├── network/            # Claude API service + models
├── ui/
│   ├── screens/        # Home, Chat, Subjects, Exams, Settings
│   ├── theme/          # Colors, Typography, Theme
│   └── navigation/     # NavGraph
└── di/                 # Hilt modules
```

---

## 🤝 Contributing

This is a personal project for a student. Feel free to fork and customize!

---

*Made with ❤️ to make studying fun and effective.*
