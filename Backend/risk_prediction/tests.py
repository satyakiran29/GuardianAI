from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status
from .ml_engine import risk_engine
from .models import RiskZone, CrimeIncident

class RiskPredictionEngineTestCase(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.rajam_lat = 18.44899
        self.rajam_lon = 83.65532

    def test_ml_model_prediction(self):
        res = risk_engine.predict_location_risk(
            lat=self.rajam_lat,
            lon=self.rajam_lon,
            time_of_day="Evening",
            area_type="Commercial",
            street_lighting=1,
            cctv_nearby=1,
            police_distance_km=0.2
        )
        self.assertTrue(res['success'])
        self.assertIn('risk_assessment', res)
        self.assertIn('risk_score', res['risk_assessment'])
        self.assertIn('risk_category', res['risk_assessment'])
        self.assertIn(res['risk_assessment']['risk_category'], ['LOW', 'MEDIUM', 'HIGH'])
        self.assertGreaterEqual(res['risk_assessment']['risk_score'], 0.0)
        self.assertLessEqual(res['risk_assessment']['risk_score'], 100.0)
        self.assertTrue(len(res['risk_factors']) > 0)

    def test_api_risk_zones_list(self):
        response = self.client.get('/api/risk-zones/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])
        self.assertGreater(response.data['count'], 0)

    def test_api_nearby_risk_zones(self):
        response = self.client.get(f'/api/risk-zones/nearby/?lat={self.rajam_lat}&lng={self.rajam_lon}&radius=15.0')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])
        self.assertIn('nearby_risk_zones', response.data)

    def test_api_predict_location(self):
        response = self.client.get(f'/api/risk-zones/predict/?lat={self.rajam_lat}&lng={self.rajam_lon}&time_of_day=Night')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])
        self.assertIn('risk_assessment', response.data)
        self.assertIn('historical_data', response.data)
        self.assertIn('risk_factors', response.data)
        self.assertIn('model', response.data)

    def test_api_heatmap(self):
        response = self.client.get(f'/api/risk-zones/heatmap/?lat={self.rajam_lat}&lng={self.rajam_lon}&radius=5.0')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])
        self.assertGreater(response.data['count'], 0)
