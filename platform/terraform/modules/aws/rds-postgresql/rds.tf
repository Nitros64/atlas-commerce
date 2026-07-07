# Create one private PostgreSQL RDS instance for the development environment.
resource "aws_db_instance" "main" {
  # Use the stable identifier provided by the live environment.
  identifier = var.identifier

  # Configure PostgreSQL and pin its exact engine version.
  engine         = "postgres"
  engine_version = var.engine_version

  # Use the requested RDS instance size.
  instance_class = var.instance_class

  # Allocate encrypted gp3 storage for database files.
  allocated_storage = var.allocated_storage_gib
  storage_type      = "gp3"
  storage_encrypted = true

  # Do not create a default database here.
  # Atlas databases will be created later by the database bootstrap process.
  db_name = null

  # Define the RDS master user without placing its password in Terraform.
  username                    = var.master_username
  manage_master_user_password = true

  # Keep RDS private inside the VPC.
  publicly_accessible = false

  # Place RDS in the private subnets created by the subnet group.
  db_subnet_group_name = aws_db_subnet_group.main.name

  # Attach only the database security group.
  vpc_security_group_ids = [
    var.database_security_group_id
  ]

  # Configure availability and recovery behavior.
  multi_az               = var.multi_az
  backup_retention_period = var.backup_retention_period_days
  copy_tags_to_snapshot   = true

  # Keep dev destruction inexpensive and explicit.
  deletion_protection      = var.deletion_protection
  skip_final_snapshot      = var.skip_final_snapshot
  final_snapshot_identifier = var.skip_final_snapshot ? null : "${var.identifier}-final"

  # Allow minor PostgreSQL patch upgrades, but never automatic major upgrades.
  auto_minor_version_upgrade = true
  allow_major_version_upgrade = false

  tags = merge(
    local.common_tags,
    {
      Name = var.identifier
      Role = "postgresql"
    }
  )
}