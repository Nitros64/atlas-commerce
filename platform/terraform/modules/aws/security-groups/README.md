# AWS Security Groups Module

Reusable Terraform module that creates baseline security groups for Atlas Commerce.

## Security Groups Created

* `alb_security_group_id`

  * Reserved for the future public Application Load Balancer.

* `eks_nodes_security_group_id`

  * Reserved for future EKS worker nodes and workloads.

* `rds_security_group_id`

  * Reserved for future PostgreSQL RDS instances.

* `redis_security_group_id`

  * Reserved for future ElastiCache Redis clusters.

## Current Ingress Rules

The module currently exposes only the public application entry point:

```text
Internet -> ALB :80
Internet -> ALB :443
```

The allowed CIDRs are configured through:

```hcl
alb_ingress_cidrs
```

The default development value is:

```hcl
["0.0.0.0/0"]
```

This is appropriate for a public application endpoint.

## Current Egress Behavior

AWS creates new security groups with a default outbound rule that allows all traffic.

At this stage, Terraform does not manage explicit egress rules yet.

Future phases will replace broad outbound access with explicit rules for the real architecture, such as:

```text
ALB -> EKS ingress targets
EKS -> RDS :5432
EKS -> Redis :6379
EKS -> required AWS services
```
## Future Security Flow

```text
Internet
   |
   | TCP 80 / 443
   v
ALB Security Group
   |
   | Target port to be defined with EKS ingress architecture
   v
EKS Security Group
   |
   | TCP 5432
   v
RDS Security Group

EKS Security Group
   |
   | TCP 6379
   v
Redis Security Group
```

The EKS, RDS, and Redis groups currently have no ingress rules because they are not attached to real AWS services yet.

## Rule Management

Ingress and egress rules are managed as separate Terraform resources:

```hcl
aws_vpc_security_group_ingress_rule
aws_vpc_security_group_egress_rule
```

This keeps each allowed connection explicit and easier to review.

## Inputs

| Name                | Description                                    |
| ------------------- | ---------------------------------------------- |
| `vpc_id`            | VPC where security groups are created          |
| `project`           | Project name used for names and tags           |
| `environment`       | Environment name such as dev, staging, or prod |
| `alb_ingress_cidrs` | CIDRs allowed to reach the future ALB          |
| `additional_tags`   | Extra tags applied to the security groups      |

## Outputs

| Name                          | Description                         |
| ----------------------------- | ----------------------------------- |
| `alb_security_group_id`       | ID of the public ALB security group |
| `eks_nodes_security_group_id` | ID of the EKS nodes security group  |
| `rds_security_group_id`       | ID of the PostgreSQL security group |
| `redis_security_group_id`     | ID of the Redis security group      |
