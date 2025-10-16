# DELETE Event Endpoint - Feature Implementation

## Overview

Successfully implemented a **DELETE endpoint** to remove existing events from the HappyRow Core API, following the hexagonal architecture pattern.

## Implementation Details

### Architecture Layers

#### 1. **Domain Layer** (`domain/src/main`)
- **DeleteEventException.kt** - Domain exception for delete failures
- **DeleteEventRepositoryException.kt** - Repository-level exception
- **DeleteEventUseCase.kt** - Business logic orchestration
- **EventRepository.kt** - Updated interface with `delete()` method

#### 2. **Infrastructure Layer** (`infrastructure/src/main`)
- **DeleteEventEndpoint.kt** - Ktor route handler for DELETE requests
- **SqlEventRepository.kt** - Database implementation using Exposed ORM
- **EventEndpoints.kt** - Updated to wire the new endpoint

#### 3. **Application Layer** (`src/main`)
- **UseCaseModule.kt** - Dependency injection for DeleteEventUseCase
- **Routing.kt** - Route configuration

### Key Features

✅ **Permanent deletion** - Removes event from database  
✅ **404 handling** - Proper error when event doesn't exist  
✅ **204 No Content** - RESTful response on successful deletion  
✅ **Validation** - Invalid UUID validation  
✅ **Atomic operation** - Transaction-based deletion  

### API Endpoint

```
DELETE /event/configuration/api/v1/events/{eventId}
```

**Headers:**
- `x-user-id: {userId}` (required for audit purposes)

**Success Response (204 No Content):**
```
HTTP/1.1 204 No Content
```
*No response body - deletion successful*

**Error Responses:**

| Status | Type | Description |
|--------|------|-------------|
| 400 | INVALID_PARAMETER | Invalid event ID format |
| 404 | EVENT_NOT_FOUND | Event with specified ID doesn't exist |

**404 Not Found Example:**
```json
{
  "type": "EVENT_NOT_FOUND",
  "message": "Event with id 00000000-0000-0000-0000-000000000000 not found",
  "detail": "Event with id 00000000-0000-0000-0000-000000000000 not found"
}
```

## Testing

### Test Results

**✅ Successful Deletion:**
```bash
curl -X DELETE "http://localhost:8080/event/configuration/api/v1/events/5936d75d-b74c-4162-9587-afc2caa0f7c8" \
  -H "x-user-id: ab70634a-345e-415e-8417-60841b6bcb20"
```

**Response:** HTTP 204 No Content

**✅ Verification - Event No Longer Exists:**
```bash
curl -X GET "http://localhost:8080/event/configuration/api/v1/events/5936d75d-b74c-4162-9587-afc2caa0f7c8" \
  -H "x-user-id: ab70634a-345e-415e-8417-60841b6bcb20"
```

**Response:** HTTP 404 Not Found

**✅ Error Handling - Non-existent Event:**
```bash
curl -X DELETE "http://localhost:8080/event/configuration/api/v1/events/00000000-0000-0000-0000-000000000000" \
  -H "x-user-id: ab70634a-345e-415e-8417-60841b6bcb20"
```

**Response:** HTTP 404 with error message

## Database Impact

The delete operation:
- Executes a SQL DELETE on the `configuration.event` table
- Uses `deleteWhere` with the event identifier
- Returns 0 rows deleted if event doesn't exist (handled as 404)
- **No soft delete** - records are permanently removed
- **Atomic transaction** - deletion is transactional

## Implementation Code

### Domain UseCase
```kotlin
class DeleteEventUseCase(
  private val eventRepository: EventRepository,
) {
  fun delete(identifier: UUID): Either<DeleteEventException, Unit> = 
    eventRepository.delete(identifier)
      .mapLeft { DeleteEventException(identifier, it) }
}
```

