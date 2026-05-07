# User Account & Settings Management System

## Project Description

The User Account & Settings Management System is a Java Swing desktop application that allows users to create accounts, log in, manage their profile, update account settings, and allows administrators to manage user accounts.

The system uses SQLite as the database and JDBC for database connectivity. It demonstrates authentication, role-based access, account status control, profile updates, settings management, and CRUD operations.

## Case Study

Case Study 1: User Account & Settings Management System

## Technologies Used

- Java
- Java Swing
- SQLite
- JDBC
- Maven
- Git
- GitHub

## Main Features

### User Features

- Register a new account
- Log in with username and password
- Log out
- View dashboard
- Edit profile information
- Change password
- Update account settings

### Settings Features

- Change theme preference
- Change language preference
- Enable or disable notifications
- Enable or disable privacy mode

### Admin Features

- View all users
- Add new users
- Delete users
- Deactivate users
- Reactivate users
- Promote users to admin
- Demote admins to regular users

## Default Admin Login

When the program runs for the first time, it automatically creates a default admin account.

```text
Username: admin
Password: admin123
```

## Project Structure

```text
UserAccountSettingsSystem/
├── pom.xml
├── README.md
├── database/
│   └── user_settings.db
├── screenshots/
└── src/
    └── main/
        └── java/
            └── app/
                └── Main.java
```

## Database Design

The application uses two main database tables: `users` and `settings`.

### users Table

The `users` table stores account information such as name, email, username, password, role, and account status.

```sql
CREATE TABLE users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'user',
    status TEXT DEFAULT 'active'
);
```

### settings Table

The `settings` table stores each user's account preferences.

```sql
CREATE TABLE settings (
    setting_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    theme TEXT DEFAULT 'light',
    notifications_enabled INTEGER DEFAULT 1,
    language TEXT DEFAULT 'English',
    privacy_mode INTEGER DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

## How to Run the Program

Make sure Java and Maven are installed.

From the project folder, run:

```bash
mvn clean compile exec:java
```

The application window will open. The SQLite database file will be created automatically inside the `database` folder.

## How to Use the Application

1. Run the program.
2. Log in using the default admin account or create a new user account.
3. Use the dashboard to access profile management and account settings.
4. Admin users can open the Admin User Management page.
5. Admin users can add, delete, deactivate, reactivate, promote, and demote users.

## Security Notes

Passwords are not stored as plain text. The program hashes passwords using SHA-256 before saving them to the database.

Admin-only features are protected by checking the logged-in user's role. Only users with the `admin` role can access the Admin User Management page.

## Screenshots for Report

Recommended screenshots include:

- Login page
- Registration page
- Dashboard
- Profile Management page
- Account Settings page
- Admin User Management page
- Admin Add User page
- Successful registration message
- Successful profile update message
- Database file or database tables
- GitHub repository
- Git commit history

## Git Commands Used

```bash
git init
git add .
git commit -m "Initial user account settings system"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/UserAccountSettingsSystem.git
git push -u origin main
```

## Example Test Accounts

The default admin account is:

```text
Username: admin
Password: admin123
```

Additional users can be created through the registration page or by using the admin Add User feature.

## Project Benefits

This system makes account management easier by storing user information and settings in a database. It allows regular users to manage their own profiles and preferences while giving administrators control over user accounts.

The project demonstrates real-world programming concepts such as database connectivity, CRUD operations, authentication, role-based access control, password hashing, and GUI development.

## Group Member Responsibilities

```text
Jelani Denmark: Database setup, Maven setup, GitHub setup
Leslie Selorm Afeawo: Login and registration features
Isaiah Ladejobi: Profile and settings features
Marc Jenkins: Admin panel, screenshots, and documentation
```

## Conclusion

The User Account & Settings Management System is a functional Java desktop application that demonstrates user authentication, profile management, settings management, and administrator-level account control. The project uses Java Swing for the interface, SQLite for data storage, and Maven for dependency management.
