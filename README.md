# Test ADRIA PFE 2027 - Mahboub Marouane

Ce README présente les étapes du TP. Le rapport PDF sera préparé à la fin, à partir du travail réalisé et des captures des tests.

## Partie B - Conception et Architecture

### 1. Diagramme de classes

Le modèle contient deux classes principales :

- **Account** : un compte bancaire avec son client, son état, sa devise et son solde.
- **Transaction** : une opération bancaire liée à un compte par `accountId`.

Un compte peut avoir plusieurs transactions. Son état est `ACTIVE` ou `BLOCKED`. Chaque microservice possède sa base de données : le lien entre les deux classes est logique, sans relation JPA entre les services.

Le diagramme présente également :

- **AccountService** et **TransactionService** : les opérations et règles métier.
- **AccountRepository** et **TransactionRepository** : l’accès aux données.
- **AccountClient** : les appels REST du service transactions vers le service comptes avec OpenFeign et Resilience4J. Ces appels passent par l’interface REST du service comptes.

Pour garder le diagramme lisible, les contrôleurs REST, GraphQL et les outils MCP sont présentés dans le schéma d’architecture. Ils appellent les services métier. Le type `Account` retourné par le client représente les données du compte reçues par HTTP ; il ne s’agit pas d’une entité JPA partagée entre les deux services.

![Diagramme de classes](conception/diagramme-classes.png)

### 2. Schéma d’architecture

Le client Angular passe par la gateway pour accéder aux services. `transaction-service` communique avec `account-service` avec OpenFeign et Resilience4J pour vérifier le compte et mettre à jour son solde.

Chaque service métier expose REST, GraphQL et MCP. Ces trois interfaces utilisent la même couche service pour appliquer les règles métier : montant positif, plafond, compte actif et devise identique.

![Schéma d’architecture](conception/architecture.png)

Les autres composants ont les rôles suivants :

- **Eureka** : découvrir les microservices.
- **Config Server** : partager le plafond `bank.transaction.max-amount=1000`.
- **Keycloak** : gérer l’authentification et les autorisations.
- **Spring AI** : permettre au chatbot d’utiliser les outils MCP des services, depuis le web, Telegram ou Discord.
- **Kafka** : recevoir un événement après l’enregistrement d’une transaction dans `bank.transactions.created`, avec `accountId` comme clé. Le consumer journalise uniquement l’événement.

Un virement doit vérifier les deux comptes avant de débiter la source et de créditer la destination. Il faudra gérer les échecs pour éviter une mise à jour partielle des soldes.

L’expérimentation Python de détection de fraude sera réalisée séparément, sans intégration aux microservices. Docker Compose et GitHub Actions seront traités dans la partie DevOps.

Les diagrammes représentent la conception prévue.

### Images des diagrammes

Les diagrammes sont disponibles au format PNG dans [conception/](conception/) et une copie est conservée dans `images/`.

## Partie C - Microservices fonctionnels

### 1. Initialisation du projet

Le projet Maven parent `adria-bank` regroupe les modules. Le premier module est `account-service`, configuré avec Java 21, Spring Boot 4.0.8, Spring Web MVC, Spring Data JPA et H2, comme dans le projet exemple.

Le service utilise le port `8056` et une base H2 en mémoire `accounts-db`. Les entités et les connecteurs REST, GraphQL et MCP seront ajoutés dans les étapes suivantes.

Compilation depuis la racine :

```bash
bash mvnw compile
```

Le POM parent déclare le module `account-service` :

![Projet Maven parent](images/initialisation-maven.png)

La commande `bash mvnw compile` compile le parent et le module avec succès :

![Compilation du projet](images/compilation-account-service.png)

### 2. Entité Account et repository

L’entité `Account` contient les champs demandés : `id` de type UUID, `customerReference`, `status`, `currency` et `balance`. L’identifiant est généré automatiquement. L’état du compte est enregistré en texte (`ACTIVE` ou `BLOCKED`) avec l’énumération `AccountStatus`.

`AccountRepository` hérite de `JpaRepository<Account, UUID>` pour enregistrer et consulter les comptes. Les classes métier se trouvent dans `domain/` et le repository dans `repository/`. Lombok génère les accesseurs, les constructeurs et le builder, comme dans le projet exemple.

Entité `Account` :

![Entité Account](images/account-entity.png)

Interface `AccountRepository` :

![AccountRepository](images/account-repository.png)

La compilation après l’ajout de l’entité et du repository se termine avec `BUILD SUCCESS` :

![Compilation de l’entité et du repository](images/compilation-account-domain.png)

### 3. Couche service et API REST des comptes

`AccountService` permet de créer un compte, de consulter un compte par UUID et de lister les comptes. `AccountRestController` délègue ces opérations au service.

| Méthode | URL | Résultat |
| --- | --- | --- |
| POST | `/accounts` | Création du compte, réponse 201 avec UUID généré |
| GET | `/accounts` | Liste des comptes, réponse 200 |
| GET | `/accounts/{id}` | Compte demandé, réponse 200 ou 404 s’il n’existe pas |

La création utilise `CreateAccountRequest`, sans identifiant fourni par le client. La référence client est obligatoire, l’état doit être renseigné, la devise doit comporter trois lettres majuscules et le solde initial doit être positif ou nul. Une requête invalide reçoit une réponse 400. La validation est également appliquée à l’entrée de la couche service pour les futurs connecteurs GraphQL et MCP.

Swagger UI est disponible à l’adresse : http://localhost:8056/swagger-ui/index.html.

Exemple de création :

