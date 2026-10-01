# Blockchain-Based Digital Evidence Integrity System

**EC8204 — Blockchain and Cyber Security**, Group Project
University of Ruhuna, Faculty of Engineering, Dept. of Electrical &
Information Engineering. Submission deadline: **31/09/2026** (3-minute
presentation, ELMS submission).

## Problem

Digital evidence (PDFs, images, logs, forensic exports) collected for a
criminal case can be modified after collection — intentionally or through
mishandling — which undermines its integrity and admissibility. A regular
database can't prove a file hasn't changed, because whoever controls the
database can also edit any hash stored alongside it.

## Solution

On upload, the system computes a **SHA-256 hash** of the evidence file and
commits that hash — plus case ID, uploader, and timestamp — to a
blockchain. The file itself is **never** stored on-chain, only its hash.
Once committed, no single party can silently alter that record without the
mismatch becoming detectable the next time the evidence is re-verified.

**Important framing:** blockchain here proves tampering *after the fact*
— it does not *prevent* someone tampering with the original file before it
is first hashed. That's why the access-control piece (only authorized
investigators can register evidence) matters alongside the hashing itself.

## Live demo script (the 3-minute pitch)

1. Investigator uploads `evidence.pdf` under a case.
2. Backend computes its SHA-256 hash.
3. Hash + metadata is committed to the blockchain (transaction hash returned).
4. The file is modified — even a single byte.
5. System recomputes the hash of the current file.
6. System compares it against the on-chain record → reports **TAMPERED**
   (mismatch) vs **VERIFIED** (match).

## Architecture

```
┌─────────────────────┐      REST (JSON)      ┌──────────────────────────┐
│   Frontend            │ ───────────────────▶ │   Spring Boot Backend     │
│   (Thymeleaf/React)   │ ◀─────────────────── │   (evidence-service)      │
└─────────────────────┘                        │                          │
                                                │ - File upload handling   │
                                                │ - SHA-256 hashing        │
                                                │ - Evidence metadata DB   │
                                                │ - Blockchain client      │
                                                └───────────┬──────────────┘
                                                            │ Web3j (JSON-RPC)
                                                            ▼
                                                ┌──────────────────────────┐
                                                │  Local Ethereum node     │
                                                │  (Hardhat)               │
                                                │  EvidenceRegistry.sol    │
                                                └──────────────────────────┘
```

Full architecture rationale, module breakdown, and build order:
see **[PROJECT_GUIDE.md](PROJECT_GUIDE.md)**.

## Repository structure

```
.
├── README.md                    # this file
├── PROJECT_GUIDE.md              # full architecture, module breakdown, build order
├── MEMBER1_BLOCKCHAIN.md         # Member 1's task: evidence registration (write path)
├── MEMBER2_BACKEND.md            # Member 2's task: evidence verification (read/tamper-check path)
├── MEMBER3_FRONTEND_DEMO.md      # Member 3's task: case mgmt, access control, audit trail, demo
├── EC8204_Aug26_Project Description.pdf   # official assignment brief
├── blockchain/                   # Hardhat project — smart contract layer
│   ├── contracts/EvidenceRegistry.sol
│   ├── test/
│   ├── scripts/seed.js
│   ├── ignition/modules/
│   └── README.md                 # blockchain-layer setup & usage
├── backend/                       # Spring Boot project — REST API + Web3j chain client
│   └── src/main/resources/static/
│       ├── register.html          # write-path page: hash + register evidence on-chain
│       └── verify.html            # tamper-check page: VERIFIED / TAMPERED
└── frontend/                      # Vite case dashboard — cases, audit trail, investigators
```

## Team split

Each member owns a full **vertical slice** — their own smart contract
function(s) plus the backend endpoint and frontend page that use them —
rather than a horizontal frontend/backend split. This way everyone writes
real Solidity, not just one person. All three slices share the same
`blockchain/`, `backend/`, and `frontend/` folders.

