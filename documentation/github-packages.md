![Sweden Connect](https://raw.githubusercontent.com/swedenconnect/technical-framework/master/img/sweden-connect.png)

# Building with Libraries from GitHub Packages

This application uses the Sweden Connect CA libraries `ca-service-base`, `cmc` and `ca-engine`. The versions it uses up to release 2.0.1 of the application are on Maven Central. Later versions of the libraries are published only to the GitHub Packages Maven registry of the Sweden Connect organization.

The `pom.xml` lists that registry as the repository with the id `github`, after Maven Central:

```
https://maven.pkg.github.com/swedenconnect/*
```

As long as every dependency is found on Maven Central, the build works as before, without any extra setup. Once the POM uses a library version that is only on GitHub Packages, every build needs a GitHub token, as described below.

-----

## Every build needs a token

GitHub Packages does not allow anonymous downloads, also not for public packages. Every build that downloads from it must authenticate, a build from your terminal as well as one in CI.

Each developer, and each CI setup, uses its own token. No shared token is handed out.

## Create a token

Use a **classic** personal access token with the `read:packages` scope only. The GitHub Packages Maven registry accepts only classic tokens.

1. On GitHub, go to **Settings**, **Developer settings**, **Personal access tokens**, **Tokens (classic)**.
2. Choose **Generate new token (classic)**.
3. Give it a name, such as `maven read packages`, and an expiration date.
4. Select the `read:packages` scope, and nothing else.
5. Generate the token and copy it. GitHub shows it once.

## Add the server to settings.xml

Maven reads credentials from `~/.m2/settings.xml`. Add a server with the id `github`, the same id as the repository in the POM:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_USERNAME}</username>
      <password>${env.GITHUB_PACKAGES_TOKEN}</password>
    </server>
  </servers>
</settings>
```

```bash
export GITHUB_USERNAME=your-github-username
export GITHUB_PACKAGES_TOKEN=your-token
```

The values may also be written directly in the file instead of being read from the environment.

## In GitHub Actions

`actions/setup-java` can write the server entry, with the values read from the environment:

```yaml
- uses: actions/setup-java@v6
  with:
    distribution: 'temurin'
    java-version: '21'
    server-id: github
    server-username: GITHUB_PACKAGES_USERNAME
    server-password: GITHUB_PACKAGES_TOKEN

- name: Build
  env:
    GITHUB_PACKAGES_USERNAME: ${{ github.actor }}
    GITHUB_PACKAGES_TOKEN: ${{ secrets.SC_PACKAGES_TOKEN }}
  run: mvn --no-transfer-progress clean verify
```

`server-username` and `server-password` name the environment variables to read, not the values.

## GitHub documentation

- [Working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

-----

Copyright &copy; 2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](https://www.digg.se). Licensed under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
