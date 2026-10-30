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

Les diagrammes représentent la conception prévue ; les services ne sont pas encore implémentés dans ce dépôt.

### Images des diagrammes

Les diagrammes sont disponibles au format PNG dans [conception/](conception/) et une copie est conservée dans `images/`.
