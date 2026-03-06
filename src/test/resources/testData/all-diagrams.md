# All Mermaid Diagram Types

Test file covering all 22+ Mermaid diagram types for manual and automated verification.

## 1. Flowchart

```mermaid
flowchart TD
    A[Start] --> B{Decision}
    B -->|Yes| C[OK]
    B -->|No| D[Cancel]
    C --> E[End]
    D --> E
```

## 2. Graph (flowchart alias)

```mermaid
graph LR
    A --> B --> C
    B --> D
```

## 3. Sequence Diagram

```mermaid
sequenceDiagram
    participant Alice
    participant Bob
    Alice->>Bob: Hello Bob
    Bob-->>Alice: Hi Alice
    Alice->>Bob: How are you?
    Bob-->>Alice: Fine thanks!
```

## 4. Class Diagram

```mermaid
classDiagram
    Animal <|-- Duck
    Animal <|-- Fish
    Animal : +int age
    Animal : +String gender
    Animal : +swim()
    Duck : +String beakColor
    Duck : +quack()
    Fish : +int sizeInFeet
    Fish : +canEat()
```

## 5. State Diagram v2

```mermaid
stateDiagram-v2
    [*] --> Still
    Still --> Moving
    Moving --> Still
    Moving --> Crash
    Crash --> [*]
```

## 6. Entity Relationship Diagram

```mermaid
erDiagram
    CUSTOMER ||--o{ ORDER : places
    ORDER ||--|{ LINE-ITEM : contains
    CUSTOMER {
        string name
        string email
    }
    ORDER {
        int orderNumber
        date created
    }
```

## 7. User Journey

```mermaid
journey
    title My working day
    section Go to work
      Make tea: 5: Me
      Go upstairs: 3: Me
      Do work: 1: Me, Cat
    section Go home
      Go downstairs: 5: Me
      Sit down: 5: Me
```

## 8. Gantt Chart

```mermaid
gantt
    title A Gantt Diagram
    dateFormat YYYY-MM-DD
    section Section
        A task          :a1, 2024-01-01, 30d
        Another task    :after a1, 20d
    section Another
        Task in Another :2024-01-12, 12d
        Another task    :24d
```

## 9. Pie Chart

```mermaid
pie title Pets adopted
    "Dogs" : 386
    "Cats" : 85
    "Rats" : 15
```

## 10. Git Graph

```mermaid
gitGraph
    commit
    commit
    branch develop
    checkout develop
    commit
    commit
    checkout main
    merge develop
    commit
```

## 11. Mindmap

```mermaid
mindmap
  root((mindmap))
    Origins
      Long history
      Popularisation
    Research
      On effectiveness
      On features
    Tools
      Pen and paper
      Mermaid
```

## 12. Timeline

```mermaid
timeline
    title History of Social Media
    2002 : LinkedIn
    2004 : Facebook
    2005 : Youtube
    2006 : Twitter
    2010 : Instagram
```

## 13. Quadrant Chart

```mermaid
quadrantChart
    title Reach and engagement
    x-axis Low Reach --> High Reach
    y-axis Low Engagement --> High Engagement
    quadrant-1 We should expand
    quadrant-2 Need to promote
    quadrant-3 Re-evaluate
    quadrant-4 May be improved
    Campaign A: [0.3, 0.6]
    Campaign B: [0.45, 0.23]
    Campaign C: [0.57, 0.69]
    Campaign D: [0.78, 0.34]
```

## 14. XY Chart (beta)

```mermaid
xychart-beta
    title "Sales Revenue"
    x-axis [jan, feb, mar, apr, may]
    y-axis "Revenue (in $)" 4000 --> 11000
    bar [5000, 6000, 7500, 8200, 9500]
    line [5000, 6000, 7500, 8200, 9500]
```

## 15. Sankey (beta)

```mermaid
sankey-beta

Agricultural 'waste',Bio-conversion,124.729
Bio-conversion,Liquid,0.597
Bio-conversion,Losses,26.862
Bio-conversion,Solid,280.322
Bio-conversion,Gas,81.144
```

## 16. Requirement Diagram

```mermaid
requirementDiagram

    requirement test_req {
        id: 1
        text: the test requirement
        risk: high
        verifymethod: test
    }

    element test_entity {
        type: simulation
    }

    test_entity - satisfies -> test_req
```

## 17. C4 Context

```mermaid
C4Context
    title System Context diagram
    Person(customer, "Customer", "A customer of the bank")
    System(banking, "Banking System", "Core banking")
    Rel(customer, banking, "Uses")
```

## 18. ZenUML

```mermaid
zenuml
    title Order Service
    @Actor Client
    @Boundary OrderController
    @Entity OrderService
    Client->OrderController.placeOrder() {
        OrderController->OrderService.create() {
            OrderService-->OrderController: order
        }
        OrderController-->Client: order
    }
```

## 19. Block (beta)

```mermaid
block-beta
    columns 3
    a["Front"] b["Middle"] c["End"]
    d e f
```

## 20. Packet (beta)

```mermaid
packet-beta
    0-15: "Source Port"
    16-31: "Destination Port"
    32-63: "Sequence Number"
    64-95: "Acknowledgment Number"
```

## 21. Architecture (beta)

```mermaid
architecture-beta
    group api(cloud)[API]

    service db(database)[Database] in api
    service server(server)[Server] in api

    db:R -- L:server
```

## 22. Kanban

```mermaid
kanban
    column1["To Do"]
        task1["Task 1"]
        task2["Task 2"]
    column2["In Progress"]
        task3["Task 3"]
    column3["Done"]
        task4["Task 4"]
```

---

## Edge Cases

### Empty block (should be skipped gracefully)

```mermaid
```

### Special characters

```mermaid
flowchart TD
    A["Node with 'quotes'"] --> B["Node with <brackets>"]
    B --> C["Node with & ampersand"]
```

### Large diagram (many nodes)

```mermaid
flowchart TD
    N1 --> N2 --> N3 --> N4 --> N5
    N5 --> N6 --> N7 --> N8 --> N9 --> N10
    N10 --> N11 --> N12 --> N13 --> N14 --> N15
    N15 --> N16 --> N17 --> N18 --> N19 --> N20
    N1 --> N10
    N5 --> N15
    N10 --> N20
```
