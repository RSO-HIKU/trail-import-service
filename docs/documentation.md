# Trail-Import Service - Technical Documentation

## Overview

The **Trail-Import Service** is a microservice responsible for importing geographic data into the HIKU hiking application. It processes GPX files to extract trail geometries and provides endpoints for bulk importing peaks and trails into the Peaks-Hikes Service database. The service handles coordinate conversion, WKT geometry generation, and validates spatial data before persistence. Access is restricted to administrators via JWT authentication.

## Table of Contents

1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Database Schema](#database-schema)
5. [Authentication & Authorization](#authentication--authorization)
7. [Configuration](#configuration)
9. [Deployment](#deployment)
10. [Local Development](#local-development)
11. [Error Handling](#error-handling)
12. [Troubleshooting](#troubleshooting)

---

## Architecture

### Key Components

- **Import Controllers**: Handle HTTP POST requests for GPX file and peak JSON imports (ImportGPX, ImportPeak)
- **Import Services**: Business logic for processing GPX files and peak data (GpxImportService, PeakImportService)
- **GPX Parser**: Extracts trail coordinates from GPX XML and converts to WKT LineString format
- **DAO Layer**: Direct database operations using JPA EntityManager for trail and peak insertion
- **Coordinate Utilities**: GpxToWkt utility for converting GPS coordinates to PostGIS-compatible geometry
- **Admin-Only Access**: All import endpoints require `@RolesAllowed("admin")` JWT authorization

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Runtime** | Java (Eclipse Temurin) | 17+ |
| **Build Tool** | Maven | 3.9 |
| **Framework** | KumuluzEE | 4.1.0 |
| **JPA Provider** | Hibernate | 5.6.15.Final |
| **Database** | PostgreSQL | 14+ |
| **Migration** | Flyway | 9.16.1 |
| **Authentication** | MicroProfile JWT | 2.1 |
| **Spatial Database** | PostGIS | 3.x |
| **GPX Processing** | Custom XML Parser | - |
| **Containerization** | Docker | - |
| **Orchestration** | Kubernetes (via Helm) | - |



## Database Schema

### Schema: `peaks_hikes_service`

**Note**: Trail-Import Service writes to the `peaks_hikes_service` schema shared with Peaks-Hikes Service. It does not own this schema but inserts data directly into `trails` and `peaks` tables. Database triggers in Peaks-Hikes Service automatically link imported trails and peaks based on spatial proximity.

#### Table: `trails`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Auto-increment trail ID |
| `name` | VARCHAR | NOT NULL | Trail name |
| `length_km` | FLOAT | - | Trail length in kilometers |
| `geometry` | GEOMETRY(LineString, 4326) | NOT NULL | Path geometry (WGS84) |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `source_file` | TEXT | - | Originating GPX/Geo source |

**Indexes**: GIST on `geometry` for spatial queries

#### Table: `peaks`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Auto-increment peak ID |
| `name` | VARCHAR | NOT NULL | Peak name |
| `territory` | VARCHAR | - | Region/territory |
| `latitude` | DOUBLE PRECISION | NOT NULL | Latitude (WGS84) |
| `longitude` | DOUBLE PRECISION | NOT NULL | Longitude (WGS84) |
| `elevation_m` | DOUBLE PRECISION | - | Elevation in meters |
| `geom` | GEOMETRY(Point, 4326) | GENERATED ALWAYS STORED | Point from `longitude`,`latitude` |

**Indexes**: GIST on `geom` for spatial queries

#### Table: `trails_peaks`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `trail_id` | INTEGER | NOT NULL, FK → `trails(id)` ON DELETE CASCADE | Linked trail |
| `peak_id` | INTEGER | NOT NULL, FK → `peaks(id)` ON DELETE CASCADE | Linked peak |

**Primary Key**: (`trail_id`, `peak_id`) — composite key preventing duplicates

**Functions**: `link_peak_to_nearby_trails(p_peak_id)` — links peaks to trails within 50 m (`ST_DWithin`).

**Triggers**: `trg_peak_insert` (after insert on `peaks` auto-links nearby trails), `trg_trail_insert` (after insert on `trails` auto-links nearby peaks).

These triggers implement robust connectivity between peaks and trails by relying solely on precise geospatial proximity (location data) rather than textual similarity of peak and trail names. This reduces manual effort needed to curate relationships, ensuring consistent, automatic linking whenever new peaks or trails are inserted.

### Database Migration

Database schema is managed using **Flyway** migrations located in `src/main/resources/db/migration/`.

Rules when working with migrations:
- Each migration has to follow the naming convention: VX__\<short name\>, where X is the next number that hasn't been used yet.
- Database migrations must be idempotent. You must not delete already existing and applied migrations.

Migrations run automatically via Kubernetes Job (see `helm/templates/migrate-job.yaml`).

---

## Configuration

### Application Configuration

Configuration file: `src/main/resources/config.yaml`

### JPA Configuration

Configuration file: `src/main/resources/META-INF/persistence.xml`





### Environment Variables

Configuration values can be set in `helm/templates/values-dev.yaml`:

| Variable | Description | Default |
|----------|-------------|---------|
| `KUMULUZEE_ENV_NAME` | Environment name | `dev` |
| `KUMULUZEE_SERVER_HTTP_PORT` | HTTP server port | `8085` |
| `KUMULUZEE_SERVER_HTTP_ADDRESS` | Bind address | `0.0.0.0` |
| `KUMULUZEE_DATASOURCES_DEFAULT_CONNECTIONURL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/hikudb` |
| `KUMULUZEE_DATASOURCES_DEFAULT_POOL_MAX_SIZE` | Connection pool size | `3` |
| `KUMULUZEE_JWT_AUTH_ISSUER` | JWT issuer URL | (required) |
| `KUMULUZEE_JWT_AUTH_JWKS_URI` | JWKS endpoint for JWT verification | (required) |

**Secrets** (local dev in `helm/templates/secret.yaml`, test/prod from Azure Key Vault):

| Secret | Description | Default |
|--------|-------------|---------|
| `KUMULUZEE_DATASOURCES_DEFAULT_USERNAME` | Database username | `hikuuser` |
| `KUMULUZEE_DATASOURCES_DEFAULT_PASSWORD` | Database password | `hikupassword` |
| `FLYWAY_USER` | Flyway Database role username | `hikuuser` |
| `FLYWAY_PASSWORD` | Flyway Database role password | `hikuadmin` |
| `PG_HOST` | Database hostname | `localhost` |

---

## Authentication & Authorization

### JWT-Based Authentication

The Trail-Import Service uses **MicroProfile JWT** with Keycloak as the identity provider. All REST endpoints require `@RolesAllowed("admin")` for administrative import operations. Standard users cannot access these endpoints.

## Deployment

#### Database Migrator Image

**Dockerfile**: `Dockerfile.migrator`

Runs Flyway migrations as a Kubernetes Job.

---

### Kubernetes (Helm)

#### Chart Structure

```
helm/
├── Chart.yaml              # Chart metadata
├── values-dev.yaml         # Development values
└── templates/
    ├── _helpers.tpl        # Template helpers
    ├── deployment.yaml     # Main application deployment
    ├── service-clusterip.yaml  # Internal service
    ├── service-nodeport.yaml   # External service (dev)
    ├── migrate-job.yaml    # Database migration job
    ├── secret.yaml         # Database credentials
    └── secretsproviderclass.yaml  # Azure Key Vault integration
```

---

### CI/CD Pipelines

#### Test Environment Pipeline

**File**: `.github/workflows/test-build-deploy.yaml`

**Triggers**:
- Push to `test` branch

**Steps**:
1. Checkout code
2. Build Docker images (app + migrator)
3. Push to Azure Container Registry (ACR)
4. Deploy to AKS test environment using ArgoCD

---

#### Production Promotion Pipeline

**File**: `.github/workflows/prod-promote.yaml`

**Triggers**:
- Manual workflow dispatch with image tag selection

**Steps**:
1. Pull images from test ACR
2. Retag images for production
3. Push to production ACR
4. Deploy to AKS production environment

Secrets used in the GitHub Actions workflows are saved as secrets in our GitHub Organization. Secrets used for deployment on the Azure cluster are provided by our Azure Key Vault.

---

## Local Development

Building images and deployment for local development is handled by Skaffold. By running the command **skaffold dev** in the root folder of the repository in a terminal window will make Skaffold automatically build and deploy the service to your local Minikube cluster. Skaffold watches your local files and when you save a change, Skaffold automatically applies it.  

### Prerequisites

#### Required Tools & Services
- **Java 17+**
- **Maven 3.9+**
- **PostgreSQL 14+**
- **Docker Desktop**
- **Keycloak**
- **RabbitMQ**
- **Minikube**
- **Skaffold**

#### Other requirements
- Docker Desktop is running,
- Minikube cluster is running on Docker Desktop,
- The database is deployed on your local cluster,
- The Traefik ingress controller is deployed on your local cluster,
- Keycloak is deployed on your local cluster.

### Steps performed by Skaffold
- Builds docker image for microservice,
- Builds docker image for database migrations,
- Deploys both images,
- Portforwards NodePort to the default port setting.

## Error Handling

### Common HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| `200 OK` | Request successful | POST importGPX, POST peak |
| `201 Created` | Resource created | - |
| `204 No Content` | Success, no response body | - |
| `400 Bad Request` | Invalid request data | Invalid GPX XML, missing coordinates |
| `401 Unauthorized` | Missing or invalid JWT | No Authorization header |
| `403 Forbidden` | Insufficient permissions | Non-admin user attempts import |
| `404 Not Found` | Resource not found | Peak/trail doesn't exist |
| `500 Internal Server Error` | Server error | Database connection failed |
| `503 Service Unavailable` | Service unhealthy | Health check failed |

### Exception Handling

The service uses JAX-RS exception handling:

- **Validation errors**: Return `400 Bad Request`
- **Resource not found**: Return `404 Not Found` (explicit in delete)
- **Database errors**: Return `500 Internal Server Error`
- **Authentication errors**: Return `401 Unauthorized`




## Contact

For questions or issues, contact the development team.


**Last Updated**: January 11, 2026  
**Version**: 0.1.0
