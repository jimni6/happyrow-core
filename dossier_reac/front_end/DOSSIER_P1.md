# Dossier Technique Front-End -- HappyRow

## 1. Stack Technique

- **Framework** : React 19.1.1
- **Langage** : TypeScript 5.8.3 (mode strict)
- **Gestionnaire de paquets** : npm 10.9 (avec `package-lock.json`)
- **Outil de build** : Vite 7.1.2 (plugin `@vitejs/plugin-react` 5.0)
- **CSS** : CSS natif avec un système de Design Tokens (variables CSS custom), pas de framework CSS externe (Tailwind, Bootstrap, etc.)
- **Routing** : React Router DOM 7.13.0
- **Authentification** : Supabase JS SDK 2.39.3 (`@supabase/supabase-js`)
- **PWA** : `vite-plugin-pwa` 1.2.0 + Workbox 7.4.0
- **Linting** : ESLint 9.33 + `eslint-plugin-react-hooks` + `eslint-plugin-react-refresh` + `typescript-eslint`
- **Formatage** : Prettier 3.6.2
- **Git hooks** : Husky 9.1.7 + lint-staged 16.1.6
- **Sécurité** : `lockfile-lint` 4.14.1 + `@lavamoat/allow-scripts` 3.4.1
- **Conteneurisation** : Docker (multi-stage Dockerfile) + Docker Compose
- **Target ES** : ES2022
- **Runtime** : Node.js 23.1 (dev)

---

## 2. Structure des Dossiers

```
happyrow-front/
├── public/                        # Assets statiques (logos, icônes PWA)
├── design-figma/                  # Maquettes Figma de référence
├── tests/
│   ├── e2e/                       # Tests E2E Playwright
│   │   └── pwa.spec.ts
│   └── features/                  # Tests unitaires (miroir de src/features/)
│       └── auth/
│           ├── services/SupabaseAuthRepository.test.ts
│           ├── views/AuthView.test.tsx
│           ├── components/ForgotPasswordForm.test.tsx
│           └── hooks/AuthProvider.test.tsx
├── src/
│   ├── main.tsx                   # Point d'entrée
│   ├── App.tsx                    # Routage principal, ErrorBoundary
│   ├── core/
│   │   ├── config/
│   │   │   ├── api.ts             # Configuration URL API back-end
│   │   │   └── supabase.ts        # Configuration client Supabase
│   │   └── styles/
│   │       ├── index.css           # Import global des styles
│   │       ├── tokens/             # Design Tokens CSS
│   │       │   ├── colors.css
│   │       │   ├── typography.css
│   │       │   ├── spacing.css
│   │       │   ├── borders.css
│   │       │   ├── shadows.css
│   │       │   └── transitions.css
│   │       └── base/
│   │           ├── reset.css
│   │           ├── global.css
│   │           └── utilities.css
│   ├── features/                  # Modules fonctionnels (Clean Architecture)
│   │   ├── auth/                  # Authentification
│   │   │   ├── components/        # LoginModal, RegisterModal, ForgotPasswordForm...
│   │   │   ├── hooks/             # AuthProvider, AuthContext, useAuth, useAuthActions
│   │   │   ├── services/          # SupabaseAuthRepository, AuthServiceFactory
│   │   │   ├── use-cases/         # RegisterUser, SignInUser, SignOutUser, ResetPassword
│   │   │   └── types/             # User, AuthRepository, AuthSession
│   │   ├── events/                # Gestion des événements
│   │   │   ├── components/        # CreateEventForm, UpdateEventForm, ConfirmDeleteModal
│   │   │   ├── hooks/             # EventsProvider, EventsContext
│   │   │   ├── services/          # HttpEventRepository
│   │   │   ├── use-cases/         # CreateEvent, GetEventById, UpdateEvent, DeleteEvent
│   │   │   └── types/             # Event, EventRepository
│   │   ├── resources/             # Gestion des ressources d'événements
│   │   │   ├── components/        # AddResourceForm, ResourceItem, InlineAddResourceForm
│   │   │   ├── hooks/             # ResourcesProvider, useResources, useResourceOperations
│   │   │   ├── services/          # HttpResourceRepository
│   │   │   ├── use-cases/         # CreateResource, GetResources, UpdateResource, DeleteResource
│   │   │   └── types/             # Resource, ResourceRepository
│   │   ├── contributions/         # Contributions aux ressources
│   │   │   ├── services/          # HttpContributionRepository
│   │   │   ├── use-cases/         # AddContribution, GetContributions, UpdateContribution, DeleteContribution
│   │   │   └── types/             # Contribution, ContributionRepository
│   │   ├── participants/          # Gestion des participants
│   │   │   ├── components/        # AddParticipantForm, AddParticipantModal, ParticipantList
│   │   │   ├── services/          # HttpParticipantRepository
│   │   │   ├── use-cases/         # AddParticipant, GetParticipants, RemoveParticipant, UpdateParticipantStatus
│   │   │   └── types/             # Participant, ParticipantRepository
│   │   ├── home/                  # Page d'accueil (EventCard, HomeView)
│   │   ├── welcome/               # Page de bienvenue (non authentifié)
│   │   └── user/                  # Profil utilisateur
│   ├── layouts/                   # Layouts partagés
│   │   ├── AppLayout/             # Layout principal avec navigation
│   │   └── AppHeader/             # Header de l'application
│   └── shared/                    # Composants partagés
│       └── components/
│           ├── Modal/             # Composant Modal générique
│           └── AppNavbar/         # Barre de navigation bottom
├── Dockerfile                     # Build multi-stage (dev/prod)
├── docker-compose.yml
├── vite.config.ts
├── tsconfig.json
├── playwright.config.ts
├── vitest.setup.ts
├── eslint.config.js
└── package.json
```

