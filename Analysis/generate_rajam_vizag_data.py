"""
Regional Multi-City Safety Dataset Generator
Coverage: Rajam, Visakhapatnam (Vizag), Vizianagaram, Srikakulam, and Palakonda
Eliminating the 'Zero-Crime Fallacy' in GuardianAI

Generates realistic geospatial safety incidents and environmental infrastructure data
anchored to EXACT Google Maps verified coordinates across North Coastal Andhra Pradesh.
"""

import os
import random
import numpy as np
import pandas as pd
from pathlib import Path

# Set fixed seed for reproducibility
np.random.seed(42)
random.seed(42)

# =============================================================================
# 1. RAJAM LOCALITIES (Exact Google Maps Coordinates, AP 532127)
# =============================================================================
RAJAM_LOCALITIES = [
    # GMRIT Campus: 18.46556° N, 83.65946° E (Google Maps Plus Code: 7MC5FM85+6Q)
    {"name": "GMRIT Campus & Hostels", "lat": 18.46556, "lon": 83.65946, "type": "Campus", "base_pol_dist": 1.2, "lighting_prob": 0.90, "cctv_prob": 0.85},
    # Rajam APSRTC Bus Complex: 18.45036° N, 83.65976° E (Google Maps)
    {"name": "Rajam RTC Bus Complex", "lat": 18.45036, "lon": 83.65976, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.80, "cctv_prob": 0.65},
    # Clock Tower & Main Road Junction: 18.45080° N, 83.65920° E
    {"name": "Main Road & Clock Tower Junction", "lat": 18.45080, "lon": 83.65920, "type": "Commercial", "base_pol_dist": 0.4, "lighting_prob": 0.85, "cctv_prob": 0.60},
    # Palakonda Road Junction: 18.45800° N, 83.65850° E
    {"name": "Palakonda Road Junction", "lat": 18.45800, "lon": 83.65850, "type": "Commercial", "base_pol_dist": 0.6, "lighting_prob": 0.75, "cctv_prob": 0.50},
    # Santha Bazar Market: 18.44850° N, 83.66120° E
    {"name": "Santha Bazar Market Area", "lat": 18.44850, "lon": 83.66120, "type": "Commercial", "base_pol_dist": 0.5, "lighting_prob": 0.65, "cctv_prob": 0.35},
    # Cheepurupalli Road: 18.44500° N, 83.65300° E
    {"name": "Cheepurupalli Road Stretch", "lat": 18.44500, "lon": 83.65300, "type": "Highway", "base_pol_dist": 0.8, "lighting_prob": 0.45, "cctv_prob": 0.20},
    # Bobbili Road: 18.45500° N, 83.65100° E
    {"name": "Bobbili Road By-pass", "lat": 18.45500, "lon": 83.65100, "type": "Highway", "base_pol_dist": 1.2, "lighting_prob": 0.40, "cctv_prob": 0.15},
    # Industrial zone: 18.44200° N, 83.66500° E
    {"name": "Rajam Industrial & Rice Mill Zone", "lat": 18.44200, "lon": 83.66500, "type": "Industrial", "base_pol_dist": 1.5, "lighting_prob": 0.30, "cctv_prob": 0.10},
    # Degree College Road: 18.45400° N, 83.66200° E
    {"name": "Degree College Road", "lat": 18.45400, "lon": 83.66200, "type": "Residential", "base_pol_dist": 0.8, "lighting_prob": 0.70, "cctv_prob": 0.30},
    # Kothapeta: 18.44700° N, 83.65200° E
    {"name": "Kothapeta Residential Colony", "lat": 18.44700, "lon": 83.65200, "type": "Residential", "base_pol_dist": 0.6, "lighting_prob": 0.60, "cctv_prob": 0.25},
    # Vangara Road Outskirts: 18.46800° N, 83.65700° E
    {"name": "Vangara Road Outskirts", "lat": 18.46800, "lon": 83.65700, "type": "Highway", "base_pol_dist": 1.8, "lighting_prob": 0.25, "cctv_prob": 0.05},
    # GMR Varalakshmi Care Hospital: 18.46150° N, 83.65830° E (Google Maps)
    {"name": "GMR Care Hospital Corridor", "lat": 18.46150, "lon": 83.65830, "type": "Transit", "base_pol_dist": 0.9, "lighting_prob": 0.95, "cctv_prob": 0.80},
    # Rajam Police Station: 18.44899° N, 83.65532° E (Google Maps / Mapcarta Ground Truth)
    {"name": "Rajam Police Station (Cheepurupalli Rd)", "lat": 18.44899, "lon": 83.65532, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.85},
    # Saradhi Nagar Colony: 18.45550° N, 83.66500° E
    {"name": "Saradhi Nagar Colony", "lat": 18.45550, "lon": 83.66500, "type": "Residential", "base_pol_dist": 1.1, "lighting_prob": 0.55, "cctv_prob": 0.20},
]

