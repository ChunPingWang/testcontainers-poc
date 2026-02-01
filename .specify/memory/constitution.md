<!--
  ============================================================================
  SYNC IMPACT REPORT
  ============================================================================
  Version change: N/A (new) → 1.0.0

  Modified principles: N/A (initial creation)

  Added sections:
  - Core Principles (7 principles)
    - I. Test-Driven Development (TDD)
    - II. Behavior-Driven Development (BDD)
    - III. Domain-Driven Design (DDD)
    - IV. SOLID Principles
    - V. Hexagonal Architecture
    - VI. Layered Architecture & Dependency Inversion
    - VII. Data Mapping Between Layers
  - Architecture Guidelines
  - Quality Standards
  - Governance

  Removed sections: N/A (initial creation)

  Templates requiring updates:
  - .specify/templates/plan-template.md: ✅ Compatible (Constitution Check section exists)
  - .specify/templates/spec-template.md: ✅ Compatible (BDD scenarios supported)
  - .specify/templates/tasks-template.md: ✅ Compatible (TDD workflow supported)
  - .specify/templates/checklist-template.md: ✅ Compatible (generic structure)

  Follow-up TODOs: None
  ============================================================================
-->

# Testcontainers POC Constitution

## Core Principles

### I. Test-Driven Development (TDD) (NON-NEGOTIABLE)

TDD is mandatory for all production code development. The Red-Green-Refactor cycle MUST be strictly enforced:

1. **Red**: Write a failing test FIRST that defines expected behavior
2. **Green**: Write the minimum code necessary to make the test pass
3. **Refactor**: Improve code structure while maintaining passing tests

**Rules**:
- Tests MUST be written before implementation code
- Tests MUST fail before implementation begins (verify the test actually tests something)
- Each test MUST focus on a single behavior or requirement
- Test code is production code - same quality standards apply
- No production code changes without corresponding test changes

**Rationale**: TDD ensures code is testable by design, provides living documentation, catches regressions early, and drives simpler implementations.

### II. Behavior-Driven Development (BDD)

All user-facing features MUST be specified using BDD scenarios following Given-When-Then format:

1. **Given**: Initial context or preconditions
2. **When**: Action or event that occurs
3. **Then**: Expected outcome or result

**Rules**:
- Acceptance criteria MUST be written in Given-When-Then format
- Scenarios MUST be written in business language, not technical jargon
- Each scenario MUST be independently executable
- Scenarios serve as executable specifications and living documentation

**Rationale**: BDD bridges communication between technical and business stakeholders, ensures shared understanding of requirements, and provides automated acceptance testing.

### III. Domain-Driven Design (DDD)

The domain model is the heart of the application. Business logic MUST reside in the domain layer:

**Strategic Design**:
- Identify and define Bounded Contexts explicitly
- Establish a Ubiquitous Language shared by developers and domain experts
- Map relationships between contexts (Context Map)

**Tactical Design**:
- **Entities**: Objects with identity that persists over time
- **Value Objects**: Immutable objects defined by their attributes
- **Aggregates**: Clusters of entities/value objects with a single root
- **Domain Services**: Operations that don't belong to any entity
- **Domain Events**: Records of something significant that happened
- **Repositories**: Abstractions for aggregate persistence (interfaces only in domain)

**Rules**:
- Domain layer MUST NOT depend on any external framework or infrastructure
- Domain objects MUST enforce their own invariants
- Aggregate boundaries MUST be respected for transactional consistency
- Domain logic MUST NOT leak into application or infrastructure layers

**Rationale**: DDD ensures the software model reflects the business domain, making it easier to understand, evolve, and maintain as business requirements change.

### IV. SOLID Principles

All code MUST adhere to SOLID principles:

1. **Single Responsibility Principle (SRP)**: A class MUST have only one reason to change
2. **Open/Closed Principle (OCP)**: Modules MUST be open for extension, closed for modification
3. **Liskov Substitution Principle (LSP)**: Subtypes MUST be substitutable for their base types
4. **Interface Segregation Principle (ISP)**: Clients MUST NOT depend on interfaces they don't use
5. **Dependency Inversion Principle (DIP)**: High-level modules MUST NOT depend on low-level modules; both MUST depend on abstractions