| Member | Slice | Details |
|---|---|---|
| 1 | Evidence registration (write path) — `registerEvidence` | [MEMBER1_BLOCKCHAIN.md](MEMBER1_BLOCKCHAIN.md) |
| 2 | Evidence verification (tamper detection) — `getEvidence` / `verifyHash` | [MEMBER2_BACKEND.md](MEMBER2_BACKEND.md) |
| 3 | Case management, access control, audit trail + demo/presentation | [MEMBER3_FRONTEND_DEMO.md](MEMBER3_FRONTEND_DEMO.md) |

## Current status

- [x] `blockchain/` — Hardhat project set up (Hardhat 2, Solidity 0.8.28)
- [x] `EvidenceRegistry.sol` — struct, storage mapping, `registerEvidence`,
      `getEvidence`, `verifyHash`, `getEvidenceByCase`, `addInvestigator` +
      `onlyInvestigator` access control
- [x] 10 passing tests: data correctness, event emission, ID auto-incrementing,
      hash verification, tamper (hash-mismatch) detection, access control,
      case indexing
- [x] Deploys to a local chain; `npm run node` / `npm run seed` for
      one-command local setup with demo data
- [x] `backend/` Spring Boot project — full REST API (register, verify,
      cases, audit trail, investigators) wired to the chain via Web3j,
      including real transaction signing (not just reads)
- [x] `frontend/` Vite case dashboard — cases, audit trail, investigator
      authorization, all reading live from chain
- [x] `register.html` / `verify.html` write-path and verify-path pages,
      sharing one visual theme and cross-linked nav with the dashboard
- [x] Demo account switcher — lets a presenter pick which test account signs
      a registration, to live-demo an unauthorized account being rejected
      and then succeeding after authorization
- [ ] End-to-end demo rehearsal + slide deck

## Getting started (full system)

Four terminals, started in this order.

**1. Local blockchain** — start it and leave it running:
```bash
cd blockchain
npm install          # first time only
npm run node
```

**2. Deploy + seed demo evidence** — new terminal, once the node is up:
```bash
cd blockchain
npm run seed
```
Prints the deployed contract address. The backend's `application.yml` already
defaults to the address a fresh local chain produces, so you normally don't
need to change anything — just double-check they match if something seems off.

**3. Backend** — new terminal:
```bash
cd backend
mvn spring-boot:run
```
No `mvn` on your PATH? Run the `EvidenceApplication` class directly from your
IDE instead (IntelliJ: right-click it → Run). Starts on `http://localhost:8080`.

**4. Frontend dashboard** — new terminal:
```bash
cd frontend
npm install          # first time only
npm run dev
```
Starts on `http://localhost:5173`.

### Once everything is running

| Page | URL | What it's for |
|---|---|---|
| Register Evidence | `http://localhost:8080/register.html` | Upload a file, pick a signing account, commit its hash on-chain |
| Verify Evidence | `http://localhost:8080/verify.html` | Re-upload a file and check it against the on-chain hash — VERIFIED / TAMPERED |
| Case Dashboard | `http://localhost:5173` | Case/evidence listing, audit trail, investigator authorization |

All three pages link to each other through the top nav bar.

### The local chain resets on every restart

`npx hardhat node` keeps all state in memory only — closing that terminal (or
restarting your machine) wipes every registered evidence item and
investigator authorization. Just repeat steps 1–2 to get a fresh, seeded
chain again. Full details: **[blockchain/README.md](blockchain/README.md)**.

## Tech stack

- **Blockchain:** Solidity `^0.8.28`, Hardhat 2, ethers.js v6
- **Backend:** Java 21, Spring Boot 3, Web3j (JSON-RPC + transaction signing)
- **Frontend:** plain JS + Vite (case dashboard); static HTML/JS pages served
  by the backend for register/verify
- **Database:** H2 (file-based) for evidence metadata cache — chain remains
  the source of truth

## License

Apache License 2.0 — see [LICENSE](LICENSE).
