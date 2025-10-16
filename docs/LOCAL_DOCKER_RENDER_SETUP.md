# Local Docker + Render Database Setup Guide

This guide shows you how to run your HappyRow Core application locally in Docker while connecting to your production PostgreSQL database on Render.

## 🎯 Overview

- **Application**: Runs locally in Docker container (port 8080)
- **Database**: Uses your production PostgreSQL database on Render
- **Benefits**: Test with real data, faster local development, no local database setup needed

## 📋 Prerequisites

1. Docker and Docker Compose installed
2. Access to your Render dashboard
3. Your Render PostgreSQL service running

## 🔧 Setup Steps

### Step 1: Get Your Render Database Credentials

1. Go to your [Render Dashboard](https://dashboard.render.com)
2. Navigate to your PostgreSQL service
3. Click on the **"Connect"** tab
4. Copy the **"External Database URL"**

The URL will look like:
```
postgresql://username:password@dpg-xxxxx-a.oregon-postgres.render.com:5432/database_name
```

### Step 2: Create Your Local Environment File

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your Render database credentials:
   ```bash
   # Replace with your actual Render database credentials
   DATABASE_URL=postgresql://your_render_user:your_render_password@dpg-xxxxx-a.oregon-postgres.render.com:5432/your_render_database
   DB_USERNAME=your_render_user
   DB_PASSWORD=your_render_password
   DB_SSL_MODE=require
   
   # Application settings
   ENVIRONMENT=development
   KTOR_ENV=development
   PORT=8080
   ```

### Step 3: Run Your Application

```bash
# Build and start your application
docker-compose up --build

# Or run in detached mode (background)
docker-compose up --build -d
```

### Step 4: Test Your Setup

1. **Check if the application is running**:
   ```bash
   curl http://localhost:8080/
   ```

2. **Test the info endpoint**:
   ```bash
   curl http://localhost:8080/info
   ```

3. **Create a test event**:
   ```bash
   curl -X POST http://localhost:8080/event/configuration/api/v1/events \
     -H "Content-Type: application/json" \
     -H "x-user-id: local-test-user" \
     -d '{
       "name": "Local Docker Test Event",
       "description": "Testing local Docker with Render database",
       "event_date": "2024-12-31T23:00:00Z",
       "location": "Local Development",
       "type": "PARTY",
       "members": []
     }'
   ```

## 🔍 Useful Commands

```bash
# View application logs
docker-compose logs -f happyrow-core

# Stop the application
docker-compose down

# Rebuild and restart
docker-compose up --build

# Run a shell inside the container (for debugging)
docker-compose exec happyrow-core sh
```

## 🛠️ Troubleshooting

### Connection Issues

1. **SSL Certificate Error**:
   - Ensure `DB_SSL_MODE=require` in your `.env` file
   - Render requires SSL connections

2. **Authentication Failed**:
   - Double-check your database credentials in `.env`
   - Verify the credentials work by testing with a PostgreSQL client

3. **Network Timeout**:
   - Check if your firewall allows outbound connections on port 5432
   - Verify the Render database host is accessible

### Database Schema Issues

If you get schema-related errors, your database might need initialization. The application should automatically create the necessary schema and tables on startup.

### Application Won't Start

1. Check Docker logs:
   ```bash
   docker-compose logs happyrow-core
   ```

2. Verify environment variables:
   ```bash
   docker-compose config
   ```

3. Test database connection manually:
   ```bash
   # Install PostgreSQL client (if not already installed)
   # macOS: brew install postgresql
   # Test connection
   psql "postgresql://your_user:your_password@your_host:5432/your_database"
   ```

## 🔒 Security Notes

- **Never commit your `.env` file** - it contains sensitive database credentials
- The `.env` file is already in `.gitignore` to prevent accidental commits
- Use strong, unique passwords for your Render database
- Consider using environment-specific databases (dev/staging/prod)

## 🚀 Benefits of This Setup

1. **Real Data**: Test with your actual production data
2. **Fast Development**: No need to set up local database
3. **Consistent Environment**: Same database schema as production
4. **Easy Deployment**: Your local changes can be easily deployed to Render
5. **Team Collaboration**: Everyone can use the same database for development

## 📝 Next Steps

Once your local setup is working:

1. Test all your API endpoints
2. Verify database operations (create, read, update)
3. Check that your application logs are working
4. Consider setting up a staging database on Render for testing

## 🆘 Need Help?

If you encounter issues:
1. Check the application logs: `docker-compose logs -f`
2. Verify your Render database is running and accessible
3. Test your database credentials with a PostgreSQL client
4. Ensure your `.env` file has the correct format and values
