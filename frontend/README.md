# QuickLink React frontend

The React development server runs at `http://localhost:5173` and forwards `/api`, `/login`, and `/logout` requests to Spring Boot at `http://localhost:8080`. Keep both servers running while using the app. Short links open through Spring Boot on port `8080`.

## Run it from Terminal

1. Start the PostgreSQL database configured in the project's `src/main/resources/application.properties`.
2. In IntelliJ, run `QuicklinkApplication`, or open a terminal at the repository root and run:

   ```sh
   ./mvnw spring-boot:run
   ```

3. Open a second terminal and run:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

4. Open the URL printed by Vite, usually `http://localhost:5173`.

## Run it from IntelliJ

Run the existing `QuicklinkApplication` configuration first. Then open IntelliJ's Terminal panel, run `cd frontend`, run `npm install` once, and run `npm run dev`. Open `http://localhost:5173` in a browser.

The first run of `npm install` needs an internet connection. `npm run build` creates the production frontend in `frontend/dist`.