# =============================================================================
# 2. VISAKHAPATNAM LOCALITIES (Exact Google Maps Coordinates, AP)
# =============================================================================
VIZAG_LOCALITIES = [
    # Siripuram: 17.71980° N, 83.31630° E (Google Maps)
    {"name": "Siripuram & Dutt Island", "lat": 17.71980, "lon": 83.31630, "type": "Commercial", "base_pol_dist": 0.4, "lighting_prob": 0.95, "cctv_prob": 0.85},
    # RK Beach: 17.71423° N, 83.32363° E (Google Maps)
    {"name": "RK Beach & Marine Drive Promenade", "lat": 17.71423, "lon": 83.32363, "type": "Transit", "base_pol_dist": 0.6, "lighting_prob": 0.90, "cctv_prob": 0.80},
    # Jagadamba Junction: 17.71150° N, 83.30050° E
    {"name": "Jagadamba Junction Cinema Area", "lat": 17.71150, "lon": 83.30050, "type": "Commercial", "base_pol_dist": 0.5, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Dwaraka Bus Station / RTC Complex: 17.72372° N, 83.30722° E (Google Maps)
    {"name": "Dwaraka Nagar & Complex Bus Depot", "lat": 17.72372, "lon": 83.30722, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.75},
    # Maddilapalem: 17.73200° N, 83.32800° E
    {"name": "Maddilapalem & AU North Campus", "lat": 17.73200, "lon": 83.32800, "type": "Campus", "base_pol_dist": 0.8, "lighting_prob": 0.85, "cctv_prob": 0.65},
    # MVP Colony: 17.74140° N, 83.33870° E (Google Maps)
    {"name": "MVP Colony Sector 1-9", "lat": 17.74140, "lon": 83.33870, "type": "Residential", "base_pol_dist": 0.7, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Rushikonda IT SEZ: 17.80250° N, 83.38530° E (Google Maps)
    {"name": "Rushikonda IT SEZ & Hillside", "lat": 17.80250, "lon": 83.38530, "type": "Campus", "base_pol_dist": 1.2, "lighting_prob": 0.75, "cctv_prob": 0.65},
    # Rushikonda Beach: 17.78250° N, 83.38510° E (Google Maps)
    {"name": "Rushikonda Beach Stretches", "lat": 17.78250, "lon": 83.38510, "type": "Highway", "base_pol_dist": 2.0, "lighting_prob": 0.35, "cctv_prob": 0.15},
    # Gajuwaka Junction: 17.70040° N, 83.21680° E (Google Maps)
    {"name": "Gajuwaka Industrial Belt", "lat": 17.70040, "lon": 83.21680, "type": "Industrial", "base_pol_dist": 1.2, "lighting_prob": 0.50, "cctv_prob": 0.25},
    # Scindia: 17.68500° N, 83.28000° E
    {"name": "Scindia & Container Terminal", "lat": 17.68500, "lon": 83.28000, "type": "Industrial", "base_pol_dist": 2.0, "lighting_prob": 0.30, "cctv_prob": 0.15},
    # Visakhapatnam Junction Railway Station: 17.72210° N, 83.28970° E (Google Maps)
    {"name": "Visakhapatnam Central Railway Station", "lat": 17.72210, "lon": 83.28970, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.85, "cctv_prob": 0.80},
    # Arilova Health City: 17.76500° N, 83.33000° E
    {"name": "Health City & Arilova Valley", "lat": 17.76500, "lon": 83.33000, "type": "Transit", "base_pol_dist": 1.0, "lighting_prob": 0.80, "cctv_prob": 0.60},
    # NAD Kotha Road: 17.74000° N, 83.23500° E
    {"name": "NAD Kotha Road Flyover Junction", "lat": 17.74000, "lon": 83.23500, "type": "Transit", "base_pol_dist": 0.6, "lighting_prob": 0.80, "cctv_prob": 0.55},
    # Pendurthi: 17.80800° N, 83.20000° E
    {"name": "Pendurthi Highway Extension", "lat": 17.80800, "lon": 83.20000, "type": "Highway", "base_pol_dist": 2.0, "lighting_prob": 0.40, "cctv_prob": 0.15},
    # Bheemili: 17.89000° N, 83.45000° E
    {"name": "Bheemili Coastal Highway Corridor", "lat": 17.89000, "lon": 83.45000, "type": "Highway", "base_pol_dist": 2.5, "lighting_prob": 0.30, "cctv_prob": 0.10},
    # Simhachalam: 17.76800° N, 83.25000° E
    {"name": "Simhachalam Foothills Road", "lat": 17.76800, "lon": 83.25000, "type": "Residential", "base_pol_dist": 1.5, "lighting_prob": 0.45, "cctv_prob": 0.20},
    # Marripalem: 17.74500° N, 83.26500° E
    {"name": "Marripalem & 104 Residential Area", "lat": 17.74500, "lon": 83.26500, "type": "Residential", "base_pol_dist": 1.0, "lighting_prob": 0.70, "cctv_prob": 0.35},
    # Seethammadhara: 17.73800° N, 83.31500° E
    {"name": "Seethammadhara North Extension", "lat": 17.73800, "lon": 83.31500, "type": "Residential", "base_pol_dist": 0.8, "lighting_prob": 0.80, "cctv_prob": 0.50},
    # Gopalapatnam: 17.75500° N, 83.21500° E
    {"name": "Gopalapatnam Transit Market", "lat": 17.75500, "lon": 83.21500, "type": "Commercial", "base_pol_dist": 1.0, "lighting_prob": 0.75, "cctv_prob": 0.40},
]

# =============================================================================
# 3. VIZIANAGARAM LOCALITIES (Exact Google Maps Coordinates, AP 535001-535003)
# =============================================================================
VIZIANAGARAM_LOCALITIES = [
    # Vizianagaram Fort: 18.11080° N, 83.41060° E (Google Maps / Wikidata Ground Truth)
    {"name": "Vizianagaram Fort & Clock Tower", "lat": 18.11080, "lon": 83.41060, "type": "Commercial", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # MG Road One Town: 18.11200° N, 83.40950° E (Google Maps)
    {"name": "Vizianagaram One Town (MG Road)", "lat": 18.11200, "lon": 83.40950, "type": "Commercial", "base_pol_dist": 0.2, "lighting_prob": 0.90, "cctv_prob": 0.80},
    # RTC Complex: 18.11320° N, 83.40450° E (Google Maps)
    {"name": "Vizianagaram RTC Complex & Depot", "lat": 18.11320, "lon": 83.40450, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.85, "cctv_prob": 0.75},
    # Vizianagaram Junction: 18.11150° N, 83.39630° E (Google Maps Ground Truth)
    {"name": "Station Road & Railway Junction", "lat": 18.11150, "lon": 83.39630, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.75},
    # Maharaja GGH Hospital: 18.10670° N, 83.39560° E (Google Maps Ground Truth)
    {"name": "Cantonment & GGH Hospital Area", "lat": 18.10670, "lon": 83.39560, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.90, "cctv_prob": 0.75},
    # Mayuri Junction: 18.11620° N, 83.40880° E (Google Maps)
    {"name": "Mayuri Junction Commercial Center", "lat": 18.11620, "lon": 83.40880, "type": "Commercial", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.65},
    # Ring Road & Phool Bagh: 18.12200° N, 83.40600° E
    {"name": "Ring Road & Phool Bagh", "lat": 18.12200, "lon": 83.40600, "type": "Residential", "base_pol_dist": 0.8, "lighting_prob": 0.75, "cctv_prob": 0.50},
    # Baba Metta: 18.10900° N, 83.40100° E
    {"name": "Baba Metta Commercial Corridor", "lat": 18.10900, "lon": 83.40100, "type": "Commercial", "base_pol_dist": 0.5, "lighting_prob": 0.70, "cctv_prob": 0.40},
    # JNTU-GV Dwarapudi: 18.11800° N, 83.41500° E (Google Maps Ground Truth)
    {"name": "Pradeep Nagar & JNTU-GV Campus", "lat": 18.11800, "lon": 83.41500, "type": "Campus", "base_pol_dist": 1.0, "lighting_prob": 0.80, "cctv_prob": 0.65},
    # Gajularega: 18.12800° N, 83.41800° E
    {"name": "Gajularega Residential & Industrial Zone", "lat": 18.12800, "lon": 83.41800, "type": "Industrial", "base_pol_dist": 1.2, "lighting_prob": 0.55, "cctv_prob": 0.30},
]

# =============================================================================
# 4. SRIKAKULAM LOCALITIES (Exact Google Maps Coordinates, AP 532001)
# =============================================================================
SRIKAKULAM_LOCALITIES = [
    # Seven Road Junction: 18.29890° N, 83.89750° E (Google Maps)
    {"name": "Seven Road Junction Commercial Area", "lat": 18.29890, "lon": 83.89750, "type": "Commercial", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.85},
    # Srikakulam Town One Town PS: 18.29850° N, 83.89700° E
    {"name": "Srikakulam Town One Town Police Station Area", "lat": 18.29850, "lon": 83.89700, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.85},
    # Srikakulam Old Bus Stand / RTC: 18.29215° N, 83.89650° E (Google Maps Ground Truth)
    {"name": "Srikakulam RTC Complex & Depot", "lat": 18.29215, "lon": 83.89650, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.75},
    # Day & Night Junction: 18.30100° N, 83.89700° E
    {"name": "Day & Night Hospital Junction", "lat": 18.30100, "lon": 83.89700, "type": "Commercial", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Palakonda Road Junction: 18.30350° N, 83.89850° E
    {"name": "Palakonda Road Junction (Srikakulam)", "lat": 18.30350, "lon": 83.89850, "type": "Commercial", "base_pol_dist": 0.4, "lighting_prob": 0.80, "cctv_prob": 0.60},
    # RIMS Hospital Balaga: 18.31430° N, 83.88860° E (Google Maps / Wikipedia Ground Truth)
    {"name": "Balaga & RIMS Government Hospital", "lat": 18.31430, "lon": 83.88860, "type": "Campus", "base_pol_dist": 0.5, "lighting_prob": 0.90, "cctv_prob": 0.80},
    # Nagavali River Bridge Road: 18.30280° N, 83.90060° E
    {"name": "Nagavali River Bridge Road", "lat": 18.30280, "lon": 83.90060, "type": "Highway", "base_pol_dist": 0.6, "lighting_prob": 0.75, "cctv_prob": 0.45},
    # Arasavalli Sun Temple: 18.28790° N, 83.91360° E (Google Maps Ground Truth)
    {"name": "Arasavalli Sun Temple Corridor", "lat": 18.28790, "lon": 83.91360, "type": "Residential", "base_pol_dist": 0.9, "lighting_prob": 0.80, "cctv_prob": 0.55},
    # Illispuram: 18.29950° N, 83.89600° E
    {"name": "Illispuram & District Court Complex", "lat": 18.29950, "lon": 83.89600, "type": "Transit", "base_pol_dist": 0.2, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Gujarathipeta: 18.29200° N, 83.88500° E
    {"name": "Gujarathipeta Residential Area", "lat": 18.29200, "lon": 83.88500, "type": "Residential", "base_pol_dist": 0.8, "lighting_prob": 0.65, "cctv_prob": 0.30},
]

# =============================================================================
# 5. PALAKONDA LOCALITIES (Exact Google Maps Coordinates, AP 535440)
# =============================================================================
PALAKONDA_LOCALITIES = [
    # Palakonda Main Road Market: 18.60060° N, 83.75750° E (Google Maps Ground Truth)
    {"name": "Palakonda Main Road Commercial Market", "lat": 18.60060, "lon": 83.75750, "type": "Commercial", "base_pol_dist": 0.2, "lighting_prob": 0.80, "cctv_prob": 0.50},
    # Palakonda Police Station: 18.60060° N, 83.75750° E (Google Maps Ground Truth)
    {"name": "Palakonda Police Station (Main Road)", "lat": 18.60060, "lon": 83.75750, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.85, "cctv_prob": 0.75},
    # Palakonda APSRTC Bus Station: 18.59891° N, 83.76035° E (Google Maps Ground Truth)
    {"name": "Palakonda RTC Bus Stand Junction", "lat": 18.59891, "lon": 83.76035, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.80, "cctv_prob": 0.60},
    # Area Hospital Palakonda: 18.60020° N, 83.75680° E (Google Maps Ground Truth)
    {"name": "Area Hospital Palakonda Zone", "lat": 18.60020, "lon": 83.75680, "type": "Transit", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.65},
    # Sub-Collector Office: 18.60200° N, 83.75850° E
    {"name": "Sub-Collector Office Road", "lat": 18.60200, "lon": 83.75850, "type": "Residential", "base_pol_dist": 0.4, "lighting_prob": 0.70, "cctv_prob": 0.40},
    # Kota Durga Temple: 18.59900° N, 83.75400° E
    {"name": "Kota Durga Temple & Approach Road", "lat": 18.59900, "lon": 83.75400, "type": "Residential", "base_pol_dist": 0.5, "lighting_prob": 0.65, "cctv_prob": 0.35},
    # Rajam-Palakonda Road Entry: 18.59400° N, 83.75000° E
    {"name": "Rajam-Palakonda Road Entry Stretch", "lat": 18.59400, "lon": 83.75000, "type": "Highway", "base_pol_dist": 0.8, "lighting_prob": 0.45, "cctv_prob": 0.20},
    # Seethampeta Agency Road: 18.60500° N, 83.76200° E
    {"name": "Seethampeta Agency Junction Road", "lat": 18.60500, "lon": 83.76200, "type": "Highway", "base_pol_dist": 0.7, "lighting_prob": 0.50, "cctv_prob": 0.25},
]


# =============================================================================
# 6. CHEEPURUPALLI LOCALITIES (Google Maps Ground Truth, AP 535128)
# =============================================================================
CHEEPURUPALLI_LOCALITIES = [
    # Cheepurupalli Railway Station: 18.31200 N, 83.57800 E
    {"name": "Cheepurupalli Railway Station Outskirts", "lat": 18.31200, "lon": 83.57800, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.65, "cctv_prob": 0.50},
    # Cheepurupalli RTC Bus Stand: 18.31550 N, 83.58250 E
    {"name": "Cheepurupalli RTC Complex & Bazaar", "lat": 18.31550, "lon": 83.58250, "type": "Commercial", "base_pol_dist": 0.2, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Cheepurupalli Police Station: 18.31420 N, 83.58050 E
    {"name": "Cheepurupalli Police Station Area", "lat": 18.31420, "lon": 83.58050, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.80},
    # Cheepurupalli - Rajam Road Stretch (User Landmark): 18.36500 N, 83.64360 E
    {"name": "Cheepurupalli-Rajam Road Highway Stretch", "lat": 18.36500, "lon": 83.64360, "type": "Highway", "base_pol_dist": 4.5, "lighting_prob": 0.30, "cctv_prob": 0.10},
    # Railway Goods Yard Isolated Stretch: 18.30800 N, 83.57500 E
    {"name": "Cheepurupalli Goods Yard Isolated Zone", "lat": 18.30800, "lon": 83.57500, "type": "Industrial", "base_pol_dist": 1.2, "lighting_prob": 0.25, "cctv_prob": 0.15},
    # Garividi Industrial Corridor: 18.28500 N, 83.53500 E
    {"name": "Garividi Shreeramnagar Industrial Corridor", "lat": 18.28500, "lon": 83.53500, "type": "Industrial", "base_pol_dist": 2.2, "lighting_prob": 0.45, "cctv_prob": 0.30},
    # Cheepurupalli Govt Degree College: 18.32100 N, 83.58900 E
    {"name": "Cheepurupalli Degree College Road", "lat": 18.32100, "lon": 83.58900, "type": "Campus", "base_pol_dist": 1.1, "lighting_prob": 0.60, "cctv_prob": 0.40},
]

# =============================================================================
# 7. HIGHWAY CORRIDOR LOCALITIES (SH-38 / SH-22 Rajam-Cheepurupalli-Srikakulam)
# =============================================================================
HIGHWAY_CORRIDOR_LOCALITIES = [
    # Rajam South Godown & Rice Mill Bypass: 18.43500 N, 83.65800 E
    {"name": "Rajam South Highway Bypass", "lat": 18.43500, "lon": 83.65800, "type": "Highway", "base_pol_dist": 1.8, "lighting_prob": 0.50, "cctv_prob": 0.25},
    # Kancharam Highway Junction: 18.41200 N, 83.65200 E
    {"name": "Kancharam Highway Junction", "lat": 18.41200, "lon": 83.65200, "type": "Highway", "base_pol_dist": 3.8, "lighting_prob": 0.35, "cctv_prob": 0.10},
    # Pogiri - Paturu Rural Highway Stretch: 18.39500 N, 83.64800 E
    {"name": "Pogiri-Paturu Rural Highway Stretch", "lat": 18.39500, "lon": 83.64800, "type": "Highway", "base_pol_dist": 4.8, "lighting_prob": 0.25, "cctv_prob": 0.05},
    # Ponugativari - Cheepurupalli Highway Bypass: 18.38500 N, 83.64700 E
    {"name": "Ponugativari-Cheepurupalli Highway Bypass", "lat": 18.38500, "lon": 83.64700, "type": "Highway", "base_pol_dist": 4.2, "lighting_prob": 0.30, "cctv_prob": 0.10},
    # Laveru Highway Link: 18.33500 N, 83.68200 E
    {"name": "Laveru-Cheepurupalli Highway Link", "lat": 18.33500, "lon": 83.68200, "type": "Highway", "base_pol_dist": 5.2, "lighting_prob": 0.25, "cctv_prob": 0.05},
    # Ranasthalam NH-16 Junction: 18.23500 N, 83.68500 E
    {"name": "Ranasthalam NH-16 Highway Junction", "lat": 18.23500, "lon": 83.68500, "type": "Highway", "base_pol_dist": 1.5, "lighting_prob": 0.70, "cctv_prob": 0.50},
    # Chilakapalem NH-16 Toll Plaza Outskirts: 18.25500 N, 83.79200 E
    {"name": "Chilakapalem NH-16 Srikakulam Toll Plaza", "lat": 18.25500, "lon": 83.79200, "type": "Highway", "base_pol_dist": 2.1, "lighting_prob": 0.75, "cctv_prob": 0.60},
]

# =============================================================================
# 8. BOBBILI LOCALITIES (Google Maps Ground Truth, AP 535558)
# =============================================================================
BOBBILI_LOCALITIES = [
    # Bobbili Fort & Market Commercial Area: 18.56700 N, 83.36400 E
    {"name": "Bobbili Fort & Market Commercial Area", "lat": 18.56700, "lon": 83.36400, "type": "Commercial", "base_pol_dist": 0.3, "lighting_prob": 0.85, "cctv_prob": 0.70},
    # Bobbili RTC Complex & Railway Station: 18.57200 N, 83.36800 E
    {"name": "Bobbili RTC Complex & Railway Station", "lat": 18.57200, "lon": 83.36800, "type": "Transit", "base_pol_dist": 0.4, "lighting_prob": 0.80, "cctv_prob": 0.65},
    # Bobbili Growth Centre Industrial Zone: 18.58500 N, 83.38200 E
    {"name": "Bobbili Growth Centre Industrial Area", "lat": 18.58500, "lon": 83.38200, "type": "Industrial", "base_pol_dist": 2.5, "lighting_prob": 0.40, "cctv_prob": 0.25},
    # Bobbili Town Police Station: 18.56900 N, 83.36200 E
    {"name": "Bobbili Town Police Station Area", "lat": 18.56900, "lon": 83.36200, "type": "Transit", "base_pol_dist": 0.1, "lighting_prob": 0.90, "cctv_prob": 0.80},
    # Bobbili - Rajam Road Highway Outskirts: 18.51500 N, 83.52000 E
    {"name": "Rajam-Bobbili Road Highway Outskirts", "lat": 18.51500, "lon": 83.52000, "type": "Highway", "base_pol_dist": 4.1, "lighting_prob": 0.30, "cctv_prob": 0.10},
]

TIMES_OF_DAY = ["Morning", "Afternoon", "Evening", "Night", "Late Night"]
TIME_PROBS = [0.22, 0.20, 0.23, 0.20, 0.15]

CRIME_TYPES = [
    "None", "Stalking", "Harassment", "Chain Snatching",
    "Night Safety Complaint", "Verbal Abuse", "Assault",
    "Unsafe Transport", "Eve Teasing", "Theft"
]

def generate_dataset(num_samples_per_locality=45):
    rows = []
    all_localities = (
        [(loc, "Rajam") for loc in RAJAM_LOCALITIES] +
        [(loc, "Visakhapatnam") for loc in VIZAG_LOCALITIES] +
        [(loc, "Vizianagaram") for loc in VIZIANAGARAM_LOCALITIES] +
        [(loc, "Srikakulam") for loc in SRIKAKULAM_LOCALITIES] +
        [(loc, "Palakonda") for loc in PALAKONDA_LOCALITIES] +
        [(loc, "Cheepurupalli") for loc in CHEEPURUPALLI_LOCALITIES] +
        [(loc, "Highway Corridor") for loc in HIGHWAY_CORRIDOR_LOCALITIES] +
        [(loc, "Bobbili") for loc in BOBBILI_LOCALITIES]
    )

    for loc, city in all_localities:
        for _ in range(num_samples_per_locality):
            # Ultra-tight realistic urban jitter (~20m to 50m) to preserve exact landmark positions on Google Maps
            jitter_lat = np.random.normal(0, 0.0003)
            jitter_lon = np.random.normal(0, 0.0003)
            lat = round(loc["lat"] + jitter_lat, 6)
            lon = round(loc["lon"] + jitter_lon, 6)

            time_of_day = np.random.choice(TIMES_OF_DAY, p=TIME_PROBS)
            is_dark_time = time_of_day in ["Night", "Late Night"]
            is_late_night = (time_of_day == "Late Night")

            # Street lighting: dependent on locality infrastructure & time
            lighting_chance = loc["lighting_prob"]
            if is_late_night:
                lighting_chance *= 0.75  # Some lights turned off or broken late at night
            street_lighting = 1 if np.random.rand() < lighting_chance else 0

            # CCTV presence
            cctv_chance = loc["cctv_prob"]
            cctv_nearby = 1 if np.random.rand() < cctv_chance else 0

            # Police station distance (tight variance)
            pol_dist = round(max(0.1, np.random.normal(loc["base_pol_dist"], 0.10)), 2)

            # Crowd density
            if is_late_night:
                crowd_density = int(np.random.uniform(5, 80))
            elif is_dark_time:
                crowd_density = int(np.random.uniform(50, 400))
            else:
                crowd_density = int(np.random.uniform(150, 950))

            # Historical Crime Count:
            # Crucial: 40% of records intentionally have ZERO historical crimes logged,
            # eliminating the Zero-Crime Fallacy!
            has_zero_crime = (np.random.rand() < 0.40)
            if has_zero_crime:
                crime_count = 0
                crime_type = "None"
            else:
                crime_count = int(np.random.exponential(scale=6) + 1)
                crime_count = min(crime_count, 50)
                crime_type = np.random.choice(CRIME_TYPES[1:])

            # Safety Score Calculation (0 to 100)
            score = 70.0

            # Lighting factor (+18 for well-lit, -25 if dark during night/late night)
            if street_lighting == 1:
                score += 18.0
            else:
                if is_late_night:
                    score -= 32.0
                elif is_dark_time:
                    score -= 22.0
                else:
                    score -= 8.0

            # CCTV factor (+12 if monitored, -5 if absent in commercial/transit)
            if cctv_nearby == 1:
                score += 12.0
            else:
                if loc["type"] in ["Commercial", "Transit"]:
                    score -= 6.0

            # Police distance factor (-5.5 points per km away beyond 0.5km)
            if pol_dist <= 0.5:
                score += 8.0
            elif pol_dist <= 1.2:
                score += 3.0
            else:
                score -= (pol_dist - 1.2) * 5.2

            # Time of day hazard
            if is_late_night:
                score -= 15.0
            elif is_dark_time:
                score -= 6.0
            elif time_of_day == "Morning":
                score += 5.0

            # Area type context
            if loc["type"] in ["Industrial", "Highway"]:
                score -= 8.0
            elif loc["type"] in ["Campus", "Residential"]:
                score += 4.0

            # Historical crime count impact
            score -= (crime_count * 0.75)

            # Clamp score between 5.0 and 98.5
            final_score = round(float(np.clip(score, 5.0, 98.5)), 1)

            # Deterministic Risk Level Stratification
            if final_score >= 75.0:
                safety_level = "Safe"
            elif final_score >= 50.0:
                safety_level = "Moderate"
            else:
                safety_level = "Unsafe"

            rows.append({
                "latitude": lat,
                "longitude": lon,
                "time_of_day": time_of_day,
                "area_type": loc["type"],
                "street_lighting": street_lighting,
                "cctv_nearby": cctv_nearby,
                "police_distance_km": pol_dist,
                "safety_level": safety_level,
                "city": city,
                "area": loc["name"],
                "crowd_density": crowd_density,
                "historical_crime_count": crime_count,
                "crime_type": crime_type,
                "safety_score": final_score
            })

    df = pd.DataFrame(rows)
    # Shuffle dataset
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    return df

if __name__ == "__main__":
    df = generate_dataset(num_samples_per_locality=45)
    print(f"Total records generated: {len(df)}")
    print("\nCity Breakdown:")
    print(df["city"].value_counts())
    print("\nSafety Level Breakdown:")
    print(df["safety_level"].value_counts())
    print("\nZero Crime Records Breakdown:")
    zero_crime_df = df[df["historical_crime_count"] == 0]
    print(f"Count of 0-crime records: {len(zero_crime_df)} ({len(zero_crime_df)/len(df)*100:.1f}%)")
    print("Safety levels for 0-crime records:")
    print(zero_crime_df["safety_level"].value_counts())

    # Save to local Analysis folder
    out_analysis = Path("Analysis/rajam_visakhapatnam_safety_dataset.csv")
    df.to_csv(out_analysis, index=False)
    print(f"\n[OK] Saved dataset to {out_analysis}")

    # Save to Backend/guardian_api
    out_backend1 = Path("Backend/guardian_api/rajam_visakhapatnam_safety_dataset.csv")
    out_backend2 = Path("Backend/guardian_api/new_crime_dataset.csv")
    df.to_csv(out_backend1, index=False)
    df.to_csv(out_backend2, index=False)
    print(f"[OK] Saved dataset to {out_backend1} and {out_backend2}")

    # Save to H:\Github\GuardianAI_Backend if drive exists
    dest_h = Path(r"H:\Github\GuardianAI_Backend")
    if dest_h.exists():
        h_csv1 = dest_h / "rajam_visakhapatnam_safety_dataset.csv"
        h_csv2 = dest_h / "guardian_api" / "new_crime_dataset.csv"
        df.to_csv(h_csv1, index=False)
        df.to_csv(h_csv2, index=False)
        print(f"[OK] Saved dataset to {h_csv1} and {h_csv2}")
