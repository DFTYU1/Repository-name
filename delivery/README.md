# Internal APK transport — authorized encrypted delivery
Application source: 594ebd5bd2fd95af02b8b55a622df53d1a991de1, verified Run39 SUCCESS.

The offline encrypt/decrypt code was tested on synthetic data. It performs RSA-OAEP-SHA256 key wrapping, AES-256-CTR encryption, authenticated manifest HMAC, chunk digest checks and final APK SHA256 verification before atomic publication. It never runs APK input.

On 2026-10-03 the user explicitly authorized the previously described encrypted Actions transport. Only ciphertext and its authenticated manifest are uploaded; no plaintext APK, model, signing secret or private decryption key enters this repository. Application source remains pinned to the verified Run39 commit.

Current output is preparation code, NOT a delivered APK. Application binary built by Run39 remains unavailable from this workspace; diagnostics contain metadata only.

After the build, download/decrypt/verify the application, privately save it and provide a normal APK link; user does not run commands or copy a model. Build success is not physical-device acceptance.
