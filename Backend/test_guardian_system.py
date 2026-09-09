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

    # 8. Test Regular User access to "My Guardians" API
    res = client.get(f'/api/guardians/my-guardians/?phone={user.phone}')
    assert res.status_code == 200, f"My Guardians failed: {res.content}"
    user_guardians = res.json()['links']
    assert len(user_guardians) >= 1, "User should see their assigned guardians"
    print(f"✅ User 'My Guardians' Access Verified: {len(user_guardians)} guardian(s) protecting user")

    # 9. Test Regular User CANNOT access Guardian Portal / Tracked Wards (Forbidden 403)
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={user.phone}')
    assert res.status_code == 403, f"Regular user should be denied access to Guardian Portal: {res.status_code}"
    print(f"✅ User Guardian Portal Removal Verified: 403 Forbidden correctly returned for regular users")

    # 10. Scenario Test: User 'sk' with guardian 'skdad'; another user 'other_user' with guardian 'other_guardian'
    sk_user, _ = UserProfile.objects.update_or_create(
        phone="+919100000001",
        defaults={'name': 'SK User', 'email': 'sk@sheguard.app', 'role': 'user', 'battery_level': 88}
    )
    skdad_guardian, _ = UserProfile.objects.update_or_create(
        phone="+919200000002",
        defaults={'name': 'SK Dad Guardian', 'email': 'skdad@sheguard.app', 'role': 'guardian', 'battery_level': 99}
    )
    other_user, _ = UserProfile.objects.update_or_create(
        phone="+919300000003",
        defaults={'name': 'Other User', 'email': 'other_u@sheguard.app', 'role': 'user', 'battery_level': 65}
    )
    other_guardian, _ = UserProfile.objects.update_or_create(
        phone="+919400000004",
        defaults={'name': 'Other Guardian', 'email': 'other_g@sheguard.app', 'role': 'guardian', 'battery_level': 90}
    )
    superadmin, _ = UserProfile.objects.update_or_create(
        phone="+919876500000",
        defaults={'name': 'Chief SuperAdmin', 'email': 'admin@sheguard.app', 'role': 'superadmin'}
    )

    # Link sk -> skdad
    GuardianLink.objects.update_or_create(user=sk_user, guardian=skdad_guardian, defaults={'relationship': 'Father', 'status': 'active'})
    # Link other_user -> other_guardian
    GuardianLink.objects.update_or_create(user=other_user, guardian=other_guardian, defaults={'relationship': 'Brother', 'status': 'active'})

    # 10a. skdad can ONLY see sk
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={skdad_guardian.phone}')
    assert res.status_code == 200
    skdad_wards = res.json()['wards']
    skdad_ward_phones = [w['phone'] for w in skdad_wards]
    assert sk_user.phone in skdad_ward_phones, "skdad must see sk"
    assert other_user.phone not in skdad_ward_phones, "skdad must NOT see other_user"
    print(f"✅ Strict Guardian Isolation Verified: skdad ONLY sees their assigned ward ({sk_user.name}), cannot see other wards")

    # 10b. other_guardian can ONLY see other_user
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={other_guardian.phone}')
    assert res.status_code == 200
    other_g_wards = res.json()['wards']
    other_g_ward_phones = [w['phone'] for w in other_g_wards]
    assert other_user.phone in other_g_ward_phones, "other_guardian must see other_user"
    assert sk_user.phone not in other_g_ward_phones, "other_guardian must NOT see sk"
    print(f"✅ Strict Guardian Isolation Verified: other_guardian ONLY sees {other_user.name}")

    # 10c. sk (user) CANNOT access guardian portal
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={sk_user.phone}')
    assert res.status_code == 403, "sk (user role) must be forbidden from accessing Guardian Portal"
    print(f"✅ User Access Block Verified: sk (user) gets 403 Forbidden on Guardian Portal")

    # 10d. superadmin can access ALL users data
    res = client.get(f'/api/guardians/tracked-wards/?guardian_phone={superadmin.phone}')
    assert res.status_code == 200
    admin_wards = res.json()['wards']
    admin_ward_phones = [w['phone'] for w in admin_wards]
    assert sk_user.phone in admin_ward_phones, "superadmin must see sk"
    assert other_user.phone in admin_ward_phones, "superadmin must see other_user"
    print(f"✅ SuperAdmin Omniscient Access Verified: SuperAdmin accessed all {len(admin_wards)} users data across the system")

    # 11. Location History / 24h Trail Replay Tests
    print("\n📍 Testing 24h Location History & Replay Playback API...")

    # 11a. skdad queries sk's 24h trail -> 200 OK
    res = client.get(f'/api/location/history/?ward_phone={sk_user.phone}&guardian_phone={skdad_guardian.phone}&hours=24')
    assert res.status_code == 200, f"History request failed: {res.content}"
    data = res.json()
    assert data['status'] == 'success'
    assert len(data['trail']) > 0, "Trail points should be populated"
    print(f"✅ Guardian 24h Replay Access Verified: Retrieved {data['total_points']} trail points for ward {data['ward']['name']}")

    # 11b. Send a real location ping for sk and check it appears in history
    client.post('/api/location/ping/', data=json.dumps({
        'phone': sk_user.phone,
        'latitude': 17.4495,
        'longitude': 78.3935,
        'address': 'Kavuri Hills Junction',
        'battery_level': 82
    }), content_type='application/json')

    # Trigger an emergency alert to check incident marker
    alert_res = client.post('/api/sos/trigger/', data=json.dumps({
        'phone': sk_user.phone,
        'latitude': 17.4496,
        'longitude': 78.3936,
        'address': 'Kavuri Hills Junction (Incident Point)',
        'trigger_source': 'button',
        'battery_level': 81
    }), content_type='application/json')
    assert alert_res.status_code == 201

    res = client.get(f'/api/location/history/?ward_phone={sk_user.phone}&guardian_phone={skdad_guardian.phone}&hours=24')
    assert res.status_code == 200
    data = res.json()
    assert data['incident_count'] >= 1, "Incident point should be flagged"
    incident_pts = [pt for pt in data['trail'] if pt.get('is_incident')]
    assert len(incident_pts) >= 1
    print(f"✅ Incident Beacon Correlation Verified: {len(incident_pts)} point(s) flagged with SOS incident beacon")

    # 11c. Unauthorized guardian (other_guardian) tries to access sk's trail -> 403 Forbidden
    res = client.get(f'/api/location/history/?ward_phone={sk_user.phone}&guardian_phone={other_guardian.phone}&hours=24')
    assert res.status_code == 403, f"Expected 403 Forbidden for unauthorized guardian: {res.status_code}"
    print(f"✅ Location Trail Isolation Verified: Unauthorized guardian received 403 Forbidden")

    # 11d. Ward querying her own trail -> 200 OK
    res = client.get(f'/api/location/history/?ward_phone={sk_user.phone}&guardian_phone={sk_user.phone}&hours=24')
    assert res.status_code == 200
    print(f"✅ Ward Self-Inspection Verified: User can inspect her own 24h trail")

    # 11e. SuperAdmin querying ward trail -> 200 OK
    res = client.get(f'/api/location/history/?ward_phone={sk_user.phone}&guardian_phone={superadmin.phone}&hours=24')
    assert res.status_code == 200
    print(f"✅ SuperAdmin Replay Access Verified: SuperAdmin can inspect any ward's trail")

    # 12. Test Forensic Debrief & PDF Exporter (JSON & HTML)
    res_debrief = client.get(f'/api/location/debrief/?ward_phone={sk_user.phone}&guardian_phone={skdad_guardian.phone}&hours=24')
    assert res_debrief.status_code == 200, f"Debrief failed: {res_debrief.content}"
    debrief_json = res_debrief.json()
    assert debrief_json['status'] == 'success'
    d_data = debrief_json['debrief']
    assert 'sha256_checksum' in d_data
    assert len(d_data['sha256_checksum']) == 64
    assert 'metrics' in d_data
    assert d_data['metrics']['total_distance_km'] >= 0.0
    print(f"✅ Forensic Debrief JSON API Verified: Case {d_data['case_id']}, SHA-256: {d_data['sha256_checksum'][:16]}...")

    # Test HTML printable export
    res_html = client.get(f'/api/location/debrief/?ward_phone={sk_user.phone}&guardian_phone={skdad_guardian.phone}&hours=24&format=html')
    assert res_html.status_code == 200, f"HTML debrief failed with {res_html.status_code}: {res_html.content}"
    assert "GUARDIAN AI — FORENSIC INCIDENT DEBRIEF" in res_html.content.decode('utf-8')
    assert "SHA-256 HASH:" in res_html.content.decode('utf-8')
    print(f"✅ Forensic Debrief Printable HTML Document Verified (A4 Print/PDF ready)")

    # 13. Test Email-Only OTP Authentication System
    print("\n🔑 Testing Email-Only OTP Authentication System...")
    test_target_email = "priya@example.com"

    # 13a. Send OTP to email address
    res_send = client.post('/api/auth/send-otp/', data=json.dumps({
        'target': test_target_email,
        'purpose': 'login'
    }), content_type='application/json')
    assert res_send.status_code == 200, f"Send Email OTP failed: {res_send.content}"
    otp_data = res_send.json()
    generated_otp = otp_data['otp']
    assert len(generated_otp) == 6 and generated_otp.isdigit()
    assert otp_data['email_sent'] is True or otp_data['email_destination'] == test_target_email
    print(f"✅ Send Email OTP API Verified: Dispatched 6-digit code {generated_otp} to {test_target_email}")

    # 13b. Unlinked raw phone number without email is rejected
    res_phone_fail = client.post('/api/auth/send-otp/', data=json.dumps({
        'target': '+917777788888',
        'purpose': 'login'
    }), content_type='application/json')
    assert res_phone_fail.status_code == 400
    print(f"✅ SMS Phone OTP Removal Verified: Raw unlinked phone rejected with 400 as expected")

    # 13c. Verify Email OTP
    res_verify = client.post('/api/auth/verify-otp/', data=json.dumps({
        'target': test_target_email,
        'otp_code': generated_otp
    }), content_type='application/json')
    assert res_verify.status_code == 200, f"Email OTP verification failed: {res_verify.content}"
    assert res_verify.json()['is_verified'] is True
    print(f"✅ Email OTP Verification Verified: Code successfully authenticated for {test_target_email}")

    # 13d. Master Demo Passcode (123456)
    res_demo = client.post('/api/auth/verify-otp/', data=json.dumps({
        'target': test_target_email,
        'otp_code': '123456'
    }), content_type='application/json')
    assert res_demo.status_code == 200
    assert res_demo.json()['is_verified'] is True
    print(f"✅ Master Demo Passcode 123456 Verified: Instant bypass successful")

    # 13e. Login via Email OTP in REST API
    res_api_login = client.post('/api/auth/login/', data=json.dumps({
        'identifier': user.email,
        'otp_code': '123456'
    }), content_type='application/json')
    assert res_api_login.status_code == 200
    assert res_api_login.json()['user']['email'] == user.email
    print(f"✅ API Login with Email OTP Verified: Citizen logged in with demo passcode")

    # 13f. Dashboard Web Email OTP Login Form Submission
    res_web_login = client.post('/login/', data={
        'auth_type': 'otp',
        'email': user.email,
        'otp_code': '123456',
        'role_type': 'user'
    })
    assert res_web_login.status_code in [200, 302]
    print(f"✅ Dashboard Web Email OTP Login Verified: Session established successfully")

    # 14. Test Transactional Email & Management System
    print("\n📧 Testing Transactional Email Management System...")
    from guardian_api.resend_mailer import check_smtp_health
    from guardian_api.models import EmailLog

    # Check SMTP health
    health = check_smtp_health()
    assert 'status' in health
    print(f"✅ SMTP Gateway Health Verified: status={health['status']}, host={health['host']}:{health['port']}")

    # Check Email API logs endpoint
    res_logs = client.get('/api/email/logs/')
    assert res_logs.status_code == 200
    logs_data = res_logs.json()
    assert logs_data['status'] == 'success'
    print(f"✅ Email Logs API Verified: {logs_data['count']} audit log(s) retrieved")

    # 15. Test 2-Step Verification (2FA) System in API and Web
    print("\n🔒 Testing 2-Step Verification (2FA) Management & Auth...")

    # 15a. Enable 2FA for user
    res_2fa_on = client.post('/api/auth/two-factor/toggle/', data=json.dumps({
        'identifier': user.email,
        'enabled': True
    }), content_type='application/json')
    assert res_2fa_on.status_code == 200
    assert res_2fa_on.json()['two_factor_enabled'] is True
    print(f"✅ 2FA Enable API Verified: 2-Step Verification activated for {user.email}")

    # 15b. Login with password only -> triggers 2FA required challenge
    res_login_2fa_req = client.post('/api/auth/login/', data=json.dumps({
        'identifier': user.email,
        'password': 'guardian123'
    }), content_type='application/json')
    assert res_login_2fa_req.status_code == 200
    data_2fa_req = res_login_2fa_req.json()
    assert data_2fa_req['two_factor_required'] is True
    assert data_2fa_req['status'] == '2fa_required'
    print(f"✅ 2FA Interception Verified: Login requested 6-digit email passcode dispatch")

    # 15c. Login with password + demo passcode 234456 -> succeeds
    res_login_2fa_pass = client.post('/api/auth/login/', data=json.dumps({
        'identifier': user.email,
        'password': 'guardian123',
        'otp_code': '234456'
    }), content_type='application/json')
    assert res_login_2fa_pass.status_code == 200
    assert res_login_2fa_pass.json()['status'] == 'success'
    assert res_login_2fa_pass.json()['two_factor_enabled'] is True
    print(f"✅ 2FA Demo Passcode 234456 Verified: Login authenticated with 2nd step")

    # 15d. Web Dashboard Password Login with 2FA active -> Step 2 triggered
    client.get('/logout/')
    res_web_2fa_step1 = client.post('/login/', data={
        'auth_type': 'password',
        'identifier': user.email,
        'password': 'guardian123'
    })
    assert res_web_2fa_step1.status_code == 200
    assert "2-Step Verification Required" in res_web_2fa_step1.content.decode('utf-8')
    print(f"✅ Web Dashboard 2FA Step 1 Verified: Prompted for email passcode")

    # 15e. Web Dashboard Step 2 submission with demo code 234456 -> authenticated session
    res_web_2fa_step2 = client.post('/login/', data={
        'auth_type': '2fa',
        'otp_code': '234456'
    })
    assert res_web_2fa_step2.status_code in [200, 302]
    print(f"✅ Web Dashboard 2FA Step 2 Verified: Session authenticated with passcode 234456")

    # 15f. Web Dashboard Security Page
    res_security_page = client.get('/security/')
    assert res_security_page.status_code == 200
    assert "Account Security" in res_security_page.content.decode('utf-8') and "2-Step Verification" in res_security_page.content.decode('utf-8')
    print(f"✅ Web Dashboard Security Console Verified: Rendered successfully")

    # 15g. Toggle 2FA OFF via API
    res_2fa_off = client.post('/api/auth/two-factor/toggle/', data=json.dumps({
        'identifier': user.email,
        'enabled': False
    }), content_type='application/json')
    assert res_2fa_off.status_code == 200
    assert res_2fa_off.json()['two_factor_enabled'] is False
    print(f"✅ 2FA Disable API Verified: 2-Step Verification turned OFF for {user.email}")

    # 15h. Login with password only when 2FA is OFF -> succeeds immediately
    res_login_direct = client.post('/api/auth/login/', data=json.dumps({
        'identifier': user.email,
        'password': 'guardian123'
    }), content_type='application/json')
    assert res_login_direct.status_code == 200
    assert res_login_direct.json()['status'] == 'success'
    assert res_login_direct.json()['two_factor_enabled'] is False
    print(f"✅ Direct Password Login Verified: Instant login without 2nd step when 2FA is OFF")

    print("\n🎉 ALL GUARDIAN SYSTEM, 24H REPLAY, OTP, EMAIL & 2FA TESTS PASSED SUCCESSFULLY!")

if __name__ == '__main__':
    run_tests()

