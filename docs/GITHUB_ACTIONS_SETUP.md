# GitHub Actions Setup for Render Deployment

This document provides a comprehensive guide to set up GitHub Actions for automated deployment of the HappyRow Core project to Render.

## Overview

We've created two GitHub Actions workflows:

1. **`deploy-render.yml`** - Simple deployment workflow focused on Render deployment
2. **`ci-cd.yml`** - Comprehensive CI/CD pipeline with security scanning, Docker builds, and multi-environment support

## Prerequisites

Before setting up the GitHub Actions pipeline, ensure you have:

1. A GitHub repository for your HappyRow Core project
2. A Render account with your service already created
3. Access to your Render API key and service ID

## Setup Instructions

### 1. Get Your Render Credentials

#### Render API Key
1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Navigate to **Account Settings** → **API Keys**
3. Create a new API key or use an existing one
4. Copy the API key (you'll need this for GitHub secrets)

#### Render Service ID
1. Go to your Render service dashboard
2. The Service ID is in the URL: `https://dashboard.render.com/web/srv-XXXXXXXXXXXXXXXXXX`
3. Copy the `srv-XXXXXXXXXXXXXXXXXX` part

### 2. Configure GitHub Secrets

In your GitHub repository, go to **Settings** → **Secrets and variables** → **Actions** and add the following secrets:

#### Required Secrets
- `RENDER_API_KEY`: Your Render API key
- `RENDER_SERVICE_ID`: Your Render service ID for production

#### Optional Secrets (for advanced CI/CD pipeline)
- `RENDER_STAGING_SERVICE_ID`: Service ID for staging environment (if you have one)

### 3. Configure GitHub Environments (Optional)

For the advanced CI/CD pipeline, you can set up environments:

1. Go to **Settings** → **Environments**
2. Create environments: `staging` and `production`
3. Add protection rules (e.g., require reviews for production deployments)

### 4. Workflow Configuration

#### Simple Deployment Workflow (`deploy-render.yml`)

This workflow:
- Runs tests on every push and PR
- Builds the application on main branch pushes
- Deploys to Render only on main branch pushes
- Provides deployment status notifications

**Triggers:**
- Push to `main` branch
- Pull requests to `main` branch
- Manual workflow dispatch

#### Advanced CI/CD Pipeline (`ci-cd.yml`)

This workflow includes:
- Comprehensive testing with coverage reports
- Docker image building and pushing to GitHub Container Registry
- Security vulnerability scanning with Trivy
- Multi-environment deployment (staging/production)
- Automated GitHub releases

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` branch
- Manual workflow dispatch

## Environment Variables

The workflows are configured to work with your existing `render.yaml` configuration. Make sure your Render service has the following environment variables configured:

```yaml
envVars:
  - key: PORT
    value: 8080
  - key: KTOR_ENV
    value: production
  - key: ENVIRONMENT
    value: production
  - key: DB_MAX_POOL_SIZE
    value: 10
  - key: DB_CONNECTION_TIMEOUT
    value: 30000
  - key: DB_IDLE_TIMEOUT
    value: 600000
  - key: DB_MAX_LIFETIME
    value: 1800000
  - key: DB_SSL_MODE
    value: require
```

## Security Considerations

Following the security memory from previous conversations, ensure that:

1. **Never commit sensitive data** to your repository
2. **Use GitHub Secrets** for all API keys and sensitive configuration
3. **Enable branch protection** on your main branch
4. **Review the security scan results** from the Trivy scanner in the advanced pipeline

## Deployment Process

### Automatic Deployment
1. Push code to the `main` branch
2. GitHub Actions will automatically:
   - Run tests
   - Build the application
   - Deploy to Render
   - Notify you of the deployment status

### Manual Deployment
1. Go to **Actions** tab in your GitHub repository
2. Select the workflow you want to run
3. Click **Run workflow**
4. Choose the branch and click **Run workflow**

## Monitoring and Troubleshooting

### Viewing Deployment Status
- Check the **Actions** tab in your GitHub repository
- Monitor your Render service logs in the Render dashboard
- Use the health check endpoint: `https://your-service.onrender.com/health`

### Common Issues

1. **Build Failures**
   - Check Java version compatibility (using JDK 21)
   - Verify Gradle wrapper permissions
   - Review test failures in the Actions logs

2. **Test Reporting Issues**
   - If you see "No test report files were found", this is normal for projects without tests yet
   - The workflows are designed to handle projects with no tests gracefully
   - Once you add tests, the reporting will work automatically

3. **Deployment Failures**
   - Verify Render API key and Service ID in GitHub secrets
   - Check Render service configuration
   - Ensure Docker build is successful

4. **Environment Issues**
   - Verify environment variables in Render dashboard
   - Check database connectivity
   - Review application logs in Render

## Workflow Features

### Code Quality Analysis (Detekt)
- Runs static code analysis with Detekt on every push and PR
- Generates multiple report formats (HTML, XML, TXT, SARIF, Markdown)
- Uploads SARIF reports to GitHub Security tab for vulnerability tracking
- Uses baseline file to avoid failing builds on existing code issues
- Enforces Kotlin coding standards and best practices
- Detects code smells, complexity issues, and potential bugs

### Testing
- Runs unit tests with `./gradlew test -PWithoutIntegrationTests`
- Intelligently detects if test files exist before attempting to upload reports
- Gracefully handles projects without tests (shows informative messages)
- Uploads test results and reports only when tests are present
- Caches Gradle dependencies for faster builds

### Building
- Uses JDK 21 with Temurin distribution
- Builds with commit SHA as version
- Creates build artifacts

### Docker Support (Advanced Pipeline)
- Builds multi-platform Docker images (linux/amd64, linux/arm64)
- Pushes to GitHub Container Registry
- Uses build cache for optimization

### Security
- Vulnerability scanning with Trivy
- SARIF report upload to GitHub Security tab
- Dependency caching with integrity checks

## Detekt Code Quality Analysis

### What is Detekt?
Detekt is a static code analysis tool for Kotlin that helps maintain code quality by detecting:
- Code smells and anti-patterns
- Complexity issues
- Formatting inconsistencies
- Potential bugs and security issues
- Unused imports and variables

### Local Usage
Run Detekt locally with these commands:

```bash
# Run code analysis
./gradlew detekt

# Generate baseline for existing code (recommended for new projects)
./gradlew detektBaseline

# Auto-fix formatting issues (where possible)
./gradlew detektFormat
```

### Configuration
- **Configuration file**: `detekt.yml` - Contains all Detekt rules and settings
- **Baseline file**: `detekt-baseline.xml` - Excludes existing issues from failing builds
- **Reports**: Generated in `build/reports/detekt/` with multiple formats

### GitHub Integration
- **Security Tab**: SARIF reports are uploaded to GitHub Security for issue tracking
- **Pull Request Reviews**: Detekt findings are visible in PR checks
- **Artifacts**: All report formats are uploaded as build artifacts

### Customizing Rules
Edit `detekt.yml` to:
- Enable/disable specific rules
- Adjust thresholds (complexity, line length, etc.)
- Add custom exclusions
- Configure rule-specific settings

## Next Steps

1. **Push your code** to GitHub to trigger the first workflow run
2. **Monitor the deployment** in both GitHub Actions and Render dashboard
3. **Review Detekt findings** in the GitHub Security tab and build artifacts
4. **Test your deployed application** using the health check endpoint
5. **Set up notifications** (optional) for deployment status via Slack, email, etc.

## Support

If you encounter issues:
1. Check the GitHub Actions logs for detailed error messages
2. Review Render service logs in the dashboard
3. Verify all secrets and environment variables are correctly configured
4. Ensure your `render.yaml` configuration matches your service setup

The workflows are designed to be robust and provide clear feedback on any issues that occur during the deployment process.
