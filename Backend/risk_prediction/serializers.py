from rest_framework import serializers
from .models import CrimeIncident, RiskZone, RiskPrediction, RiskAssessmentHistory, MLModelVersion

class CrimeIncidentSerializer(serializers.ModelSerializer):
    class Meta:
        model = CrimeIncident
        fields = '__all__'

class RiskZoneSerializer(serializers.ModelSerializer):
    class Meta:
        model = RiskZone
        fields = '__all__'

class RiskPredictionSerializer(serializers.ModelSerializer):
    class Meta:
        model = RiskPrediction
        fields = '__all__'

class RiskAssessmentHistorySerializer(serializers.ModelSerializer):
    class Meta:
        model = RiskAssessmentHistory
        fields = '__all__'

class MLModelVersionSerializer(serializers.ModelSerializer):
    class Meta:
        model = MLModelVersion
        fields = '__all__'

