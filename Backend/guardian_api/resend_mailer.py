"""
Transactional Email & Delivery Management Engine for GuardianAI
Dispatches HTML OTP codes, SOS Emergency Alerts, and Diagnostic Test Emails via Gmail SMTP.
Logs all transactional emails into the EmailLog database table for full delivery visibility.
"""
import os
import smtplib
import logging
import threading
from django.conf import settings
from django.utils import timezone
from django.core.mail import EmailMultiAlternatives
from django.utils.html import strip_tags

logger = logging.getLogger(__name__)


def _send_email_task(to_email, subject, html_content, from_email=None, email_type="otp", metadata=None):
    """
    Worker function to dispatch transactional emails safely via Gmail SMTP and record delivery status.
    """
    from .models import EmailLog

    from_email = from_email or getattr(settings, 'DEFAULT_FROM_EMAIL', 'GuardianAI Safety <satyakiran294@gmail.com>')
    recipient_str = to_email if isinstance(to_email, str) else ", ".join(to_email)

    email_log = None
    try:
        email_log = EmailLog.objects.create(
            recipient=recipient_str,
            subject=subject[:255],
            email_type=email_type,
            status='queued',
            metadata=metadata or {}
        )
    except Exception as log_init_err:
        logger.warning(f"⚠️ Could not create initial EmailLog: {log_init_err}")

    try:
        recipients = [to_email] if isinstance(to_email, str) else to_email
        text_content = strip_tags(html_content)
        msg = EmailMultiAlternatives(subject, text_content, from_email, recipients)
        msg.attach_alternative(html_content, "text/html")
        sent = msg.send(fail_silently=False)

        if sent and sent > 0:
            if email_log:
                email_log.status = 'sent'
                email_log.delivered_at = timezone.now()
                email_log.save(update_fields=['status', 'delivered_at'])
            logger.info(f"📧 [Gmail SMTP] Email dispatched successfully to {recipient_str}")
            return True
        else:
            raise Exception("SMTP returned 0 delivered messages")

    except Exception as smtp_err:
        err_msg = str(smtp_err)
        if email_log:
            email_log.status = 'failed'
            email_log.error_message = err_msg[:500]
            email_log.save(update_fields=['status', 'error_message'])
        logger.warning(f"⚠️ [Gmail SMTP] Failed to send email to {recipient_str}: {err_msg}")
        return False


def send_email_async(to_email, subject, html_content, from_email=None, email_type="otp", metadata=None):
    """
    Non-blocking async dispatch of emails via background thread to guarantee zero HTTP latency.
    """
    try:
        t = threading.Thread(
            target=_send_email_task,
            args=(to_email, subject, html_content, from_email, email_type, metadata),
            daemon=True
        )
        t.start()
        return True
    except Exception as e:
        logger.error(f"❌ Failed to spawn email thread: {e}")
        return False


# Backwards compatibility alias
send_email_via_resend = send_email_async


