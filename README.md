# **Crypto Wallet API**

A simple **Spring Boot** application to manage a cryptocurrency wallet, update asset prices, and retrieve wallet summaries.

---

## **Features**

- Add crypto assets to your wallet.
- Fetch and update the latest prices of your assets (manually or automatically).
- Set a scheduler to update prices at a custom frequency.
- Retrieve wallet summaries with current or historical performance.

The **API documentation** is available at:
> **[http://localhost:8080](http://localhost:8080)**
(Access this URL after running the application to test the endpoints.)

---

## **Tech Stack**

- **Java 17**
- **Spring Boot 3.x**
- **Spring WebFlux** (non-blocking WebClient)
- **H2 Database** (in-memory database)

---

## **Prerequisites**

- **Java 17+**
- **Maven**
- Internet access (for CoinCap API).

---

## **Getting Started**

### **Run with Maven**

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Diogo23Sousa/cryptowallet.git
   cd cryptowallet
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

4. Open the API documentation in your browser:
   ```
   http://localhost:8080
   ```

### **Run with Docker**

1. **Ensure Docker and Docker Compose are installed**.

2. **Build and start the container**:
   ```bash
   docker-compose up --build
   ```

3. Access the API documentation:
   ```
   http://localhost:8080
   ```

---

## **H2 Database Console**

To view the database data:

1. Open the H2 Console:
   > **[http://localhost:8080/h2-console](http://localhost:8080/h2-console)**

2. Use the following credentials:
   - **JDBC URL**: `jdbc:h2:mem:testdb`
   - **Username**: `sa`
   - **Password**: (leave it blank)

---

## **Logs**

Logs are saved to the following file when running the application:
```
logs/app.log
```

---

Happy coding! 🚀