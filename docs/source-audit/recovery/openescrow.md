# Source audit: openescrow

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/omslice/OpenEscrow

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

abebc3b6543a975e7ec5943e9bdd53cea6dc168a

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [foundry.toml](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/foundry.toml), lines 1-20; role: deployment.
- [LICENSE](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/LICENSE), lines 1-21; role: license.
- [contracts/OpenEscrow.sol](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/contracts/OpenEscrow.sol), lines 1-20, 37-87, 153-162, 179-188, 300-309, 516-590, 737-804, 840-905; role: implementation.
- [contracts/libraries/YieldEscrowAccounting.sol](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/contracts/libraries/YieldEscrowAccounting.sol), lines 1-45; role: implementation.
- [test/Claims.t.sol](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/test/Claims.t.sol), lines 1-65; role: tests.
- [test/Lifecycle.t.sol](https://github.com/omslice/OpenEscrow/blob/abebc3b6543a975e7ec5943e9bdd53cea6dc168a/test/Lifecycle.t.sol), lines 1-65, 167-176; role: tests.

## Runtime architecture

Solidity escrow contracts with Foundry tests; this is a rental-deposit domain, not a physical-property recovery payment service.

## Domain model

Landlord, tenant participants, arbiter, agreement phase, deposit/contributions, claim windows, disputes, evidence hashes and withdrawable/locked/withdrawn balances.

## Storage model

On-chain agreement mappings, monetary allocation fields and emitted lifecycle/evidence events; off-chain media referenced by hashes/URIs is outside the contract trust boundary.

## AI and matching implementation

Deterministic state/amount/role/deadline checks, not AI. OpenEscrow funding checks exact token balance deltas; claims require authorized role and valid phase/window; closed-state withdrawals use checks-effects-interactions/nonReentrant.

## Security model

msg.sender, phase and deadline checks gate mutating transitions. SafeERC20 and ReentrancyGuard are imported. This static selected-function review is not a smart-contract security audit or permission to hold funds.

## API design

Contract entry points cover agreement creation, funding, claims, dispute resolution/timeouts and pull withdrawals. Contract tenant means rental party, not Dzoka application tenant isolation.

## Event model

Funding/claim/evidence/dispute/withdrawal events carry stable agreement IDs. Network/indexer finality and duplicate event reconciliation still need off-chain controls.

## Deployment model

foundry.toml pins solc 0.8.26, optimizer/via_ir and fuzz/invariant parameters. Deployment addresses, keys, chain guarantees and external token behavior were not validated.

## Test coverage

Inspected real lifecycle/claim tests assert unauthorized/phase/deadline rejection, exact boundaries, duplicate withdrawal prevention and balance conservation. Tests were not run and no contract certification is claimed.

## Strengths

Explicit terminal states, fixed dispute deadlines, role gates, conservation assertions and separating fund allocation from withdrawal.

## Weaknesses

Rental-specific parties/deadlines and blockchain dependencies do not directly implement voluntary recovery rewards. YieldEscrowAccounting says it is not wired into the deployed MVP; its presence does not prove live yield functionality.

## Security concerns

Do not expose verification answers or identity evidence in public event/URI payloads. Payment custody, reversal, dispute and legal obligations need separate decisions and provider contracts.

## Scalability concerns

Blockchain gas/finality and token integrations add costs/operational failure modes; they are unjustified defaults for the requested recovery network.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Reference deterministic reward/escrow phase transitions, idempotent terminal actions and ledger conservation; implement normal payment-rail adapters and reconciliation first.

## Do not reuse

Do not require blockchain, copy rental defaults as recovery policy, claim on-chain identity proves ownership or infer deployed capability from an unused accounting library.

## Architecture lessons

Rewards need independently auditable reserved/authorized/released/refunded/disputed states, deterministic release conditions and reconciliation after confirmed recovery.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: deterministic reward ledger phases, disputes, reconciliation and normal payment-provider boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Full contract coverage, deployed-bytecode provenance, chain/token risks and financial/legal deployment constraints remain outside this review; no runtime blockchain adoption is proposed.
