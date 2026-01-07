# SLIDE 13 : DEVOPS ET DÉPLOIEMENT

## 🚀 Pipeline CI/CD Automatisé

```
GitHub Push → GitHub Actions → Render Deploy
```

### 📋 Workflow GitHub Actions

1. **Detekt** : Analyse statique du code
2. **Tests** : Exécution tests unitaires/intégration  
3. **Build** : Compilation avec Gradle
4. **Docker** : Construction de l'image
5. **Deploy** : Déploiement automatique sur Render

⏱️ **Durée totale** : ~5 minutes

### 🐳 Conteneurisation Docker

```dockerfile
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar

FROM openjdk:21-jre-slim
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
```

### ☁️ Hébergement Render

- **Déploiement automatique** depuis GitHub
- **PostgreSQL managé** inclus
- **SSL/TLS** automatique
- **Logs** centralisés
- **Monitoring** intégré

### ✅ Résultats

✅ Déploiement automatique à chaque push  
✅ Application accessible : happyrow-core.onrender.com  
✅ Infrastructure as Code (render.yaml)  
✅ Zero downtime deployment

### 🎓 Compétence CDA

**CDA-3.1** : Préparer le déploiement sécurisé
