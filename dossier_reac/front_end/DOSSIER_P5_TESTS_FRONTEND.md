# Tests Front-End -- HappyRow

> **Destination** : `10_PLAN_DE_TESTS.md` (tests front-end).

---

## 1. Tests Unitaires -- Vitest + Testing Library

### 1.1. Outils utilisés

| Outil | Version | Rôle |
|-------|---------|------|
| Vitest | ^3.2.4 | Framework de tests unitaires (compatible Vite, API similaire à Jest) |
| jsdom | ^27.0.0 | Environnement DOM simulé pour exécuter les tests sans navigateur |
| @testing-library/react | ^16.3.0 | Utilitaires pour tester les composants React de manière centrée utilisateur |
| @testing-library/jest-dom | ^6.4.2 | Matchers supplémentaires pour les assertions DOM (`toBeInTheDocument`, `toHaveTextContent`...) |
| @testing-library/user-event | ^14.6.1 | Simulation réaliste des interactions utilisateur (clics, saisie...) |

### 1.2. Configuration

```typescript
// vitest.setup.ts
import "@testing-library/jest-dom";
```

```typescript
// vite.config.ts (extrait -- configuration Vitest)
test: {
  globals: true,
  environment: 'jsdom',
  setupFiles: './vitest.setup.ts',
  exclude: [
    '**/node_modules/**',
    '**/dist/**',
    '**/e2e/**',
    '**/.{idea,git,cache,output,temp}/**',
    '**/{karma,rollup,webpack,vite,vitest,jest,ava,babel,nyc,cypress,tsup,build,playwright}.config.*',
  ],
},
```

**Commande** : `npm run test`

### 1.3. Fichiers de tests existants

| Fichier | Scope | Nombre de tests |
|---------|-------|-----------------|
| `tests/features/auth/services/SupabaseAuthRepository.test.ts` | Repository d'authentification Supabase | 13 tests |
| `tests/features/auth/hooks/AuthProvider.test.tsx` | Provider de contexte d'authentification | 10 tests |
| `tests/features/auth/views/AuthView.test.tsx` | Vue d'authentification (navigation entre formulaires) | 10 tests |
| `tests/features/auth/components/ForgotPasswordForm.test.tsx` | Formulaire de mot de passe oublié | 13 tests |

### 1.4. Exemple de test significatif -- SupabaseAuthRepository

Ce test est le plus représentatif car il teste le **repository d'authentification** qui constitue la couche d'accès au SDK Supabase. Il utilise des **mocks Vitest** pour isoler complètement le SDK Supabase et vérifie chaque opération d'authentification.

```typescript
// tests/features/auth/services/SupabaseAuthRepository.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SupabaseAuthRepository } from '@/features/auth';
import type { UserCredentials, UserRegistration } from '@/features/auth';

// Mock complet du client Supabase
const mockSupabaseClient = {
  auth: {
    signUp: vi.fn(),
    signInWithPassword: vi.fn(),
    signOut: vi.fn(),
    getUser: vi.fn(),
    getSession: vi.fn(),
    refreshSession: vi.fn(),
    resetPasswordForEmail: vi.fn(),
    updateUser: vi.fn(),
    onAuthStateChange: vi.fn(),
  },
};

vi.mock('@supabase/supabase-js', () => ({
  createClient: vi.fn(() => mockSupabaseClient),
}));

describe('SupabaseAuthRepository', () => {
  let repository: SupabaseAuthRepository;

  beforeEach(() => {
    vi.clearAllMocks();
    repository = new SupabaseAuthRepository('test-url', 'test-key');
  });

  describe('register', () => {
    it('should successfully register a user', async () => {
      const userData: UserRegistration = {
        email: 'test@example.com',
        password: 'password123',
        firstname: 'Test',
        lastname: 'User',
        metadata: { additionalInfo: 'test' },
      };

      const mockSupabaseUser = {
        id: 'user-id',
        email: 'test@example.com',
        email_confirmed_at: null,
        created_at: '2023-01-01T00:00:00.000Z',
        updated_at: '2023-01-01T00:00:00.000Z',
        user_metadata: {
          firstname: 'Test', lastname: 'User', additionalInfo: 'test',
        },
      };

      mockSupabaseClient.auth.signUp.mockResolvedValue({
        data: { user: mockSupabaseUser },
        error: null,
      });

      const result = await repository.register(userData);

      expect(mockSupabaseClient.auth.signUp).toHaveBeenCalledWith({
        email: userData.email,
        password: userData.password,
        options: {
          data: {
            firstname: userData.firstname,
            lastname: userData.lastname,
            ...userData.metadata,
          },
        },
      });

      expect(result).toEqual({
        id: 'user-id',
        email: 'test@example.com',
        emailConfirmed: false,
        firstname: 'Test',
        lastname: 'User',
        createdAt: new Date('2023-01-01T00:00:00.000Z'),
        updatedAt: new Date('2023-01-01T00:00:00.000Z'),
        metadata: { firstname: 'Test', lastname: 'User', additionalInfo: 'test' },
      });
    });

    it('should throw error when registration fails', async () => {
      mockSupabaseClient.auth.signUp.mockResolvedValue({
        data: { user: null },
        error: { message: 'Registration failed' },
      });

      await expect(repository.register({
        email: 'test@example.com', password: 'password123',
        firstname: 'Test', lastname: 'User',
      })).rejects.toThrow('Registration failed: Registration failed');
    });
  });

  describe('signIn', () => {
    it('should successfully sign in a user', async () => {
      const credentials: UserCredentials = {
        email: 'test@example.com',
        password: 'password123',
      };

      const mockSupabaseUser = {
        id: 'user-id', email: 'test@example.com',
        email_confirmed_at: '2023-01-01T00:00:00.000Z',
        created_at: '2023-01-01T00:00:00.000Z',
        updated_at: '2023-01-01T00:00:00.000Z',
        user_metadata: { firstname: 'Test', lastname: 'User' },
      };

      const mockSupabaseSession = {
        access_token: 'access-token',
        refresh_token: 'refresh-token',
        expires_at: Math.floor(Date.now() / 1000) + 3600,
        user: mockSupabaseUser,
      };

      mockSupabaseClient.auth.signInWithPassword.mockResolvedValue({
        data: { session: mockSupabaseSession, user: mockSupabaseUser },
        error: null,
      });

      const result = await repository.signIn(credentials);

      expect(result).toEqual({
        user: expect.objectContaining({
          id: 'user-id', email: 'test@example.com',
        }),
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        expiresAt: expect.any(Date),
      });
    });
  });

  describe('signOut', () => {
    it('should successfully sign out', async () => {
      mockSupabaseClient.auth.signOut.mockResolvedValue({ error: null });
      await expect(repository.signOut()).resolves.not.toThrow();
    });
  });

  describe('onAuthStateChange', () => {
    it('should set up auth state change listener', () => {
      const mockCallback = vi.fn();
      const mockUnsubscribe = vi.fn();
      mockSupabaseClient.auth.onAuthStateChange.mockReturnValue({
        data: { subscription: { unsubscribe: mockUnsubscribe } },
      });

      const unsubscribe = repository.onAuthStateChange(mockCallback);
      expect(mockSupabaseClient.auth.onAuthStateChange).toHaveBeenCalled();

      unsubscribe();
      expect(mockUnsubscribe).toHaveBeenCalled();
    });
  });
});
```

