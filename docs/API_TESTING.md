# API Testing Guide

This guide explains how to test the HappyRow Core API endpoints using IntelliJ's HTTP Client or cURL.

## Setup

### 1. Configure Environment Variables

Copy the example environment file:

```bash
cp http-client.env.json.example http-client.env.json
```

Edit `http-client.env.json` with your actual user ID:

```json
{
  "local": {
    "baseUrl": "http://localhost:8080",
    "userId": "your-actual-user-id",
    "apiPath": "/event/configuration/api/v1"
  },
  "render": {
    "baseUrl": "https://happyrow-core.onrender.com",
    "userId": "your-actual-user-id",
    "apiPath": "/event/configuration/api/v1"
  },
  "raspberry": {
    "baseUrl": "http://raspberrypi.local:8080",
    "userId": "your-actual-user-id",
    "apiPath": "/event/configuration/api/v1"
  }
}
```

> **Note:** `http-client.env.json` is gitignored to keep your user ID private.

## Using IntelliJ HTTP Client

### 1. Open the API Endpoints File

Open `api-endpoints.http` in IntelliJ IDEA.

### 2. Select Environment

Click on the environment selector dropdown (top right of the HTTP file editor) and choose:
- **local** - Test against your local Docker container or development server
- **render** - Test against the Render.com deployment
- **raspberry** - Test against your Raspberry Pi deployment

### 3. Run Requests

Click the green play button next to any request to execute it.

## Available Endpoints

### Health Checks

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/info` | Get server information |
| GET | `/health` | Health check endpoint |

### Event Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/event/configuration/api/v1/events` | Create a new event |
| GET | `/event/configuration/api/v1/events?organizer={userId}` | Get events by organizer |
| GET | `/event/configuration/api/v1/events/{eventId}` | Get specific event by ID |

## Event Types

The API supports the following event types:
- `PARTY` - Party events
- `BIRTHDAY` - Birthday celebrations
- `DINER` - Dinner events
- `SNACK` - Snack/coffee break events

## Example: Create Event

### Using IntelliJ HTTP Client

```http
POST {{baseUrl}}{{apiPath}}/events
x-user-id: {{userId}}
Content-Type: application/json

{
  "name": "Summer Party 2025",
  "description": "Annual summer celebration",
  "event_date": "2025-06-15T19:00:00Z",
  "location": "Backyard Garden",
  "type": "PARTY"
}
```

### Using cURL

```bash
curl -X POST http://localhost:8080/event/configuration/api/v1/events \
  -H "Content-Type: application/json" \
  -H "x-user-id: your-user-id" \
  -d '{
    "name": "Summer Party 2025",
    "description": "Annual summer celebration",
    "event_date": "2025-06-15T19:00:00Z",
    "location": "Backyard Garden",
    "type": "PARTY"
  }'
```

## Testing Different Environments

### Local (Docker Container)

```bash
# Start your Docker container first
docker start happyrow-app

# Select "local" environment in IntelliJ
# Or use cURL with localhost
curl http://localhost:8080/info
```

### Render Deployment

```bash
# Select "render" environment in IntelliJ
# Or use cURL
curl https://happyrow-core.onrender.com/info
```

### Raspberry Pi

```bash
# Ensure your Raspberry Pi is accessible
# Select "raspberry" environment in IntelliJ
# Or use cURL
curl http://raspberrypi.local:8080/info
```

## Troubleshooting

### Connection Refused (Local)

Make sure your Docker container is running:
```bash
docker ps | grep happyrow-app
# If not running:
docker start happyrow-app
```

### Invalid User ID

Ensure your `x-user-id` header matches a valid user ID in your system.

### CORS Issues

If testing from a browser or web app, ensure CORS is properly configured in the application.

## Response Examples

### Successful Event Creation (201 Created)

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Summer Party 2025",
  "description": "Annual summer celebration",
  "event_date": "2025-06-15T19:00:00Z",
  "location": "Backyard Garden",
  "type": "PARTY",
  "organizer": "ab70634a-345e-415e-8417-60841b6bcb20",
  "created_at": "2025-10-16T10:00:00Z"
}
```

### Error Response (400 Bad Request)

```json
{
  "error": "Invalid request",
  "message": "Event date must be in the future"
}
```

## Next Steps

- Check out [API documentation](../README.md) for more details
- Review [deployment guides](./LOCAL_DOCKER_RENDER_SETUP.md) for environment setup
- See [database setup](./DATABASE_SETUP.md) for schema information
