CLAUDE — Backend notes

Purpose
- Guidance and quick commands for working with the backend when using Claude for code assistance or analysis.

Repository location
- Path: siliconbay-backend

Quick environment
- Java 11+ (or project-targeted JDK)
- Maven
- Tomcat (optional) for deployment

Common commands
- Build: `mvn -DskipTests package`
- Run tests: `mvn test`
- Deploy: copy generated WAR from `target/` to a Tomcat `webapps/` directory or use your IDE/Tomcat integration

Key files & folders
- Source: `src/main/java/com/hogger/`
- Resources: `src/main/resources/app.properties`, `src/main/resources/hibernate.cfg.xml`

Notes for Claude prompts
- When asking Claude about backend code include the module path (siliconbay-backend) and relevant file paths.
- Provide the exact error, stack trace, or failing test output when available.
- Indicate runtime environment (JDK version, Maven goals, Tomcat version) for configuration questions.

Security
- Do not paste secrets, private keys, or credentials when interacting with Claude.

Maintainers
- See project README or contact repository owners for more details.