---

## 3. Dépendances Principales

### Dépendances de production

| Dépendance | Version | Rôle |
|---|---|---|
| `react` | ^19.1.1 | Bibliothèque UI (dernières fonctionnalités React 19) |
| `react-dom` | ^19.1.1 | Rendu DOM pour React |
| `react-router-dom` | 7.13.0 | Routage SPA (client-side routing) |
| `@supabase/supabase-js` | ^2.39.3 | SDK Supabase pour l'authentification |

### Dépendances de développement notables

| Dépendance | Version | Rôle |
|---|---|---|
| `typescript` | ~5.8.3 | Typage statique |
| `vite` | ^7.1.2 | Bundler et serveur de développement |
| `@vitejs/plugin-react` | ^5.0.0 | Plugin React pour Vite (Fast Refresh) |
| `vite-plugin-pwa` | 1.2.0 | Génération du Service Worker PWA |
| `vitest` | ^3.2.4 | Framework de tests unitaires |
| `@testing-library/react` | ^16.3.0 | Utilitaires de test React |
| `@testing-library/jest-dom` | ^6.4.2 | Matchers DOM pour les assertions |
| `playwright` | 1.59.0-alpha | Tests End-to-End |
| `eslint` | ^9.33.0 | Linting du code |
| `prettier` | ^3.6.2 | Formatage automatique du code |
| `husky` | ^9.1.7 | Git hooks (pre-commit) |
| `lint-staged` | ^16.1.6 | Lint uniquement les fichiers staged |
| `jsdom` | ^27.0.0 | Environnement DOM simulé pour Vitest |
| `lockfile-lint` | 4.14.1 | Validation de sécurité du lockfile |

**Point notable** : Le projet n'utilise que **3 dépendances de production** (React, React Router, Supabase), ce qui démontre une approche minimaliste.

---

## 4. Gestion de l'Authentification Supabase

### Architecture

L'authentification suit une **Clean Architecture** avec inversion de dépendances :

