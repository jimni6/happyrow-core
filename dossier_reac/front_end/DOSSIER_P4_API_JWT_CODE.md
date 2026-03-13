# Code d'Appel API et Gestion du JWT Côté Client -- HappyRow Front-End

> **Destination** : `08_REALISATIONS.md` section 8.1, `09_SECURITE.md` (auth côté client), et `ANNEXES.md` Annexe E.

---

## 1. Authentification Côté Front

### 1.1. Déclenchement du Login Supabase

Le login est déclenché depuis le composant `App.tsx`. Lorsque l'utilisateur soumet le formulaire de connexion, la fonction `handleLogin` est appelée :

```typescript
// src/App.tsx (extrait)
const handleLogin = async (credentials: UserCredentials) => {
  setLoginLoading(true);
  setLoginError(null);

  try {
    const { SignInUser } = await import(
      '@/features/auth/use-cases/SignInUser'
    );
    const signInUseCase = new SignInUser(authRepository!);
    await signInUseCase.execute(credentials);

    // Login successful, modal will close automatically via auth state change
    setShowLoginModal(false);
  } catch (error) {
    setLoginError(error instanceof Error ? error.message : 'Login failed');
  } finally {
    setLoginLoading(false);
  }
};
```

Le use case `SignInUser` valide les entrées puis délègue au repository :

```typescript
// src/features/auth/use-cases/SignInUser.ts
export class SignInUser {
  private authRepository: AuthRepository;

  constructor(authRepository: AuthRepository) {
    this.authRepository = authRepository;
  }

  async execute(credentials: UserCredentials): Promise<AuthSession> {
    if (!credentials.email || !credentials.password) {
      throw new Error('Email and password are required');
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(credentials.email)) {
      throw new Error('Invalid email format');
    }

    try {
      return await this.authRepository.signIn(credentials);
    } catch (error) {
      if (error instanceof Error) {
        throw new Error(`Sign in failed: ${error.message}`);
      }
      throw new Error('Sign in failed: Unknown error');
    }
  }
}
```

L'implémentation concrète appelle le SDK Supabase :

```typescript
// src/features/auth/services/SupabaseAuthRepository.ts (extrait)
async signIn(credentials: UserCredentials): Promise<AuthSession> {
  const { data, error } = await this.supabase.auth.signInWithPassword({
    email: credentials.email,
    password: credentials.password,
  });

  if (error) {
    throw new Error(`Sign in failed: ${error.message}`);
  }

  if (!data.session || !data.user) {
    throw new Error('Sign in failed: No session data returned');
  }

  return this.mapSupabaseSessionToAuthSession(data.session, data.user);
}
```

---

### 1.2. Stockage du JWT

Le JWT est stocké à **deux niveaux** :

**Niveau 1 -- SDK Supabase (automatique)** : Le SDK `@supabase/supabase-js` persiste automatiquement la session (access_token + refresh_token) dans le `localStorage` du navigateur.

**Niveau 2 -- React Context (état applicatif)** : Le `AuthProvider` maintient la session dans le state React pour la rendre accessible à toute l'application.

```typescript
// src/features/auth/hooks/AuthProvider.tsx
export const AuthProvider: React.FC<AuthProviderProps> = ({
  children, authRepository,
}) => {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const getInitialSession = async () => {
      try {
        const currentSession = await authRepository.getCurrentSession();
        if (currentSession) {
          const now = new Date();
          if (currentSession.expiresAt && currentSession.expiresAt > now) {
            setSession(currentSession);
            setUser(currentSession.user);
          } else {
            // Session expirée, tentative de rafraîchissement
            try {
              const refreshedSession = await authRepository.refreshSession();
              setSession(refreshedSession);
              setUser(refreshedSession.user);
            } catch (refreshError) {
              setSession(null);
              setUser(null);
            }
          }
        }
      } catch (error) {
        if (error instanceof Error &&
            error.message.includes('session_not_found')) {
          setSession(null);
          setUser(null);
        }
      } finally {
        setLoading(false);
      }
    };

    getInitialSession();

    // Écoute en temps réel des changements d'état d'authentification
    const unsubscribe = authRepository.onAuthStateChange(newSession => {
      setSession(newSession);
      setUser(newSession?.user || null);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [authRepository]);

  const value: AuthContextType = {
    user, session, loading,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
```

Le type `AuthSession` qui encapsule le JWT :

