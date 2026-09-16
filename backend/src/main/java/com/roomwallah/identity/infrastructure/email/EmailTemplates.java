package com.roomwallah.identity.infrastructure.email;

public final class EmailTemplates {

    private EmailTemplates() {}

    public static String buildVerificationHtml(String fullName, String verificationLink, long expirationHours) {
        String safeName = (fullName != null && !fullName.isBlank()) ? escapeHtml(fullName) : "Valued Member";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Verify Your RoomWallah Account</title>
              <style>
                body { margin: 0; padding: 0; background-color: #060913; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
                .container { width: 100%%; max-width: 580px; margin: 30px auto; background: #0f172a; border: 1px solid #1e293b; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5); }
                .header { padding: 32px 36px; background: linear-gradient(135deg, #1e1b4b 0%%, #0f172a 100%%); border-bottom: 1px solid #1e293b; text-align: center; }
                .brand { display: inline-block; font-size: 22px; font-weight: 800; color: #ffffff; text-decoration: none; }
                .brand-accent { color: #818cf8; }
                .content { padding: 36px 36px 28px; }
                h1 { font-size: 20px; font-weight: 700; color: #ffffff; margin-top: 0; margin-bottom: 16px; }
                p { font-size: 14px; line-height: 1.6; color: #94a3b8; margin: 0 0 20px; }
                .btn-box { text-align: center; margin: 30px 0; }
                .btn { display: inline-block; padding: 14px 34px; background: #6366f1; color: #ffffff !important; text-decoration: none; border-radius: 12px; font-weight: 600; font-size: 15px; box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.4); }
                .link-box { background: #090e1a; border: 1px solid #1e293b; border-radius: 10px; padding: 14px; font-size: 12px; word-break: break-all; color: #818cf8; font-family: monospace; }
                .notice { font-size: 12px; color: #64748b; line-height: 1.5; border-top: 1px solid #1e293b; padding-top: 20px; margin-top: 28px; }
                .footer { padding: 20px 36px; background: #070c18; text-align: center; font-size: 11px; color: #475569; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #060913; padding: 20px 0;">
                <tr>
                  <td align="center">
                    <div class="container">
                      <div class="header">
                        <div class="brand">Room<span class="brand-accent">Wallah</span></div>
                      </div>
                      <div class="content">
                        <h1>Welcome to RoomWallah, %s!</h1>
                        <p>Thank you for joining India's next-generation zero-brokerage rental platform. Please verify your email address to activate your account and start exploring verified properties.</p>
                        <div class="btn-box">
                          <a href="%s" class="btn" target="_blank">Verify Email Address</a>
                        </div>
                        <p style="font-size: 12px; color: #64748b; margin-bottom: 8px;">Or copy and paste this verification link into your browser:</p>
                        <div class="link-box">%s</div>
                        <div class="notice">
                          <strong>Note:</strong> This verification link will expire in %d hours.<br>
                          If you did not register for a RoomWallah account, you can safely ignore this email.
                        </div>
                      </div>
                      <div class="footer">
                        &copy; 2026 RoomWallah. Zero Brokerage. Verified Homes. Direct Trust.
                      </div>
                    </div>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, verificationLink, verificationLink, expirationHours);
    }

    public static String buildVerificationText(String fullName, String verificationLink, long expirationHours) {
        String safeName = (fullName != null && !fullName.isBlank()) ? fullName : "Valued Member";
        return """
            Hello %s,

            Thank you for registering with RoomWallah! To activate your account and complete your sign-up, please verify your email address by visiting the link below:

            %s

            This verification link will expire in %d hours.

            If you did not create a RoomWallah account, please disregard this message.

            Warm regards,
            The RoomWallah Team
            https://www.roomwallah.co.in
            """.formatted(safeName, verificationLink, expirationHours);
    }

    public static String buildPasswordResetHtml(String fullName, String resetCode, long expirationMinutes) {
        String safeName = (fullName != null && !fullName.isBlank()) ? escapeHtml(fullName) : "Valued Member";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Reset Your RoomWallah Password</title>
              <style>
                body { margin: 0; padding: 0; background-color: #060913; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
                .container { width: 100%%; max-width: 580px; margin: 30px auto; background: #0f172a; border: 1px solid #1e293b; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5); }
                .header { padding: 32px 36px; background: linear-gradient(135deg, #1e1b4b 0%%, #0f172a 100%%); border-bottom: 1px solid #1e293b; text-align: center; }
                .brand { display: inline-block; font-size: 22px; font-weight: 800; color: #ffffff; text-decoration: none; }
                .brand-accent { color: #818cf8; }
                .content { padding: 36px 36px 28px; }
                h1 { font-size: 20px; font-weight: 700; color: #ffffff; margin-top: 0; margin-bottom: 16px; }
                p { font-size: 14px; line-height: 1.6; color: #94a3b8; margin: 0 0 20px; }
                .code-box { background: #090e1a; border: 1px solid #312e81; border-radius: 12px; padding: 18px; text-align: center; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #818cf8; margin: 24px 0; font-family: monospace; }
                .notice { font-size: 12px; color: #64748b; line-height: 1.5; border-top: 1px solid #1e293b; padding-top: 20px; margin-top: 28px; }
                .footer { padding: 20px 36px; background: #070c18; text-align: center; font-size: 11px; color: #475569; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #060913; padding: 20px 0;">
                <tr>
                  <td align="center">
                    <div class="container">
                      <div class="header">
                        <div class="brand">Room<span class="brand-accent">Wallah</span></div>
                      </div>
                      <div class="content">
                        <h1>Password Reset Request</h1>
                        <p>Hello %s, we received a request to reset your RoomWallah password. Enter the verification code below into the security portal to set a new password:</p>
                        <div class="code-box">%s</div>
                        <div class="notice">
                          <strong>Security Notice:</strong> This code will expire in %d minutes.<br>
                          Never share this code with anyone. If you did not request a password reset, please secure your account immediately.
                        </div>
                      </div>
                      <div class="footer">
                        &copy; 2026 RoomWallah. Zero Brokerage. Verified Homes. Direct Trust.
                      </div>
                    </div>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeName, resetCode, expirationMinutes);
    }

    public static String buildPasswordResetText(String fullName, String resetCode, long expirationMinutes) {
        String safeName = (fullName != null && !fullName.isBlank()) ? fullName : "Valued Member";
        return """
            Hello %s,

            We received a request to reset your RoomWallah password.

            Your password reset verification code is:
            %s

            This code will expire in %d minutes.

            If you did not request a password reset, please ignore this email or contact support.

            Warm regards,
            The RoomWallah Team
            https://www.roomwallah.co.in
            """.formatted(safeName, resetCode, expirationMinutes);
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
