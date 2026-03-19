# .well-known Directory

## Digital Asset Links (`assetlinks.json`)

This file enables Android App Links verification for the SafeAnot app.
Android uses it to confirm that our domain authorizes the app to handle URLs.

### How it works

When a user taps a link to our domain, Android fetches
`https://<domain>/.well-known/assetlinks.json` and checks whether the
installed app's signing certificate matches a fingerprint listed in the file.

### Updating the SHA-256 fingerprint

The fingerprint in `assetlinks.json` must match the certificate used to sign
the release APK/AAB. Update it whenever the signing key changes (e.g., key
rotation or new upload key).

**Get the fingerprint from a keystore:**

```bash
keytool -list -v -keystore your-release.keystore -alias your-alias \
  | grep "SHA256:"
```

**Get the fingerprint from Google Play (if using Play App Signing):**

1. Open Google Play Console.
2. Go to **Setup > App signing**.
3. Copy the **SHA-256 certificate fingerprint** from the
   "App signing key certificate" section.

**Apply the update:**

1. Replace the value in `sha256_cert_fingerprints` inside `assetlinks.json`.
2. Keep the colon-separated uppercase hex format
   (e.g., `AA:BB:CC:...`).
3. Deploy the website so the new file is live.
4. Verify with: `https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://<domain>&relation=delegate_permission/common.handle_all_urls`

### Important notes

- **Production must contain only release fingerprints.** Never commit debug
  fingerprints to the main branch.
- The file must be served with `Content-Type: application/json`.
- Cloudflare Pages serves static files from `.well-known/` without any
  special redirect configuration.
