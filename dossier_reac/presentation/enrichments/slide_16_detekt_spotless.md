# Slide 16 — Detekt et Spotless (extraits + explications)

> **Type** : EXISTANT — Extraits de `build.gradle.kts` (config Detekt et Spotless)

## Extrait 1 : Configuration Detekt (`build.gradle.kts`)

```kotlin
// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}
```

## Extrait 2 : Configuration Spotless (`build.gradle.kts`)

```kotlin
// Spotless configuration for auto-formatting
spotless {
    kotlin {
        ktlint().editorConfigOverride(mapOf(
            "ktlint_standard_property-naming" to "disabled",
            "ktlint_standard_value-argument-comment" to "disabled",
            "ktlint_standard_value-parameter-comment" to "disabled",
            "ktlint_standard_comment-spacing" to "disabled",
            "ktlint_standard_discouraged-comment-location" to "disabled"
        ))
        target("src/**/*.kt", "domain/src/**/*.kt", "infrastructure/src/**/*.kt")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

## Extrait 3 : Config Detekt cle (`detekt.yml`, extrait)

```yaml
build:
  maxIssues: 0            # Zero tolerance : aucun issue tolere
  excludeCorrectable: false

config:
  validation: true
  warningsAsErrors: false  # Les warnings ne bloquent pas le build
```

> `maxIssues: 0` signifie que le moindre probleme detecte fait echouer le pipeline.

## Tableau comparatif Detekt vs Spotless

| Aspect | Detekt | Spotless |
|--------|--------|----------|
| **Role** | Analyse statique (code smells, complexite) | Formatage automatique du code |
| **Moteur** | Regles Detekt personnalisables | ktlint (Kotlin linter) |
| **Execution** | `./gradlew detekt` | `./gradlew spotlessCheck` / `spotlessApply` |
| **Bloquant en CI** | Oui (`maxIssues: 0`) | Oui si `spotlessCheck` echoue |
| **Rapports** | HTML, XML, SARIF, TXT, Markdown | Diff du code non formate |
| **Baseline** | Fichier `detekt-baseline.xml` pour les issues herites | N/A |
| **Scope** | Tous les fichiers `.kt` des modules | `src/**`, `domain/src/**`, `infrastructure/src/**` |

## Ce qu'il faut dire (notes orales)

La qualite du code repose sur deux outils complementaires :

**Detekt** est l'outil d'analyse statique. Il detecte les code smells, les problemes de complexite cyclomatique, les fonctions trop longues, les classes trop couplees. La configuration `maxIssues: 0` signifie qu'on a une politique de zero tolerance : le moindre probleme detecte bloque le pipeline. J'ai aussi configure un fichier baseline pour gerer les eventuels faux positifs. Detekt genere 5 formats de rapports (HTML, XML, SARIF, TXT, Markdown) qui sont archivee comme artefacts dans GitHub Actions.

**Spotless** est l'outil de formatage automatique. Il utilise ktlint comme moteur pour garantir un style de code homogene sur l'ensemble du projet. Quelques regles ont ete desactivees (`property-naming`, `comment-spacing`) car elles entraient en conflit avec les conventions choisies. Spotless cible les 3 arborescences de sources : `src`, `domain/src` et `infrastructure/src`. Les deux operations `trimTrailingWhitespace` et `endWithNewline` completent le formatage.

Le point important : ces deux outils sont **complementaires**. Detekt cherche les problemes de qualite, Spotless assure l'homogeneite du formatage. Les deux sont bloquants en CI.