```typescript
// src/features/auth/types/User.ts (extrait)
export interface AuthSession {
  user: User;
  accessToken: string;   // JWT Supabase
  refreshToken: string;
  expiresAt: Date;
}
```

---

### 1.3. Envoi du JWT dans les Headers des Requêtes API

Le token est transmis aux services HTTP via une **fonction callback `getToken`** injectée par le composant parent dans les providers :

```typescript
// src/App.tsx (extrait -- injection du token)
<EventsProvider getToken={() => session?.accessToken || null}>
  <ResourcesProvider getToken={() => session?.accessToken || null}>
    <Routes>
      {/* ... */}
    </Routes>
  </ResourcesProvider>
</EventsProvider>
```

Chaque repository HTTP reçoit cette fonction et l'utilise pour construire le header `Authorization` :

```typescript
// src/features/events/services/HttpEventRepository.ts (extrait)
export class HttpEventRepository implements EventRepository {
  private baseUrl: string;
  private getToken: () => string | null;

  constructor(
    getToken: () => string | null,
    baseUrl: string = import.meta.env.VITE_API_BASE_URL ||
      'https://happyrow-core.onrender.com/event/configuration/api/v1'
  ) {
    this.baseUrl = baseUrl;
    this.getToken = getToken;
  }

  // Chaque méthode vérifie le token avant l'appel :
  async createEvent(eventData: EventCreationRequest): Promise<Event> {
    const token = this.getToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    const response = await fetch(`${this.baseUrl}/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(apiRequest),
    });
    // ...
  }
}
```

Ce pattern est identique dans les 4 repositories HTTP :
- `HttpEventRepository`
- `HttpResourceRepository`
- `HttpContributionRepository`
- `HttpParticipantRepository`

---

### 1.4. Gestion de la Déconnexion

La déconnexion est déclenchée depuis le `UserProfilePage` ou le `AppHeader` :

```typescript
// src/features/user/views/UserProfilePage.tsx (extrait)
const handleSignOut = async () => {
  try {
    await authActions.signOut.execute();
  } catch (error) {
    console.error('Sign out failed:', error);
  }
};
```

Le use case `SignOutUser` délègue au repository :

```typescript
// src/features/auth/services/SupabaseAuthRepository.ts (extrait)
async signOut(): Promise<void> {
  const { error } = await this.supabase.auth.signOut();
  if (error) {
    throw new Error(error.message);
  }
}
```

Après la déconnexion, le listener `onAuthStateChange` du `AuthProvider` reçoit l'événement `SIGNED_OUT`, met `session` et `user` à `null`, ce qui déclenche automatiquement le rendu conditionnel vers la `WelcomeView`.

---

### 1.5. Protection des Routes (Routes Protégées)

La protection des routes est implémentée par **rendu conditionnel** dans le composant `AppContent` :

```typescript
// src/App.tsx (extrait -- protection des routes)
const AppContent: React.FC = () => {
  const { user, session, isAuthenticated } = useAuth();

  // Si non authentifié → afficher WelcomeView + modales auth
  if (!isAuthenticated) {
    return (
      <>
        <WelcomeView
          onCreateAccount={() => setShowRegisterModal(true)}
          onLogin={() => setShowLoginModal(true)}
        />
        {/* Modales LoginModal et RegisterModal */}
      </>
    );
  }

  // Si authentifié → afficher l'application avec les routes protégées
  return (
    <EventsProvider getToken={() => session?.accessToken || null}>
      <ResourcesProvider getToken={() => session?.accessToken || null}>
        <Routes>
          <Route path="/" element={<AppLayout user={user!} authRepository={authRepository!} />}>
            <Route index element={<HomePage user={user!} />} />
            <Route path="profile" element={<UserProfilePage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ResourcesProvider>
    </EventsProvider>
  );
};
```

Il n'y a pas de guard ou middleware dédié. La protection repose entièrement sur le flag `isAuthenticated` fourni par le hook `useAuth()`.

---

## 2. Appels API vers le Back-End

### 2.1. Fichier de Configuration API

```typescript
// src/core/config/api.ts
interface ApiConfig {
  baseUrl: string;
}

export const getApiConfig = (): ApiConfig => {
  const isProduction = import.meta.env.PROD;
  const envApiUrl = import.meta.env.VITE_API_BASE_URL;

  if (envApiUrl) {
    return { baseUrl: envApiUrl };
  }

  if (isProduction) {
    return { baseUrl: 'https://happyrow-core.onrender.com' };
  } else {
    return { baseUrl: '/api' };  // Proxy Vite en développement
  }
};

export const apiConfig = getApiConfig();
```

Configuration du proxy en développement :

```typescript
// vite.config.ts (extrait)
server: {
  proxy: {
    '/api': {
      target: 'https://happyrow-core.onrender.com',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api/, ''),
    },
  },
},
```

---

### 2.2. Exemple d'Appel POST -- Création d'Événement

```typescript
// src/features/events/services/HttpEventRepository.ts
async createEvent(eventData: EventCreationRequest): Promise<Event> {
  // Mapping frontend → backend (camelCase → snake_case)
  const apiRequest: EventApiRequest = {
    name: eventData.name,
    description: eventData.description,
    event_date: eventData.date,
    location: eventData.location,
    type: eventData.type,
  };

  const token = this.getToken();
  if (!token) {
    throw new Error('Authentication required');
  }

  const response = await fetch(`${this.baseUrl}/events`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(apiRequest),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(
      errorData.message || `HTTP error! status: ${response.status}`
    );
  }

  const eventResponse: EventApiResponse = await response.json();

  // Mapping backend → frontend (snake_case → camelCase)
  return {
    id: eventResponse.identifier,
    name: eventResponse.name,
    description: eventResponse.description,
    date: new Date(eventResponse.event_date),
    location: eventResponse.location,
    type: this.mapStringToEventType(eventResponse.type),
    organizerId: eventResponse.creator || eventData.organizerId,
  };
}
```

---

### 2.3. Exemple d'Appel GET -- Liste des Événements

```typescript
// src/features/events/services/HttpEventRepository.ts
async getEventsByOrganizer(organizerId: string): Promise<Event[]> {
  const token = this.getToken();
  if (!token) {
    throw new Error('Authentication required');
  }

  const response = await fetch(`${this.baseUrl}/events`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const eventsResponse: EventApiResponse[] = await response.json();
  return eventsResponse.map(event => ({
    id: event.identifier,
    name: event.name,
    description: event.description,
    date: new Date(event.event_date),
    location: event.location,
    type: this.mapStringToEventType(event.type),
    organizerId: event.creator || organizerId,
  }));
}
```

---

### 2.4. Gestion des Erreurs Côté Client

La gestion des erreurs est faite à **trois niveaux** :

**Niveau 1 -- Repository HTTP** : Vérification de `response.ok` et parsing du message d'erreur JSON :

```typescript
if (!response.ok) {
  const errorData = await response.json().catch(() => ({}));
  throw new Error(
    errorData.message || `HTTP error! status: ${response.status}`
  );
}
```

**Niveau 2 -- Use Case** : Encapsulation de l'erreur avec contexte métier :

```typescript
// src/features/events/use-cases/CreateEvent.ts
try {
  return await this.eventRepository.createEvent(eventRequest);
} catch (error) {
  throw new Error(
    `Failed to create event: ${error instanceof Error ? error.message : 'Unknown error'}`
  );
}
```

**Niveau 3 -- Provider/Context** : Mise à jour du state d'erreur React + rollback optimiste :

```typescript
// src/features/events/hooks/EventsProvider.tsx (extrait -- updateEvent avec rollback)
const updateEvent = useCallback(
  async (id: string, eventData: Partial<EventCreationRequest>): Promise<Event> => {
    setError(null);
    const previousEvents = [...events]; // Sauvegarde pour rollback

    try {
      const updatedEvent = await updateEventUseCase.execute({ id, ...eventData });
      setEvents(prev => prev.map(e => (e.id === id ? updatedEvent : e)));
      return updatedEvent;
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : 'Failed to update event';
      setError(errorMessage);
      setEvents(previousEvents); // Rollback
      throw err;
    }
  },
  [updateEventUseCase, events]
);
```

**Cas spécifiques gérés** :
- **401 Unauthorized** : Chaque repository vérifie la présence du token avant l'appel (`if (!token) throw new Error('Authentication required')`)
- **Session expirée (403)** : Le `SupabaseAuthRepository` gère les erreurs `session_not_found` et tente un `refreshSession()`
- **Erreurs réseau** : Capturées par le `catch` des `Promise` dans les use cases
- **409 Optimistic Lock** : Pas de gestion spécifique côté front -- l'erreur est propagée de manière générique via le message du serveur

**Note** : L'application n'utilise pas d'interceptor global (pas d'Axios). Chaque repository gère ses erreurs individuellement.
