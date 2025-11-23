# PhotoVault - Enterprise Photo Gallery Platform

A fully functional, production-ready enterprise photo gallery platform built with Spring Boot, Next.js, and PostgreSQL. Designed to handle 100,000+ concurrent users with distributed caching, message queues, and scalable architecture.

## 🎯 Features

### Core Functionality
- ✅ **Multi-tenant photographer accounts** with JWT authentication
- ✅ **Album management** with client sharing
- ✅ **Photo & Video upload** with automatic processing
- ✅ **Image optimization** with multiple sizes (thumbnail, preview, optimized)
- ✅ **Video transcoding** with H.264 codec, quality control (CRF), and automatic thumbnail generation
- ✅ **Public gallery** for clients to view and download media
- ✅ **Analytics tracking** (views, downloads)
- ✅ **Distributed caching** with Hazelcast
- ✅ **Async processing** with Apache Kafka
- ✅ **RESTful API** with Spring Boot
- ✅ **Modern UI** with Next.js and Tailwind CSS

### Technical Highlights
- Spring Boot 3.2 with Java 21
- PostgreSQL 16 with optimized schema
- Hazelcast IMDG for distributed caching
- Apache Kafka for event-driven asynchronous processing
- FFmpeg for video transcoding and thumbnail generation
- Next.js 14 for both admin and client frontends
- Docker Compose for local development
- Comprehensive unit tests

## 📁 Project Structure

```
gallary4j/
├── photovault-backend/          # Spring Boot API
│   ├── src/main/java/com/photovault/
│   │   ├── config/              # Configuration classes
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Data access
│   │   ├── entity/              # JPA entities
│   │   └── dto/                 # Data transfer objects
│   ├── src/main/resources/
│   │   └── application.yml      # Application configuration
│   └── pom.xml                  # Maven dependencies
│
├── photovault-admin/            # Next.js Admin Dashboard
│   ├── src/app/
│   │   ├── dashboard/           # Dashboard pages
│   │   ├── albums/              # Album management
│   │   └── register/            # Registration
│   └── package.json
│
├── photovault-gallery/          # Next.js Client Gallery
│   ├── src/app/
│   │   └── gallery/[slug]/      # Public gallery pages
│   └── package.json
│
├── docker-compose.yml           # Local development stack
├── init.sql                     # Database initialization
└── README.md                    # This file
```

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Node.js 18+
- Docker and Docker Compose
- Maven 3.8+

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL, Kafka, and Zookeeper
docker-compose up -d

# Verify services are running
docker-compose ps
```

This will start:
- PostgreSQL on `localhost:5432`
- Apache Kafka on `localhost:9092`
- Zookeeper on `localhost:2181`

### 2. Start the Backend API

```bash
cd photovault-backend

# Build the application
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

**Test the API:**
```bash
curl http://localhost:8080/api/v1/auth/health
# Should return: {"status":"ok"}
```

### 3. Start the Admin Dashboard

```bash
cd photovault-admin

# Install dependencies
npm install

# Start development server
npm run dev
```

Admin dashboard will be available at `http://localhost:3000`

### 4. Start the Client Gallery

```bash
cd photovault-gallery

# Install dependencies
npm install

# Start development server
npm run dev
```

Client gallery will be available at `http://localhost:3001`

## 📖 Usage Guide

### 1. Create an Account

1. Navigate to `http://localhost:3000/register`
2. Fill in:
   - Studio Name: "My Photography Studio"
   - Email: "photographer@example.com"
   - Password: "password123"
3. Click "Create Account"

### 2. Create an Album

1. After login, click "Create Album"
2. Fill in album details:
   - Title: "Wedding Photos"
   - Description: "John & Jane's Wedding"
   - Client Name: "John Doe"
   - Client Email: "john@example.com"
3. Click "Create Album"

### 3. Upload Photos

1. Click on your newly created album
2. Click "Upload Photos"
3. Select multiple photos from your computer
4. Wait for upload and processing to complete
5. Thumbnails will appear as photos are processed

### 4. Share with Clients

1. From the dashboard, click the "Share" button on an album
2. Copy the public gallery URL (e.g., `http://localhost:3001/gallery/wedding-photos-abc123`)
3. Send this URL to your client
4. Clients can view and download photos (if enabled)

## 🔧 Configuration

### Backend Configuration

Edit `photovault-backend/src/main/resources/application.yml`:

