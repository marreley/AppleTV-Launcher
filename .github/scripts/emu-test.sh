#!/usr/bin/env bash
# Lance l'APK sur l'émulateur Android TV et enregistre captures d'écran et journaux dans diag/.
mkdir -p diag
APK=app/build/outputs/apk/debug/AppleTVLauncher-debug.apk

adb logcat -c
adb install -r "$APK" > diag/install.txt 2>&1
adb shell pm list packages 2>/dev/null | grep -i appletv >> diag/install.txt
adb shell pm list packages 2>/dev/null | wc -l > diag/package-count.txt

adb shell am start -W -n com.appletv.launcher/.MainActivity > diag/start.txt 2>&1
sleep 20
adb exec-out screencap -p > diag/screen1.png
adb shell pidof com.appletv.launcher > diag/pid1.txt 2>&1

adb shell input keyevent KEYCODE_DPAD_RIGHT
sleep 2
adb shell input keyevent KEYCODE_DPAD_RIGHT
sleep 2
adb exec-out screencap -p > diag/screen2.png
adb shell pidof com.appletv.launcher > diag/pid2.txt 2>&1
adb shell dumpsys activity activities 2>/dev/null | grep -E "topResumedActivity|mResumedActivity" > diag/top.txt

adb logcat -d > diag/logcat-full.txt 2>&1
grep -n -A45 "FATAL EXCEPTION" diag/logcat-full.txt > diag/crash.txt || echo "aucun FATAL EXCEPTION" > diag/crash.txt
grep -n -i -E "appletv|AndroidRuntime" diag/logcat-full.txt | head -200 > diag/app-lines.txt
exit 0