### 1.5. Exemple de test -- AuthProvider (test d'intégration composant)

```typescript
// tests/features/auth/hooks/AuthProvider.test.tsx (extrait)
describe('AuthProvider', () => {
  it('should provide authenticated state when session exists', async () => {
    const mockUser = createMockUser({ email: 'test@example.com' });
    const mockSession = createMockSession({ email: 'test@example.com' });

    mockAuthRepository.setMockSession(mockSession);
    mockAuthRepository.setMockUser(mockUser);

    render(
      <AuthProvider authRepository={mockAuthRepository}>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
    });

    expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    expect(screen.getByTestId('user-email')).toHaveTextContent('test@example.com');
    expect(screen.getByTestId('session-token')).toHaveTextContent('mock-access-token');
  });

  it('should handle complete authentication lifecycle', async () => {
    // 1. Initial loading state
    expect(screen.getByTestId('loading')).toBeInTheDocument();

    // 2. No session → unauthenticated
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });

    // 3. User signs in → state change callback fires
    authStateCallback!(signInSession);
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    // 4. User signs out → null session
    authStateCallback!(null);
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });
  });
});
```

---

## 2. Tests End-to-End -- Playwright

### 2.1. Outils utilisés

| Outil | Version | Rôle |
|-------|---------|------|
| Playwright | 1.59.0-alpha | Framework de tests E2E multi-navigateurs |
| @playwright/mcp | 0.0.62 | Intégration MCP pour Playwright |

### 2.2. Configuration

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run preview',
    url: 'http://localhost:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
```

**Commandes** :
- `npm run test:e2e` -- exécution headless
- `npm run test:e2e:ui` -- mode interactif avec UI Playwright
- `npm run test:e2e:headed` -- exécution avec navigateur visible
- `npm run playwright:codegen` -- génération de tests par enregistrement

### 2.3. Tests E2E existants -- PWA

```typescript
// tests/e2e/pwa.spec.ts
import { test, expect } from '@playwright/test';

test.describe('PWA Installation', () => {
  test('should have PWA manifest', async ({ page }) => {
    await page.goto('/');
    const manifestLink = page.locator('link[rel="manifest"]');
    await expect(manifestLink).toHaveCount(1);
    const themeColor = page.locator('meta[name="theme-color"]');
    await expect(themeColor).toHaveAttribute('content', '#5FBDB4');
  });

  test('should have Apple Touch icon', async ({ page }) => {
    await page.goto('/');
    const appleTouchIcon = page.locator('link[rel="apple-touch-icon"]');
    await expect(appleTouchIcon).toHaveAttribute('href', '/apple-touch-icon.png');
  });

  test('should have correct page title', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/HappyRow/);
  });

  test('should load and display the app', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: 'tests/e2e/screenshots/home.png' });
    const errors: string[] = [];
    page.on('pageerror', error => { errors.push(error.message); });
    await page.waitForTimeout(1000);
    expect(errors).toHaveLength(0);
  });

  test('should register service worker', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const swRegistration = await page.evaluate(() => {
      return navigator.serviceWorker.getRegistration();
    });
    expect(swRegistration).toBeTruthy();
  });

  test('should work offline (basic assets)', async ({ page, context }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Simuler le mode offline
    await context.setOffline(true);
    await page.reload();
    await expect(page).toHaveTitle(/HappyRow/);

    await context.setOffline(false);
  });
});
```

---

## 3. Synthèse

| Type de test | Framework | Nombre de fichiers | Scope |
|-------------|-----------|-------------------|-------|
| Tests unitaires | Vitest + Testing Library | 4 fichiers | Authentification (repository, provider, vue, composant) |
| Tests E2E | Playwright | 1 fichier | PWA (manifest, service worker, offline) |

Les tests se concentrent actuellement sur le **module d'authentification** (la fonctionnalité la plus critique) et sur les **capacités PWA**. Les modules métier (events, resources, contributions) ne disposent pas encore de tests automatisés.
