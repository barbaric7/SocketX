# Client–Server Chat and File Transfer Application
### Computer Networks Course Project

---

## 📁 Project Structure

```
SocketX/
├── src/
│   ├── common/
│   │   └── Message.java          # Serializable message passed over TCP
│   ├── database/
│   │   └── DatabaseManager.java  # JDBC + SQLite: users & chat history
│   ├── server/
│   │   ├── ChatServer.java       # Accepts TCP connections, spawns threads
│   │   └── ClientHandler.java    # One thread per client; routes messages
│   ├── client/
│   │   └── ChatClient.java       # TCP socket wrapper + background receiver
│   └── gui/
│       ├── Main.java             # Entry point for the client app
│       ├── LoginWindow.java      # Registration / Login screen
│       └── ChatWindow.java       # Main chat interface
├── lib/
│   └── sqlite-jdbc-3.45.1.0.jar # Place SQLite JDBC driver here
├── build.sh                      # Linux/macOS build & run script
├── build.bat                     # Windows build & run script
└── README.md
```

---

## ⚙️ Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or higher |
| SQLite JDBC | 3.45.x |

### Download SQLite JDBC Driver
Download `sqlite-jdbc-3.45.1.0.jar` from:
```
https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
```
Place it in the `lib/` folder.

---

## 🔨 Build

### Linux / macOS
```bash
chmod +x build.sh
./build.sh
```

### Windows
```batch
build.bat
```

This compiles all sources and creates two executable JARs:
- `ChatServer.jar` — the server
- `ChatClient.jar` — the client GUI

---

## 🚀 Running the Application

### Step 1 — Start the Server
```bash
# Default port 5000
java -jar ChatServer.jar

# Custom port
java -jar ChatServer.jar 6000
```

### Step 2 — Start One or More Clients
```bash
java -jar ChatClient.jar
```
- Enter the server IP, port, username, and password
- Click **Register** (first time) or **Login**

---

## 💬 Features

| Feature | Description |
|---|---|
| User Registration & Login | Credentials stored in SQLite via JDBC |
| Global Broadcast Chat | Send messages to all online users |
| Private Messaging | Click a user in the sidebar → private chat |
| Online User List | Live-updated list of connected users |
| Chat History | Last 50 messages loaded from database |
| File Transfer | Send files up to 50 MB through the server |
| Multi-Client | Server handles unlimited simultaneous clients via multithreading |

---

## 🏗 Architecture

```
  [Client GUI]   [Client GUI]   [Client GUI]
       |               |               |
       └───────────────┼───────────────┘
                       │  TCP Sockets (port 5000)
                       ▼
              ┌─────────────────┐
              │   ChatServer    │
              │  ─────────────  │
              │  ClientHandler  │ ← one Thread per client
              │  (×N threads)   │
              │  ─────────────  │
              │  DatabaseMgr    │
              └────────┬────────┘
                       │ JDBC
                       ▼
                 chatapp.db (SQLite)
                 ┌──────────┐
                 │  users   │
                 │ messages │
                 └──────────┘
```

### Message Protocol

All communication uses Java Object Serialization over TCP. The `Message` class carries a `Type` enum:

| Type | Direction | Purpose |
|---|---|---|
| `LOGIN` | Client → Server | Authenticate existing user |
| `SUCCESS` | Client → Server | Register new user |
| `BROADCAST` | Both | Global chat message |
| `PRIVATE` | Both | Direct message |
| `FILE_REQUEST` | Both | File transfer payload |
| `USER_LIST` | Server → Client | Updated online user list |
| `CHAT_HISTORY` | Both | History request/response |
| `ERROR` | Server → Client | Error notification |
| `LOGOUT` | Client → Server | Disconnect |

---

## 🗄 Database Schema

```sql
CREATE TABLE users (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT NOT NULL UNIQUE,
    password   TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    sender    TEXT NOT NULL,
    receiver  TEXT,              -- NULL = broadcast
    content   TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🔑 CN Concepts Demonstrated

- **TCP Socket Programming** — `ServerSocket` / `Socket` in Java
- **Client-Server Architecture** — centralized server routes all messages
- **Multi-threading** — `Thread` per client for concurrent handling
- **Serialization** — Java `ObjectInputStream` / `ObjectOutputStream`
- **JDBC** — SQLite database via JDBC driver
- **GUI** — Java Swing for the client interface

---

## 📝 Notes

- Passwords are stored in plain text for simplicity. In production, use bcrypt.
- File transfers are buffered entirely in memory; for large files consider streaming.
- The `chatapp.db` SQLite file is created automatically in the directory where the server runs.
