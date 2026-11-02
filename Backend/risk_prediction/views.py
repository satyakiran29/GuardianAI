import logging
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status, permissions
from .ml_engine import risk_engine, haversine_km
from .models import RiskZone, CrimeIncident, RiskPrediction, RiskAssessmentHistory, MLModelVersion
from .serializers import (
    RiskZoneSerializer, CrimeIncidentSerializer, RiskPredictionSerializer,
    RiskAssessmentHistorySerializer, MLModelVersionSerializer
)

logger = logging.getLogger(__name__)

class RiskZonesListView(APIView):
    """
    GET /api/risk-zones/
    Retrieves all regional risk zones with scores, categories, and circle radii.
    """
    permission_classes = [permissions.AllowAny]

    def get(self, request):
        try:
            lat = float(request.query_params.get('lat', 18.455))
            lon = float(request.query_params.get('lng', request.query_params.get('lon', 83.660)))
            radius = float(request.query_params.get('radius', 25.0))

            zones = risk_engine.generate_regional_grid_zones(lat, lon, radius)
            return Response({
                'success': True,
                'count': len(zones),
                'zones': zones
            }, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error listing risk zones: {e}")
            return Response({'success': False, 'message': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class NearbyRiskZonesView(APIView):
    """
    GET /api/risk-zones/nearby/?lat=...&lng=...&radius=...
    Retrieves nearby risk zones ordered by risk score (highest risk first) or proximity.
    """
    permission_classes = [permissions.AllowAny]

    def get(self, request):
        try:
            lat = request.query_params.get('lat')
            lon = request.query_params.get('lng') or request.query_params.get('lon')
            radius = float(request.query_params.get('radius', 12.0))

            if not lat or not lon:
                return Response({
                    'success': False,
                    'message': 'lat and lng parameters are required'
                }, status=status.HTTP_400_BAD_REQUEST)

            lat = float(lat)
            lon = float(lon)

            zones = risk_engine.generate_regional_grid_zones(lat, lon, radius)
            # Filter and sort by distance or risk
            sort_by = request.query_params.get('sort', 'risk')
            if sort_by == 'distance':
                zones.sort(key=lambda z: z['distance_km'])
            else:
                zones.sort(key=lambda z: z['risk_score'], reverse=True)

            return Response({
                'success': True,
                'center': {'latitude': lat, 'longitude': lon},
                'radius_km': radius,
                'count': len(zones),
                'nearby_risk_zones': zones
            }, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error fetching nearby risk zones: {e}")
            return Response({'success': False, 'message': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class PredictLocationRiskView(APIView):
    """
    GET or POST /api/risk-zones/predict/
    Generates AI-Based continuous risk prediction (0-100), category (LOW, MEDIUM, HIGH),
    historical crime analytics, and Explainable AI contributing factors.
    """
    permission_classes = [permissions.AllowAny]

    def _handle_prediction(self, request, data):
        lat = data.get('lat') or data.get('latitude')
        lon = data.get('lng') or data.get('lon') or data.get('longitude')
        time_of_day = data.get('time_of_day', 'Evening')
        area_type = data.get('area_type', 'Commercial')
        street_lighting = data.get('street_lighting')
        cctv_nearby = data.get('cctv_nearby')
        police_dist = data.get('police_distance_km')
        user_phone = data.get('phone') or data.get('user_phone', '')
        user_id = data.get('user_id', '')

        if lat is None or lon is None:
            return Response({
                'success': False,
                'message': 'latitude and longitude are required'
            }, status=status.HTTP_400_BAD_REQUEST)

        lat = float(lat)
        lon = float(lon)
        if street_lighting is not None: street_lighting = int(street_lighting)
        if cctv_nearby is not None: cctv_nearby = int(cctv_nearby)
        if police_dist is not None: police_dist = float(police_dist)

        result = risk_engine.predict_location_risk(
            lat=lat, lon=lon,
            time_of_day=time_of_day,
            area_type=area_type,
            street_lighting=street_lighting,
            cctv_nearby=cctv_nearby,
            police_distance_km=police_dist
        )

        # Log to RiskAssessmentHistory
        try:
            RiskAssessmentHistory.objects.create(
                user_id=user_id,
                user_phone=user_phone,
                latitude=lat,
                longitude=lon,
                address_name=data.get('address_name', ''),
                risk_score=result['risk_assessment']['risk_score'],
                risk_category=result['risk_assessment']['risk_category'],
                confidence=result['risk_assessment']['prediction_confidence']
            )
        except Exception as e:
            logger.debug(f"Assessment history logging: {e}")

        return Response(result, status=status.HTTP_200_OK)

    def get(self, request):
        return self._handle_prediction(request, request.query_params)

    def post(self, request):
        return self._handle_prediction(request, request.data)


class RiskHeatmapView(APIView):
    """
    GET /api/risk-zones/heatmap/?lat=...&lng=...&radius=...
    Retrieves dense weighted coordinates [latitude, longitude, risk_intensity]
    optimized for Leaflet Heatmap rendering.
    """
    permission_classes = [permissions.AllowAny]

    def get(self, request):
        try:
            lat = float(request.query_params.get('lat', 18.455))
            lon = float(request.query_params.get('lng', request.query_params.get('lon', 83.660)))
            radius = float(request.query_params.get('radius', 15.0))

            points = risk_engine.generate_heatmap_points(lat, lon, radius)
            return Response({
                'success': True,
                'count': len(points),
                'heatmap_points': points
            }, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error generating heatmap: {e}")
            return Response({'success': False, 'message': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class RiskZoneDetailView(APIView):
    """
    GET /api/risk-zones/<zone_id>/
    Retrieves complete analytical detail for a specific risk zone.
    """
    permission_classes = [permissions.AllowAny]

    def get(self, request, zone_id):
        try:
            zones = risk_engine.generate_regional_grid_zones(radius_km=50.0)
            target = next((z for z in zones if str(z['id']) == str(zone_id)), None)
            if not target:
                return Response({'success': False, 'message': 'Risk Zone not found'}, status=status.HTTP_404_NOT_FOUND)

            detail = risk_engine.predict_location_risk(target['latitude'], target['longitude'])
            detail['zone_meta'] = target
            return Response(detail, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error fetching zone detail: {e}")
            return Response({'success': False, 'message': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


class RiskAssessmentHistoryView(APIView):
    """
    GET /api/risk-zones/history/?phone=...
    Retrieves past requested risk assessments with timestamps.
    """
    permission_classes = [permissions.AllowAny]

    def get(self, request):
        try:
            phone = request.query_params.get('phone', '')
            qs = RiskAssessmentHistory.objects.all()
            if phone:
                qs = qs.filter(user_phone=phone)
            qs = qs.order_by('-created_at')[:25]
            serializer = RiskAssessmentHistorySerializer(qs, many=True)
            return Response({
                'success': True,
                'count': len(serializer.data),
                'history': serializer.data
            }, status=status.HTTP_200_OK)
        except Exception as e:
            logger.error(f"Error fetching history: {e}")
            return Response({'success': False, 'message': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