def send_otp_email(to_email, otp_code, purpose="Authentication"):
    """
    Sends a styled OTP verification email via Gmail SMTP and tracks delivery.
    """
    subject = f"🛡️ {otp_code} is your GuardianAI Verification Code"

    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b1120; color: #f8fafc; margin: 0; padding: 24px; }}
        .card {{ max-width: 480px; margin: 0 auto; background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }}
        .header {{ text-align: center; margin-bottom: 24px; }}
        .logo {{ font-size: 36px; }}
        .title {{ font-size: 22px; font-weight: 700; color: #ffffff; margin: 8px 0 4px; }}
        .subtitle {{ font-size: 14px; color: #94a3b8; }}
        .otp-box {{ background: linear-gradient(135deg, rgba(139,92,246,0.15), rgba(236,72,153,0.15)); border: 2px dashed #8b5cf6; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0; }}
        .otp-code {{ font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #c084fc; font-family: monospace; margin: 0; }}
        .note {{ font-size: 13px; color: #94a3b8; line-height: 1.5; text-align: center; }}
        .footer {{ margin-top: 28px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #334155; padding-top: 16px; }}
      </style>
    </head>
    <body>
      <div class="card">
        <div class="header">
          <div class="logo">🛡️</div>
          <div class="title">GuardianAI Security</div>
          <div class="subtitle">Purpose: {purpose.capitalize()}</div>
        </div>
        <p style="color: #cbd5e1; font-size: 14px;">Please use the following 6-digit passcode to verify your GuardianAI identity. This code is valid for <strong>10 minutes</strong>.</p>
        <div class="otp-box">
          <div class="otp-code">{otp_code}</div>
        </div>
        <p class="note">If you did not request this verification code, please ignore this email or notify your guardian team immediately.</p>
        <div class="footer">
          &copy; 2026 GuardianAI Incident Command System • SheGuard Safety Network
        </div>
      </div>
    </body>
    </html>
    """
    return send_email_async(
        to_email=to_email,
        subject=subject,
        html_content=html,
        email_type="otp",
        metadata={"otp_code": otp_code, "purpose": purpose}
    )


def send_sos_alert_email(to_email, user_name, user_phone, address, lat, lng, trigger_source="SOS Button"):
    """
    Sends a high-priority emergency SOS notification email to emergency contacts.
    """
    subject = f"🚨 CRITICAL SOS ALERT: {user_name} needs urgent assistance!"
    maps_url = f"https://www.google.com/maps/search/?api=1&query={lat},{lng}"

    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #450a0a; color: #fef2f2; margin: 0; padding: 24px; }}
        .card {{ max-width: 520px; margin: 0 auto; background: #1c1917; border: 2px solid #ef4444; border-radius: 16px; padding: 32px; box-shadow: 0 10px 30px rgba(239,68,68,0.3); }}
        .badge {{ background: #dc2626; color: #fff; display: inline-block; padding: 6px 14px; border-radius: 20px; font-weight: 700; font-size: 13px; text-transform: uppercase; letter-spacing: 1px; }}
        .title {{ font-size: 24px; font-weight: 800; color: #fee2e2; margin: 16px 0 8px; }}
        .info-row {{ background: #292524; border-radius: 8px; padding: 12px 16px; margin-bottom: 8px; font-size: 14px; }}
        .btn {{ display: block; background: #ef4444; color: #ffffff !important; text-align: center; text-decoration: none; padding: 14px 20px; border-radius: 10px; font-weight: 700; font-size: 16px; margin: 24px 0 16px; }}
      </style>
    </head>
    <body>
      <div class="card">
        <span class="badge">🚨 Emergency Incident</span>
        <div class="title">SOS Triggered by {user_name}</div>
        <p style="color: #fca5a5; font-size: 14px;">An emergency panic signal has been dispatched through GuardianAI.</p>

        <div class="info-row"><strong>👤 Citizen:</strong> {user_name} ({user_phone})</div>
        <div class="info-row"><strong>📍 Location:</strong> {address}</div>
        <div class="info-row"><strong>⚡ Trigger Type:</strong> {trigger_source.capitalize()}</div>
        <div class="info-row"><strong>🌐 Coordinates:</strong> {lat}, {lng}</div>

        <a href="{maps_url}" class="btn" target="_blank">📍 Open Live Location in Google Maps</a>

        <p style="font-size: 12px; color: #a8a29e; text-align: center;">Please reach out to the citizen or dispatch emergency authorities (112 / Police) immediately.</p>
      </div>
    </body>
    </html>
    """
    return send_email_async(
        to_email=to_email,
        subject=subject,
        html_content=html,
        email_type="sos_alert",
        metadata={"user_name": user_name, "user_phone": user_phone, "address": address, "lat": lat, "lng": lng}
    )


def send_test_email(to_email, note="Dashboard Diagnostic Test"):
    """
    Sends a test verification email to confirm SMTP connectivity and delivery.
    """
    now_str = timezone.now().strftime("%Y-%m-%d %H:%M:%S UTC")
    subject = f"✅ GuardianAI Email System Diagnostic Test [{now_str}]"

    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #0b1120; color: #f8fafc; margin: 0; padding: 24px; }}
        .card {{ max-width: 500px; margin: 0 auto; background: #1e293b; border: 1px solid #10b981; border-radius: 16px; padding: 30px; }}
        .badge {{ background: rgba(16, 185, 129, 0.2); color: #34d399; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 700; }}
        .title {{ font-size: 20px; font-weight: 700; color: #ffffff; margin: 14px 0 8px; }}
        .details {{ background: #0f172a; border-radius: 8px; padding: 14px; margin: 16px 0; font-family: monospace; font-size: 13px; color: #cbd5e1; }}
      </style>
    </head>
    <body>
      <div class="card">
        <span class="badge">SMTP ONLINE</span>
        <div class="title">GuardianAI Email Pipeline Operational</div>
        <p style="font-size: 14px; color: #94a3b8;">This diagnostic email verifies that the GuardianAI transactional email engine is functioning with zero errors.</p>
        <div class="details">
          <div><strong>Host:</strong> {settings.EMAIL_HOST}:{settings.EMAIL_PORT}</div>
          <div><strong>Sender:</strong> {settings.DEFAULT_FROM_EMAIL}</div>
          <div><strong>Recipient:</strong> {to_email}</div>
          <div><strong>Timestamp:</strong> {now_str}</div>
          <div><strong>Note:</strong> {note}</div>
        </div>
        <p style="font-size: 12px; color: #64748b; text-align: center; margin: 0;">&copy; 2026 GuardianAI Safety Command Center</p>
      </div>
    </body>
    </html>
    """
    return send_email_async(
        to_email=to_email,
        subject=subject,
        html_content=html,
        email_type="test",
        metadata={"note": note, "timestamp": now_str}
    )


def check_smtp_health():
    """
    Performs a live ping test to the configured SMTP server and returns connection status.
    """
    host = getattr(settings, 'EMAIL_HOST', 'smtp.gmail.com')
    port = getattr(settings, 'EMAIL_PORT', 587)
    user = getattr(settings, 'EMAIL_HOST_USER', '')
    pwd = getattr(settings, 'EMAIL_HOST_PASSWORD', '')

    try:
        server = smtplib.SMTP(host, port, timeout=6)
        if getattr(settings, 'EMAIL_USE_TLS', True):
            server.starttls()
        if user and pwd:
            server.login(user, pwd)
        server.quit()
        return {
            'status': 'healthy',
            'host': host,
            'port': port,
            'authenticated_as': user,
            'message': 'Connected and authenticated with SMTP server'
        }
    except Exception as e:
        return {
            'status': 'degraded',
            'host': host,
            'port': port,
            'authenticated_as': user,
            'message': str(e)
        }
