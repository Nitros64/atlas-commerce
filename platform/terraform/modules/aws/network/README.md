# AWS Network Module

Reusable Terraform module that creates the base network for Atlas Commerce.

## Resources

* VPC
* Internet Gateway
* Public subnets across multiple Availability Zones
* Private subnets across multiple Availability Zones
* Public and private route tables
* Optional single NAT Gateway

## NAT Gateway

`enable_nat_gateway` is disabled by default to avoid unnecessary AWS costs in development.

When enabled, the module creates:

* One Elastic IP
* One NAT Gateway in the first public subnet
* Default outbound routes for all private subnets

This single-NAT design is suitable for development. A future production version can use one NAT Gateway per Availability Zone for high availability.

## Example

```hcl
module "network" {
  source = "../../../modules/aws/network"

  project     = "atlas-commerce"
  environment = "dev"

  vpc_cidr = "10.0.0.0/16"

  availability_zone_count = 2

  public_subnet_cidrs = [
    "10.0.1.0/24",
    "10.0.2.0/24"
  ]

  private_subnet_cidrs = [
    "10.0.11.0/24",
    "10.0.12.0/24"
  ]

  enable_nat_gateway = false

  additional_tags = {
    Owner = "nitro"
  }
}
```

## Important Outputs

* `vpc_id`
* `public_subnet_ids`
* `private_subnet_ids`
* `availability_zones`
* `internet_gateway_id`
* `nat_gateway_id`



modules/aws/network/
├── variables.tf   # entradas: CIDR, AZs, NAT habilitado, tags...
├── locals.tf      # Calculated names and tags
├── data.tf        # AWS Availability Zones
├── vpc.tf         # VPC and Internet Gateway
├── subnets.tf     # Public and private subnets
├── routes.tf      # Route tables and associations
├── nat.tf         # Optional NAT Gateway, disabled by default
├── outputs.tf     # vpc_id, subnet IDs, route tables...