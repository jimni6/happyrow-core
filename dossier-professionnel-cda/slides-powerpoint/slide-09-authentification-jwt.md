# SLIDE 9 : AUTHENTIFICATION JWT AVEC SUPABASE

---

## 🔐 Architecture JWT

```
┌────────────────────────────────────────────────────────────────┐
│                        CLIENT (Frontend)                        │
│                                                                  │
│  1. Login → Supabase Auth                                       │
│  2. Reçoit JWT Token                                            │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            │ Authorization: Bearer {token}
                            ▼
┌────────────────────────────────────────────────────────────────┐
│                    HAPPYROW CORE API (Backend)                  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         JwtAuthenticationPlugin (Interceptor)            │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ 3. Extrait le token du header Authorization              │  │
│  │ 4. Valide avec SupabaseJwtService                        │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│                       ▼                                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           SupabaseJwtService (Validateur)                │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ 5. Vérifie signature (HMAC256)                           │  │
│  │ 6. Vérifie issuer (Supabase URL)                         │  │
│  │ 7. Vérifie audience ("authenticated")                    │  │
│  │ 8. Extrait userId et email du payload                    │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│                       ▼                                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      Endpoints (CreateEvent, GetEvents, etc.)            │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │ 9. Utilise call.getAuthenticatedUser()                   │  │
│  │ 10. Accède à userId et email                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Implémentation SupabaseJwtService

```kotlin
class SupabaseJwtService(
  private val config: SupabaseJwtConfig
) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)
  
  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> {
    return Either.catch {
      // Création du vérificateur JWT
      val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)      // Ex: "https://xxx.supabase.co/auth/v1"
        .withAudience(config.audience)  // "authenticated"
        .build()
      
      // Vérification du token
      val verifiedJwt = verifier.verify(token)
      
      // Extraction des claims
      extractUser(verifiedJwt)
    }
  }
  
  private fun extractUser(jwt: DecodedJWT): AuthenticatedUser {
    val userId = jwt.subject 
      ?: throw JWTVerificationException("Missing 'sub' claim")
    
    val email = jwt.getClaim("email").asString()
      ?: throw JWTVerificationException("Missing 'email' claim")
    
    return AuthenticatedUser(userId, email)
  }
}
```

---

## ⚙️ Configuration Sécurisée

```kotlin
data class SupabaseJwtConfig(
  val jwtSecret: String,    // Secret de signature
  val issuer: String,       // URL Supabase
  val audience: String      // "authenticated"
) {
  companion object {
    fun fromEnvironment(): SupabaseJwtConfig {
      val supabaseUrl = System.getenv("SUPABASE_URL")
        ?: error("SUPABASE_URL required")
      
      val jwtSecret = System.getenv("SUPABASE_JWT_SECRET")
        ?: error("SUPABASE_JWT_SECRET required")
      
      return SupabaseJwtConfig(
        jwtSecret = jwtSecret,
        issuer = "$supabaseUrl/auth/v1",
        audience = "authenticated"
      )
    }
  }
}
```

✅ **Secrets en variables d'environnement** : Jamais dans le code  
✅ **Configuration externalisée** : Différente par environnement  
✅ **Fail-fast** : Erreur au démarrage si config manquante

---

## 🔌 Plugin Ktor Personnalisé

```kotlin
class JwtAuthenticationPlugin(
  private val jwtService: SupabaseJwtService
) {
  fun intercept(call: ApplicationCall) {
    val authHeader = call.request.header("Authorization")
    
    if (authHeader?.startsWith("Bearer ") == true) {
      val token = authHeader.removePrefix("Bearer ")
      
      jwtService.validateToken(token)
        .map { user -> 
          // Stocke l'utilisateur dans les attributs de la requête
          call.attributes.put(authenticatedUserKey, user)
        }
    }
  }
}

// Installation dans Ktor
fun Application.application() {
  val jwtService by inject<SupabaseJwtService>()
  
  install(JwtAuthenticationPlugin) {
    this.jwtService = jwtService
  }
}
```

---

## 👤 Extraction de l'Utilisateur

```kotlin
// Extension function pour récupérer l'utilisateur authentifié
fun ApplicationCall.getAuthenticatedUser(): Either<Throwable, AuthenticatedUser> {
  return Either.catch {
    attributes[authenticatedUserKey]
  }
}

// Utilisation dans un endpoint
fun Route.createEventEndpoint(useCase: CreateEventUseCase) {
  post("/events") {
    call.receive<CreateEventRequestDto>()
      // 1. Récupère l'utilisateur JWT
      .let { requestDto ->
        call.getAuthenticatedUser()
          .map { user -> requestDto.toDomain(user.userId) }
      }
      // 2. Exécute le Use Case avec l'userId du token
      .flatMap { request -> useCase.execute(request) }
      // 3. Retourne la réponse
      .fold(
        ifLeft = { error -> call.respondError(error) },
        ifRight = { event -> call.respond(HttpStatusCode.Created, event.toDto()) }
      )
  }
}
```

---

## 🔒 Sécurité JWT

### ✅ Vérifications Effectuées

| Vérification | Description | Sécurité |
|--------------|-------------|----------|
| **Signature** | HMAC256 avec secret | ✅ Empêche falsification |
| **Issuer** | URL Supabase vérifiée | ✅ Empêche tokens d'autres services |
| **Audience** | "authenticated" requis | ✅ Empêche tokens mal ciblés |
| **Expiration** | Token expiré rejeté | ✅ Limite fenêtre d'attaque |
| **Subject** | userId présent | ✅ Identification obligatoire |

### 🛡️ Protection Contre

❌ **Token forgé** : Signature invalide → Rejeté  
❌ **Token volé d'autre service** : Issuer différent → Rejeté  
❌ **Token expiré** : Hors validité → Rejeté  
❌ **Token modifié** : Signature ne correspond plus → Rejeté

---

## 📊 Exemple de Token JWT

```
Header (Base64)
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload (Base64)
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  ← userId
  "email": "user@example.com",                    ← email
  "iss": "https://xxx.supabase.co/auth/v1",       ← issuer
  "aud": "authenticated",                          ← audience
  "exp": 1735732800,                               ← expiration
  "iat": 1735729200                                ← issued at
}

Signature (HMAC256)
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

**Format final** : `header.payload.signature`

---

## ✅ Avantages de l'Implémentation

| Avantage | Bénéfice |
|----------|----------|
| **🔐 Sécurité renforcée** | Authentification cryptographique forte |
| **🏢 Service professionnel** | Supabase gère users, passwords, 2FA |
| **🔌 Plugin réutilisable** | Logique centralisée et maintenable |
| **🧪 Testable** | Service mockable pour tests |
| **📦 Découplage** | Frontend délègue auth à Supabase |

---

## 🎓 Compétences CDA

**CDA-3.1** : Préparer le déploiement sécurisé  
**CDA-3.2** : Sécuriser l'accès aux données  
**CDA-3.3** : Sécuriser les échanges
