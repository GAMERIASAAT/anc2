# AncKit

A modular security framework for Android and JVM, written in Kotlin. Inspired by the architecture of open-source penetration testing frameworks, AncKit brings structured exploit development, payload generation, session management, and post-exploitation to Android and the JVM.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Building](#building)
- [Android App](#android-app)
- [AncVenom CLI](#ancvenom-cli)
- [Framework API](#framework-api)
- [Modules](#modules)
- [Rex Toolkit](#rex-toolkit)
- [Running Tests](#running-tests)
- [Technology Stack](#technology-stack)

---

## Overview

AncKit is an Android-first security framework. Every component runs on Android API 26+ and the JVM with no platform-specific divergence. The core design principles:

- **Coroutines** replace threads — all module `run()` and payload `generate()` calls are `suspend` functions
- **Sealed class hierarchies** for type-safe payloads, sessions, and events
- **Interfaces as mixins** — `Remote`, `Local`, `CmdStager`, `Handler` are Kotlin interfaces with default implementations
- **Flow-based event bus** for real-time output between the framework and UI layers
- **ModuleRegistry** — compile-time module list (instead of `ServiceLoader`) for Android compatibility

---

## Project Structure

```
anc2/
├── base/                    Platform abstractions (Logging, Config, JSON, Storage)
├── core/                    Framework core — managers, base classes, interfaces
│   └── src/main/kotlin/anc/core/
│       ├── Framework.kt         Singleton entry point
│       ├── ModuleManager.kt     Module discovery, lookup, instantiation
│       ├── PayloadManager.kt    Payload-specific queries and creation
│       ├── EncoderManager.kt    Encoder chain and auto-encode
│       ├── SessionManager.kt    Active session registry
│       ├── EventBus.kt          SharedFlow-based event system
│       ├── AncModule.kt         Abstract base for all modules
│       ├── Payload.kt           Abstract payload base
│       ├── Encoder.kt           Abstract encoder base
│       ├── Options.kt           Typed option definitions
│       ├── Datastore.kt         Per-module key-value config store
│       └── Session.kt           Abstract session base
├── rex/                     Low-level toolkit (sockets, crypto, protocols)
│   └── src/main/kotlin/rex/
│       ├── arch/                Platform, Arch, OS enums
│       ├── socket/              TcpSocket, UdpSocket, SslSocket
│       ├── crypto/              Aes, Rc4, Sha256
│       ├── text/                Hex, Pattern utilities
│       └── proto/               Http, Dns, Ftp, Smb, Ssh clients
├── modules/                 All module implementations
│   └── src/main/kotlin/anc/modules/
│       ├── ModuleRegistry.kt    Compile-time module registry
│       ├── auxiliary/           Scanner and info-gathering modules
│       ├── exploits/            Exploit and handler modules
│       ├── payloads/            Reverse shells and staged payloads
│       ├── encoders/            Payload encoders
│       └── nops/                NOP sled generators
├── ui/android/              Jetpack Compose UI library
│   └── src/main/kotlin/anc/ui/android/
│       ├── screens/
│       │   ├── ConsoleScreen.kt     Interactive terminal UI
│       │   ├── ModulesScreen.kt     Module browser with search and filters
│       │   └── SessionsScreen.kt    Active session list and management
│       └── NavGraph.kt              Bottom navigation with 3 tabs
├── android-app/             APK entry point
│   └── src/main/kotlin/com/anckit/
│       ├── App.kt               Application class — Koin DI + Framework init
│       └── MainActivity.kt      Single-activity host with Compose NavGraph
├── tools/ancvenom/          Payload generator CLI (fat JAR)
│   └── src/main/kotlin/ancvenom/Main.kt
└── gradle/
    └── libs.versions.toml   Centralized dependency version catalog
```

---

## Building

### Requirements

| Requirement | Version |
|-------------|---------|
| JDK | 21+ |
| Android SDK | API 26+ target, API 35 compile |
| Gradle | 8.11+ (wrapper included) |

### Build all JVM modules

```bash
./gradlew :base:build :rex:build :core:build :modules:build
```

### Build the Android APK

```bash
./gradlew :android-app:assembleDebug
# Output: android-app/build/outputs/apk/debug/android-app-debug.apk
```

### Build the ancvenom fat JAR

```bash
./gradlew :tools:ancvenom:jar
# Output: tools/ancvenom/build/libs/ancvenom.jar
```

### Install APK via ADB

```bash
adb install -r android-app/build/outputs/apk/debug/android-app-debug.apk
```

---

## Android App

The AncKit Android app provides a full GUI for the framework. It runs on Android 8.0+ (API 26).

### Screens

#### Console

The interactive terminal. Communicates with the framework via the `EventBus` and `CommandDispatcher`.

- Dark GitHub-style theme
- Color-coded output lines:
  - `[+]` — Good/success (green)
  - `[-]` — Bad/failure (red)
  - `[*]` — Status/info (blue)
  - `[!]` — Warning (orange)
- Live prompt: `anc(module/path) >` when a module is active, or `anc >`
- Running indicator while a module is executing
- 2000-line scrollback history

**Available commands:**

| Command | Description |
|---------|-------------|
| `use <module>` | Load a module by full or partial path |
| `show options` | Display current module options |
| `set <KEY> <VALUE>` | Set a module option |
| `run` / `exploit` | Execute the current module |
| `sessions` | List active sessions |
| `session -i <id>` | Interact with a session |
| `back` | Unload current module |
| `help` | Show help |

#### Modules

Browse all registered modules by type. Tap any module to load it into the Console.

- Search bar (searches name and description, case-insensitive)
- Filter tabs: **All** · **EXPLOIT** · **AUXILIARY** · **PAYLOAD** · **ENCODER** · **NOP** · **POST** · **EVASION**
- Each row shows: type badge, full path, description, rank
- Rank colors: Excellent/Great → green · Good/Normal → blue · Average → orange

#### Sessions

View and manage all active sessions opened by the handler or other modules.

- Lists all sessions with type, ID, remote address, and alive/closed status
- Tap the **×** button to close a session
- Session types: **shell** (green) · **phantom** (blue)

### Launching

The app registers 8 modules on startup. You can verify in logcat:

```
adb logcat -s AncKit
```

Expected output:
```
AncKit initializing...
Registered module: auxiliary/scanner/portscan/tcp
Registered module: auxiliary/scanner/http/http_version
Registered module: exploits/multi/handler
Registered module: payloads/singles/linux/shell_reverse_tcp
Registered module: payloads/singles/android/shell_reverse_tcp
Registered module: encoders/x86/xor_additive
Registered module: nops/x86/opty2
Registered module: nops/arm/simple
AncKit started — 8 modules ready
```

---

## AncVenom CLI

`ancvenom` is a command-line payload generator that wraps the framework's `PayloadManager` and `EncoderManager`.

### Usage

```
ancvenom [OPTIONS] [VARS]...

Options:
  -p, --payload TEXT    Payload path (e.g. linux/shell_reverse_tcp)
  -f, --format CHOICE   Output format [raw|hex|c|python|base64|kotlin]  (default: hex)
  -o, --output TEXT     Write output to file instead of stdout
  -e, --encoder TEXT    Encoder path (e.g. x86/xor_additive)
  -l, --list            List all available payloads, encoders, and NOPs
  -h, --help            Show help
```

`VARS` are positional `KEY=VALUE` pairs passed to the payload's datastore (e.g. `LHOST=192.168.1.10 LPORT=4444`).

### Running via Gradle

```bash
./gradlew :tools:ancvenom:run --args="<args>"
```

### Examples

**List all available modules:**
```bash
./gradlew :tools:ancvenom:run --args="-l"
```

**Linux x86 reverse shell — hex output:**
```bash
./gradlew :tools:ancvenom:run --args="-p singles/linux/shell_reverse_tcp LHOST=192.168.1.10 LPORT=4444 -f hex"
```

**Linux x86 reverse shell — C format:**
```bash
./gradlew :tools:ancvenom:run --args="-p singles/linux/shell_reverse_tcp LHOST=192.168.1.10 LPORT=4444 -f c"
```

**Android reverse shell with XOR encoder — base64:**
```bash
./gradlew :tools:ancvenom:run --args="-p singles/android/shell_reverse_tcp LHOST=10.0.0.5 LPORT=9001 -e x86/xor_additive -f base64"
```

**Raw binary to file:**
```bash
./gradlew :tools:ancvenom:run --args="-p singles/linux/shell_reverse_tcp LHOST=192.168.1.1 LPORT=4444 -f raw -o shell.bin"
```

**Kotlin literal output (for embedding in source):**
```bash
./gradlew :tools:ancvenom:run --args="-p singles/linux/shell_reverse_tcp LHOST=127.0.0.1 LPORT=4444 -f kotlin"
```

### Output Formats

| Format | Description | Example |
|--------|-------------|---------|
| `hex` | Lowercase hex string | `6a66586a015b...` |
| `c` | C string with `\x` escapes, 16 bytes/line | `"\x6a\x66\x58..."` |
| `python` | Python bytes literal, 16 bytes/line | `b"\x6a\x66..."` |
| `base64` | Base64-encoded | `amZY...` |
| `kotlin` | `byteArrayOf(...)`, 12 bytes/line | `byteArrayOf(0x6a, 0x66, ...)` |
| `raw` | Raw binary bytes (writes to stdout or `-o` file) | — |

### Stderr Status

The tool always writes metadata to stderr and payload bytes to stdout, so piping works cleanly:

```
[*] Payload   : payloads/singles/linux/shell_reverse_tcp
[*] Size      : 80 bytes
[*] Format    : hex
[*] Encoder   : encoders/x86/xor_additive  (if -e was used)
[+] Saved to  : shell.bin                  (if -o was used)
```

---

## Framework API

### Initialization

```kotlin
val fw = Framework.initialize(ModuleRegistry.all)
```

Call once at application start (e.g. in `Application.onCreate()`). Subsequent calls return the same instance. Use `Framework.getInstance()` anywhere after initialization.

### ModuleManager

```kotlin
val mgr = fw.moduleManager

mgr.count                           // total registered modules
mgr.allMeta()                       // List<ModuleMetadata>, sorted by fullName
mgr.search("portscan")              // case-insensitive search by name/description
mgr.byType(ModuleType.PAYLOAD)      // filter by type
mgr.meta("payloads/singles/linux/shell_reverse_tcp")  // get metadata
mgr.create("payloads/singles/linux/shell_reverse_tcp") // instantiate module
```

### PayloadManager

```kotlin
val pm = fw.payloadManager

pm.count                                    // number of payloads
pm.all()                                    // List<ModuleMetadata>
pm.create("singles/linux/shell_reverse_tcp")   // returns Payload? (short path ok)
pm.create("payloads/singles/linux/shell_reverse_tcp") // full path also works
pm.forPlatform(Platform.LINUX)              // List<ModuleMetadata>
pm.forPlatform(Platform.ANDROID)
pm.forArch(Arch.X86)
pm.forPlatformAndArch(Platform.LINUX, Arch.X86)
```

### EncoderManager

```kotlin
val em = fw.encoderManager

em.count                                          // number of encoders
em.all()                                          // List<ModuleMetadata>
em.create("x86/xor_additive")                     // returns Encoder?
em.encode(rawBytes, "x86/xor_additive")           // suspend, returns encoded ByteArray
em.encodeChain(rawBytes, listOf("x86/xor_additive")) // multiple encoders in sequence
em.autoEncode(rawBytes, badChars = byteArrayOf(0x00)) // auto-select compatible encoder
```

### Generating a Payload

```kotlin
val fw = Framework.initialize(ModuleRegistry.all)

val payload = fw.payloadManager.create("singles/linux/shell_reverse_tcp")!!
payload.datastore["LHOST"] = "192.168.1.10"
payload.datastore["LPORT"] = "4444"

val raw: ByteArray = runBlocking { payload.generate() }

// Optionally encode
val encoded = runBlocking { fw.encoderManager.encode(raw, "x86/xor_additive") }
```

### SessionManager

```kotlin
val sm = fw.sessionManager

sm.count                        // active sessions
sm.all()                        // List<Session>, sorted by ID
sm.get(1)                       // Session? by ID
sm.close(1)                     // close session, emits SessionClosed event
sm.closeAll()
```

### EventBus

```kotlin
// Collect events in a coroutine
fw.eventBus.events.collect { event ->
    when (event) {
        is EventBus.Event.ConsoleOutput  -> println(event.line)
        is EventBus.Event.SessionOpened  -> println("Session ${event.sessionId} opened")
        is EventBus.Event.SessionClosed  -> println("Session ${event.sessionId} closed")
        is EventBus.Event.ModuleOutput   -> println("[${event.fullName}] ${event.line}")
        is EventBus.Event.FrameworkStatus -> println(event.message)
        else -> {}
    }
}
```

### Writing a Module

```kotlin
class MyScanner : AncModule() {
    override val name        = "My TCP Scanner"
    override val description = "Scans TCP ports on a target"
    override val moduleType  = ModuleType.AUXILIARY
    override val modulePath  = "scanner/my_scanner"
    override val rank        = Rank.NORMAL

    override val options = Options().apply {
        required<String>("RHOSTS", "Target host or CIDR")
        required<Int>("RPORT", "Port to scan", default = 80)
    }

    override suspend fun run() {
        val host = datastore.getString("RHOSTS")
        val port = datastore.getInt("RPORT")
        printStatus("Scanning $host:$port...")
        // ... scanning logic
        printGood("$host:$port is open")
    }
}
```

Register it in `ModuleRegistry.kt`:
```kotlin
object ModuleRegistry {
    val all: List<() -> AncModule> = listOf(
        // ... existing modules ...
        { MyScanner() },
    )
}
```

---

## Modules

### Auxiliary

#### `auxiliary/scanner/portscan/tcp` — TCP Port Scanner

Scans one or more hosts for open TCP ports using parallel coroutines.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `RHOSTS` | String | required | Target IP, hostname, or CIDR (e.g. `192.168.1.0/24`) |
| `PORTS` | String | `1-1024` | Port range (e.g. `22,80,443` or `1-65535`) |
| `THREADS` | Int | `100` | Concurrent connection workers |
| `TIMEOUT` | Int | `1000` | Per-port connect timeout (ms) |

#### `auxiliary/scanner/http/http_version` — HTTP Version Detection

Sends an HTTP request and extracts the `Server` header and status line.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `RHOSTS` | String | required | Target host |
| `RPORT` | Int | `80` | Target port |
| `SSL` | Boolean | `false` | Use HTTPS |
| `VHOST` | String | — | Virtual host override |

---

### Exploits

#### `exploits/multi/handler` — Generic Payload Handler

Listens for incoming reverse connections and registers them as sessions.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `LHOST` | String | `0.0.0.0` | Listening address |
| `LPORT` | Int | `4444` | Listening port |
| `SESSION_TYPE` | String | `shell` | Session type: `shell` or `phantom` |
| `EXIT_ON_SESSION` | Boolean | `false` | Stop listener after first session |

---

### Payloads

#### `payloads/singles/linux/shell_reverse_tcp` — Linux x86 Reverse Shell

80-byte position-independent x86 shellcode. Uses `socketcall` (int 0x80), `dup2`, and `execve("/bin//sh")`. LHOST and LPORT are patched at fixed offsets at generation time.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `LHOST` | String | required | Attacker IP |
| `LPORT` | Int | required | Attacker port (1–65535) |
| `PrependFork` | Boolean | `false` | Fork before connect |

**Platform:** Linux · **Arch:** x86 · **Rank:** Great

#### `payloads/singles/android/shell_reverse_tcp` — Android Reverse Shell

Generates a reverse shell command or Java reflection payload targeting Android's `/system/bin/sh`.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `LHOST` | String | required | Attacker IP |
| `LPORT` | Int | required | Attacker port |
| `TYPE` | String | `cmd` | `cmd` (shell one-liner) or `java` (Java source with `Socket`) |

**Platform:** Android · **Rank:** Great

---

### Encoders

#### `encoders/x86/xor_additive` — XOR-Additive Encoder

Encodes a payload using single-byte XOR with an additive feedback counter. Prepends a position-independent x86 decoder stub that decodes the payload in-memory at execution.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `BADCHARS` | String | `\x00` | Bytes to avoid in encoded output |
| `KEY` | Int | `0` | XOR key byte (0 = random) |

**Arch:** x86 · **Rank:** Good

---

### NOP Generators

#### `nops/x86/opty2` — x86 NOP Sled

Generates x86 NOP sleds. Uses `0x90` (NOP) by default. Falls back to single-byte semantically-equivalent instructions (`INC`, `DEC`, `CLD`, `CLC`) if `0x90` is in the bad-chars list.

Methods available via `NopX86`:
- `generate(length, badChars)` — standard sled
- `generatePolymorphic(length)` — rotates through all allowed candidates

#### `nops/arm/simple` — ARM/Thumb NOP Sled

Generates NOP sleds for ARM and Thumb instruction sets.

- **ARM mode:** `e3 20 f0 00` × n (must be multiple of 4)
- **Thumb mode:** `00 bf` × n (must be multiple of 2)
- Throws `IllegalArgumentException` if bad chars overlap with the NOP pattern

---

## Rex Toolkit

`rex` is the low-level networking and cryptography library used internally by modules.

### Sockets

```kotlin
// TCP
val sock = TcpSocket.connect("192.168.1.1", 80)
val data = sock.recv(4096)
sock.send("GET / HTTP/1.0\r\n\r\n".toByteArray())
sock.close()

// UDP
UdpSocket.sendOnce(data, "8.8.8.8", 53)
val reply = UdpSocket.sendReceive(query, "8.8.8.8", 53)
```

### Crypto

```kotlin
// AES-256-CBC
val encrypted = Aes.encrypt(plaintext, key, iv)
val decrypted = Aes.decrypt(encrypted, key, iv)

// RC4
val ciphertext = Rc4.crypt(data, key)
val stateful   = Rc4.stateful(key)
val chunk1     = stateful.process(part1)
val chunk2     = stateful.process(part2)

// SHA-256
val digest = Sha256.digest(data)                   // raw bytes
val hex    = Sha256.hex("hello")                   // hex string
val hmac   = Sha256.hmac(data, key)
val valid  = Sha256.verify(a, b)                   // constant-time compare
```

### Protocols

```kotlin
// DNS
val ips = Dns.resolveAll("example.com")
val ptr = Dns.reverseLookup("8.8.8.8")

// HTTP
val resp = Http.get("http://example.com/")
val post = Http.post("http://api.example.com/data", body)

// FTP
val ftp = FtpClient.connect("ftp.example.com", 21, "user", "pass")
ftp.list("/pub")

// SMB
val result = Smb.negotiate("192.168.1.1")
println(result.dialect)

// SSH
val ssh = SshClient.connectPassword("192.168.1.1", 22, "root", "password")
val out = ssh.exec("id")
ssh.download("/etc/passwd", localPath)
```

### Architecture Enums

```kotlin
// Platforms
Platform.LINUX, Platform.ANDROID, Platform.WINDOWS, Platform.MACOS,
Platform.IOS, Platform.FREEBSD, Platform.UNKNOWN

// Architectures
Arch.X86, Arch.X64, Arch.ARM, Arch.ARM64, Arch.MIPS, Arch.MIPS64, Arch.UNKNOWN

// Operating Systems (rex.arch.OS)
OS.LINUX, OS.ANDROID, OS.WINDOWS, OS.MACOS, OS.IOS, OS.FREEBSD, OS.UNKNOWN
```

---

## Running Tests

```bash
# All JVM tests (rex + core + modules)
./gradlew :rex:test :core:test :modules:test

# Individual module
./gradlew :rex:test
./gradlew :core:test
./gradlew :modules:test

# Specific test class
./gradlew :modules:test --tests "anc.modules.payloads.ReverseShellX86Test"
```

**Current test count: 200 tests, 0 failures**

Test coverage includes:
- Rex: `TcpSocket`, `UdpSocket`, `Aes`, `Rc4`, `Sha256`, `Http`, `Dns`, `Ftp`, `Smb`, `Ssh`, `Hex`, `Pattern`
- Core: `Framework`, `ModuleManager`, `SessionManager`, `EventBus`, `Options`, `Datastore`, `Nop`
- Modules: `ReverseShellX86`, `AndroidReverseShell`, `NopX86`, `NopArm`, `PayloadManager`, `EncoderManager`

---

## Technology Stack

| Concern | Library | Version |
|---------|---------|---------|
| Language | Kotlin | 2.1.0 |
| Android Gradle Plugin | AGP | 8.8.0 |
| Concurrency | Kotlin Coroutines | 1.9.0 |
| Serialization | kotlinx.serialization | 1.7.3 |
| HTTP (Android-safe) | OkHttp | 4.12.0 |
| HTTP client | Ktor Client | 3.0.3 |
| HTTP server (JVM) | Ktor Server | 3.0.3 |
| Dependency Injection | Koin | 4.0.0 |
| UI | Jetpack Compose BOM | 2025.01.00 |
| Navigation | Navigation Compose | 2.8.5 |
| Cryptography | Bouncy Castle | 1.79 |
| SSH client | sshj | 0.38.0 |
| CLI parsing | Clikt | 5.0.2 |
| Terminal UI | Mordant | 3.0.2 |
| Testing | Kotest | 5.9.1 |
| Mocking | MockK | 1.13.14 |
| Android min SDK | — | 26 (Android 8.0) |
| Android compile SDK | — | 35 |
| JVM toolchain | OpenJDK | 21 |

---

## License

For authorized security testing and research use only.
