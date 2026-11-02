from django.urls import path
from .views import (
    RiskZonesListView, NearbyRiskZonesView, PredictLocationRiskView,
    RiskHeatmapView, RiskZoneDetailView, RiskAssessmentHistoryView
)

urlpatterns = [
    path('', RiskZonesListView.as_view(), name='risk-zones-list'),
    path('nearby/', NearbyRiskZonesView.as_view(), name='risk-zones-nearby'),
    path('predict/', PredictLocationRiskView.as_view(), name='risk-zones-predict'),
    path('heatmap/', RiskHeatmapView.as_view(), name='risk-zones-heatmap'),
    path('history/', RiskAssessmentHistoryView.as_view(), name='risk-zones-history'),
    path('<str:zone_id>/', RiskZoneDetailView.as_view(), name='risk-zone-detail'),
]

