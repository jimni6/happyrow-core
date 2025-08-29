# Database Setup - Render PostgreSQL Integration

This document explains how to set up and use the PostgreSQL database connection infrastructure for the HappyRow Core application.

## Architecture Overview

The database infrastructure consists of:
- **DatabaseConfig**: Configuration data class for database connection parameters
- **DatabaseFactory**: Factory object for creating and managing database connections
- **ConfigurationManager**: Handles environment variables and configuration parsing
- **HikariCP**: Connection pooling for optimal performance
- **Exposed ORM**: Kotlin SQL framework for database operations

## Environment Setup

### Local Development

For local development, configure your database settings in `src/main/resources/application.conf`:

```hocon
database {
    url = "jdbc:postgresql://localhost:5432/happyrow_dev"
    username = "your_username"
    password = "your_password"
    maxPoolSize = 10
    connectionTimeout = 30000
    idleTimeout = 600000
    maxLifetime = 1800000
    sslMode = "disable"
}
```

### Render Deployment

For Render deployment, the application automatically detects the `DATABASE_URL` environment variable:

1. **In Render Dashboard**: 
   - Go to your service settings
   - Add the `DATABASE_URL` environment variable
   - Format: `postgresql://username:password@host:port/database`

2. **Automatic Configuration**: 
   - The `ConfigurationManager` automatically parses the Render DATABASE_URL
   - SSL is automatically enabled for Render PostgreSQL connections

## Usage

### Database Initialization

Database initialization happens automatically when the application starts:

```kotlin
fun Application.module() {
    // Database is initialized here
    configureDatabases()
    configureRouting()
}
```

### Creating Tables

Use the `DatabaseFactory.createTables()` method to create your database schema:

```kotlin
import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    
    override val primaryKey = PrimaryKey(id)
}

// In your application startup:
DatabaseFactory.createTables(Users)
```

### Database Operations

Use Exposed ORM for database operations:

```kotlin
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

// Insert data
transaction {
    Users.insert {
        it[name] = "John Doe"
        it[email] = "john@example.com"
    }
}

// Query data
val users = transaction {
    Users.selectAll().map {
        User(it[Users.id], it[Users.name], it[Users.email])
    }
}
```

## Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `url` | JDBC database URL | Required |
| `username` | Database username | Required |
| `password` | Database password | Required |
| `maxPoolSize` | Maximum connection pool size | 10 |
| `connectionTimeout` | Connection timeout in milliseconds | 30000 |
| `idleTimeout` | Idle connection timeout in milliseconds | 600000 |
| `maxLifetime` | Maximum connection lifetime in milliseconds | 1800000 |
| `sslMode` | SSL mode (require, disable, etc.) | "require" |

## SSL Configuration

For Render PostgreSQL, SSL is automatically configured:
- SSL mode is set to "require"
- SSL certificates are handled automatically
- No additional SSL configuration needed

## Troubleshooting

### Common Issues

1. **Connection Refused**: 
   - Check if PostgreSQL is running (local development)
   - Verify DATABASE_URL format (Render deployment)

2. **SSL Certificate Issues**:
   - Ensure SSL mode is set to "require" for Render
   - Check if certificates are properly configured

3. **Pool Exhaustion**:
   - Increase `maxPoolSize` if needed
   - Check for connection leaks in your code

### Debugging

Enable database logging by adding to your `application.conf`:

```hocon
ktor {
    development = true
}
```

### Health Check

The application logs database initialization success:
```
Database initialized successfully
Database URL: jdbc:postgresql://host:port/database
```

## Best Practices

1. **Use Transactions**: Always wrap database operations in transactions
2. **Connection Pooling**: The HikariCP pool is configured automatically
3. **Error Handling**: Implement proper error handling for database operations
4. **Migration Strategy**: Use Exposed's schema management for database migrations
5. **Environment Variables**: Never commit database credentials to version control

## Example Implementation

Here's a complete example of a simple user service:

```kotlin
// User entity
data class User(val id: Int, val name: String, val email: String)

// Database table
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    
    override val primaryKey = PrimaryKey(id)
}

// Service class
class UserService {
    fun createUser(name: String, email: String): User {
        return transaction {
            val id = Users.insert {
                it[Users.name] = name
                it[Users.email] = email
            } get Users.id
            
            User(id, name, email)
        }
    }
    
    fun getAllUsers(): List<User> {
        return transaction {
            Users.selectAll().map {
                User(it[Users.id], it[Users.name], it[Users.email])
            }
        }
    }
}
```
