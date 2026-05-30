# Security Policy

> This is a learning/demo project that simulates a payment gateway. Do **not** use it to process
> real card data or real money without a full security review and PCI-DSS compliance work.

## Reporting a vulnerability

Please **do not** open a public issue for security problems. Instead, report privately via
GitHub Security Advisories ("Report a vulnerability") on this repository, or contact the maintainer
directly. We aim to acknowledge reports within a few business days.

## Handling secrets

- Real credentials (Stripe/Razorpay keys, JWT secret, DB passwords) must come from environment
  variables — see `.env.example`. Only `*.env.example` files are committed.
- Webhook payloads are signed with HMAC-SHA256; inbound provider webhooks are signature-verified.
- Rotate the `JWT_SECRET` and provider keys if they are ever exposed.