```yaml
# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/photovault
    username: photovault
    password: password

# Storage Configuration
storage:
  type: local  # Options: local, r2, s3

  # Local filesystem (development)
  local:
    path: ./uploads

  # Cloudflare R2 (production)
  r2:
    endpoint: https://your-account.r2.cloudflarestorage.com
    access-key: your-r2-access-key
    secret-key: your-r2-secret-key
    bucket: photovault-media

  # AWS S3 (for backups)
  s3:
    access-key: your-s3-access-key
    secret-key: your-s3-secret-key
    bucket: photovault-backups
    region: us-east-1

  # Daily backup configuration
  backup:
    enabled: true
    cron: "0 0 2 * * *"  # Daily at 2:00 AM
    retention-days: 30   # Keep backups for 30 days

  cdn:
    url: https://cdn.yourphoto vault.com

# JWT Security
security:
  jwt:
    secret: your-secret-key-change-in-production
    expiration: 86400000  # 24 hours
```

### Frontend Configuration

Admin Dashboard (`photovault-admin`):
- API endpoint: `http://localhost:8080`
- Runs on port: `3000`

Client Gallery (`photovault-gallery`):
- API endpoint: `http://localhost:8080`
- Runs on port: `3001`

## 🧪 Running Tests

### Backend Tests

```bash
cd photovault-backend

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AlbumServiceTest

# Run with coverage
./mvnw clean test jacoco:report
```

### Frontend Tests

```bash
cd photovault-admin
npm test

cd ../photovault-gallery
npm test
```

## 📊 Database Schema

The platform uses PostgreSQL with the following main tables:

- **photographers**: User accounts with subscription info
- **albums**: Photo collections with settings
- **media**: Individual photos/videos with metadata
- **selections**: Client favorites
- **upload_sessions**: Track multi-file uploads

See `init.sql` for complete schema.

## 🏗️ Architecture

### Backend Stack
- **Framework**: Spring Boot 3.2
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Cache**: Hazelcast IMDG
- **Message Streaming**: Apache Kafka
- **Security**: JWT + BCrypt
- **Storage**: Local filesystem (S3/R2 ready)
- **Video Processing**: FFmpeg

### Frontend Stack
- **Framework**: Next.js 14
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State**: React hooks
- **Images**: Next.js Image optimization

### Key Design Patterns
- Repository pattern for data access
- DTO pattern for API responses
- Service layer for business logic
- Async processing for heavy operations
- Distributed caching for performance

## 🔐 Security

- JWT-based authentication
- BCrypt password hashing
- CORS protection
- SQL injection prevention (JPA/Hibernate)
- XSS protection (sanitized inputs)
- Role-based access control

## 📈 Performance Optimizations

1. **Caching**: Hazelcast distributed cache for albums and media
2. **Async Processing**: Event-driven media processing with Apache Kafka
3. **Image Optimization**: Multiple sizes generated automatically (300px, 800px, 1920px)
4. **Video Transcoding**: H.264 encoding with configurable quality (CRF)
5. **Database Indexing**: Optimized queries with proper indexes
6. **Connection Pooling**: HikariCP for database connections

## ☁️ Cloud Storage Setup

### Cloudflare R2 Configuration

1. **Create R2 Bucket**
   ```bash
   # Login to Cloudflare dashboard
   # Navigate to R2 Object Storage
   # Create bucket: photovault-media
   ```

2. **Generate API Tokens**
   ```bash
   # In R2 dashboard, create API token with read/write permissions
   # Save the Access Key ID and Secret Access Key
   ```

3. **Configure Application**
   ```yaml
   # application.yml
   storage:
     type: r2
     r2:
       endpoint: https://your-account-id.r2.cloudflarestorage.com
       access-key: YOUR_R2_ACCESS_KEY
       secret-key: YOUR_R2_SECRET_KEY
       bucket: photovault-media
     cdn:
       url: https://pub-xxxx.r2.dev  # R2 public URL or custom domain
   ```

### AWS S3 Backup Configuration

1. **Create S3 Bucket**
   ```bash
   aws s3 mb s3://photovault-backups --region us-east-1
   ```

2. **Create IAM User for Backups**
   ```bash
   # Create IAM user with S3 write permissions
   # Attach policy: AmazonS3FullAccess (or custom policy)
   # Generate access key and secret key
   ```

3. **Configure Daily Backups**
   ```yaml
   # application.yml
   storage:
     s3:
       access-key: YOUR_S3_ACCESS_KEY
       secret-key: YOUR_S3_SECRET_KEY
       bucket: photovault-backups
       region: us-east-1
     backup:
       enabled: true
       cron: "0 0 2 * * *"  # Daily at 2:00 AM
       retention-days: 30
   ```

### Backup Monitoring

**Check backup status:**
```bash
# View application logs for backup results
tail -f photovault-backend/logs/application.log | grep "Backup"

# Expected output:
# ✅ Backup completed: 1,234 files, 15,678 MB, 45,123 ms
```

**List backups in S3:**
```bash
aws s3 ls s3://photovault-backups/backups/

# Example output:
# PRE 2024-01-15/
# PRE 2024-01-16/
# PRE 2024-01-17/
```

