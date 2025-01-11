[中文](README.md)

---

### Project Overview
An Enterprise Authentication Service built with Javalin framework, providing RESTful APIs with Swagger documentation and Bearer Token authentication.

### Requirements
- OpenJDK 17.0.2+
- Maven

### Project Structure
```
.
├── conf/                 # Configuration directory
│   ├── cert/            # Certificates
│   └── security.properties
├── src/
│   └── main/
│       ├── java/        # Source code
│       └── resources/   # Resource files
├── .env.example        # Environment variables example
└── pom.xml             # Maven configuration
```

### Core Modules
- **Config**: Application configuration management, including environment variables, error handling, and Swagger setup
- **Exception**: Global exception handling
- **Model**: Data model definitions, including POJOs and unified response format
- **Middleware**: Middleware components, such as authentication
- **Router**: Route registration
- **Handler**: Request handlers that invoke Services for business logic
- **Service**: Stateless business logic implementation

### Design Principles
- Handlers manage state (e.g., database connections)
- Services maintain stateless design
- Unified error handling and response format
- Modular project structure




