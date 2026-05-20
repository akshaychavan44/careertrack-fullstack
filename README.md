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

## Resume Line

Built CareerTrack, a full-stack job application tracker using React.js, Spring Boot, Spring Security, JWT, and MongoDB to manage job applications, interview schedules, application statuses, and dashboard analytics.

## Resume Bullet Points

- Developed a full-stack job tracking platform using React.js, Spring Boot, and MongoDB with secure JWT-based authentication.
- Implemented REST APIs for user authentication, job application management, interview tracking, status filtering, and dashboard statistics.
- Designed MongoDB collections for users, applications, and interviews to manage job search data efficiently.
- Built a responsive frontend using React.js, Bootstrap, and Axios for managing applications and interview schedules.
