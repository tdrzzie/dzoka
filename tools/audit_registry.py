"""Validate source provenance and enforce the pre-implementation audit gate.

Uses only the Python standard library. Never downloads or executes upstream code.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = Path("docs/source-audit/source-audit.lock.json")
SHA = re.compile(r"[0-9a-f]{40}\Z")
DIGEST = re.compile(r"[0-9a-f]{64}\Z")
ID = re.compile(r"[a-z0-9]+(?:-[a-z0-9]+)*\Z")
STATES = {"PENDING", "IN_PROGRESS", "COMPLETE", "STALE"}
DISPOSITIONS = {"ADOPT", "ADAPT", "REFERENCE", "REJECT"}
SECTIONS = (
    "Repository identity", "Audited branch", "Exact commit SHA", "Audit date",
    "License", "Files inspected", "Runtime architecture", "Domain model",
    "Storage model", "AI and matching implementation", "Security model",
    "API design", "Event model", "Deployment model", "Test coverage",
    "Strengths", "Weaknesses", "Security concerns", "Scalability concerns",
    "Licensing consequences", "Reusable components", "Do not reuse",
    "Architecture lessons", "Disposition", "ADRs triggered", "Unresolved questions",
)


def local_path(root: Path, value: object) -> Path:
    """Reject escapes, including symlinks and Windows-style paths on POSIX CI."""
    if not isinstance(value, str) or not value or "\\" in value or ":" in value:
        raise ValueError("expected a repository-relative POSIX path")
    path = Path(value)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError("path escapes repository")
    resolved = (root / path).resolve()
    if not resolved.is_relative_to(root.resolve()):
        raise ValueError("path escapes repository")
    return resolved


def read_json(path: Path) -> object:
    def unique_pairs(pairs: list[tuple[str, object]]) -> dict:
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f"duplicate JSON key: {key}")
            result[key] = value
        return result
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_pairs)


def validate(root: Path, *, gate: bool = False) -> list[str]:
    errors: list[str] = []
    try:
        data = read_json(root / LOCK)
    except (OSError, ValueError) as exc:
        return [f"registry: {exc}"]
    if not isinstance(data, dict) or data.get("schemaVersion") != 1:
        return ["registry: expected schemaVersion 1 object"]
    sources = data.get("sources")
    required = data.get("requiredSources")
    if not isinstance(sources, list) or not isinstance(required, list) or not required:
        return ["registry: nonempty requiredSources and sources arrays required"]
    if any(not isinstance(x, str) or not ID.fullmatch(x) for x in required):
        return ["registry: invalid required source IDs"]
    if len(set(required)) != len(required):
        errors.append("registry: duplicate required source IDs")
    seen: set[str] = set()
    audit_paths: set[str] = set()
    for source in sources:
        if not isinstance(source, dict):
            errors.append("registry: source must be an object")
            continue
        sid = source.get("id")
        if not isinstance(sid, str) or not ID.fullmatch(sid):
            errors.append("registry: invalid source ID")
            continue
        if sid in seen:
            errors.append(f"{sid}: duplicate source ID")
        seen.add(sid)
        status = source.get("status")
        if not isinstance(status, str) or status not in STATES:
            errors.append(f"{sid}: invalid status")
        disposition = source.get("disposition")
        if disposition is not None and (not isinstance(disposition, str) or disposition not in DISPOSITIONS):
            errors.append(f"{sid}: invalid disposition")
        kind = source.get("kind")
        if not isinstance(kind, str) or kind not in {"repository", "standard"}:
            errors.append(f"{sid}: invalid source kind")
        repository = source.get("repository")
        if kind == "repository" and (not isinstance(repository, str) or not re.fullmatch(
            r"https://github\.com/[\w.-]+/[\w.-]+", repository
        )):
            errors.append(f"{sid}: invalid repository identity")
        revision = source.get("revision")
        if revision is not None and (not isinstance(revision, str) or not SHA.fullmatch(revision)):
            errors.append(f"{sid}: revision must be a full lowercase commit SHA")
        try:
            audit_name = source.get("audit")
            audit = local_path(root, audit_name)
            if not str(audit_name).startswith("docs/source-audit/") or audit.suffix != ".md":
                raise ValueError("audit must be Markdown under docs/source-audit")
            if audit_name in audit_paths:
                errors.append(f"{sid}: duplicate canonical audit path")
            audit_paths.add(audit_name)
            document = audit.read_text(encoding="utf-8")
        except (OSError, ValueError) as exc:
            errors.append(f"{sid}: audit: {exc}")
            continue
        for section in SECTIONS:
            if not re.search(rf"^## {re.escape(section)}\s*$", document, re.MULTILINE):
                errors.append(f"{sid}: missing audit section {section}")
        expected_status = f"Status: {status}"
        if not re.search(rf"^{re.escape(expected_status)}$", document, re.MULTILINE):
            errors.append(f"{sid}: document status differs from registry")
        if isinstance(revision, str) and revision not in document:
            errors.append(f"{sid}: document lacks pinned revision")
        if gate and sid in required and status != "COMPLETE":
            errors.append(f"{sid}: mandatory audit is {status}")
        evidence_name = source.get("evidence")
        evidence = None
        if evidence_name is not None:
            try:
                evidence_path = local_path(root, evidence_name)
                raw = evidence_path.read_bytes()
                if hashlib.sha256(raw).hexdigest() != source.get("evidenceSha256"):
                    errors.append(f"{sid}: evidence manifest digest mismatch")
                evidence = read_json(evidence_path)
                errors.extend(validate_evidence(sid, source, evidence))
            except (OSError, ValueError) as exc:
                errors.append(f"{sid}: evidence: {exc}")
        if status == "COMPLETE":
            if not isinstance(disposition, str) or disposition not in DISPOSITIONS:
                errors.append(f"{sid}: complete audit needs disposition")
            try:
                date.fromisoformat(source.get("auditedAt", ""))
            except (ValueError, TypeError):
                errors.append(f"{sid}: complete audit needs ISO audit date")
            if kind == "repository" and (not isinstance(revision, str) or not isinstance(source.get("branch"), str) or not source["branch"]):
                errors.append(f"{sid}: complete repository audit needs branch and revision")
            if kind == "standard" and not source.get("edition"):
                errors.append(f"{sid}: complete standard audit needs edition")
            if not source.get("license") or not source.get("licenseFinding"):
                errors.append(f"{sid}: complete audit needs explicit license finding")
            if not isinstance(evidence, dict) or not isinstance(evidence.get("files"), list):
                errors.append(f"{sid}: complete audit needs evidence manifest")
            else:
                roles = {f.get("role") for f in evidence["files"] if isinstance(f, dict) and isinstance(f.get("role"), str)}
                if kind == "repository" and "implementation" not in roles:
                    errors.append(f"{sid}: complete audit needs implementation evidence")
                for role in ("license", "tests", "deployment"):
                    limitations = evidence.get("limitations")
                    if role not in roles and (not isinstance(limitations, dict) or not limitations.get(role)):
                        errors.append(f"{sid}: {role} needs inspected evidence or explicit limitation")
    for missing in sorted(set(required) - seen):
        errors.append(f"{missing}: mandatory source missing from registry")
    # The separately versioned scope prevents accidental deletion from requiredSources.
    try:
        scope = read_json(root / "docs/source-audit/scope.json")
        if not isinstance(scope, dict) or set(scope.get("requiredSources", [])) != set(required):
            errors.append("registry: required sources differ from scope.json")
    except (OSError, ValueError, TypeError) as exc:
        errors.append(f"scope: {exc}")
    return errors


def validate_evidence(sid: str, source: dict, evidence: object) -> list[str]:
    errors = []
    if not isinstance(evidence, dict):
        return [f"{sid}: evidence manifest must be object"]
    if evidence.get("sourceId") != sid or evidence.get("revision") != source.get("revision"):
        errors.append(f"{sid}: evidence identity or revision differs from registry")
    if evidence.get("repository") != source.get("repository"):
        errors.append(f"{sid}: evidence repository differs from registry")
    files = evidence.get("files")
    if not isinstance(files, list) or not files:
        return errors + [f"{sid}: evidence files must be nonempty array"]
    seen = set()
    for entry in files:
        if not isinstance(entry, dict):
            errors.append(f"{sid}: invalid evidence file")
            continue
        path = entry.get("path")
        try:
            local_path(Path.cwd(), path)
        except ValueError:
            errors.append(f"{sid}: invalid upstream file path")
        if not isinstance(path, str):
            continue
        if path in seen:
            errors.append(f"{sid}: duplicate evidence file {path}")
        seen.add(path)
        if not isinstance(entry.get("sha256"), str) or not DIGEST.fullmatch(entry["sha256"]):
            errors.append(f"{sid}: invalid content digest for {path}")
        if source.get("kind") == "repository":
            expected = f"{source['repository']}/blob/{source['revision']}/{path}"
            if entry.get("url") != expected:
                errors.append(f"{sid}: evidence URL must use pinned commit for {path}")
            if not isinstance(entry.get("blobSha"), str) or not SHA.fullmatch(entry["blobSha"]):
                errors.append(f"{sid}: invalid Git blob digest for {path}")
        if not entry.get("observation"):
            errors.append(f"{sid}: evidence requires observation for {path}")
        ranges = entry.get("lines")
        line_count = entry.get("lineCount")
        if type(line_count) is not int or line_count < 1:
            errors.append(f"{sid}: invalid source line count for {path}")
        if not isinstance(ranges, list) or not ranges or any(
            not isinstance(r, list) or len(r) != 2
            or any(type(n) is not int for n in r) or r[0] < 1 or r[1] < r[0]
            or (type(line_count) is int and r[1] > line_count)
            for r in ranges
        ):
            errors.append(f"{sid}: invalid inspected line ranges for {path}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("check", "gate", "summary"))
    args = parser.parse_args()
    if args.command == "summary":
        data = read_json(ROOT / LOCK)
        for status in sorted(STATES):
            print(f"{status}: {sum(s['status'] == status for s in data['sources'])}")
        return 0
    errors = validate(ROOT, gate=args.command == "gate")
    if errors:
        for error in errors:
            print(error)
        print(f"FAIL: {len(errors)} issue(s)")
        return 1
    print("PASS: " + ("mandatory source audit gate" if args.command == "gate" else "registry integrity"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
