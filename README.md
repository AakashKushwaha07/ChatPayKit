🚀 ChatPayKit

Multi-Tenant Payment Management System with Razorpay Integration, JWT Authentication & WhatsApp Notifications

🔗 Live Frontend: https://chatpaykit.vercel.app

🔗 Live Backend API: https://chatpaykit.onrender.com

📌 Overview

ChatPayKit is a multi-tenant SaaS payment system that allows businesses (tenants) to:

Create accounts

Manage orders

Process payments via Razorpay

Receive webhook-based payment confirmations

Send automated WhatsApp notifications

Securely authenticate using JWT

This project demonstrates:

Spring Boot production backend

JWT-based authentication

Multi-tenant architecture

Razorpay payment integration

Webhook verification

React + Vite frontend

Docker deployment

Render + Vercel cloud deployment

🏗 Architecture
Frontend (React + Vite)  →  Backend (Spring Boot)  →  Razorpay
                                 ↓
                           WhatsApp API


Frontend: Vercel

Backend: Render (Dockerized)

Database: H2 (can upgrade to PostgreSQL)

Auth: JWT (Stateless)

Payments: Razorpay

Notifications: WhatsApp Cloud API

✨ Features
🔐 Authentication

Multi-tenant signup

JWT login

Role-based access (ADMIN)

Stateless session management

💳 Payment System

Razorpay order creation

Secure webhook verification

Signature validation

Payment status tracking

📲 WhatsApp Integration

Automatic payment confirmation message

Meta Cloud API integration

🌐 Multi-Tenant Architecture

Tenant isolation

Tenant-based login

Admin access per tenant

🚀 Production Ready

Dockerized backend

CORS configured

Spring Security configured

Environment variable support

Vercel SPA routing fix

🛠 Tech Stack
Backend

Java 21

Spring Boot 3

Spring Security

JWT

Razorpay SDK

Maven

Docker

Frontend

React

Vite

TypeScript

Axios

Tailwind CSS

Deployment

Render (Backend)

Vercel (Frontend)

📂 Project Structure
ChatPayKit/
│
├── backend/
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── application.yml
│
├── frontend/
│   ├── src/
│   ├── vercel.json
│   ├── package.json
│   └── .env files
│
└── README.md

⚙️ Environment Variables
🔹 Backend (Render)
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
APP_RAZORPAY_WEBHOOKSECRET=
WHATSAPP_ACCESS_TOKEN=
WHATSAPP_PHONE_NUMBER_ID=

🔹 Frontend (Vercel)
VITE_API_BASE=https://chatpaykit.onrender.com

🔄 Razorpay Webhook Setup

Webhook URL:

https://chatpaykit.onrender.com/webhooks/razorpay


Events:

payment.captured

payment.failed

Secret must match:

APP_RAZORPAY_WEBHOOKSECRET

🧪 Local Development
Backend
cd backend
mvn clean install
mvn spring-boot:run


Runs on:

http://localhost:8080

Frontend
cd frontend
npm install
npm run dev


Runs on:

http://localhost:5173

🐳 Docker (Backend)
docker build -t chatpaykit .
docker run -p 8080:8080 chatpaykit

🔒 Security

JWT-based stateless authentication

Role-based authorization

Webhook signature validation

CORS configured for production

Environment variable secrets

🚀 Deployment Steps
Backend (Render)

Connect GitHub repo

Root directory: backend

Use Docker

Add environment variables

Deploy

Frontend (Vercel)

Root directory: frontend

Framework: Vite

Add vercel.json rewrite

Add VITE_API_BASE

Deploy

🧠 What This Project Demonstrates

Real-world SaaS backend architecture

Payment gateway integration

Webhook handling with signature validation

Production security setup

CORS debugging & deployment debugging

Full-stack cloud deployment

📈 Future Improvements

PostgreSQL production database

Redis caching

Email notifications

Stripe integration option

Subscription billing

Admin dashboard analytics

CI/CD pipeline

👨‍💻 Author

Aakash Kumar
B.Tech | Java Backend Developer
Spring Boot | DSA | React | System Design
