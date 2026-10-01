# Video Demo Script — Blockchain-Based Digital Evidence Integrity System

Target length: **3:00** (per EC8204 project brief). Times below are cumulative
targets, not hard cuts — rehearse once with a timer and adjust pacing rather
than rushing the narration.

## Pre-recording checklist

Do this *before* you hit record — nothing kills a demo video like a dead
service mid-sentence.

- [ ] `blockchain/` — `npm run node` running in its own terminal (leave visible or minimized, not closed)
- [ ] `blockchain/` — `npm run seed` already run once against that node (gives you 3 pre-seeded evidence items across 2 cases, so you're not starting from an empty dashboard)
- [ ] `backend/` — `mvn spring-boot:run` running (`http://localhost:8080`)
- [ ] `frontend/` — `npm run dev` running (`http://localhost:5173`)
- [ ] Browser tabs open and ready, in this order: Dashboard → Register → Verify
- [ ] Have **one** evidence file ready on your desktop to register live (e.g. `witness_statement.pdf`) and a **copy of it** you can quickly edit to simulate tampering (e.g. open in Notepad and add one character, save, that's your "tampered" version)
- [ ] Zoom browser to ~110–125% so text is readable on a recorded screen
- [ ] Close other notifications/apps that might pop up during recording

## Scene-by-scene script

### Scene 1 — Problem (0:00–0:25)

**Say:**
> "Digital evidence — PDFs, photos, forensic logs — can be modified after
> it's collected, whether intentionally or through mishandling. A regular
> database can't prove a file hasn't changed, because whoever controls that
> database can also edit the hash sitting next to it."

**Show:** Title slide or just talk to camera — no screen needed yet.

### Scene 2 — Solution, one sentence (0:25–0:45)

**Say:**
> "Our solution: compute a SHA-256 hash of the evidence file and commit
> that hash — never the file itself — to a blockchain. Once it's there, no
> single person can quietly alter the record without the mismatch becoming
> detectable."

**Show:** Architecture diagram (from `README.md` / `PROJECT_GUIDE.md`) — a
simple screenshot or the terminal `cat` of that block works fine.

### Scene 3 — Case Dashboard (0:45–1:05)

**Say:**
> "Here's our case dashboard. It's reading live from the blockchain, not a
> local database — these cases, evidence items, and the audit trail below
> are all pulled directly from on-chain data."

**Show:** `http://localhost:5173` — scroll past the metrics row, point at
one case card's evidence list, then the Audit Trail table at the bottom
(point out the transaction hash column specifically — that's the proof
this is really on a blockchain).

### Scene 4 — Register evidence live (1:05–1:45)

**Say:**
> "Let's register a new piece of evidence. I'll upload this witness
> statement under a case ID."

**Show:** Click the **Register** nav link → `http://localhost:8080/register.html`.
Fill in:
- Case ID: `CASE-DEMO-001`
- Investigator name: your name
- Evidence file: the prepared PDF
Click **Register on blockchain**.

**Say (as the REGISTERED banner appears):**
> "And there it is — evidence ID, the SHA-256 hash of the file, and the
> actual blockchain transaction hash. That hash is now permanent."

### Scene 5 — Verify unmodified (1:45–2:05)

**Say:**
> "Now let's prove it works. I'll click Verify, enter that same evidence
> ID, and re-upload the exact same file."

**Show:** Click **Verify** nav link. Enter the evidence ID from Scene 4,
upload the *same* file, submit.

**Say:**
> "VERIFIED — the hash I just computed matches the one on the blockchain
> exactly."

### Scene 6 — Tamper + re-verify (the payoff moment) (2:05–2:35)

**Say:**
> "Now watch what happens if the file is tampered with — even by a single
> character."

**Show:** Switch to the tampered copy of the file (the one you edited
beforehand). Submit it under the *same* evidence ID on the Verify page.

**Say (as the TAMPERED banner appears, pointing at the two hash values):**
> "TAMPERED. The stored hash and the computed hash are completely
> different — the system catches it instantly, because the original hash
> can't be altered once it's on-chain."

### Scene 7 — The caveat + closing (2:35–3:00)

**Say:**
> "One important nuance: this detects tampering *after the fact* — it
> doesn't stop someone from altering a file *before* it's first hashed.
> That's why our smart contract also restricts who can register evidence
> in the first place, to authorized investigators only. Combined, these
> two mechanisms — tamper detection and access control — give us a
> tamper-evident, access-controlled evidence trail built on Solidity,
> Spring Boot, and Web3j."

**Show:** Cut back to the dashboard or a closing slide with the team name /
module code.

## Optional: live access-control demo (adds ~30s, cuts something above if included)

The Investigators panel is currently hidden on the dashboard
(`frontend/index.html`, the `<section class="panel" hidden>` wrapping it).
To include a live "unauthorized account gets rejected, then succeeds after
authorization" demo instead of just mentioning access control verbally:

1. Remove the `hidden` attribute from that section in `frontend/index.html`
   (Vite hot-reloads automatically).
2. On **Register**, pick a non-admin account from the **Signing account**
   dropdown (e.g. "Account #2") and submit → shows a clear on-screen
   rejection: *"Not an authorized investigator."*
3. Go to the Dashboard's Investigators panel, paste/select that same
   address, click **Authorize**.
4. Go back to **Register** with the same account selected, submit again →
   succeeds.

This is a stronger demo of the access-control story but costs time — only
include it if you can trim Scene 3 or speak faster elsewhere to stay near
3:00.

## Optional: prove it's a real blockchain transaction (bonus, for Q&A or an extended cut)

Everything in Scene 4–6 could, in theory, be faked by a backend that just
writes to a regular database and shows a fabricated "transaction hash." If
a judge asks "how do we know this is really on a blockchain," here's how to
prove it live, independent of your own UI.

**Say:**
> "To prove this isn't just a database with a fake hash on screen, let's
> look the transaction up directly on the chain itself, outside our own
> application."

**Show:**
1. Copy the **Transaction hash** from the REGISTERED banner in Scene 4.
2. In a terminal, inside `blockchain/`, run:
   ```bash
   npx hardhat console --network localhost
   ```
3. Paste and run:
   ```javascript
   await ethers.provider.getTransactionReceipt("<paste the tx hash here>")
   ```
4. Point at the output: `blockNumber`, `status: 1` (success), `gasUsed`, and
   the `to` address matching the deployed `EvidenceRegistry` contract.

**Say:**
> "Block number, gas used, a confirmed status — this is a real, mined
> transaction on the chain, independently verifiable outside our own app."

This is a strong rebuttal to "is this actually blockchain or just a nice
UI," but it adds ~20–30 seconds — treat it as a Q&A backup or an extended
cut, not part of the core 3:00 timing above.

## If something goes wrong during recording

- **A page shows a connection error** → one of the three services died.
  Check the terminal it runs in; if the Hardhat node terminal was closed,
  all chain data is gone and you must re-run `npm run seed` before
  continuing (see `blockchain/README.md`).
- **Registration fails with a size error** → the file is over 50MB; use a
  smaller sample file for the demo.
- **Don't try to fix a broken take live on camera** — stop recording, fix
  the service, and re-record that scene. It's much faster than narrating
  a recovery.
