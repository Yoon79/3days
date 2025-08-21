# 작심삼일 (3days)

Android app that lets you set a goal and reminds you after 3 days. You can change the goal anytime; the timer resets to 3 days from the change.

## Package
- `com.goodafteryoon.threedays`

## Build
```bash
./gradlew :app:assembleDebug
```

## Features
- Goal input and start button
- 3-day reminder via WorkManager
- Notification actions: remind again, open to change goal
- Data persistence with DataStore
