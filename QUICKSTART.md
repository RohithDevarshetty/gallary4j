# PhotoVault Quick Start Guide

Get the entire PhotoVault platform running in under 5 minutes!

## One-Time Setup

### 1. Start Infrastructure (1 minute)

```bash
# From project root
docker-compose up -d

# Wait for services to be ready (about 30 seconds)
docker-compose ps
```

You should see both PostgreSQL and RabbitMQ as "Up (healthy)"

### 2. Start Backend API (2 minutes)

```bash
cd photovault-backend

# First time only: Build the project
./mvnw clean install -DskipTests

# Start the backend
./mvnw spring-boot:run
```

Wait for the message: `Started PhotoVaultApplication`

Keep this terminal open!

### 3. Start Admin Dashboard (1 minute)

**Open a new terminal:**

```bash
cd photovault-admin

# First time only: Install dependencies
npm install

# Start the admin dashboard
npm run dev
```

Wait for: `Ready on http://localhost:3000`

Keep this terminal open!

### 4. Start Client Gallery (1 minute)

**Open another new terminal:**

```bash
cd photovault-gallery

# First time only: Install dependencies
npm install

# Start the gallery
npm run dev
```

Wait for: `Ready on http://localhost:3001`

Keep this terminal open!

## Test the Platform (2 minutes)

### Step 1: Create Account

1. Open browser to `http://localhost:3000/register`
2. Fill in:
   - Studio Name: "My Photo Studio"
   - Email: "demo@example.com"
   - Password: "demo123"
3. Click "Create Account"
4. You'll be redirected to the dashboard

### Step 2: Create Album

1. Click "Create Album"
2. Fill in:
   - Title: "Sample Album"
   - Description: "My first album"
   - Client Name: "Demo Client"
3. Click "Create Album"

### Step 3: Upload Photos

1. Click on your "Sample Album"
2. Click "Upload Photos"
3. Select 2-3 JPG images from your computer
4. Wait for processing (thumbnails will appear)
5. Photos are automatically resized to 3 sizes!

### Step 4: View Public Gallery

1. From dashboard, click "Share" on your album
2. Copy the gallery URL
3. Open the URL in a new tab (or incognito window)
4. You'll see the public gallery view
5. Click a photo to view full size
6. Try the "Download" button

## Success! 🎉

You now have a fully functional enterprise photo gallery platform running locally!

## What's Running?

- **Backend API**: http://localhost:8080
- **Admin Dashboard**: http://localhost:3000
- **Client Gallery**: http://localhost:3001
- **PostgreSQL**: localhost:5432
- **RabbitMQ**: localhost:5672
- **RabbitMQ Admin**: http://localhost:15672 (guest/guest)

## Stop Everything

```bash
# Stop the Java backend (Ctrl+C in backend terminal)
# Stop the admin dashboard (Ctrl+C in admin terminal)
# Stop the client gallery (Ctrl+C in gallery terminal)

# Stop Docker services
docker-compose down
```

## Restart Everything

```bash
# Start infrastructure
docker-compose up -d

# In separate terminals:
cd photovault-backend && ./mvnw spring-boot:run
cd photovault-admin && npm run dev
cd photovault-gallery && npm run dev
```

## Troubleshooting

### Port Already in Use

```bash
# Check what's using the ports
lsof -i :8080  # Backend
lsof -i :3000  # Admin
lsof -i :3001  # Gallery
lsof -i :5432  # PostgreSQL
lsof -i :5672  # RabbitMQ

# Kill the process using the port
kill -9 <PID>
```

### Database Connection Error

```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Upload Not Working

```bash
# Create upload directory
mkdir -p photovault-backend/uploads
chmod 755 photovault-backend/uploads
```

### Photos Not Processing

```bash
# Check RabbitMQ
docker-compose logs rabbitmq

# Restart backend
# Ctrl+C in backend terminal, then:
./mvnw spring-boot:run
```

## Next Steps

1. ✅ Read the full [README.md](README.md) for detailed documentation
2. ✅ Explore the API at http://localhost:8080/actuator/health
3. ✅ Check RabbitMQ queues at http://localhost:15672
4. ✅ Review the architecture in [CLAUDE.md](CLAUDE.md)
5. ✅ Run tests: `cd photovault-backend && ./mvnw test`

## Key Features to Try

- [ ] Create multiple albums
- [ ] Upload different image types (JPG, PNG)
- [ ] View auto-generated thumbnails
- [ ] Track analytics (view counts)
- [ ] Test the public gallery
- [ ] Download photos
- [ ] Create password-protected albums (via API)
- [ ] Soft delete albums

## API Testing

```bash
# Health check
curl http://localhost:8080/api/v1/auth/health

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"api@example.com","password":"test123","studioName":"API Test"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"demo123"}'
```

Enjoy exploring PhotoVault! 🚀
