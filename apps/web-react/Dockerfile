# Stage 1: Build
FROM node:20 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Custom nginx config to listen on 3002 and support SPA routing
RUN echo 'server { 
    listen 3002; 
    location / { 
        root /usr/share/nginx/html; 
        index index.html; 
        try_files $uri $uri/ /index.html; 
    } 
}' > /etc/nginx/conf.d/default.conf

EXPOSE 3002
CMD ["nginx", "-g", "daemon off;"]
