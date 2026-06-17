# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Run stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Touch files and update permissions so nginx can run as non-root user
RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /var/log/nginx /etc/nginx/conf.d

# Copy static assets and set ownership to nginx user
COPY --from=build --chown=nginx:nginx /app/dist /usr/share/nginx/html

# Expose non-privileged port
EXPOSE 5173

# Copy custom nginx configuration
COPY --chown=nginx:nginx nginx.conf /etc/nginx/conf.d/default.conf

# Actuator health check on port 5173
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:5173/ || exit 1

# Drop privileges and execute as non-root nginx context
USER nginx

CMD ["nginx", "-g", "daemon off;"]