**Restore from backup:**
```bash
# Download specific backup
aws s3 sync s3://photovault-backups/backups/2024-01-15/ ./restore/

# Or use the S3 console to browse and restore files
```

### Storage Cost Optimization

**Cloudflare R2 Benefits:**
- Zero egress fees (free bandwidth)
- $0.015 per GB/month storage
- S3-compatible API
- Automatic global distribution

**Example costs at scale:**
```
1 TB storage on R2: $15/month
+ S3 backup (1 TB): $23/month
= $38/month total

vs. AWS S3 alone:
1 TB storage: $23/month
+ 5 TB egress: $450/month
= $473/month total

Savings: $435/month (92% reduction)
```

## 🚀 Production Deployment

### Environment Variables

Create `.env` file:

```bash
# Database
DB_HOST=your-postgres-host
DB_PORT=5432
DB_NAME=photovault
DB_USERNAME=photovault
DB_PASSWORD=strong-password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=your-kafka-host:9092

# JWT
JWT_SECRET=your-strong-secret-key-256-bits

# Cloudflare R2 Storage
STORAGE_TYPE=r2
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
R2_ACCESS_KEY=your-r2-access-key
R2_SECRET_KEY=your-r2-secret-key
R2_BUCKET=photovault-media
CDN_URL=https://pub-xxxx.r2.dev

# AWS S3 Backups
S3_ACCESS_KEY=your-s3-access-key
S3_SECRET_KEY=your-s3-secret-key
S3_BUCKET=photovault-backups
S3_REGION=us-east-1
BACKUP_ENABLED=true
BACKUP_CRON=0 0 2 * * *
BACKUP_RETENTION_DAYS=30
```

### Build for Production

```bash
# Backend
cd photovault-backend
./mvnw clean package -DskipTests
java -jar target/photovault-backend-1.0.0.jar

# Admin Frontend
cd photovault-admin
npm run build
npm start

# Gallery Frontend
cd photovault-gallery
npm run build
npm start
```

## 📝 API Documentation

### Authentication

**Register**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "photographer@example.com",
  "password": "password123",
  "studioName": "My Studio"
}
```

**Login**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "photographer@example.com",
  "password": "password123"
}
```

### Albums

**Create Album**
```http
POST /api/v1/albums
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Wedding Album",
  "description": "Beautiful wedding photos",
  "clientName": "John Doe",
  "clientEmail": "john@example.com"
}
```

**Get Albums**
```http
GET /api/v1/albums
Authorization: Bearer <token>
```

**Get Album by Slug (Public)**
```http
GET /api/v1/albums/slug/wedding-album-abc123
```

### Media

**Upload Photo**
```http
POST /api/v1/media/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <image-file>
albumId: <album-uuid>
```

**Get Album Media (Public)**
```http
GET /api/v1/media/album/<album-id>?page=0&size=50
```

## 🐛 Troubleshooting

### Database Connection Issues

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Upload Issues

1. Check upload directory permissions:
```bash
chmod 755 ./uploads
```

2. Check storage configuration in `application.yml`

3. Verify disk space:
```bash
df -h
```

### Media Processing Not Working

1. Check Kafka is running:
```bash
docker-compose ps kafka
```

2. View Kafka topics:
```bash
docker exec -it photovault-kafka kafka-topics --list --bootstrap-server localhost:9092
```

3. Monitor Kafka consumer lag:
```bash
docker exec -it photovault-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group photovault-consumer-group --describe
```

4. Check application logs for processing errors

## 📦 Technology Versions

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.2.0 |
| PostgreSQL | 16 |
| Hazelcast | 5.3.6 |
| Apache Kafka | 7.5.0 |
| Next.js | 14.0.4 |
| React | 18.2.0 |
| Node.js | 18+ |
| FFmpeg | Latest |

## 🤝 Contributing

This is a demonstration project implementing the enterprise architecture described in CLAUDE.md.

## 📄 License

See LICENSE file for details.

## 🎓 Learning Resources

This project demonstrates:
- Microservices architecture
- Event-driven design
- Distributed caching
- Async processing
- RESTful API design
- Modern React patterns
- Database optimization
- Security best practices

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review application logs
3. Check Docker container status
4. Verify configuration settings

## 🗺️ Roadmap

Implemented features:
- [x] Video processing support with H.264 transcoding
- [x] Cloudflare R2 cloud storage
- [x] Daily automated S3 backups

Future enhancements (as per CLAUDE.md):
- [ ] AI-powered face detection
- [ ] Auto-tagging with AI
- [ ] iOS app (Swift/SwiftUI)
- [ ] Android app (Kotlin)
- [ ] Advanced analytics dashboard
- [ ] Stripe payment integration
- [ ] Email notifications
- [ ] Watermarking support
- [ ] Kubernetes deployment configs

---

**Built with ❤️ following enterprise-grade architecture patterns**
