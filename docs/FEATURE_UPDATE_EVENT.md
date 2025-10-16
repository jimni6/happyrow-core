# PUT Event Endpoint - Feature Implementation

## Overview

Successfully implemented a **PUT endpoint** to update existing events in the HappyRow Core API, following the hexagonal architecture pattern.

## Implementation Details

### Architecture Layers

#### 1. **Domain Layer** (`domain/src/main`)
- **UpdateEventRequest.kt** - Request model containing event data and identifier
- **UpdateEventException.kt** - Domain exception for update failures
- **UpdateEventRepositoryException.kt** - Repository-level exception
- **UpdateEventUseCase.kt** - Business logic orchestration
- **EventRepository.kt** - Updated interface with `update()` method

#### 2. **Infrastructure Layer** (`infrastructure/src/main`)
- **UpdateEventRequestDto.kt** - HTTP request DTO with validation
- **UpdateEventEndpoint.kt** - Ktor route handler for PUT requests
- **SqlEventRepository.kt** - Database implementation using Exposed ORM
- **EventEndpoints.kt** - Updated to wire the new endpoint

#### 3. **Application Layer** (`src/main`)
- **UseCaseModule.kt** - Dependency injection for UpdateEventUseCase
- **Routing.kt** - Route configuration

### Key Features

✅ **Full event update** - Updates all editable fields (name, description, location, type, event_date, members)  
✅ **Automatic timestamp** - Updates `update_date` automatically  
✅ **Error handling** - Proper HTTP status codes (200 OK, 404 Not Found, 409 Conflict)  
✅ **Validation** - Invalid UUID and missing event validation  
✅ **Immutable creator** - Original creator preserved during updates  

### API Endpoint

```
PUT /event/configuration/api/v1/events/{eventId}
```

**Headers:**
- `Content-Type: application/json`
- `x-user-id: {userId}` (required)

**Request Body:**
```json
{
  "name": "Updated Event Name",
  "description": "Updated description",
  "event_date": "2025-10-25T17:00:00.000Z",
  "location": "Updated Location",
  "type": "BIRTHDAY"
}
```

**Success Response (200 OK):**
```json
{
  "identifier": "74b06aa2-df2e-4245-9a09-839cdcb2fd77",
  "name": "UPDATED Event Name",
  "description": "This event has been successfully updated!",
  "event_date": 1761411600000,
  "creation_date": 1760612207278,
  "update_date": 1760612227643,
  "creator": "ab70634a-345e-415e-8417-60841b6bcb20",
  "location": "New Updated Location",
  "type": "BIRTHDAY",
  "members": []
}
```

**Error Responses:**

| Status | Type | Description |
|--------|------|-------------|
| 400 | INVALID_BODY | Request body parsing failed |
| 400 | INVALID_PARAMETER | Invalid event ID format |
| 404 | EVENT_NOT_FOUND | Event with specified ID doesn't exist |
| 409 | NAME_ALREADY_EXISTS | Event name conflicts with existing event |

## Testing

### Test Results

**✅ Successful Update:**
```bash
curl -X PUT "http://localhost:8080/event/configuration/api/v1/events/74b06aa2-df2e-4245-9a09-839cdcb2fd77" \
  -H "Content-Type: application/json" \
  -H "x-user-id: ab70634a-345e-415e-8417-60841b6bcb20" \
  -d '{
    "name": "UPDATED Event Name",
    "description": "This event has been successfully updated!",
    "event_date": "2025-10-25T17:00:00.000Z",
    "location": "New Updated Location",
    "type": "BIRTHDAY"
  }'
```

**Response:** HTTP 200 with updated event

**✅ Error Handling - Event Not Found:**
```bash
curl -X PUT "http://localhost:8080/event/configuration/api/v1/events/00000000-0000-0000-0000-000000000000" \
  -H "Content-Type: application/json" \
  -H "x-user-id: ab70634a-345e-415e-8417-60841b6bcb20" \
  -d '{"name": "Test", "description": "Test", "event_date": "2025-10-25T17:00:00.000Z", "location": "Test", "type": "PARTY"}'
```

**Response:** HTTP 404 with error message

## Database Impact

The update operation:
- Modifies all specified fields in the `configuration.event` table
- Automatically updates the `update_date` timestamp
- Preserves the original `creation_date` and `creator`
- Returns 0 rows updated if event doesn't exist (handled as 404)

## Files Created/Modified

### Created Files (8)
1. `domain/.../event/update/model/UpdateEventRequest.kt`
2. `domain/.../event/update/error/UpdateEventException.kt`
3. `domain/.../event/common/error/UpdateEventRepositoryException.kt`
4. `domain/.../event/update/UpdateEventUseCase.kt`
5. `infrastructure/.../event/update/driving/dto/UpdateEventRequestDto.kt`
6. `infrastructure/.../event/update/driving/UpdateEventEndpoint.kt`
7. `docs/API_TESTING.md` (updated)
8. `docs/FEATURE_UPDATE_EVENT.md` (this file)

### Modified Files (6)
1. `domain/.../EventRepository.kt` - Added `update()` method
2. `infrastructure/.../SqlEventRepository.kt` - Implemented update logic
3. `infrastructure/.../EventEndpoints.kt` - Wired up endpoint
4. `src/.../Routing.kt` - Added use case injection
5. `src/.../UseCaseModule.kt` - Registered UpdateEventUseCase
6. `.http/api-endpoints.http` - Added test requests

## Best Practices Applied

✅ **Hexagonal Architecture** - Clean separation of domain, infrastructure, and application layers  
✅ **Functional Programming** - Arrow Either for error handling  
✅ **Dependency Injection** - Koin for loose coupling  
✅ **Error Handling** - Comprehensive exception handling with proper HTTP codes  
✅ **Validation** - Input validation at all layers  
✅ **Immutability** - Domain models are immutable  
✅ **Testing** - Manual testing performed, ready for automated tests  

## Next Steps

- [ ] Add automated integration tests
- [ ] Add DELETE endpoint for event deletion
- [ ] Implement partial updates (PATCH)
- [ ] Add event versioning/audit trail
- [ ] Implement authorization checks (verify updater has permission)

## Related Documentation

- [API Testing Guide](./API_TESTING.md)
- [Database Setup](./DATABASE_SETUP.md)
- [Local Docker Setup](./LOCAL_DOCKER_RENDER_SETUP.md)
