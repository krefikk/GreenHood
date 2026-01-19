# GreenHood: Community Waste Management System
**GreenHood** is a robust desktop application designed to streamline community waste management and recycling processes. It facilitates seamless interaction between residents (Neighbors) and waste management entities (Companies) through a secure, localized, and efficient interface.
This project demonstrates advanced Java Swing development, rigorous security and PostgreSQL practices.

## Key Features
* **Role-Based Access Control:** Distinct interfaces and functionalities for **User** (Neighbor) and **Company** roles.
* **Dual-Language Support:** Full localization support for **English** and **Turkish**, dynamically switchable.
* **Secure Authentication:** Industry-standard password security and session management.
* **Smart Password Recovery:** A secure, OTP/Link-based recovery system protected against spam and replay attacks.
* **Native Experience:** Self-contained installers for macOS (`.dmg`) and Windows (`.exe`), requiring no pre-installed Java on the client machine.

## Tech Stack & Architecture
This project was engineered with a focus on **performance**, **security**, and **maintainability**.

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 25 | Core logic and modern language features. |
| **GUI** | Java Swing | Custom-styled desktop interface. |
| **Database** | PostgreSQL | Relational data storage. |
| **ORM/JDBC** | JDBC & HikariCP | Optimized database interaction. |
| **Security** | BCrypt | Password hashing and salting. |
| **Packaging** | jpackage | Native executable generation. |

## ER Diagram of Database
![ER](img/ERDiagram.png)

## Engineering Highlights

### Security
* **SQL Injection Prevention:** All database queries utilize `PreparedStatement` to ensure data integrity and prevent injection attacks.
* **Password Hashing:** User passwords are never stored in plain text. We utilize **BCrypt** with per-user salting.
* **Anti-Spam Recovery System:** The password recovery module implements a **Server-Side Time Validation** algorithm. It calculates the delta between the database timestamp and the server time (not the client time) to enforce a strict rate limit, mitigating 100% of client-side time manipulation attacks.

### Performance
* **Connection Pooling:** Integrated **HikariCP** to manage database connections, reducing query latency.
* **Resource Management:** Implemented a strict `try-with-resources` pattern across the data layer to ensure **leak-free** operation and proper closure of connections.

## Screenshots

| Title | Screenshot |
| :---: | :---: |
| Login Page | ![Login](img/login.png) |
| Main Dashboard | ![Dashboard](img/dashboard.png) |
| Stats Page | ![Stats](img/stats.png) |
| Edit Profile | ![EditProfile](img/editprofile.png) |

## Installation

### For End Users
Go to the **[Releases](https://github.com/krefikk/GreenHood/releases)** page to download the latest installer for your operating system:

*Note: You do not need Java installed to run these files.*
