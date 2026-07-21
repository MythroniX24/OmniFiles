# Contributing to OmniFiles

We love your input! We want to make contributing as easy and transparent as possible.

## Development Process

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Pull Request Requirements

- Code must follow Kotlin coding conventions
- All tests must pass
- New features should include tests
- UI changes should include screenshots
- Update documentation as needed

## Code Style

- Follow Kotlin official style guide
- Use meaningful variable names
- Add KDoc comments for public APIs
- Keep functions small and focused
- Write unit tests for business logic

## Architecture Guidelines

- Follow Clean Architecture principles
- Keep UI layer thin (no business logic in Composables)
- Use repository pattern for data access
- Use ViewModels for UI state management
- Inject dependencies via Hilt

## Testing

- Write unit tests for ViewModels and Repositories
- Write integration tests for critical flows
- Test edge cases and error states

## Reporting Issues

Use the issue templates provided in `.github/ISSUE_TEMPLATE/`.
