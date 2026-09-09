from django.db import models
from django.utils import timezone
import datetime

ROLE_CHOICES = [
    ('superadmin', 'Super Admin'),
    ('guardian', 'Guardian / Responder'),
    ('user', 'Protected User'),
]

ALERT_STATUS_CHOICES = [
    ('active', 'Active Emergency'),
    ('dispatched', 'Guardian Dispatched'),
    ('resolved', 'Resolved / Safe'),
]

TRIGGER_SOURCE_CHOICES = [
    ('button', 'SOS Button'),
    ('shake', 'Shake Gesture'),
    ('voice', 'Voice Keyword'),
    ('timer', 'Safety Timer Expired'),
    ('battery', 'Low Battery (15%) Alert'),
    ('safe_mode', 'Safe Mode Broadcast'),
    ('trip', 'Trip Off-Route Divergence'),
]

class UserProfile(models.Model):
    name = models.CharField(max_length=150)
    email = models.EmailField(unique=True)
    phone = models.CharField(max_length=20, unique=True)
    password = models.CharField(max_length=255, default='guardian123')
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default='user')
    is_verified = models.BooleanField(default=True)
    is_active = models.BooleanField(default=True)
    
    # Telemetry / Live Status
    last_latitude = models.FloatField(null=True, blank=True, default=17.3850)
    last_longitude = models.FloatField(null=True, blank=True, default=78.4867)
    last_address = models.CharField(max_length=255, blank=True, default='Hyderabad, Telangana')
    battery_level = models.IntegerField(default=85)
    fcm_token = models.TextField(blank=True, default='')
    
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['role', 'is_active']),
            models.Index(fields=['phone']),
            models.Index(fields=['email']),
        ]

    def __str__(self):
        return f"{self.name} ({self.get_role_display()}) - {self.phone}"


class EmergencyAlert(models.Model):
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='alerts')
    latitude = models.FloatField()
    longitude = models.FloatField()
    address = models.CharField(max_length=255, blank=True, default='Live Location')
    trigger_source = models.CharField(max_length=30, choices=TRIGGER_SOURCE_CHOICES, default='button')
    status = models.CharField(max_length=20, choices=ALERT_STATUS_CHOICES, default='active')
    siren_active = models.BooleanField(default=True)
    battery_level = models.IntegerField(default=100)
    responder = models.ForeignKey(UserProfile, on_delete=models.SET_NULL, null=True, blank=True, related_name='responded_alerts')
    timestamp = models.DateTimeField(auto_now_add=True)
    resolved_at = models.DateTimeField(null=True, blank=True)
    notes = models.TextField(blank=True, default='')

    class Meta:
        ordering = ['-timestamp']
        indexes = [
            models.Index(fields=['status', '-timestamp']),
            models.Index(fields=['user', 'status']),
            models.Index(fields=['-timestamp']),
        ]

    def __str__(self):
        return f"SOS #{self.id} - {self.user.name} ({self.status})"


class ExpiredStatus(int):
    """
    A boolean-compatible status object that is both callable and evaluates to a boolean directly.
    Allows:
      - record.is_expired()
      - record.is_expired
      - {% if record.is_expired %} in Django templates
    """
    def __call__(self):
        return bool(self)


class OtpRecord(models.Model):
    target = models.CharField(max_length=100, db_index=True) # Phone number or Email
    otp_code = models.CharField(max_length=6, db_index=True)
    purpose = models.CharField(max_length=50, default='registration') # registration, login, reset, sos
    is_verified = models.BooleanField(default=False, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)
    expires_at = models.DateTimeField()

    @property
    def is_expired(self):
        return ExpiredStatus(1 if timezone.now() > self.expires_at else 0)

    @property
    def remaining_seconds(self):
        delta = (self.expires_at - timezone.now()).total_seconds()
        return max(0, int(delta))

    def __str__(self):
        return f"OTP {self.otp_code} for {self.target} ({'Verified' if self.is_verified else 'Pending'})"


