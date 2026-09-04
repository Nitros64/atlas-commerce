#!/usr/bin/env python3
"""Replace exactly one service tag in the canonical DEV image values file."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


IMAGE_VALUES = Path("platform/helm/atlas-commerce/values/images.dev.yaml")
PROMOTION_WORKFLOW = Path(".github/workflows/promote-dev-image.yml")
TAG_PATTERN = re.compile(r"sha-[0-9a-f]{40}-[0-9]+-[0-9]+")
SERVICE_REPOSITORIES = {
    "auth": "atlas-commerce/auth-service",
    "catalog": "atlas-commerce/catalog-service",
    "cart": "atlas-commerce/cart-service",
    "pricing": "atlas-commerce/pricing-service",
    "coupon": "atlas-commerce/coupon-service",
    "inventory": "atlas-commerce/inventory-service",
    "payment": "atlas-commerce/payment-service",
    "order": "atlas-commerce/order-service",
    "shipping": "atlas-commerce/shipping-service",
    "notification": "atlas-commerce/notification-service",
    "audit": "atlas-commerce/audit-service",
    "gateway": "atlas-commerce/api-gateway",
}


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", required=True, choices=SERVICE_REPOSITORIES)
    parser.add_argument("--image-tag", required=True)
    parser.add_argument("--file", type=Path, default=IMAGE_VALUES)
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate the requested one-line mutation without writing it",
    )
    return parser.parse_args()


def main() -> None:
    args = arguments()

    if not TAG_PATTERN.fullmatch(args.image_tag):
        raise SystemExit(
            "ERROR: image tag must match sha-<40 lowercase hex>-<run id>-<attempt>."
        )

    original = args.file.read_bytes()
    lines = original.decode("utf-8").splitlines(keepends=True)

    top_level_services = tuple(
        line.rstrip("\r\n")[:-1]
        for line in lines
        if re.fullmatch(r"[a-z]+:\r?\n?", line)
    )
    expected_services = tuple(SERVICE_REPOSITORIES)
    if top_level_services != expected_services:
        raise SystemExit(
            "ERROR: canonical DEV services or their order do not match the promotion contract."
        )

    workflow_lines = PROMOTION_WORKFLOW.read_text(encoding="utf-8").splitlines()
    service_input = workflow_lines.index("      service:")
    image_tag_input = workflow_lines.index("      image_tag:")
    workflow_services = tuple(
        match.group(1)
        for line in workflow_lines[service_input:image_tag_input]
        if (match := re.fullmatch(r"          - ([a-z]+)", line))
    )
    if workflow_services != expected_services:
        raise SystemExit(
            "ERROR: workflow service choices do not match the canonical DEV services."
        )

    requested_tag_index: int | None = None
    previous_tag: str | None = None

    for service, expected_repository in SERVICE_REPOSITORIES.items():
        start = next(
            index
            for index, line in enumerate(lines)
            if line.rstrip("\r\n") == f"{service}:"
        )
        end = next(
            (
                index
                for index in range(start + 1, len(lines))
                if re.fullmatch(r"[a-z]+:\r?\n?", lines[index])
            ),
            len(lines),
        )

        repository_matches = [
            (index, match.group(1))
            for index in range(start + 1, end)
            if (match := re.fullmatch(r"      repository: (\S+)\r?\n?", lines[index]))
        ]
        tag_matches = [
            (index, match.group(1))
            for index in range(start + 1, end)
            if (match := re.fullmatch(r"      tag: (\S+)\r?\n?", lines[index]))
        ]

        if (
            len(repository_matches) != 1
            or repository_matches[0][1] != expected_repository
        ):
            raise SystemExit(f"ERROR: {service} does not have its expected DEV repository.")
        if len(tag_matches) != 1:
            raise SystemExit(f"ERROR: {service} must have exactly one DEV image tag.")

        if service == args.service:
            requested_tag_index, previous_tag = tag_matches[0]

    if requested_tag_index is None or previous_tag is None:
        raise SystemExit(f"ERROR: service {args.service} was not found.")
    if previous_tag == args.image_tag:
        raise SystemExit(f"ERROR: {args.service} already uses {args.image_tag}.")

    line_ending = "\r\n" if lines[requested_tag_index].endswith("\r\n") else "\n"
    lines[requested_tag_index] = f"      tag: {args.image_tag}{line_ending}"
    updated = "".join(lines).encode("utf-8")

    changed_lines = sum(
        before != after
        for before, after in zip(original.splitlines(), updated.splitlines(), strict=True)
    )
    if changed_lines != 1:
        raise SystemExit(f"ERROR: expected one changed line, found {changed_lines}.")

    if not args.check:
        args.file.write_bytes(updated)

    mode = "Validated" if args.check else "Updated"
    print(f"{mode} {args.service}: {previous_tag} -> {args.image_tag}")


if __name__ == "__main__":
    main()
