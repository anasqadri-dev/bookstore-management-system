# Bookstore Management System (BMS)

An offline-first desktop application designed to digitize and streamline the daily operations of a local bookstore.

Developed as a **Software Quality Engineering (CS304)** final project at **Sukkur IBA University**, Department of Computer Science, Fall 2026.

---

## Project Overview

The Bookstore Management System (BMS) is a standalone desktop application designed for Qadri Book Store, located in Shahi Bazaar, Rohri.

The system aims to replace manual bookkeeping with a centralized digital solution for inventory management, point-of-sale operations, school bulk orders, supplier management, and business reporting.

It is designed to operate offline on the bookstore's existing Windows computer, with locally stored data, authentication, role-based access, and backup and restore capabilities.

## Problem Statement

The bookstore currently manages inventory, sales, purchases, and school orders through manual records. This creates inaccurate stock information, billing difficulties, human errors, and delays in fulfilling bulk orders.

BMS addresses these challenges by providing a reliable, user-friendly, and quality-tested digital management solution.

## Key Features

- **User Authentication:** Secure login and role-based access for Admin and Staff.
- **Inventory Management:** Add, update, delete, and search products, monitor stock, and receive low-stock alerts.
- **Point of Sale (POS):** Process walk-in sales, calculate totals, update inventory, and generate receipts.
- **School Order Management:** Register schools, manage bulk orders, generate quotations, and track order statuses.
- **Supplier Management:** Maintain supplier records, create purchase orders, and record received goods.
- **Reporting:** Generate sales and inventory reports.
- **System Administration:** Manage user accounts and perform data backup and restoration.
- **Printing:** Print receipts, invoices, and reports.

## Technology Stack

| Component        | Technologies                  |
| ---------------- | ----------------------------- |
| Backend          | Java 25, Spring Boot          |
| Frontend         | Next.js, TypeScript           |
| Desktop          | Electron                      |
| Database         | SQLite                        |
| ORM              | Spring Data JPA, Hibernate    |
| Backend Build    | Maven                         |
| Frontend Runtime | Node.js, npm                  |
| Backend Testing  | JUnit 5, Mockito              |
| Frontend Testing | Vitest, React Testing Library |
| Version Control  | Git, GitHub                   |
| Diagrams         | Draw.io, PlantUML             |
| Target OS        | Windows 11                    |

## Architecture

The application follows a modular architecture that separates the user interface, business logic, and data-access responsibilities.

```text
+----------------------------------+
|         Electron Desktop         |
|                                  |
|  +----------------------------+  |
|  |     Next.js + TypeScript    |  |
|  |       User Interface       |  |
|  +-------------+--------------+  |
|                |                 |
|                | REST API        |
|                v                 |
|  +----------------------------+  |
|  |     Spring Boot Backend    |  |
|  |   Business Logic & APIs    |  |
|  +-------------+--------------+  |
|                |                 |
|                v                 |
|  +----------------------------+  |
|  |       SQLite Database      |  |
|  |       Local Storage        |  |
|  +----------------------------+  |
+----------------------------------+
```

_The final deployment architecture and local process communication will be documented and validated during implementation._

## Software Quality Engineering

Quality assurance is a central part of this project. The team will apply systematic software testing and quality management throughout the development lifecycle.

### Quality Objectives

- Define functional and non-functional requirements.
- Establish measurable acceptance criteria.
- Conduct requirements, design, and code reviews.
- Apply black-box testing techniques.
- Perform unit, integration, system, and user acceptance testing.
- Identify, classify, document, and retest defects.
- Evaluate product quality using measurable metrics.
- Maintain test evidence and requirements traceability.

### Testing Techniques

- Equivalence Partitioning
- Boundary Value Analysis
- Positive Testing
- Negative Testing
- Regression Testing
- Functional Testing
- Integration Testing
- User Acceptance Testing

### Quality Attributes

- Performance
- Accuracy
- Reliability
- Security
- Usability
- Documentation

## Project Structure

```text
bookstore-management-system/
├── backend/          # Spring Boot backend
├── frontend/         # Next.js frontend
├── desktop/          # Electron desktop integration
├── docs/             # Project and SQE documentation
├── tests/            # Cross-component tests
├── assets/           # Screenshots and diagrams
├── .github/          # GitHub templates and workflows
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
└── .gitignore
```

## Getting Started

Development and installation instructions will be added as the individual application components are implemented.

### Prerequisites

- Java Development Kit (JDK) 25
- Maven
- Node.js and npm
- Git
- Windows 11 (for target desktop compatibility testing)

### Development Setup

The project consists of three components:

1. Backend: Spring Boot application.
2. Frontend: Next.js application.
3. Desktop: Electron application.

Refer to the README files in their respective directories for component-specific instructions as development progresses.

## Project Documentation

All project documentation and quality assurance evidence are maintained in the `docs/` directory.

| Document            | Description                         |
| ------------------- | ----------------------------------- |
| Project Proposal    | Initial project proposal            |
| SRS                 | Software Requirements Specification |
| Use Cases           | System actors and interactions      |
| Architecture        | Application architecture            |
| Database Design     | Database schema and relationships   |
| SQA Plan            | Software Quality Assurance Plan     |
| Test Plan           | Testing strategy and scope          |
| Test Cases          | Detailed test scenarios             |
| Traceability Matrix | Requirements-to-test mapping        |
| Defect Log          | Identified and tracked defects      |
| Quality Metrics     | Measured quality indicators         |
| Final Report        | Final academic project report       |

## Team Members

Department of Computer Science  
Sukkur IBA University  
BS Software Engineering – Semester V, Section A  
Fall 2026

| Member              | CMS ID      | Primary Responsibility                           |
| ------------------- | ----------- | ------------------------------------------------ |
| Muhammad Anas Qadri | 053-24-0016 | Project Coordination, Architecture & Integration |
| Isra Memon          | 053-24-0017 | UI/UX & Feature Development                      |
| Muhammad Waleed     | 053-24-0100 | Quality Assurance & Testing                      |

**Instructor:** Dr. Khursheed Ali

All members collaborate across requirements engineering, design, development, testing, reviews, and documentation.

## Development Schedule

| Week | Focus                       | Major Deliverables                                                 |
| ---- | --------------------------- | ------------------------------------------------------------------ |
| 1    | Requirements Engineering    | Proposal, SRS, use cases                                           |
| 2    | Design & Quality Planning   | Architecture, database design, SQA and test plans                  |
| 3    | Development & Integration   | Working beta, unit and integration test results                    |
| 4    | Verification & Finalization | Final application, defect report, quality metrics and final report |

## Contribution Guidelines

We welcome structured collaboration from all project team members.

1. Create or select an appropriate issue before starting a task.
2. Create a separate feature or bugfix branch.
3. Follow the project's coding and commit conventions.
4. Test your changes before submitting them.
5. Open a Pull Request targeting `develop`.
6. Request a review from another team member.
7. Merge only after the required review and checks are complete.

See [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## Project Status

**Development Status:** Planning and Initial Setup

Features, test results, documentation, and project milestones will be updated as the project progresses.

## License

License and distribution terms will be finalized with the project team.

---

**Bookstore Management System (BMS)**  
Software Quality Engineering – CS304  
Department of Computer Science  
Sukkur IBA University | Fall 2026