```
[Composants UI] --> [useAuth hook] --> [AuthContext/AuthProvider]
                                            |
                                      [AuthRepository (interface)]
                                            |
                                      [SupabaseAuthRepository (implémentation)]
                                            |
                                      [Supabase JS SDK]
```

### 4.1. Initialisation du client Supabase

Le client Supabase est configuré dans `src/core/config/supabase.ts` via des variables d'environnement :

```typescript
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;
```

Une **Factory Singleton** (`AuthServiceFactory`) crée et gère l'instance unique du repository :

```typescript
static getAuthRepository(): AuthRepository {
  if (!this.instance) {
    this.instance = new SupabaseAuthRepository(
      supabaseConfig.url,
      supabaseConfig.anonKey
    );
  }
  return this.instance;
}
```

### 4.2. Login (Connexion)

La connexion est gérée par le use-case `SignInUser` qui :
1. Valide les inputs (email, mot de passe, format email)
2. Délègue au `SupabaseAuthRepository.signIn()` qui appelle `supabase.auth.signInWithPassword()`
3. Retourne un objet `AuthSession` contenant `user`, `accessToken`, `refreshToken` et `expiresAt`

```typescript
// SupabaseAuthRepository.ts
async signIn(credentials: UserCredentials): Promise<AuthSession> {
  const { data, error } = await this.supabase.auth.signInWithPassword({
    email: credentials.email,
    password: credentials.password,
  });
  // ... mapping vers le domaine AuthSession
  return {
    user: this.mapSupabaseUserToUser(data.user),
    accessToken: supabaseSession.access_token,
    refreshToken: supabaseSession.refresh_token,
    expiresAt: new Date(supabaseSession.expires_at * 1000),
  };
}
```

### 4.3. Stockage du JWT

Le SDK Supabase gère automatiquement le stockage du JWT dans le **localStorage** du navigateur. Côté application, le token est exposé via le **React Context** :

- `AuthProvider` maintient l'état `session: AuthSession | null` dans un `useState`
- Au démarrage, `AuthProvider` récupère la session existante via `getCurrentSession()` et vérifie l'expiration
- Si la session est expirée, il tente un `refreshSession()` automatiquement
- Un listener `onAuthStateChange()` met à jour le state React en temps réel lors de tout changement d'état d'authentification

```typescript
// AuthProvider.tsx
const [session, setSession] = useState<AuthSession | null>(null);

useEffect(() => {
  // Récupération de la session initiale
  const currentSession = await authRepository.getCurrentSession();
  if (currentSession?.expiresAt > now) {
    setSession(currentSession);
  } else {
    const refreshedSession = await authRepository.refreshSession();
    setSession(refreshedSession);
  }
  // Écoute des changements d'état
  const unsubscribe = authRepository.onAuthStateChange(newSession => {
    setSession(newSession);
  });
  return () => unsubscribe();
}, []);
```

### 4.4. Envoi du JWT dans les Headers

Le token est transmis aux repositories HTTP via une **fonction callback `getToken`** injectée par le composant parent :

```typescript
// App.tsx -- Injection du token dans les providers
<EventsProvider getToken={() => session?.accessToken || null}>
  <ResourcesProvider getToken={() => session?.accessToken || null}>
```

Chaque repository HTTP utilise cette fonction pour ajouter le header `Authorization: Bearer <token>` à chaque requête :

```typescript
// HttpEventRepository.ts
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
```

---

## 5. Appels API vers le Back-End

### 5.1. Client HTTP

Le projet utilise l'**API native `fetch`** du navigateur. Aucune bibliothèque tierce (Axios, ky, etc.) n'est utilisée.

### 5.2. Configuration des URL

La configuration de l'URL du back-end est centralisée dans `src/core/config/api.ts` :

