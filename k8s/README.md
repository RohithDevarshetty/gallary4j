# PhotoVault Kubernetes Deployment

This directory contains Kubernetes manifests for deploying PhotoVault API to a production Kubernetes cluster.

## Prerequisites

- Kubernetes cluster (v1.24+)
- `kubectl` CLI installed and configured
- `kustomize` (optional, for easier deployment)
- PostgreSQL database (external or in-cluster)
- Kafka cluster (external or in-cluster)
- Cloudflare R2 bucket configured
- AWS S3 bucket for backups

## Architecture

```
┌─────────────────────────────────────────┐
│          LoadBalancer Service           │
│          (photovault-api-service)       │
└──────────────┬──────────────────────────┘
               │
        ┌──────▼──────┐
        │   Ingress   │ (Optional)
        └──────┬──────┘
               │
     ┌─────────▼─────────┐
     │  Deployment        │
     │  (3-20 replicas)   │
     │  - photovault-api  │
     │  - Hazelcast cache │
     └─────────┬──────────┘
               │
     ┌─────────▼──────────┐
     │ Headless Service   │
     │ (hazelcast-service)│
     └────────────────────┘
```

## Quick Start

### 1. Create Namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Create Secrets

**IMPORTANT:** Never commit secrets to git. Create them manually:

```bash
kubectl create secret generic photovault-secrets \
  --from-literal=db.username=photovault \
  --from-literal=db.password=YOUR_DB_PASSWORD \
  --from-literal=jwt.secret=YOUR_JWT_SECRET_256_BIT \
  --from-literal=r2.endpoint=https://your-account.r2.cloudflarestorage.com \
  --from-literal=r2.access.key=YOUR_R2_ACCESS_KEY \
  --from-literal=r2.secret.key=YOUR_R2_SECRET_KEY \
  --from-literal=s3.access.key=YOUR_S3_ACCESS_KEY \
  --from-literal=s3.secret.key=YOUR_S3_SECRET_KEY \
  --namespace photovault
```

### 3. Update ConfigMap

Edit `configmap.yaml` with your environment-specific values:

```yaml
data:
  db.host: "your-postgres-host"
  kafka.bootstrap.servers: "your-kafka:9092"
  cdn.url: "https://cdn.yourphotovault.com"
```

Apply:
```bash
kubectl apply -f configmap.yaml
```

### 4. Deploy RBAC (for Hazelcast discovery)

```bash
kubectl apply -f rbac.yaml
```

### 5. Deploy Application

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml
```

Or use kustomize:
```bash
kubectl apply -k .
```

### 6. Verify Deployment

```bash
# Check pod status
kubectl get pods -n photovault

# Check logs
kubectl logs -f deployment/photovault-api -n photovault

# Check services
kubectl get svc -n photovault

# Check HPA
kubectl get hpa -n photovault
```

## Deployment Details

### Deployment Specification

- **Replicas:** 3 (minimum), auto-scales to 20 based on CPU/memory
- **Strategy:** RollingUpdate with zero downtime
- **Resources:**
  - Requests: 500m CPU, 1Gi memory
  - Limits: 2000m CPU, 2Gi memory
- **Probes:**
  - Liveness: `/actuator/health/liveness`
  - Readiness: `/actuator/health/readiness`

### Auto-scaling

HPA scales based on:
- **CPU:** 70% utilization
- **Memory:** 80% utilization
- **Scale Up:** Aggressive (100% increase every 30s, max 4 pods)
- **Scale Down:** Conservative (50% decrease after 5 min, max 2 pods)

### Hazelcast Clustering

Hazelcast automatically discovers cluster members using Kubernetes API:
- Service: `hazelcast-service` (headless)
- Port: 5701
- Discovery: Kubernetes API

### Resource Limits

Per pod:
```yaml
requests:
  memory: 1Gi
  cpu: 500m
limits:
  memory: 2Gi
  cpu: 2000m
```

### Storage

The application uses:
- **Primary:** Cloudflare R2 (configured via secrets)
- **Backup:** AWS S3 (daily backup at 2 AM)
- **Temporary:** EmptyDir volume for `/tmp`

## Monitoring

### Prometheus Metrics

Metrics exposed at: `http://pod-ip:8080/actuator/prometheus`

Annotations enable auto-discovery:
```yaml
prometheus.io/scrape: "true"
prometheus.io/port: "8080"
prometheus.io/path: "/actuator/prometheus"
```

### Health Checks

