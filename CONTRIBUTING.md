# Contributing Guidelines

Thank you for contributing to the Bookstore Management System (BMS).

These guidelines help maintain code quality, consistency, and collaboration across the project team.

## 1. Branching Strategy

- `main`: Stable and submission-ready code.
- `develop`: Integration branch.
- `feature/*`: New functionality.
- `bugfix/*`: Bug fixes.
- `test/*`: Testing-related changes.
- `docs/*`: Documentation changes.

## 2. Commit Message Convention

Use the following prefixes:

| Prefix | Purpose |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `test:` | Add or update tests |
| `docs:` | Documentation |
| `refactor:` | Code restructuring |
| `style:` | Formatting changes |
| `chore:` | Maintenance tasks |

Examples:

- `feat: add inventory management`
- `fix: correct receipt calculation`
- `test: add product validation tests`
- `docs: update SRS`

## 3. Development Workflow

1. Check existing GitHub Issues.
2. Select an assigned task.
3. Create a branch from `develop`.
4. Implement the changes.
5. Run relevant tests.
6. Commit and push your changes.
7. Open a Pull Request targeting `develop`.
8. Request a review from another member.
9. Address review comments.
10. Merge after approval.

## 4. Code Quality

- Write readable and maintainable code.
- Follow language-specific conventions.
- Avoid unnecessary code duplication.
- Validate user inputs.
- Handle errors appropriately.
- Add tests for new functionality.
- Keep documentation updated.

## 5. Testing Requirements

New functionality should include relevant tests.

Backend testing uses JUnit 5 and Mockito.

Frontend testing uses Vitest and React Testing Library.

Defects must be documented, fixed, and retested.

## 6. Pull Request Guidelines

Every Pull Request should include:

- A clear title.
- A description of the changes.
- Related issue references.
- Testing performed.
- Screenshots when UI changes are involved.

## 7. Team Collaboration

All team members are encouraged to participate in code reviews, testing, documentation, and development.

The objective is to maintain software quality throughout the project lifecycle.