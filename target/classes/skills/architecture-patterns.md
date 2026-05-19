# Software Architecture Patterns

This skill provides reference knowledge for common software architecture patterns
used when reviewing Architecture Decision Records (ADRs).

## Structural Patterns

### Layered Architecture (N-Tier)
- **Use when**: Clear separation between UI, business logic, and data is needed
- **Trade-offs**: Simple to understand; can create tight coupling between layers
- **Warning signs in ADR**: Missing description of how layers will communicate

### Hexagonal Architecture (Ports & Adapters)
- **Use when**: Domain logic must be isolated from infrastructure concerns
- **Trade-offs**: High testability and flexibility; more boilerplate and indirection
- **Warning signs in ADR**: No mention of domain boundaries or port definitions

### Microservices
- **Use when**: Independent scalability and deployment of services is required
- **Trade-offs**: High scalability; significant operational complexity (distributed tracing, retries, etc.)
- **Warning signs in ADR**: No mention of service boundaries, communication protocols, or data ownership

### Event-Driven Architecture
- **Use when**: Decoupling producers from consumers, eventual consistency acceptable
- **Trade-offs**: High decoupling; complex debugging, eventual consistency challenges
- **Warning signs in ADR**: No mention of ordering guarantees or event schema versioning

## Data Patterns

### CQRS (Command Query Responsibility Segregation)
- **Use when**: Read and write workloads have very different requirements
- **Trade-offs**: Optimized reads/writes; eventual consistency between models

### Event Sourcing
- **Use when**: Full audit trail of state changes is required
- **Trade-offs**: Complete history; storage growth and complex query patterns

## Integration Patterns

### API Gateway
- **Trade-offs**: Centralized cross-cutting concerns; single point of failure risk

### Saga Pattern
- **Trade-offs**: Distributed transaction management; complex compensation logic

## Evaluation Checklist for ADR Reviews

When an ADR proposes adopting a pattern, verify:
1. Is the pattern appropriate for the stated problem?
2. Are the known trade-offs of this pattern acknowledged in the ADR?
3. Does the team have experience with this pattern?
4. Are there simpler alternatives that would solve the problem adequately?
5. What is the migration path if this decision needs to be reversed?
