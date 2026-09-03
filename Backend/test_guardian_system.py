import os
import sys
import django
import json

# Ensure UTF-8 stdout
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'guardian_backend.settings')
django.setup()


from guardian_api.models import UserProfile, GuardianLink, ChatMessage, EmergencyAlert
from django.test import Client

def run_tests():
    print("🚀 Starting Guardian Role, Tracking, and Chat System Tests...")
    client = Client()

    # 1. Setup Test Users
    user, _ = UserProfile.objects.update_or_create(
        phone="+919876543210",
        defaults={
            'name': 'Priya Sharma (Protected User)',
            'email': 'priya@example.com',
            'role': 'user',
            'battery_level': 78,
            'last_latitude': 17.4482,
            'last_longitude': 78.3914,
            'last_address': 'Madhapur, Hitech City, Hyderabad',
            'is_verified': True
        }
    )
    print(f"✅ Created/Loaded Protected User: {user.name} (Battery: {user.battery_level}%)")

    guardian, _ = UserProfile.objects.update_or_create(
        phone="+919988776655",
        defaults={
            'name': 'Rajesh Sharma (Guardian Unit)',
            'email': 'rajesh@example.com',
            'role': 'guardian',
            'battery_level': 95,
            'last_latitude': 17.4490,
            'last_longitude': 78.3920,
            'last_address': 'Cyber Towers, Hyderabad',
            'is_verified': True
        }
    )
    print(f"✅ Created/Loaded Guardian: {guardian.name} (Battery: {guardian.battery_level}%)")

    # 2. Test Linking Guardian via API
    res = client.post('/api/guardians/link/', data=json.dumps({
        'user_phone': user.phone,
        'guardian_phone': guardian.phone,
        'guardian_name': guardian.name,
        'relationship': 'Father'
    }), content_type='application/json')
    assert res.status_code in [200, 201], f"Link failed: {res.content}"
    print(f"✅ Guardian Link API: {res.json()['message']}")

    # 3. Test Guardian Tracking Wards (Battery % & GPS Location)
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={guardian.phone}')
    assert res.status_code == 200, f"Tracked wards failed: {res.content}"
    data = res.json()
    assert data['tracked_wards_count'] >= 1, "No wards found for guardian"
    ward_info = data['wards'][0]
    print(f"✅ Guardian Wards Radar API: Ward={ward_info['name']}, Battery={ward_info['battery_level']}%, Status={ward_info['battery_status']}, Location=({ward_info['latitude']}, {ward_info['longitude']})")

    # 4. Test Chat Message from User to Guardian
    res = client.post('/api/chat/send/', data=json.dumps({
        'sender_phone': user.phone,
        'receiver_phone': guardian.phone,
        'message': 'Dad, I have reached the metro station safely!',
        'battery_level': 75,
        'latitude': 17.4485,
        'longitude': 78.3918
    }), content_type='application/json')
    assert res.status_code == 201, f"Send chat failed: {res.content}"
    print(f"✅ User Sent Chat: \"Dad, I have reached the metro station safely!\"")

    # 5. Test Chat Message from Guardian to User
    res = client.post('/api/chat/send/', data=json.dumps({
        'sender_phone': guardian.phone,
        'receiver_phone': user.phone,
        'message': 'Great! Stay on the main road and keep your battery charged.',
        'battery_level': 94
    }), content_type='application/json')
    assert res.status_code == 201, f"Send chat failed: {res.content}"
    print(f"✅ Guardian Sent Chat: \"Great! Stay on the main road and keep your battery charged.\"")

    # 6. Test Chat History Retrieval
    res = client.get(f'/api/chat/messages/?user1={user.phone}&user2={guardian.phone}')
    assert res.status_code == 200, f"Get chat failed: {res.content}"
    chat_data = res.json()
    assert len(chat_data['messages']) >= 2, "Chat history missing messages"
    print(f"✅ Chat History API: {len(chat_data['messages'])} message(s) retrieved successfully")

    # 7. Test Location & Battery Ping Sync
    res = client.post('/api/location/ping/', data=json.dumps({
        'phone': user.phone,
        'latitude': 17.4501,
        'longitude': 78.3950,
        'address': 'Inorbit Mall Road, Madhapur',
        'battery_level': 72
    }), content_type='application/json')
    assert res.status_code == 200, f"Ping failed: {res.content}"
    print(f"✅ Real-time Telemetry Ping: Updated user to 72% battery at Inorbit Mall Road")

    # Re-verify Guardian sees updated battery & location
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={guardian.phone}')
    ward_info = res.json()['wards'][0]
    assert ward_info['battery_level'] == 72
    assert ward_info['address'] == 'Inorbit Mall Road, Madhapur'
    print(f"✅ Guardian Radar Verified: Live Telemetry is {ward_info['battery_level']}% battery at {ward_info['address']}")

    print("\n🎉 ALL GUARDIAN SYSTEM VERIFICATION TESTS PASSED SUCCESSFULLY!")

if __name__ == '__main__':
    run_tests()