class EmergencyContact(models.Model):
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='contacts')
    name = models.CharField(max_length=100)
    phone = models.CharField(max_length=20)
    relationship = models.CharField(max_length=50, default='Family')
    is_primary = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.name} ({self.relationship}) -> {self.user.name}"


class TripLog(models.Model):
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='trips')
    vehicle_number = models.CharField(max_length=50)
    driver_name = models.CharField(max_length=100, blank=True)
    destination = models.CharField(max_length=255)
    status = models.CharField(max_length=30, default='active') # active, completed, alert
    last_latitude = models.FloatField(null=True, blank=True)
    last_longitude = models.FloatField(null=True, blank=True)
    start_time = models.DateTimeField(auto_now_add=True)
    end_time = models.DateTimeField(null=True, blank=True)

    def __str__(self):
        return f"Trip {self.vehicle_number} - {self.user.name}"


class GuardianLink(models.Model):
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='guardian_links')
    guardian = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='ward_links')
    relationship = models.CharField(max_length=50, default='Family')
    status = models.CharField(max_length=20, default='active') # active, pending, revoked
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'guardian')
        indexes = [
            models.Index(fields=['guardian', 'status']),
            models.Index(fields=['user', 'status']),
        ]

    def __str__(self):
        return f"{self.guardian.name} (Guardian) -> {self.user.name} (Protected User) [{self.relationship}]"


class ChatMessage(models.Model):
    sender = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='sent_messages')
    receiver = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='received_messages')
    message = models.TextField()
    is_sos = models.BooleanField(default=False)
    latitude = models.FloatField(null=True, blank=True)
    longitude = models.FloatField(null=True, blank=True)
    battery_level = models.IntegerField(null=True, blank=True)
    is_read = models.BooleanField(default=False)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['timestamp']
        indexes = [
            models.Index(fields=['sender', 'receiver', 'timestamp']),
            models.Index(fields=['receiver', 'is_read']),
        ]

    def __str__(self):
        return f"[{self.timestamp.strftime('%H:%M')}] {self.sender.name} -> {self.receiver.name}: {self.message[:30]}"


class LocationHistory(models.Model):
    user = models.ForeignKey(UserProfile, on_delete=models.CASCADE, related_name='location_history')
    latitude = models.FloatField()
    longitude = models.FloatField()
    address = models.CharField(max_length=255, blank=True, default='')
    battery_level = models.IntegerField(null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-timestamp']
        indexes = [
            models.Index(fields=['user', '-timestamp']),
        ]

    def __str__(self):
        return f"{self.user.name} @ ({self.latitude}, {self.longitude}) - {self.timestamp.strftime('%Y-%m-%d %H:%M')}"


class EmailLog(models.Model):
    EMAIL_TYPE_CHOICES = (
        ('otp', 'OTP Passcode'),
        ('sos_alert', 'Emergency SOS Alert'),
        ('test', 'System Diagnostic Test'),
        ('system', 'System Notification'),
    )

    STATUS_CHOICES = (
        ('queued', 'Queued'),
        ('sent', 'Sent Successfully'),
        ('failed', 'Delivery Failed'),
    )

    recipient = models.CharField(max_length=255, db_index=True)
    subject = models.CharField(max_length=255)
    email_type = models.CharField(max_length=30, choices=EMAIL_TYPE_CHOICES, default='otp')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='queued', db_index=True)
    error_message = models.TextField(blank=True, null=True)
    metadata = models.JSONField(blank=True, default=dict)
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)
    delivered_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['status', '-created_at']),
            models.Index(fields=['recipient', '-created_at']),
        ]

    def __str__(self):
        return f"[{self.get_status_display()}] {self.email_type.upper()} -> {self.recipient} ({self.created_at.strftime('%Y-%m-%d %H:%M')})"

