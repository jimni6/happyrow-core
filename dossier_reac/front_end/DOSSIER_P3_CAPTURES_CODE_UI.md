# Captures d'Écran et Code des Interfaces Utilisateur -- HappyRow Front-End

> **Destination** : `08_REALISATIONS.md` section 8.1 (captures d'écran + code interfaces) et `ANNEXES.md` Annexes A et B.

---

## 1. Page de Connexion (Login via Supabase)

### 1.1. Description de l'écran

L'écran de connexion se présente sous forme de **modale** superposée à la page WelcomeView. Elle contient :

- Un overlay semi-transparent couvrant tout l'écran
- Une carte modale blanche centrée avec animation d'apparition
- Un message d'accueil "Hello..." suivi du titre "Login"
- Un champ email avec placeholder "info@example.com"
- Un champ mot de passe avec icône de verrouillage (toggle visibilité)
- Un bouton "Login" pleine largeur (couleur teal `#5FBDB4`)
- Un lien "Don't have account? Register" pour basculer vers l'inscription
- Un bouton de fermeture (X) en haut à droite
- Animation de fermeture fluide (400ms)

### 1.2. Code du composant principal

```typescript
// src/features/auth/components/LoginModal.tsx
import React, { useState } from 'react';
import type { UserCredentials } from '../types/User';
import './LoginModal.css';

interface LoginModalProps {
  onClose: () => void;
  onSwitchToRegister: () => void;
  onSubmit: (credentials: UserCredentials) => void;
  loading: boolean;
  error: string | null;
}

export const LoginModal: React.FC<LoginModalProps> = ({
  onClose,
  onSwitchToRegister,
  onSubmit,
  loading,
  error,
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [errors, setErrors] = useState<{
    email?: string;
    password?: string;
  }>({});

  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};
    if (!email) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Invalid email format';
    }
    if (!password) {
      newErrors.password = 'Password is required';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit({ email, password });
    }
  };

  const handleClose = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setIsClosing(true);
    setTimeout(() => { onClose(); }, 400);
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) handleClose();
  };

  return (
    <div
      className={`auth-modal-overlay ${isClosing ? 'closing' : ''}`}
      onClick={handleBackdropClick}
    >
      <div
        className={`auth-modal-content ${isClosing ? 'closing' : ''}`}
        onClick={e => e.stopPropagation()}
      >
        <button type="button" className="auth-modal-close"
          onClick={handleClose} disabled={loading} aria-label="Close">✕</button>

        <div className="auth-modal-header">
          <p className="auth-greeting">Hello...</p>
          <h2 className="auth-title">Login</h2>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {error && <div className="auth-error">{error}</div>}

          <div className="auth-form-group">
            <label htmlFor="email" className="auth-label">email</label>
            <input id="email" type="email"
              className={`auth-input ${errors.email ? 'error' : ''}`}
              placeholder="info@example.com"
              value={email} onChange={e => setEmail(e.target.value)}
              disabled={loading} />
            {errors.email && <span className="auth-field-error">{errors.email}</span>}
          </div>

          <div className="auth-form-group">
            <div className="auth-input-wrapper">
              <input id="password"
                type={showPassword ? 'text' : 'password'}
                className={`auth-input ${errors.password ? 'error' : ''}`}
                placeholder="password"
                value={password} onChange={e => setPassword(e.target.value)}
                disabled={loading} />
              <button type="button" className="auth-password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                aria-label="Toggle password visibility">🔒</button>
            </div>
            {errors.password && <span className="auth-field-error">{errors.password}</span>}
          </div>

          <button type="submit" className="auth-submit-btn" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>

          <div className="auth-switch-link">
            <span>Don't have account? </span>
            <button type="button" className="auth-link-btn"
              onClick={onSwitchToRegister} disabled={loading}>Register</button>
          </div>
        </form>
      </div>
    </div>
  );
};
```

### 1.3. Code du service d'authentification (appel API)

```typescript
// src/features/auth/services/SupabaseAuthRepository.ts (extrait -- méthode signIn)
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

## 2. Dashboard / Liste des Événements

### 2.1. Description de l'écran

Le dashboard est l'écran principal après connexion. Il affiche :

- Une liste verticale de cartes d'événements (`EventCard`)
- Chaque carte montre : la date (mois, jour, heure), le nom de l'événement, le nombre de participants, la localisation
- Couleur alternée des cartes (coral / teal) pour distinguer visuellement les événements
- Boutons d'action rapide en bas de chaque carte : message, ressources, ajouter participant
- Un message encourageant si aucun événement n'existe
- Un indicateur de chargement pendant le fetch initial
- La barre de navigation bottom (`AppNavbar`) avec les icônes Home, Profile et le bouton "+" pour créer un événement

### 2.2. Code du composant principal

```typescript
// src/features/home/views/HomePage.tsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '@/features/auth';
import { useEvents } from '@/features/events';
import type { Event } from '@/features/events';
import { EventCard } from '../components/EventCard';
import { EventDetailsView } from '@/features/events';
import { GetParticipants } from '@/features/participants';
import { HttpParticipantRepository } from '@/features/participants';
import { AddParticipant } from '@/features/participants';
import { ParticipantStatus } from '@/features/participants';
import { AddParticipantModal } from '@/features/participants';
import './HomeView.css';

interface HomePageProps {
  user: { id: string };
}

export const HomePage: React.FC<HomePageProps> = ({ user }) => {
  const { session } = useAuth();
  const { events, loading } = useEvents();
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);
  const [participantCounts, setParticipantCounts] = useState<
    Record<string, number>
  >({});
  const [addParticipantEventId, setAddParticipantEventId] = useState<
    string | null
  >(null);

  const { loadEvents } = useEvents();

  useEffect(() => {
    loadEvents(user.id);
  }, [user.id, loadEvents]);

  useEffect(() => {
    const loadParticipantCounts = async () => {
      if (events.length === 0) return;
      const participantRepository = new HttpParticipantRepository(
        () => session?.accessToken || null
      );
      const getParticipantsUseCase = new GetParticipants(participantRepository);
      const counts: Record<string, number> = {};
      await Promise.all(
        events.map(async event => {
          try {
            const participants = await getParticipantsUseCase.execute({
              eventId: event.id,
            });
            counts[event.id] = participants.length;
          } catch (error) {
            counts[event.id] = 0;
          }
        })
      );
      setParticipantCounts(counts);
    };
    loadParticipantCounts();
  }, [events, session]);

  if (selectedEvent) {
    return (
      <EventDetailsView
        event={selectedEvent}
        onBack={() => setSelectedEvent(null)}
        onEventUpdated={(updatedEvent) => setSelectedEvent(updatedEvent)}
        onEventDeleted={() => setSelectedEvent(null)}
      />
    );
  }

  return (
    <div className="home-screen">
      <div className="home-content">
        {loading ? (
          <div className="loading-events">Loading events...</div>
        ) : events.length === 0 ? (
          <div className="no-events">
            <p>You haven't created any events yet.</p>
            <p>Click the "+" button below to get started!</p>
          </div>
        ) : (
          <div className="events-list">
            {events.map((event, index) => (
              <EventCard
                key={event.id || `event-${index}`}
                event={event}
                participantCount={participantCounts[event.id] || 0}
                onClick={() => setSelectedEvent(event)}
                showToggle={true}
                onAddParticipant={eventId => setAddParticipantEventId(eventId)}
              />
            ))}
          </div>
        )}
      </div>
      {addParticipantEventId && (
        <AddParticipantModal
          isOpen={!!addParticipantEventId}
          onClose={() => setAddParticipantEventId(null)}
          onSubmit={async (email) => { /* ... appel API ... */ }}
        />
      )}
    </div>
  );
};
```

### 2.3. Code du service API (liste des événements)

```typescript
// src/features/events/services/HttpEventRepository.ts (extrait -- méthode getEventsByOrganizer)
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