**Rules**:
- Classes exceeding 200 lines MUST be reviewed for SRP violations
- New features SHOULD extend existing code, not modify it
- Interfaces MUST be small and focused (prefer many specific interfaces over one general)
- Dependencies MUST be injected, not instantiated directly

**Rationale**: SOLID principles produce code that is maintainable, testable, extensible, and resistant to bugs during modifications.

### V. Hexagonal Architecture (Ports & Adapters)

The application MUST follow Hexagonal Architecture with three concentric layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                            │
│  (Frameworks, DB, HTTP, Message Queues, External Services)  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                    APPLICATION                       │    │
│  │  (Use Cases, Application Services, Orchestration)   │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │                  DOMAIN                      │    │    │
│  │  │  (Entities, Value Objects, Domain Services) │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Ports (Interfaces)**:
- **Driving Ports** (Primary): Define how external actors interact with the application (e.g., `UserService` interface)
- **Driven Ports** (Secondary): Define how the application interacts with external systems (e.g., `UserRepository` interface)

**Adapters (Implementations)**:
- **Driving Adapters**: REST controllers, CLI handlers, message consumers
- **Driven Adapters**: Database repositories, external API clients, message publishers

**Rules**:
- Domain layer MUST NOT import from application or infrastructure
- Application layer MUST NOT import from infrastructure
- All external frameworks MUST reside in infrastructure layer only
- Inner layers define interfaces (ports); outer layers provide implementations (adapters)

**Rationale**: Hexagonal architecture isolates business logic from technical concerns, enabling technology changes without affecting the core domain.

### VI. Layered Architecture & Dependency Inversion

Strict layering with dependency inversion MUST be enforced:

**Layer Definitions**:

| Layer | Purpose | Allowed Dependencies |
|-------|---------|---------------------|
| **Domain** | Business logic, entities, domain services | None (innermost) |
| **Application** | Use cases, orchestration, DTOs | Domain layer only |
| **Infrastructure** | Frameworks, persistence, external I/O | Application + Domain layers |

**Dependency Rules**:

1. **Infrastructure → Application**: ALLOWED (direct usage)
2. **Infrastructure → Domain**: ALLOWED (direct usage)
3. **Application → Domain**: ALLOWED (direct usage)
4. **Application → Infrastructure**: FORBIDDEN (use interfaces/ports)
5. **Domain → Application**: FORBIDDEN
6. **Domain → Infrastructure**: FORBIDDEN

**Implementation Pattern**:
```
Domain Layer:       Defines interface UserRepository
Application Layer:  Uses UserRepository interface via dependency injection
Infrastructure:     Implements JpaUserRepository implements UserRepository
```

**Rules**:
- Dependencies MUST flow inward only (outer to inner)
- Inner layers MUST define interfaces for capabilities they require from outer layers
- Dependency injection MUST be used to wire implementations to interfaces
- Package/module boundaries MUST enforce layer separation

**Rationale**: Dependency inversion decouples the core business logic from infrastructure details, enabling testability with mocks and flexibility to swap implementations.

### VII. Data Mapping Between Layers

Data transfer between layers MUST use explicit mappers:

**Mapper Responsibilities**:
- Convert domain objects to/from DTOs (Data Transfer Objects)
- Convert domain objects to/from persistence models
- Convert external API responses to domain objects
- Never expose domain objects directly to external interfaces

**Required Mappers**:

| From | To | Mapper Location |
|------|----|--------------------|
| Domain Entity | Response DTO | Application layer |
| Request DTO | Domain Command/Query | Application layer |
| Domain Entity | Persistence Model | Infrastructure layer |
| Persistence Model | Domain Entity | Infrastructure layer |
| External API Response | Domain Object | Infrastructure layer |

**Rules**:
- Mappers MUST be pure functions without side effects
- Mappers MUST NOT contain business logic (validation, transformation rules)
- Domain objects MUST NOT have mapping annotations (e.g., `@Entity`, `@JsonProperty`)
- Each layer MUST have its own data representation classes