```json
{
  "customerReference": "CLIENT-001",
  "status": "ACTIVE",
  "currency": "MAD",
  "balance": 2500
}
```

Les tests d’intégration vérifient la création et la lecture, les erreurs 404/400 et le refus des données invalides sans enregistrement. Résultat de `bash mvnw test` : 3 tests réussis, aucune erreur.

Pour démarrer le service depuis la racine :

```bash
bash mvnw -pl account-service spring-boot:run
```

Les comptes sont stockés dans H2 en mémoire : ils sont perdus à l’arrêt du service.

#### Tests dans Swagger UI

Création du compte `CLIENT-001` avec un solde de 2500 MAD : réponse 201 avec un UUID généré.

![Création du compte - 201](images/swagger-account-create-201.png)

Consultation de la liste des comptes : réponse 200 contenant le compte créé.

![Liste des comptes - 200](images/swagger-account-list-200.png)

Consultation du compte avec son UUID : réponse 200 avec les mêmes informations.

![Consultation par UUID - 200](images/swagger-account-get-200.png)

Consultation d’un UUID inexistant : réponse 404 avec le message « Compte introuvable ».

![Compte introuvable - 404](images/swagger-account-not-found-404.png)

Tentative de création avec un solde initial de -500 : la requête est refusée avec une réponse 400.

![Solde initial négatif - 400](images/swagger-account-invalid-400.png)

### 4. Connecteur GraphQL des comptes

Le schéma `graphql/schema.graphqls` expose les requêtes `accounts`, `account(id)` et la mutation `createAccount(input)`. Le contrôleur dans `web/graphql/` appelle le même `AccountService` que REST.

GraphiQL : http://localhost:8056/graphiql. L’endpoint HTTP est `/graphql`.

Création d’un compte :

```graphql
mutation {
  createAccount(input: {
    customerReference: "CLIENT-GQL"
    status: ACTIVE
    currency: "MAD"
    balance: 1500
  }) {
    id
    customerReference
    status
    currency
    balance
  }
}
```

Liste des comptes :

```graphql
query {
  accounts {
    id
    customerReference
    status
    currency
    balance
  }
}
```

Consultation par UUID (remplacer `UUID_DU_COMPTE` par l’identifiant retourné à la création) :

```graphql
query {
  account(id: "UUID_DU_COMPTE") {
    id
    customerReference
    status
    currency
    balance
  }
}
```

Pour un compte inexistant, utiliser `00000000-0000-0000-0000-000000000000` : la réponse contient `account: null` et une erreur `NOT_FOUND`. Pour vérifier la validation, reprendre la mutation avec `balance: -100` : une erreur `BAD_REQUEST` est retournée, sans création de compte.

Contrairement aux réponses REST 404/400, ces erreurs d’exécution GraphQL sont présentes dans le champ `errors` du JSON avec une réponse HTTP 200.

Validation : `bash mvnw test` exécute 7 tests réussis (3 REST et 4 GraphQL). Les tests GraphQL couvrent la création, la liste, la consultation, la lecture par REST, le compte inexistant et les données invalides.

Création du compte `CLIENT-GQL` avec un solde de 1500 MAD depuis GraphiQL :

![Création du compte avec GraphQL](images/graphql-account-create.png)

Consultation de la liste des comptes depuis GraphiQL :

![Liste des comptes avec GraphQL](images/graphql-account-list.png)

Consultation du compte créé par son UUID :

![Consultation du compte avec GraphQL](images/graphql-account-get.png)

Recherche d’un compte inexistant : `account` vaut `null` et l’erreur est classée `NOT_FOUND`.

![Compte inexistant avec GraphQL](images/graphql-account-not-found.png)

Création avec un solde négatif : la mutation est refusée avec l’erreur `BAD_REQUEST`.

![Solde négatif avec GraphQL](images/graphql-account-invalid.png)

### 5. Connecteur MCP des comptes

`AccountMcpTools`, dans le package `mcp/`, expose trois outils avec Spring AI 2.0.1 :

| Outil | Paramètres | Opération |
| --- | --- | --- |
| `list_accounts` | Aucun | Lister les comptes |
| `get_account` | `id` | Consulter un compte par UUID |
| `create_account` | `request` | Créer un compte |

Les outils appellent le même `AccountService` que REST et GraphQL. La validation du solde initial reste donc identique pour les trois connecteurs.

Le serveur utilise le transport **Streamable HTTP** à l’adresse `http://localhost:8056/mcp`. Les annotations `@McpTool` et `@McpToolParam` décrivent les outils et leurs paramètres.

Pour ouvrir MCP Inspector :

```bash
npx @modelcontextprotocol/inspector
```

Ouvrir l’adresse affichée dans le terminal, sélectionner le transport Streamable HTTP et connecter le serveur `http://localhost:8056/mcp`.

Paramètres de `create_account` :

```json
{
  "request": {
    "customerReference": "CLIENT-MCP",
    "status": "ACTIVE",
    "currency": "MAD",
    "balance": 2000
  }
}
```

Vérifications effectuées sur le serveur : découverte des trois outils, création, consultation par UUID, liste, refus d’un solde négatif et erreur pour un compte inexistant. Les erreurs d’exécution des outils sont signalées par `isError: true`. Les 7 tests REST/GraphQL passent également après l’ajout de MCP.

Création du compte `CLIENT-MCP` avec un solde de 2000 MAD dans MCP Inspector :

![Création du compte avec MCP](images/mcp-account-create.png)

Consultation du compte `CLIENT-MCP` par UUID dans MCP Inspector :

![Consultation du compte avec MCP](images/mcp-account-get.png)
