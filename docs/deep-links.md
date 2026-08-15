# Deep links and Android App Links

Syncwich understands the public Mealie recipe and cookbook URLs below:

- `https://<mealie-host>/g/home/r/<recipe-slug>` opens the recipe.
- `https://<mealie-host>/g/home/c/<cookbook-slug>` opens the cookbook.

The current release manifest accepts these paths on the `nom.brkn.lol` HTTP and HTTPS hosts. The
HTTPS host can be associated with Syncwich automatically through Android App Links when it
publishes the release certificate association at:

```text
https://<mealie-host>/.well-known/assetlinks.json
```

The response must be an unauthenticated `200 OK` JSON response with the `application/json` content
type. For the `nom.brkn.lol` Mealie instance, the NixOS configuration serves this response from
the Mealie virtual host. Its contents are equivalent to:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "dev.pschmitt.syncwich",
      "sha256_cert_fingerprints": [
        "AE:03:2C:4E:C7:0B:D1:8E:29:48:3A:87:85:1A:F0:1A:DC:9F:D0:CF:48:E7:E8:4B:8A:54:7C:64:DF:47:21:A4"
      ]
    }
  }
]
```

This fingerprint belongs to the persistent CI/release signing key. Do not replace it with the
local debug keystore fingerprint: debug builds use the `.debug` application id and are not the
same Android application. If the release key changes, update both the NixOS asset-links response
and this documentation before publishing the next release.

For another Mealie host, serve the same JSON from that host and add its valid domain to the app's
URL intent filter in `AndroidManifest.xml` before building a new release. After deploying the
server configuration, verify the response without authentication:

```sh
curl -i https://nom.brkn.lol/.well-known/assetlinks.json
```

On a device with the release app installed, Android's association state can be inspected with:

```sh
adb shell pm get-app-links dev.pschmitt.syncwich
adb shell am start -a android.intent.action.VIEW \
  -d 'https://nom.brkn.lol/g/home/r/example-recipe'
```