- **Développement** : les requêtes passent par un proxy Vite (`/api` -> `https://happyrow-core.onrender.com`), configuré dans `vite.config.ts`
- **Production** : URL directe vers `https://happyrow-core.onrender.com`
- **Surcharge** : possible via la variable d'environnement `VITE_API_BASE_URL`

```typescript
// vite.config.ts -- Proxy en développement
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

### 5.3. Pattern Repository

Chaque domaine métier possède son propre **Repository HTTP** implémentant une interface :

| Repository | Interface | Endpoints |
|---|---|---|
| `HttpEventRepository` | `EventRepository` | `GET/POST/PUT/DELETE /events` |
| `HttpResourceRepository` | `ResourceRepository` | `GET/POST/PUT/DELETE /events/:id/resources` |
| `HttpContributionRepository` | `ContributionRepository` | `GET/POST/DELETE /events/:id/resources/:id/contributions` |
| `HttpParticipantRepository` | `ParticipantRepository` | `GET/POST/PUT/DELETE /events/:id/participants` |

Base URL des API métier : `https://happyrow-core.onrender.com/event/configuration/api/v1`

### 5.4. Mapping des données

Chaque repository contient des méthodes de **mapping** entre le format API (snake_case) et le format domaine (camelCase) :

```typescript
// Exemple : HttpResourceRepository.ts
// Backend: { identifier, event_id, current_quantity }
// Frontend: { id, eventId, currentQuantity }
private mapApiResponseToResource(response: ResourceApiResponse): Resource {
  return {
    id: response.identifier,
    eventId: response.event_id,
    name: response.name,
    category: this.mapStringToResourceCategory(response.category),
    currentQuantity: response.current_quantity || 0,
    // ...
  };
}
```

### 5.5. Orchestration via Use Cases

Les appels API ne sont jamais faits directement depuis les composants. Ils passent par des **use cases** (Clean Architecture) :

```
[Composant React] --> [Context/Provider] --> [Use Case] --> [Repository HTTP] --> [fetch API]
```

Chaque use case :
- Valide les données d'entrée
- Délègue au repository
- Gère les erreurs métier

---

## 6. Tests Front-End

### 6.1. Tests Unitaires -- Vitest

- **Framework** : Vitest 3.2.4 (compatible Vite, API similaire à Jest)
- **Environnement DOM** : jsdom 27.0
- **Utilitaires** : `@testing-library/react` 16.3 + `@testing-library/jest-dom` 6.4 + `@testing-library/user-event` 14.6
- **Configuration** : `vitest.setup.ts` importe `@testing-library/jest-dom` pour les matchers DOM
- **Commande** : `npm run test`

**Fichiers de tests existants** (4 fichiers) :

- `tests/features/auth/services/SupabaseAuthRepository.test.ts` -- Tests du repository d'authentification (register, signIn, signOut, getCurrentUser, resetPassword, onAuthStateChange) avec mock complet du SDK Supabase
- `tests/features/auth/views/AuthView.test.tsx` -- Tests de la vue d'authentification
- `tests/features/auth/components/ForgotPasswordForm.test.tsx` -- Tests du formulaire de mot de passe oublié
- `tests/features/auth/hooks/AuthProvider.test.tsx` -- Tests du provider d'authentification

Les tests utilisent les **mocks Vitest** (`vi.fn()`, `vi.mock()`) pour isoler les dépendances (SDK Supabase).

### 6.2. Tests End-to-End -- Playwright

- **Framework** : Playwright 1.59.0
- **Configuration** : `playwright.config.ts`
- **Navigateur** : Chromium (Desktop Chrome)
- **Commande** : `npm run test:e2e` ou `npm run test:e2e:ui` (mode interactif)
- **Serveur** : Lance automatiquement `npm run preview` avant les tests

**Fichier de test E2E existant** (1 fichier) :

- `tests/e2e/pwa.spec.ts` -- Tests des fonctionnalités PWA

---

*Ce document représente l'analyse technique complète du front-end HappyRow au 05/03/2026.*