- **Liveness:** `GET /actuator/health/liveness`
- **Readiness:** `GET /actuator/health/readiness`
- **Info:** `GET /actuator/info`

## Updating the Deployment

### Update Image

```bash
kubectl set image deployment/photovault-api \
  photovault-api=photovault/api:v2.0.0 \
  -n photovault
```

### Rolling Restart

```bash
kubectl rollout restart deployment/photovault-api -n photovault
```

### Rollback

```bash
kubectl rollout undo deployment/photovault-api -n photovault
```

### Check Rollout Status

```bash
kubectl rollout status deployment/photovault-api -n photovault
```

## Configuration Management

### Update ConfigMap

```bash
kubectl edit configmap photovault-config -n photovault
# Then restart deployment
kubectl rollout restart deployment/photovault-api -n photovault
```

### Update Secrets

```bash
kubectl delete secret photovault-secrets -n photovault
kubectl create secret generic photovault-secrets \
  --from-literal=... \
  --namespace photovault
kubectl rollout restart deployment/photovault-api -n photovault
```

## Scaling

### Manual Scaling

```bash
kubectl scale deployment photovault-api --replicas=5 -n photovault
```

### View HPA Status

```bash
kubectl get hpa photovault-api-hpa -n photovault
kubectl describe hpa photovault-api-hpa -n photovault
```

## Troubleshooting

### View Logs

```bash
# All pods
kubectl logs -l app=photovault-api -n photovault

# Specific pod
kubectl logs photovault-api-xxxxx -n photovault

# Follow logs
kubectl logs -f deployment/photovault-api -n photovault

# Previous container logs
kubectl logs photovault-api-xxxxx --previous -n photovault
```

### Debug Pod

```bash
kubectl exec -it photovault-api-xxxxx -n photovault -- /bin/sh
```

### Check Events

```bash
kubectl get events -n photovault --sort-by='.lastTimestamp'
```

### Check Pod Status

```bash
kubectl describe pod photovault-api-xxxxx -n photovault
```

### Common Issues

#### Pod CrashLoopBackOff

```bash
# Check logs
kubectl logs photovault-api-xxxxx -n photovault

# Common causes:
# - Database connection failure
# - Missing secrets
# - Invalid configuration
```

#### ImagePullBackOff

```bash
# Check image name and tag
kubectl describe pod photovault-api-xxxxx -n photovault

# Verify image exists in registry
```

#### Hazelcast Not Clustering

```bash
# Check service
kubectl get svc hazelcast-service -n photovault

# Check RBAC permissions
kubectl get role photovault-api-role -n photovault
kubectl get rolebinding photovault-api-rolebinding -n photovault

# Check logs for Hazelcast
kubectl logs -f deployment/photovault-api -n photovault | grep Hazelcast
```

## Security

### Network Policies

TODO: Add network policies to restrict pod-to-pod communication

### Pod Security

- Runs as non-root user (UID 1000)
- Read-only root filesystem (future)
- No privilege escalation

### Secrets Management

Consider using:
- **AWS Secrets Manager** with External Secrets Operator
- **HashiCorp Vault** with Vault Secrets Operator
- **Sealed Secrets** for GitOps workflows

## Production Checklist

- [ ] Database configured and accessible
- [ ] Kafka cluster configured
- [ ] Secrets created securely
- [ ] ConfigMap updated with production values
- [ ] Resource limits appropriate for workload
- [ ] HPA tested under load
- [ ] Monitoring and alerts configured
- [ ] Backup verification (S3)
- [ ] Disaster recovery plan
- [ ] Network policies applied
- [ ] TLS/SSL configured for LoadBalancer
- [ ] DNS records pointing to LoadBalancer
- [ ] Log aggregation configured

## Performance Tuning

### JVM Options

Add to deployment env:
```yaml
- name: JAVA_OPTS
  value: "-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Connection Pool

Adjust in application.yml:
```yaml
spring.datasource.hikari.maximum-pool-size: 20
spring.datasource.hikari.minimum-idle: 5
```

## Cost Optimization

### Resource Requests

Monitor actual usage and adjust:
```bash
kubectl top pods -n photovault
```

### HPA Tuning

Adjust min/max replicas based on traffic patterns:
```yaml
minReplicas: 3  # Reduce for dev/staging
maxReplicas: 20
```

## Support

For issues:
1. Check logs: `kubectl logs`
2. Check events: `kubectl get events`
3. Check resources: `kubectl top pods`
4. Review configuration: `kubectl describe`
