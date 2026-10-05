# MicroLearn frontend

React 18 + Vite + React Router, plain CSS. Talks only to the API gateway (port 8080) through a dev proxy.

## Run
```
npm install        (first time only)
npm run dev        http://localhost:5173
```
The backend (gateway, services, RabbitMQ, MySQL) must be running.

## Production build
```
npm run build      creates the dist/ folder
npm run preview    serves dist/ on http://localhost:4173 (also proxies /api)
```
To host `dist/` yourself, serve it with any web server and forward `/api` to the gateway. Example nginx:
```
location /       { root /var/www/microlearn; try_files $uri /index.html; }
location /api/   { proxy_pass http://localhost:8080; }
```

## Pages
| Route | Who | Purpose |
|---|---|---|
| `/`, `/courses`, `/courses/:id`, `/verify`, `/verify/:code`, `/login`, `/register` | everyone | home, catalog, course page, certificate check, sign in/up |
| `/enrollments`, `/notifications`, `/certificates`, `/learn/:id`, `/learn/:id/quiz` | student | enrollments (live status), messages, certificates, lessons, quiz |
| `/teach`, `/teach/new`, `/teach/:id`, `/teach/:id/quiz` | instructor | my courses, editor, lessons, publish, quiz builder |
| `/admin/users`, `/admin/courses`, `/admin/enrollments` | admin | disable users, suspend courses, view enrollments |

## Code map
- `src/api/client.js`: every request goes through `api()` (adds the token, readable errors, signs out on an expired token).
- `src/api/useFetch.js`: load data for a page. `src/auth/`: sign-in state and `ProtectedRoute` (role check).
- `src/components/Layout.jsx`: navbar. Add a link in its `NAV` list (with `roles`) when you add a page.
- `src/limits.js`: field length limits, matching the backend. `src/styles.css`: all styling (colors are variables at the top).

## Add a page
1. Create `src/pages/MyPage.jsx`. 2. Add a `<Route>` in `src/App.jsx` (wrap in `ProtectedRoute` if needed). 3. Add a link to `NAV` in `Layout.jsx`.
