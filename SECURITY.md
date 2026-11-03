# Security Policy

j-lawyer.org is used by law firms to manage confidential client data. We take
security issues seriously and appreciate responsible disclosure.

## Supported Versions

Security fixes are provided for the **latest release** only. Fixes are not
backported to older releases; please upgrade to the current version.

| Version              | Supported          |
| -------------------- | ------------------ |
| latest release       | :white_check_mark: |
| older releases       | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
pull requests, discussions or the forum.**

Report them privately via GitHub instead:

1. Open the [Security tab](https://github.com/jlawyerorg/j-lawyer-org/security)
   of this repository.
2. Click **"Report a vulnerability"**.
3. Fill in the form.

The report is visible only to you and to the project maintainers.

If you cannot use GitHub, use the contact information on
[www.j-lawyer.org](https://www.j-lawyer.org/) and ask for a private channel.
Do not include vulnerability details in that first message.

### What to include

To help us triage quickly, please provide as much of the following as possible:

- affected component (desktop client, server/EJB, REST API `j-lawyer-io`,
  backup manager, Docker setup, ...)
- affected version(s)
- type of issue (e.g. authentication/authorization bypass, injection,
  path traversal, information disclosure, insecure cryptography)
- step-by-step instructions to reproduce, ideally with a proof of concept
- the impact you see, and how an attacker might exploit the issue

## What to Expect

- **Acknowledgement** within 5 working days.
- **Initial assessment** (confirmed / not reproducible / not a security issue)
  within 14 days.
- We keep you informed while we develop the fix, and may ask you to verify it.
- Once a fixed release is available, we publish a GitHub Security Advisory and
  request a CVE where appropriate. Unless you prefer to stay anonymous, we
  credit you in the advisory.

Please give us a reasonable amount of time to release a fix before you disclose
the issue publicly. We aim to resolve confirmed issues within 90 days and will
coordinate the disclosure date with you.

## Scope

In scope:

- the source code in this repository (client, server, REST API, supporting modules)
- the official Docker images and setup scripts published by the project

Out of scope:

- vulnerabilities in third-party dependencies that are already publicly known
  and have no specific impact on j-lawyer.org (Dependabot already tracks these)
- issues that require a fully compromised server, or physical access to an
  unlocked client machine
- insecure configuration of individual installations, e.g. unchanged default
  credentials, unencrypted connections or databases exposed to the internet
- denial-of-service through excessive traffic or resource exhaustion
- social engineering

## Safe Harbor

We will not take legal action against researchers who act in good faith:
test only against installations you own or are authorized to test, do not
access, modify or keep data of others, and report findings privately as
described above.
