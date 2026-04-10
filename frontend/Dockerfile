FROM nginx:1.27-alpine

COPY frontend/index.html /usr/share/nginx/html/index.html
COPY frontend/choose-driver.html /usr/share/nginx/html/choose-driver.html
COPY frontend/script.js /usr/share/nginx/html/script.js
COPY frontend/choose-driver.js /usr/share/nginx/html/choose-driver.js
COPY frontend/styles.css /usr/share/nginx/html/styles.css
COPY frontend/assets /usr/share/nginx/html/assets

EXPOSE 80