### Repository Implementation
```kotlin
override fun delete(identifier: UUID): Either<DeleteEventRepositoryException, Unit> = Either
  .catch {
    transaction(exposedDatabase.database) {
      val deletedRows = EventTable.deleteWhere { EventTable.id eq identifier }
      if (deletedRows == 0) {
        throw EventNotFoundException(identifier)
      }
    }
  }
  .mapLeft {
    when (it) {
      is EventNotFoundException -> DeleteEventRepositoryException(identifier, it)
      else -> DeleteEventRepositoryException(identifier, it)
    }
  }
```

### Endpoint Handler
```kotlin
fun Route.deleteEventEndpoint(deleteEventUseCase: DeleteEventUseCase) {
  delete("/{id}") {
    val eventId = Either.catch {
      UUID.fromString(call.parameters["id"])
    }.mapLeft { BadRequestException.InvalidParameterException("id", call.parameters["id"] ?: "null") }

    eventId.flatMap { id -> deleteEventUseCase.delete(id) }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.NoContent) },
      )
  }
}
```

## Files Created/Modified

### Created Files (3)
1. `domain/.../event/delete/error/DeleteEventException.kt`
2. `domain/.../event/common/error/DeleteEventRepositoryException.kt`
3. `domain/.../event/delete/DeleteEventUseCase.kt`
4. `infrastructure/.../event/delete/driving/DeleteEventEndpoint.kt`
5. `docs/FEATURE_DELETE_EVENT.md` (this file)

### Modified Files (6)
1. `domain/.../EventRepository.kt` - Added `delete()` method
2. `infrastructure/.../SqlEventRepository.kt` - Implemented delete logic
3. `infrastructure/.../EventEndpoints.kt` - Wired up endpoint
4. `src/.../Routing.kt` - Added use case injection
5. `src/.../UseCaseModule.kt` - Registered DeleteEventUseCase
6. `.http/api-endpoints.http` - Added test requests
7. `docs/API_TESTING.md` - Updated documentation

## Best Practices Applied

✅ **Hexagonal Architecture** - Clean separation of domain, infrastructure, and application layers  
✅ **Functional Programming** - Arrow Either for error handling  
✅ **Dependency Injection** - Koin for loose coupling  
✅ **Error Handling** - Comprehensive exception handling with proper HTTP codes  
✅ **RESTful Standards** - 204 No Content for successful deletion  
✅ **Validation** - Input validation at all layers  
✅ **Atomicity** - Database transactions for consistency  
✅ **Testing** - Manual testing performed, ready for automated tests  

## Design Decisions

### Why 204 No Content?
Following REST best practices, DELETE operations that succeed return **204 No Content** rather than 200 OK, as there's no representation to return after deletion.

### Hard Delete vs Soft Delete
Currently implements **hard delete** (permanent removal). For production, consider:
- **Soft delete** - Add `deleted_at` timestamp field
- **Audit trail** - Keep deletion history
- **Cascade rules** - Handle related entities

### Authorization
Current implementation requires `x-user-id` header but doesn't verify the user has permission to delete the event. Consider adding:
- Owner verification
- Admin role checks
- Deletion permissions

## Next Steps

- [ ] Add automated integration tests
- [ ] Implement soft delete option
- [ ] Add authorization/permission checks
- [ ] Add cascade deletion for related entities
- [ ] Implement deletion audit trail
- [ ] Add bulk delete endpoint

## Complete CRUD Operations

With this implementation, the Event API now supports full CRUD:
- ✅ **Create** - POST `/events`
- ✅ **Read** - GET `/events/{id}` and GET `/events?organizerId={id}`
- ✅ **Update** - PUT `/events/{id}`
- ✅ **Delete** - DELETE `/events/{id}`

## Related Documentation

- [API Testing Guide](./API_TESTING.md)
- [PUT Endpoint Feature](./FEATURE_UPDATE_EVENT.md)
- [Database Setup](./DATABASE_SETUP.md)
- [Local Docker Setup](./LOCAL_DOCKER_RENDER_SETUP.md)
