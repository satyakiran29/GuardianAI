from django.db import models
from django.utils import timezone

class CrimeIncident(models.Model):
    SEVERITY_CHOICES = (
        ('LOW', 'Low Severity'),
        ('MEDIUM', 'Medium Severity'),
        ('HIGH', 'High Severity'),
    )

    title = models.CharField(max_length=255)
    crime_type = models.CharField(max_length=100)
    severity = models.CharField(max_length=20, choices=SEVERITY_CHOICES, default='MEDIUM')
    latitude = models.FloatField(db_index=True)
    longitude = models.FloatField(db_index=True)
    city = models.CharField(max_length=100, default='Rajam')
    area = models.CharField(max_length=255, blank=True, default='')
    time_of_day = models.CharField(max_length=50, default='Night')
    day_of_week = models.CharField(max_length=20, blank=True, default='Monday')
    street_lighting = models.IntegerField(default=1) # 1 = well lit, 0 = poor/dark
    cctv_nearby = models.IntegerField(default=0) # 1 = yes, 0 = no
    crowd_density = models.IntegerField(default=200)
    verified = models.BooleanField(default=True)
    incident_date = models.DateTimeField(default=timezone.now)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-incident_date']
        indexes = [
            models.Index(fields=['latitude', 'longitude']),
            models.Index(fields=['city', 'area']),
        ]

    def __str__(self):
        return f"{self.crime_type} at {self.area or self.city} ({self.latitude:.4f}, {self.longitude:.4f})"


class RiskZone(models.Model):
    CATEGORY_CHOICES = (
        ('LOW', 'Low Risk (Safe)'),
        ('MEDIUM', 'Medium Risk (Caution)'),
        ('HIGH', 'High Risk (Danger)'),
    )

    zone_id = models.CharField(max_length=64, unique=True)
    name = models.CharField(max_length=255)
    city = models.CharField(max_length=100, default='Rajam')
    latitude = models.FloatField(db_index=True)
    longitude = models.FloatField(db_index=True)
    radius_meters = models.FloatField(default=150.0)
    risk_score = models.FloatField(default=20.0) # 0 to 100
    risk_category = models.CharField(max_length=20, choices=CATEGORY_CHOICES, default='LOW')
    confidence = models.FloatField(default=0.85)
    total_incidents = models.IntegerField(default=0)
    common_crimes = models.JSONField(default=list, blank=True)
    risk_factors = models.JSONField(default=list, blank=True)
    color = models.CharField(max_length=20, default='#10B981')
    is_active = models.BooleanField(default=True)
    last_evaluated = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ['-risk_score']
        indexes = [
            models.Index(fields=['latitude', 'longitude']),
            models.Index(fields=['risk_category']),
        ]

    def __str__(self):
        return f"[{self.risk_category}] {self.name} - Score: {self.risk_score}"


class RiskPrediction(models.Model):
    latitude = models.FloatField(db_index=True)
    longitude = models.FloatField(db_index=True)
    risk_score = models.FloatField()
    risk_category = models.CharField(max_length=20)
    confidence = models.FloatField(default=0.85)
    common_crimes = models.JSONField(default=list, blank=True)
    risk_factors = models.JSONField(default=list, blank=True)
    time_of_day = models.CharField(max_length=50, default='Evening')
    model_version = models.CharField(max_length=50, default='1.0')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Prediction at ({self.latitude:.4f}, {self.longitude:.4f}) - {self.risk_category} ({self.risk_score}%)"


class RiskAssessmentHistory(models.Model):
    user_id = models.CharField(max_length=128, blank=True, null=True)
    user_phone = models.CharField(max_length=32, blank=True, null=True)
    latitude = models.FloatField()
    longitude = models.FloatField()
    address_name = models.CharField(max_length=255, blank=True, default='')
    risk_score = models.FloatField()
    risk_category = models.CharField(max_length=20)
    confidence = models.FloatField(default=0.85)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.user_phone or 'User'} - {self.risk_category} ({self.risk_score}%) at {self.address_name or 'Pinned Location'}"


class MLModelVersion(models.Model):
    version_tag = models.CharField(max_length=32, unique=True)
    algorithm_name = models.CharField(max_length=64)
    accuracy = models.FloatField(default=0.0)
    precision_score = models.FloatField(default=0.0)
    recall_score = models.FloatField(default=0.0)
    f1_score = models.FloatField(default=0.0)
    is_active = models.BooleanField(default=True)
    model_path = models.CharField(max_length=255, blank=True, default='')
    trained_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-trained_at']

    def __str__(self):
        return f"{self.algorithm_name} v{self.version_tag} [F1: {self.f1_score:.2f}]"