**Rationale**: Explicit mappers prevent coupling between layers, allow each layer to evolve independently, and keep the domain model clean from infrastructure concerns.

## Architecture Guidelines

### Project Structure

```
src/
├── domain/                    # Innermost layer - pure business logic
│   ├── model/                 # Entities, Value Objects, Aggregates
│   ├── service/               # Domain Services
│   ├── event/                 # Domain Events
│   └── repository/            # Repository interfaces (ports)
│
├── application/               # Use cases and orchestration
│   ├── port/
│   │   ├── in/                # Driving ports (use case interfaces)
│   │   └── out/               # Driven ports (repository/service interfaces)
│   ├── service/               # Application services (use case implementations)
│   ├── dto/                   # Request/Response DTOs
│   └── mapper/                # Application-level mappers
│
├── infrastructure/            # Outermost layer - technical implementations
│   ├── adapter/
│   │   ├── in/
│   │   │   ├── web/           # REST controllers
│   │   │   └── cli/           # CLI handlers
│   │   └── out/
│   │       ├── persistence/   # Database adapters
│   │       └── external/      # External API clients
│   ├── config/                # Framework configuration
│   └── mapper/                # Infrastructure-level mappers

tests/
├── unit/                      # Unit tests (domain + application logic)
├── integration/               # Integration tests (adapters with real dependencies)
└── contract/                  # Contract tests (API specifications)
```

### Technology Isolation

All frameworks and libraries MUST be confined to the infrastructure layer:

- **Web Frameworks** (Spring, Express, FastAPI): infrastructure/adapter/in/web
- **ORM/Database** (Hibernate, TypeORM, SQLAlchemy): infrastructure/adapter/out/persistence
- **Message Queues** (Kafka, RabbitMQ): infrastructure/adapter/out/messaging
- **External HTTP Clients**: infrastructure/adapter/out/external

## Quality Standards

### Testing Requirements

| Test Type | Location | Purpose | Coverage Target |
|-----------|----------|---------|-----------------|
| Unit Tests | tests/unit/ | Domain logic, pure functions | 80%+ domain layer |
| Integration Tests | tests/integration/ | Adapter implementations | All adapters |
| Contract Tests | tests/contract/ | API specifications | All public APIs |
| Acceptance Tests | tests/acceptance/ | BDD scenarios | All user stories |

### Code Quality Gates

Before merging any code:

1. All tests MUST pass
2. Test coverage MUST NOT decrease
3. No new linting violations
4. All public interfaces MUST be documented
5. Architecture rules (layer dependencies) MUST NOT be violated

### Review Checklist

- [ ] Tests written first (TDD compliance)
- [ ] BDD scenarios for user-facing features
- [ ] Domain logic free from framework dependencies
- [ ] Proper layer separation maintained
- [ ] Mappers used for cross-layer data transfer
- [ ] SOLID principles adhered to
- [ ] No direct infrastructure dependencies in domain/application

## Governance

### Amendment Process

1. Propose changes via pull request to this constitution
2. Changes require documented justification
3. Breaking changes (removing principles, weakening standards) require team consensus
4. All dependent templates MUST be updated in the same PR

### Versioning Policy

Constitution follows Semantic Versioning:
- **MAJOR**: Removal or fundamental redefinition of principles
- **MINOR**: New principles, expanded guidance, new sections
- **PATCH**: Clarifications, typo fixes, non-semantic changes

### Compliance Review

- All PRs MUST verify compliance with this constitution
- Architecture Decision Records (ADRs) MUST reference relevant principles
- Quarterly review of constitution applicability and effectiveness
- Violations MUST be documented with explicit justification if exceptions are granted

### Exceptions

Exceptions to any principle require:
1. Written justification in the PR description
2. Documentation of the specific principle being violated
3. Explanation of why alternatives were insufficient
4. Approval from at least one additional reviewer

**Version**: 1.0.0 | **Ratified**: 2026-02-01 | **Last Amended**: 2026-02-01
