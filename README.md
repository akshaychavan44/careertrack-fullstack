# CareerTrack - Full Stack Job Application Tracker

CareerTrack is a resume-ready full-stack project built with React, Spring Boot, JWT authentication, and MongoDB.

## Tech Stack

- Frontend: React, Vite, Bootstrap, Axios
- Backend: Spring Boot, Spring Security, JWT, Spring Data MongoDB
- Database: MongoDB
- Tools: STS, Maven, Postman, VS Code

## Features

- User registration and login
- JWT-based protected APIs
- Add, edit, delete job applications
- Track status: Applied, Interview, Offer, Rejected
- Add interview details
- Dashboard statistics
- Responsive UI

## How To Run

### 1. Start MongoDB

Make sure MongoDB is running locally:

```bash
mongodb://localhost:27017
```

The backend will automatically use/create this database:

```bash
careertrack
```

### 2. Run Backend

Open this folder in STS:

```bash
careertrack-backend
```

Then run:

```bash
mvn spring-boot:run
```

Backend URL:

```bash
http://localhost:8080
```

### 3. Run Frontend

Open another terminal:

```bash
cd careertrack-frontend
npm install
npm run dev
```

Frontend URL:

```bash
http://localhost:5173
```

## Jeweller profile integration

The login form requires a store name. After authentication, the backend posts only
`storeName` and the authenticated user's `email` to the jeweller API. Configure
`JEWELER_API_KEY` as a secret environment variable on the backend hosting service
(Render), then redeploy. Never put the key in a `VITE_` variable or commit it.
`JEWELER_API_URL` optionally overrides the default endpoint:
`https://jewelry-one-ebon.vercel.app/api/v1/jewelers`.

Successful creation saves the store name and a creation flag in MongoDB. Later
logins skip the external request. API failures keep the login form open for retry;
HTTP 409 requires the administrator to link the existing profile. Registration
continues to create only a CareerTrack account; profile creation happens on login.

The saved flag prevents repeat requests after a confirmed success. Exactly-once
creation across multiple backend instances or a timeout after remote creation
requires idempotency support from the external API; its contract does not specify
that support.

Run backend checks with `mvn test`, frontend checks with `npm run build`, and the
deployment checks with `API_URL` set to the backend `/api` URL and
`CHECK_AUTH_ERRORS=true` using `node scripts/smoke-test.mjs`. The smoke check creates
a clearly named test jeweller profile when logging in with a new test account.

## Resume Line

Built CareerTrack, a full-stack job application tracker using React.js, Spring Boot, Spring Security, JWT, and MongoDB to manage job applications, interview schedules, application statuses, and dashboard analytics.

## Resume Bullet Points

- Developed a full-stack job tracking platform using React.js, Spring Boot, and MongoDB with secure JWT-based authentication.
- Implemented REST APIs for user authentication, job application management, interview tracking, status filtering, and dashboard statistics.
- Designed MongoDB collections for users, applications, and interviews to manage job search data efficiently.
- Built a responsive frontend using React.js, Bootstrap, and Axios for managing applications and interview schedules.
