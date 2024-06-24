﻿using System.ComponentModel.DataAnnotations;

namespace TheWebApiServer.Requests
{
    public class ForgotPasswordRequest
    {
        [Required]
        [EmailAddress]
        public String Email { get; set; }
    }
}