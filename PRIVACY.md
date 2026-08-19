# Syncwich Privacy Policy

**Effective date:** 2026-08-14

Syncwich is an offline-first Android client for a self-hosted [Mealie](https://mealie.io)
instance. This privacy policy describes the information handled by the Syncwich app
(`dev.pschmitt.syncwich`).

## Information we collect

Syncwich does not collect, transmit, sell, or share personal information to us or any third
party. It has no analytics, advertising, tracking, or crash-reporting service of its own.

The only server Syncwich talks to is the Mealie server URL you configure yourself. Your Mealie
server's base URL and the API token you provide are stored locally on your device, encrypted at
rest. Syncwich does not retain your Mealie account password; a one-time password login is used
only to mint a long-lived API token. Recipes, meal plans,
shopping lists, and cookbooks fetched from your Mealie server are cached locally on your device
so the app works offline; that data never leaves your device except in requests to your own
configured server.

When OIDC is selected, Syncwich displays the Mealie OIDC sign-in flow in an in-app sign-in window.
The identity provider receives the credentials entered there; Syncwich receives only the callback
code, the temporary Mealie session cookie, and the resulting Mealie session token. The session
token is stored encrypted, refreshed through Mealie when needed, and the identity-provider
password and client secret are never stored by Syncwich. Cached data remains available while an
OIDC session needs reauthentication.

Removing the app or clearing its application data removes all locally stored information
according to Android's normal behavior.

## External links

The About/Settings screen may link to the Syncwich GitHub repository and this privacy policy in
an external browser. Those services may collect information under their own privacy policies;
Syncwich does not control or receive that information.

## Changes and contact

We may update this policy when the app's behavior changes. The current version is always
available in the Syncwich repository. Questions or privacy concerns can be reported through the
[GitHub issue tracker](https://github.com/pschmitt/syncwich/issues).