## 3. Page de Détail d'un Événement

### 3.1. Description de l'écran

Cet écran remplace le contenu du dashboard quand l'utilisateur clique sur une carte d'événement. Il affiche :

- Un header avec bouton retour (←), le nom de l'événement, le nombre de participants et la localisation
- Un bouton "Edit" (visible uniquement pour l'organisateur)
- Deux sections de ressources : **Food** et **Drinks**, chacune avec :
  - La liste des ressources existantes avec quantité actuelle / suggérée
  - Pour chaque ressource : boutons +/- pour sélectionner une quantité, bouton "Validate" pour confirmer la contribution
  - L'indication de la contribution personnelle de l'utilisateur
  - Un formulaire inline "Add new +" pour ajouter une nouvelle ressource
- Un bouton "Delete Event" en bas (visible uniquement pour l'organisateur)

### 3.2. Code du composant principal

```typescript
// src/features/events/views/EventDetailsView.tsx
import React, { useEffect, useMemo } from 'react';
import { useAuth } from '@/features/auth';
import { useResources, ResourceCategory } from '@/features/resources';
import type { Event } from '../types/Event';
import { Modal } from '@/shared/components/Modal';
import { UpdateEventForm } from '../components/UpdateEventForm';
import { ConfirmDeleteModal } from '../components/ConfirmDeleteModal';
import { ResourceCategorySection } from '../components/ResourceCategorySection';
import { useParticipants } from '../hooks/useParticipants';
import { useEventActions } from '../hooks/useEventActions';
import './EventDetailsView.css';

interface EventDetailsViewProps {
  event: Event;
  onBack: () => void;
  onEventUpdated?: (updatedEvent: Event) => void;
  onEventDeleted?: () => void;
}

export const EventDetailsView: React.FC<EventDetailsViewProps> = ({
  event, onBack, onEventUpdated, onEventDeleted,
}) => {
  const { user, session } = useAuth();
  const {
    resources, loading, error: resourceError,
    loadResources, addResource,
    addContribution, updateContribution, deleteContribution,
  } = useResources();

  const { participants } = useParticipants({ eventId: event.id, session });
  const {
    currentEvent, syncEvent, error, clearError,
    isEditModalOpen, setIsEditModalOpen, isUpdating,
    isDeleteModalOpen, setIsDeleteModalOpen, isDeleting,
    handleUpdateEvent, handleDeleteEvent,
  } = useEventActions({ event, user, onEventUpdated, onEventDeleted });

  useEffect(() => {
    syncEvent(event);
    loadResources(event.id);
  }, [event, event.id, loadResources, syncEvent]);

  const isOrganizer =
    user?.id === currentEvent.organizerId ||
    user?.email === currentEvent.organizerId;

  const resourcesByCategory = useMemo(() => ({
    FOOD: resources.filter(r => r.category === 'FOOD'),
    DRINK: resources.filter(r => r.category === 'DRINK'),
  }), [resources]);

  return (
    <div className="event-details-view">
      <div className="event-header">
        <button className="back-button" onClick={onBack}>←</button>
        <div className="event-header-info">
          <h1 className="event-name">{currentEvent.name}</h1>
          <div className="event-meta">
            <span>👥 {participants.length} participants</span>
            <span>📍 {currentEvent.location}</span>
          </div>
        </div>
        {isOrganizer && (
          <button className="edit-button-header"
            onClick={() => setIsEditModalOpen(true)}>Edit</button>
        )}
      </div>

      {loading ? (
        <div className="loading-state">Loading resources...</div>
      ) : (
        <div className="categories-container">
          <ResourceCategorySection
            title="Food" category={ResourceCategory.FOOD}
            resources={resourcesByCategory.FOOD}
            currentUserId={user?.email || ''}
            onAddContribution={/* ... */} onUpdateContribution={/* ... */}
            onDeleteContribution={/* ... */} onAddResource={/* ... */}
          />
          <ResourceCategorySection
            title="Drinks" category={ResourceCategory.DRINK}
            resources={resourcesByCategory.DRINK}
            currentUserId={user?.email || ''}
            onAddContribution={/* ... */} onUpdateContribution={/* ... */}
            onDeleteContribution={/* ... */} onAddResource={/* ... */}
          />
        </div>
      )}

      {isOrganizer && (
        <button className="delete-event-button"
          onClick={() => setIsDeleteModalOpen(true)}>Delete Event</button>
      )}

      <Modal isOpen={isEditModalOpen}
        onClose={() => { setIsEditModalOpen(false); clearError(); }}
        title="Edit Event" size="medium">
        <UpdateEventForm event={currentEvent}
          onSubmit={handleUpdateEvent}
          onCancel={() => { setIsEditModalOpen(false); clearError(); }}
          isLoading={isUpdating} />
      </Modal>

      <ConfirmDeleteModal isOpen={isDeleteModalOpen}
        eventName={currentEvent.name}
        onConfirm={handleDeleteEvent}
        onCancel={() => { setIsDeleteModalOpen(false); clearError(); }}
        loading={isDeleting} />
    </div>
  );
};
```

### 3.3. Code du service API (ressources)

```typescript
// src/features/resources/services/HttpResourceRepository.ts (extrait -- getResourcesByEvent)
async getResourcesByEvent(eventId: string): Promise<Resource[]> {
  const token = this.getToken();
  if (!token) {
    throw new Error('Authentication required');
  }

  const response = await fetch(
    `${this.baseUrl}/events/${eventId}/resources`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const resourcesResponse: ResourceApiResponse[] = await response.json();
  return resourcesResponse.map(r => this.mapApiResponseToResource(r));
}
```

---

## 4. Formulaire de Création d'Événement

### 4.1. Description de l'écran

Le formulaire de création d'événement s'affiche dans une **modale** déclenchée par le bouton "+" de la navbar. Il contient :

- Champ "Event Name" (texte, min 3 caractères)
- Champ "Description" (textarea, min 3 caractères)
- Ligne date/heure : input date + input time (pré-remplis avec maintenant + 1h)
- Champ "Location" (texte, min 3 caractères)
- Sélecteur "Event Type" (dropdown : Party, Birthday, Diner, Snack)
- Bouton "Cancel" et bouton "Create Event" avec état de chargement
- Validation côté client avec messages d'erreur sous chaque champ
- Vérification que la date est dans le futur

### 4.2. Code du composant principal

```typescript
// src/features/events/components/CreateEventForm.tsx
import React, { useState } from 'react';
import { EventType } from '../types/Event';
import './CreateEventForm.css';

interface CreateEventFormProps {
  onSubmit: (eventData: {
    name: string; description: string; date: Date;
    location: string; type: EventType;
  }) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

const getDefaultDateTime = () => {
  const now = new Date();
  now.setHours(now.getHours() + 1);
  const dateStr = now.toISOString().split('T')[0];
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  return { date: dateStr, time: `${hours}:${minutes}` };
};

export const CreateEventForm: React.FC<CreateEventFormProps> = ({
  onSubmit, onCancel, isLoading = false,
}) => {
  const defaultDateTime = getDefaultDateTime();
  const [formData, setFormData] = useState({
    name: '', description: '',
    date: defaultDateTime.date, time: defaultDateTime.time,
    location: '', type: '' as EventType | '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim() || formData.name.trim().length < 3)
      newErrors.name = 'Event name must be at least 3 characters long';
    if (!formData.description.trim() || formData.description.trim().length < 3)
      newErrors.description = 'Description must be at least 3 characters long';
    if (!formData.location.trim() || formData.location.trim().length < 3)
      newErrors.location = 'Location must be at least 3 characters long';
    if (!formData.type.trim())
      newErrors.type = 'Please select an event type';
    if (!formData.date) newErrors.date = 'Event date is required';
    if (!formData.time) newErrors.time = 'Event time is required';
    if (formData.date && formData.time) {
      const selectedDateTime = new Date(`${formData.date}T${formData.time}`);
      if (selectedDateTime <= new Date())
        newErrors.date = 'Event date and time must be in the future';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    const combinedDateTime = new Date(`${formData.date}T${formData.time}`);
    await onSubmit({
      name: formData.name.trim(), description: formData.description.trim(),
      date: combinedDateTime, location: formData.location.trim(),
      type: formData.type as EventType,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="create-event-form">
      {/* Champs : name, description, date, time, location, type */}
      {/* Boutons : Cancel + Create Event */}
    </form>
  );
};
```

### 4.3. Code du service API (création d'événement)

```typescript
// src/features/events/services/HttpEventRepository.ts (extrait -- createEvent)
async createEvent(eventData: EventCreationRequest): Promise<Event> {
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

## 5. Ajout de Contribution à une Ressource

### 5.1. Description de l'écran

La contribution se fait **inline** dans la page de détail d'un événement, directement sur chaque ressource. Pour chaque ressource affichée :

- Le nom de la ressource et l'indication de la contribution personnelle ("Your contribution: X")
- La quantité actuelle par rapport à la quantité suggérée (ex: "3/5")
- Boutons "−" et "+" pour ajuster la quantité à contribuer
- Affichage dynamique de la sélection en cours (ex: "+2" en vert, "-1" en rouge)
- Bouton "Validate" qui apparaît uniquement quand une sélection est en cours
- État de chargement "Saving..." pendant l'appel API
- Logique intelligente : si l'utilisateur n'a pas encore contribué → `addContribution`, s'il met à jour → `updateContribution`, si la quantité tombe à 0 → `deleteContribution`

### 5.2. Code du composant principal

```typescript
// src/features/resources/components/ResourceItem.tsx
import React, { useState } from 'react';
import type { Resource } from '../types/Resource';
import './ResourceItem.css';

interface ResourceItemProps {
  resource: Resource;
  currentUserId: string;
  onAddContribution: (resourceId: string, quantity: number) => Promise<void>;
  onUpdateContribution: (resourceId: string, quantity: number) => Promise<void>;
  onDeleteContribution: (resourceId: string) => Promise<void>;
}

export const ResourceItem: React.FC<ResourceItemProps> = ({
  resource, currentUserId,
  onAddContribution, onUpdateContribution, onDeleteContribution,
}) => {
  const [selectedQuantity, setSelectedQuantity] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  const userContribution = resource.contributors.find(
    c => c.userId === currentUserId
  );
  const userQuantity = userContribution?.quantity || 0;
  const hasSelection = selectedQuantity !== 0;

  const handleIncrement = () => setSelectedQuantity(prev => prev + 1);

  const handleDecrement = () => {
    if (selectedQuantity > 0) {
      setSelectedQuantity(prev => prev - 1);
    } else if (userQuantity > 0) {
      setSelectedQuantity(prev => prev - 1);
    }
  };

  const handleValidate = async () => {
    try {
      setIsSaving(true);
      const newQuantity = userQuantity + selectedQuantity;
      if (newQuantity <= 0) {
        await onDeleteContribution(resource.id);
      } else if (userQuantity === 0) {
        await onAddContribution(resource.id, selectedQuantity);
      } else {
        await onUpdateContribution(resource.id, newQuantity);
      }
      setSelectedQuantity(0);
    } catch (error) {
      console.error('Error updating contribution:', error);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="resource-item-card">
      <div className="resource-item-content">
        <div className="resource-item-header">
          <span className="resource-item-name">{resource.name}</span>
          {userQuantity > 0 && (
            <span className="resource-user-contribution">
              Your contribution: {userQuantity}
            </span>
          )}
        </div>
        <div className="resource-item-controls">
          <span className="resource-item-quantity">
            {resource.currentQuantity}
            {resource.suggestedQuantity && `/${resource.suggestedQuantity}`}
          </span>
          <div className="resource-item-buttons">
            <button className="resource-btn resource-btn-minus"
              onClick={handleDecrement}
              disabled={(selectedQuantity === 0 && userQuantity === 0) || isSaving}>−</button>
            {hasSelection && (
              <span className={`resource-selected-quantity ${selectedQuantity < 0 ? 'negative' : ''}`}>
                {selectedQuantity > 0 ? '+' : ''}{selectedQuantity}
              </span>
            )}
            <button className="resource-btn resource-btn-plus"
              onClick={handleIncrement} disabled={isSaving}>+</button>
          </div>
        </div>
      </div>
      {hasSelection && (
        <div className="resource-item-actions">
          <button className="resource-validate-btn"
            onClick={handleValidate} disabled={isSaving}>
            {isSaving ? 'Saving...' : 'Validate'}
          </button>
        </div>
      )}
    </div>
  );
};
```

### 5.3. Code du service API (contribution)

```typescript
// src/features/contributions/services/HttpContributionRepository.ts (extrait -- createContribution)
async createContribution(
  data: ContributionCreationRequest
): Promise<Contribution> {
  const apiRequest: ContributionApiRequest = {
    quantity: data.quantity,
  };

  const token = this.getToken();
  if (!token) {
    throw new Error('Authentication required');
  }

  const response = await fetch(
    `${this.baseUrl}/events/${data.eventId}/resources/${data.resourceId}/contributions`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(apiRequest),
    }
  );

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(
      errorData.message || `HTTP error! status: ${response.status}`
    );
  }

  const contributionResponse: ContributionApiResponse = await response.json();
  return this.mapApiResponseToContribution(contributionResponse, data.eventId);
}
```
