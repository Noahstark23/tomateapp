# BUILD_RUN — compilar y correr en Mac sin Android Studio

Verificado 2026-09-26 en Mac arm64, macOS 26.6.2.

## 1. Prerrequisitos
- JDK 17 (brew): `/Users/stark/homebrew/opt/openjdk@17` → `export JAVA_HOME=.../libexec/openjdk.jdk/Contents/Home`.
- Android SDK en `~/Library/Android/sdk`: `cmdline-tools/latest` (v19), `platform-tools`,
  `platforms/android-36` + `android-36.1`, `build-tools/36.1.0`, `emulator 37.1.11`,
  `system-images/android-36.1/google_apis/arm64-v8a`.
- `local.properties` con `sdk.dir=/Users/stark/Library/Android/sdk` (solo local, no commitear cambios).
- Gradle wrapper `9.7.1` generado hoy (`gradlew` + `gradle/wrapper/`). Si falta: `gradle wrapper --gradle-version 9.7.1`.

## 2. Variables de entorno (cada terminal)
```bash
export JAVA_HOME=/Users/stark/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

## 3. `.env` y signing
- `.env` existe (copia de `.env.example`, `GEMINI_API_KEY=MY_GEMINI_API_KEY`). No se usa en código
  (`firebase-ai` comentado en `app/build.gradle.kts:95`); no bloquea el build.
- Debug firma con `debug.keystore` del repo (commiteado — rotar post-hoy, ver `BACKLOG.md`).
  No quitar `signingConfig debugConfig` (el paso 5 del README viejo de AI Studio no aplica aquí).
- Release (`my-upload-key.jks` + `STORE_PASSWORD/KEY_PASSWORD`) no existe local → solo compila debug.

## 4. Comandos
```bash
./gradlew assembleDebug                              # APK: app/build/outputs/apk/debug/app-debug.apk (~11MB)
./gradlew testDebugUnitTest                          # unit tests (FinancialEngineTest, 13 tests)
adb devices                                          # emulador NicaLab_API35 = emulator-5580
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 shell am start -n com.aistudio.dashboard.nortex/com.example.MainActivity
adb -s emulator-5580 exec-out screencap -p > /tmp/tomate.png
```

## 5. Troubleshooting
- `No such file gradlew` → regenerar wrapper (sección 1).
- `FAIL_ON_PROJECT_REPOS` → solo google()/mavenCentral() en `settings.gradle.kts`; no agregar jitpack sin actualizar.
- `compileSdk 36.1` requiere `platforms;android-36.1` instalado (ya está).
- `Room KSP` corre en `kspDebugKotlin`; warnings `fallbackToDestructiveMigration`, `Locale(String,String)`,
  `menuAnchor()` son conocidos y no bloquean.
- Dos emuladores a la vez exceden la RAM (~5GB libres requeridos) y el segundo muere `offline` →
  usar solo emulator-5580. El AVD `tomate` creado hoy existe pero no es necesario.
- Bluetooth/impresora en emulador siempre retorna `false` ("Error al imprimir. Verifique BT.") — normal.
