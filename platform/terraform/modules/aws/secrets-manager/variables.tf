variable "project" {
  description = "Project name used in secret paths and tags."
  type        = string
}

variable "environment" {
  description = "Environment name used in secret paths and tags."
  type        = string
}

variable "secret_names" {
  description = "Logical secret names created below the project/environment path."
  type        = set(string)

  validation {
    condition = alltrue([
      for name in var.secret_names : length(trimspace(name)) > 0
    ])

    error_message = "secret_names cannot contain empty values."
  }
}

variable "recovery_window_in_days" {
  description = "Deletion recovery period. Zero force-deletes immediately; otherwise AWS requires 7 to 30 days."
  type        = number
  default     = 7

  validation {
    condition = (
      var.recovery_window_in_days == 0 ||
      (
        var.recovery_window_in_days >= 7 &&
        var.recovery_window_in_days <= 30
      )
    )

    error_message = "recovery_window_in_days must be 0 or between 7 and 30."
  }
}

variable "additional_tags" {
  description = "Additional tags applied to Secrets Manager resources."
  type        = map(string)
  default     = {}
}